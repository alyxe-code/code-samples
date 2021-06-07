package com.kmgi.unicorns.product.details

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.kmgi.unicorns.common.uikit.SnackBarNotifyUtility.Companion.notifyUtility
import com.kmgi.unicorns.common.uikit.setVisibilityInOut
import com.kmgi.unicorns.common.uikit.view.toolbar.CoroutineAppToolbar
import com.kmgi.unicorns.core.android.ext.navigation.activityNavigationRouter
import com.kmgi.unicorns.core.android.ext.navigation.routerArgumentsOrNull
import com.kmgi.unicorns.core.appViewModels
import com.kmgi.unicorns.core.models.FileInfo
import com.kmgi.unicorns.core.models.Product
import com.kmgi.unicorns.core.navigation.MainNavigationRoute
import com.kmgi.unicorns.core.navigation.MainNavigationRoute.ProductEditor.StartType
import com.kmgi.unicorns.product.R
import com.kmgi.unicorns.product.databinding.FragmentProductBinding
import com.kmgi.unicorns.product.details.cells.*
import com.kmgi.unicorns.recycler.staticRecyclerAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ProductFragment : Fragment(), ProductViewModel.Navigation {

    private val viewModel by appViewModels {
        val id = routerArgumentsOrNull<MainNavigationRoute.ProductDetails>()?.id
            ?: arguments?.getInt("id", -1)
            ?: throw IllegalArgumentException()

        ProductViewModel(
            productId = id,
            repository = ProductRepositoryImpl(
                dao = persistence.dao(),
                productService = networkAdapter.product(),
                accountStorage = accountStorage,
            )
        )
    }

    private val router by activityNavigationRouter<MainNavigationRoute>()

    private lateinit var binding: FragmentProductBinding

    private val listAdapter by staticRecyclerAdapter(with = {
        stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
    })

    private var viewAnimator: ProductViewAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.recycler.adapter = listAdapter

        val toolbar = CoroutineAppToolbar.build {
            title = getString(R.string.back_to_list_2l)
        }
        binding.toolbar.toolbar = toolbar

        viewAnimator = ProductViewAnimator(
            binding = binding,
            toolbarConfig = toolbar.config,
            repository = object : ProductViewAnimator.Repository {
                override var savedFactor: Float
                    get() = viewModel.scrollFactor
                    set(value) {
                        viewModel.scrollFactor = value
                    }
            }
        )

        viewModel.state.observe(viewLifecycleOwner, this::onStateChanged)

        viewLifecycleOwner.lifecycleScope.launch { viewModel.navigation.observe(this@ProductFragment) }
    }

    override fun onResume() {
        super.onResume()
        viewAnimator?.refresh()
    }

    override fun onStop() {
        super.onStop()
        binding.initialLoader.stopImmediate()
    }

    private fun onStateChanged(state: ProductViewModel.State?) {
        state ?: return

        when (state) {
            ProductViewModel.State.Loading -> {
                setVisibilityInOut(binding.initialLoader, true)
                binding.initialLoader.play(viewLifecycleOwner.lifecycleScope)
            }

            is ProductViewModel.State.Ready -> {
                val cells = constructCells(
                    details = state.details,
                    showAdvantage = state.showAdvantage,
                    wishlist = state.wishlist,
                )

                listAdapter.updateList(cells)

                stopLoader()
            }

            else -> Unit
        }
    }

    private fun stopLoader() {
        if (binding.initialLoader.visibility == View.GONE) {
            showContent()
        } else {
            binding.initialLoader.stop(viewLifecycleOwner.lifecycleScope) {
                setVisibilityInOut(binding.initialLoader as View, visible = false)
                delay(100)
                showContent()
            }
        }
    }

    private fun showContent() {
        setVisibilityInOut(binding.headerContainer, visible = true)
        setVisibilityInOut(binding.nestedScroll, visible = true)
        setVisibilityInOut(binding.headerShadow, visible = true)
    }

    private fun constructCells(
        details: ProductDetails,
        wishlist: Boolean,
        showAdvantage: Boolean,
    ) = listOfNotNull(
        ProductActionsCell(details, wishlist, viewModel),

        ProductSummaryCell(
            details = details,
            showAdvantage = showAdvantage,
            object : ProductSummaryCell.ViewModel.Repository {
                override fun onWatchBroadcast() = viewModel.watchVideoFromShow()
                override fun onDisplayFullSummary() = viewModel.onToggleAdvantageVisibility()
            }
        ),

        run {
            val presentation = details
                .product
                .documentPresentation
                ?.firstOrNull { it.url != null }
                ?: return@run null

            ProductProductPresentationCell(
                presentation = presentation,
                onViewPresentation = viewModel::onViewPresentation
            )
        },

        details.product
            .documentText
            ?.takeIf { it.isNotEmpty() }
            ?.let { ProductDocumentsCell(it, viewModel::onViewDocument) },

        details.investmentHistory
            ?.takeIf { it.isNotEmpty() }
            ?.let(::ProductInvestmentProgressCell),

        run {
            val views = details.views ?: return@run null
            val points = statsToChartPoints(views).takeIf { it.isNotEmpty() } ?: return@run null
            ProductChartCell(
                stats = views,
                points = points,
                type = ProductChartCell.Type.Views
            )
        },

        run {
            val transactions = details.transactions ?: return@run null
            val points = statsToChartPoints(transactions).takeIf { it.isNotEmpty() } ?: return@run null
            ProductChartCell(
                stats = transactions,
                points = points,
                type = ProductChartCell.Type.Transactions
            )
        },
    )

    private fun statsToChartPoints(stats: ProductDetails.Statistics): List<Pair<Float, Float>> {
        val max = stats.picks
            .maxOrNull()
            ?.toFloat()
            ?: return emptyList()

        return stats.picks.mapIndexed { index, pick ->
            val x = index.toFloat() / (stats.picks.size - 1)
            val y = pick / max

            x to y
        }
    }


    override fun navigateChat() = notifyUtility().show("navigate chat")

    override fun navigateInvestments() = notifyUtility().show("navigate investments")

    override fun recordVideoQuestion() = notifyUtility().show("record video question")

    override fun viewDocument(file: FileInfo) {
        val url = file.formattedUrl() ?: return
        navigateLink(url)
    }

    override fun viewPresentation(file: FileInfo) {
        val url = file.formattedUrl() ?: return
        navigateLink(url)
    }

    override fun navigateLink(link: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, link.toUri()))
        } catch (t: Throwable) {
            t.printStackTrace()
            notifyUtility().show(getString(R.string.something_gone_wrong))
        }
    }

    override fun navigateEditor(product: Product) = router.navigate { MainNavigationRoute.ProductEditor(product, StartType.Edit) }

    override fun displayComingSoon() = notifyUtility().show(getString(R.string.coming_soon))

    override fun cancel() = router.popBackStack()
}
