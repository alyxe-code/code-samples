/*
 * Copyright (c) 2021.
 */

package com.kmgi.unicorns.core.network.ktor.ktor

import com.kmgi.unicorns.core.models.Product
import com.kmgi.unicorns.core.network.ProductService
import com.kmgi.unicorns.core.network.ProductService.ProductListParameters.Sorting
import com.kmgi.unicorns.core.network.ProductService.ProductUpdateModel
import com.kmgi.unicorns.core.network.UploadFileInfo
import com.kmgi.unicorns.core.network.ktor.KtorApiModule
import com.kmgi.unicorns.core.network.ktor.KtorApiModule.Configuration
import com.kmgi.unicorns.core.network.ktor.Service
import com.kmgi.unicorns.core.network.ktor.mappers.ProductMapper
import com.kmgi.unicorns.core.network.ktor.models.IndustryNetworkModel
import com.kmgi.unicorns.core.network.ktor.models.ProductNetworkModel
import com.kmgi.unicorns.core.network.ktor.models.ProductUpdateNetworkModel
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ProductServiceImplPreview(
    configuration: Configuration,
    module: KtorApiModule,
) : Service(configuration, module),
    ProductService {

    override suspend fun findById(id: Int): Product = module.proceed {
        val result = httpClient.get<ProductNetworkModel.Default> {
            url("$hostname/something/$id")
            withUserAgent()
            withAuthentication()
        }

        ProductMapper.toCommon(result)
    }

    override suspend fun paging(
        offset: Int,
        count: Int,
        params: ProductService.ProductListParameters?,
    ): List<Product> = module.proceed {
        val result = httpClient.get<List<ProductNetworkModel.Default>> {
            val urlParams = listOfNotNull(
                "_start=$offset",
                "_limit=$count",
                when (params?.sorting ?: Sorting.Newest) {
                    Sorting.Newest -> "_sort=created_at:desc"
                    Sorting.Oldest -> "_sort=created_at:asc"
                },
                params
                    ?.categories
                    ?.joinToString("&") {
                        val key = when (it) {
                            Product.Category.EarlyInvestments -> "early_investments"
                            Product.Category.VentureCapital -> "venture_capital"
                            Product.Category.PrivateInvestments -> "private_investments"
                            Product.Category.GoingPublic -> "going_public"
                            Product.Category.IPO -> "ipo"
                            Product.Category.Uncategorized -> "uncategorized"
                        }

                        "category=${key}"
                    },
                params?.query?.let { "company_contains=${it}" }
            ).joinToString("&")

            url("$hostname/something?$urlParams")
            withUserAgent()
            withAuthentication()
        }

        result.map(ProductMapper::toCommon)
    }

    override suspend fun count(): Int = module.proceed {
        httpClient.get {
            url("$hostname/something/count")
            withUserAgent()
            withAuthentication()
        }
    }

    override suspend fun update(
        id: Int,
        model: ProductUpdateModel,
        logo: UploadFileInfo?,
        summaryPhotos: List<UploadFileInfo>?,
        advantagePhotos: List<UploadFileInfo>?,
        documentsText: List<UploadFileInfo>?,
        documentsPresentation: List<UploadFileInfo>?,
        removeFiles: Set<String>?,
    ): Product = module.proceed {
        val result = httpClient.put<ProductNetworkModel.Default> {
            url("https://api-dev.unicornhunters.com/something/5")
            withUserAgent()
            withAuthentication()
            body = MultiPartFormDataContent(
                formData {
                    append(
                        key = "data",
                        value = Json.encodeToString(prepareProductUpdateModel(model)),
                        headers = headersOf(
                            HttpHeaders.ContentType,
                            ContentType.Application.Json.toString()
                        )
                    )

                    if (logo != null) {
                        append(
                            key = "files.logo",
                            value = logo.contents,
                            headers = headersOf(
                                HttpHeaders.ContentType,
                                ContentType.Image.Any.toString()
                            )
                        )
                    }

                    if (!summaryPhotos.isNullOrEmpty()) {
                        summaryPhotos.forEach {
                            append(
                                key = "files.photo_summary",
                                value = it.contents,
                                headers = headersOf(
                                    HttpHeaders.ContentType,
                                    ContentType.Image.Any.toString()
                                )
                            )
                        }
                    }

                    if (!advantagePhotos.isNullOrEmpty()) {
                        advantagePhotos.forEach {
                            append(
                                key = "files.photo_advantage",
                                value = it.contents,
                                headers = headersOf(
                                    HttpHeaders.ContentType,
                                    ContentType.Image.Any.toString()
                                )
                            )
                        }
                    }

                    if (!documentsText.isNullOrEmpty()) {
                        documentsText.forEach {
                            append(
                                key = "files.document_text",
                                value = it.contents,
                                headers = headersOf(
                                    HttpHeaders.ContentType,
                                    ContentType.Application.OctetStream.toString()
                                )
                            )
                        }
                    }

                    if (!documentsPresentation.isNullOrEmpty()) {
                        documentsPresentation.forEach {
                            append(
                                key = "files.document_presentation",
                                value = it.contents,
                                headers = headersOf(
                                    HttpHeaders.ContentType,
                                    ContentType.Application.OctetStream.toString()
                                )
                            )
                        }
                    }
                }
            )
        }

        ProductMapper.toCommon(result)
    }

    private fun prepareProductUpdateModel(model: ProductUpdateModel) = ProductUpdateNetworkModel(
        category = model.category?.let {
            when (it) {
                Product.Category.EarlyInvestments -> ProductNetworkModel.Category.EarlyInvestments
                Product.Category.VentureCapital -> ProductNetworkModel.Category.VentureCapital
                Product.Category.PrivateInvestments -> ProductNetworkModel.Category.PrivateInvestments
                Product.Category.GoingPublic -> ProductNetworkModel.Category.GoingPublic
                Product.Category.IPO -> ProductNetworkModel.Category.IPO
                Product.Category.Uncategorized -> ProductNetworkModel.Category.Uncategorized
            }
        },
        company = model.company,
        url = model.url,
        facebook = model.facebook,
        instagram = model.instagram,
        linkedin = model.linkedin,
        twitter = model.twitter,
        vimeo = model.vimeo,
        youtube = model.youtube,
        businessType = model.businessType?.let {
            when (it) {
                Product.BusinessType.B2C -> ProductNetworkModel.BusinessType.B2C
                Product.BusinessType.B2B -> ProductNetworkModel.BusinessType.B2B
                Product.BusinessType.B2G -> ProductNetworkModel.BusinessType.B2G
            }
        },
        yearInBusiness = model.yearInBusiness?.let {
            when (it) {
                Product.YearInBusiness.Junior -> ProductNetworkModel.YearInBusiness.Junior
                Product.YearInBusiness.Middle -> ProductNetworkModel.YearInBusiness.Middle
                Product.YearInBusiness.Senior -> ProductNetworkModel.YearInBusiness.Senior
                Product.YearInBusiness.Elder -> ProductNetworkModel.YearInBusiness.Elder
            }
        },
        numberOfEmployees = model.numberOfEmployees?.let {
            when (it) {
                Product.NumberOfEmployees.Small -> ProductNetworkModel.NumberOfEmployees.Small
                Product.NumberOfEmployees.Little -> ProductNetworkModel.NumberOfEmployees.Little
                Product.NumberOfEmployees.Medium -> ProductNetworkModel.NumberOfEmployees.Medium
                Product.NumberOfEmployees.Big -> ProductNetworkModel.NumberOfEmployees.Big
                Product.NumberOfEmployees.Large -> ProductNetworkModel.NumberOfEmployees.Large
                Product.NumberOfEmployees.ExtraLarge -> ProductNetworkModel.NumberOfEmployees.ExtraLarge
            }
        },
        industry = model.businessIndustry?.let { IndustryNetworkModel(it.id, it.name) },
        summary = model.summary,
        advantage = model.advantage,
    )
}