/*
 * Copyright (c) 2021.
 */

package com.kmgi.unicornhunters.components.dashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.kmgi.unicornhunters.R
import com.kmgi.unicornhunters.ui.common.images.fileResource
import com.kmgi.unicornhunters.ui.common.images.glide.GlideImage
import com.kmgi.unicornhunters.ui.common.images.localResource
import com.kmgi.unicornhunters.ui.common.product.ProductCategory
import com.kmgi.unicornhunters.ui.common.utils.noRippleClickable
import com.kmgi.unicornhunters.ui.theme.Colors
import com.kmgi.unicornhunters.ui.theme.Fonts
import com.kmgi.unicornhunters.ui.theme.Shapes
import com.kmgi.unicorns.core.models.Product
import com.kmgi.unicorns.core.models.randomSampleProduct

@Composable
fun FutureUnicorns(products: List<Product>, onProductSelected: (Product) -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Colors.White,
                shape = Shapes.medium,
            )
            .padding(vertical = 20.dp, horizontal = 17.3.dp)
    ) {
        Text(
            text = "future unicorns".toUpperCase(Locale.current),
            fontFamily = Fonts.Montserrat,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Normal,
            fontSize = 14.sp,
            color = Colors.Text.Dark,
            textAlign = TextAlign.Start,
        )

        Spacer(Modifier.height(12.dp))

        val count = minOf(products.size, 5)
        products
            .take(count)
            .mapIndexed { index, product ->
                Unicorn(
                    product = product,
                    modifier = Modifier.padding(
                        top = 8.dp.takeIf { index != 0 } ?: 4.dp,
                        bottom = 8.dp.takeIf { index != count - 1 } ?: 4.dp,
                    ),
                    onClick = onProductSelected,
                )
            }
    }
}

@Preview
@Composable
fun FutureUnicornsPreview() = FutureUnicorns((0..5).map { randomSampleProduct(false) })

@Composable
fun Unicorn(
    product: Product,
    modifier: Modifier = Modifier,
    onClick: (Product) -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .noRippleClickable { onClick(product) }
    ) {
        Box(Modifier.padding(top = 4.dp)) {
            GlideImage(
                resource = product.logo?.formattedUrl()?.let(::fileResource),
                alt = localResource(R.drawable.shape_image_logo_default),
                modifier = Modifier
                    .width(26.dp)
                    .height(26.dp),
                onLoading = {
                    CircularProgressIndicator(
                        color = Color.Black,
                        modifier = Modifier
                            .width(26.dp)
                            .height(26.dp)
                            .padding(4.dp),
                    )
                },
                requestBuilder = {
                    transform(CircleCrop())
                }
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = product.company,
                fontFamily = Fonts.NunitoSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.7.sp,
                color = Colors.Text.Brown,
            )

            Spacer(Modifier.height(5.7.dp))

            ProductCategory(category = product.category)
        }
    }
}

@Preview
@Composable
fun UnicornPreview() = Unicorn(randomSampleProduct(false))