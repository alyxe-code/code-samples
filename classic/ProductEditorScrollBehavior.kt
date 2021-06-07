package com.kmgi.unicorns.product.editor.ui

import android.view.View
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.kmgi.unicorns.common.uikit.utils.ViewUtility.padding
import com.kmgi.unicorns.product.R
import com.kmgi.unicorns.product.databinding.FragmentProductEditorBinding
import kotlin.math.roundToInt

class ProductEditorScrollBehavior private constructor(
    private val binding: FragmentProductEditorBinding,
    private val repository: Repository,
) {
    private fun update() {
        if (!repository.canScrollTop()) {
            return reset()
        }

        val factor = computeScrollFactor(repository.verticalOffset()).inverted()

        updateOffsetTop(factor)
        updateCardColumnOffsets(factor)
        updateToolbarVisibility(factor)
        updateProgress(factor)
    }

    private fun reset() {
        updateOffsetTop(null)
        updateCardColumnOffsets(null)
        updateToolbarVisibility(null)
        updateProgress(null)
    }

    private fun computeScrollFactor(offsetTop: Int): Float {
        return if (offsetTop == 0)
            0f
        else
            offsetTop.toFloat() / initialCardOffsetTop
    }

    private val initialCardOffsetTop = binding
        .root
        .context
        .resources
        .getDimensionPixelOffset(R.dimen.toolbar_wave_height_inset_top_medium)
    private val finalCardOffsetTop = binding
        .root
        .context
        .resources
        .getDimensionPixelOffset(R.dimen.offset_24)

    private fun updateOffsetTop(factor: Float? = null) {
        val value = if (factor == null) {
            initialCardOffsetTop
        } else {
            val fixedFactor = factor.fixed()
            ((initialCardOffsetTop - finalCardOffsetTop) * fixedFactor).toInt() + finalCardOffsetTop
        }

        binding.productCardContainer.padding(top = value)
    }

    private val imageMaxSize = binding.root.context.resources.getDimensionPixelSize(R.dimen.product_editor_logo_max_size)
    private val imageSizeDiff = imageMaxSize - binding.root.context.resources.getDimensionPixelSize(R.dimen.product_editor_logo_min_size)
    private val cardColumnItemsOffsetTop = binding.root.context.resources.getDimensionPixelOffset(R.dimen.offset_8)

    private fun updateCardColumnOffsets(factor: Float?) {
        if (factor == null) {
            binding.productCard.moneyProgress.alpha = 1f
            binding.productCard.moneyProgress.visibility = View.VISIBLE

            binding.productCard.moneyTextContainer.alpha = 1f
            binding.productCard.moneyTextContainer.visibility = View.VISIBLE

            binding.productCard.moneyProgress.updateLayoutParams<LinearLayout.LayoutParams> { topMargin = cardColumnItemsOffsetTop }
            binding.productCard.moneyTextContainer.updateLayoutParams<LinearLayout.LayoutParams> { topMargin = cardColumnItemsOffsetTop }
            return
        }

        val fixedFactor = factor.fixed()

        val newSize = imageMaxSize - (imageSizeDiff * (fixedFactor.inverted())).toInt()
        binding.productCard.logo.updateLayoutParams<ConstraintLayout.LayoutParams> {
            width = newSize
            height = newSize
        }

        val segmentedFactor = ((fixedFactor - 0.5) * 5).takeIf { it <= 1.0 } ?: 1.0

        val alphaFactor = segmentedFactor.toFloat()
        binding.productCard.moneyProgress.alpha = alphaFactor
        binding.productCard.moneyTextContainer.alpha = alphaFactor

        val top = (cardColumnItemsOffsetTop * segmentedFactor).toInt()
        binding.productCard.moneyProgress.updateLayoutParams<LinearLayout.LayoutParams> { topMargin = top }
        binding.productCard.moneyTextContainer.updateLayoutParams<LinearLayout.LayoutParams> { topMargin = top }

        binding.productCard.edit.alpha = ((fixedFactor - (1.0 - CARD_EDIT_HIDE_FACTOR)) / CARD_EDIT_HIDE_FACTOR).toFloat()
    }

    private fun updateToolbarVisibility(factor: Float?) {
        val fixedFactor = factor.fixed()

        if (factor == null) {
            binding.toolbar?.modifyConfig {
                opacity = 1f
                active = true
            }
            binding.skip.alpha = 1f
        } else {
            binding.toolbar?.modifyConfig {
                opacity = fixedFactor
                active = fixedFactor >= 0.8
            }
            binding.skip.alpha = fixedFactor
        }
    }

    private val maxProgressHeight = binding.root.context.resources.getDimensionPixelOffset(R.dimen.offset_32)
    private val progressHeightDiff = maxProgressHeight - binding.root.context.resources.getDimensionPixelOffset(R.dimen.offset_8)

    private fun updateProgress(factor: Float?) {
        if (factor == null) {
            binding.productCard.progress.updateLayoutParams<ConstraintLayout.LayoutParams> { height = maxProgressHeight }
            binding.productCard.progressValue.alpha = 1f
        } else {
            val fixedFactor = factor.fixed()

            binding.productCard.progress.updateLayoutParams<ConstraintLayout.LayoutParams> {
                height = (maxProgressHeight - (progressHeightDiff * (1f - fixedFactor))).roundToInt()
            }

            binding.productCard.progressValue.alpha = fixedFactor
        }
    }

    interface Repository {
        fun canScrollTop(): Boolean
        fun verticalOffset(): Int
    }

    companion object {
        private const val CARD_EDIT_HIDE_FACTOR = 0.6f

        private fun Float?.fixed(): Float = when {
            this ?: 0f <= 0.0 -> 0.0f
            this ?: 0f >= 1.0 -> 1.0f
            else -> this ?: 0f
        }

        private fun Float.inverted(): Float = (1f - this)

        fun withRecyclerView(
            binding: FragmentProductEditorBinding,
            recyclerView: RecyclerView,
        ): ProductEditorScrollBehavior {
            var offsetTop = 0

            val behavior = ProductEditorScrollBehavior(
                binding = binding,
                repository = object : Repository {
                    override fun canScrollTop() = recyclerView.canScrollVertically(-1)
                    override fun verticalOffset() = offsetTop
                },
            )

            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    offsetTop += dy
                    behavior.update()
                }
            })

            return behavior
        }

        fun ProductEditorScrollBehavior.withLifecycle(
            lifecycle: Lifecycle,
        ) = lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_CREATE -> Unit
                    Lifecycle.Event.ON_START -> Unit
                    Lifecycle.Event.ON_RESUME -> {
                        binding.recycler.requestLayout()
                    }
                    Lifecycle.Event.ON_PAUSE -> Unit
                    Lifecycle.Event.ON_STOP -> Unit
                    Lifecycle.Event.ON_DESTROY -> {
                        reset()
                        lifecycle.removeObserver(this)
                    }
                    Lifecycle.Event.ON_ANY -> update()
                }
            }
        })
    }
}