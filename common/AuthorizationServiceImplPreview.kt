/*
 * Copyright (c) 2021.
 */

package com.kmgi.unicorns.core.network.ktor.ktor

import com.kmgi.unicorns.core.network.AuthorizationService
import com.kmgi.unicorns.core.network.AuthorizationService.*
import com.kmgi.unicorns.core.network.ktor.KtorApiModule
import com.kmgi.unicorns.core.network.ktor.KtorApiModule.Configuration
import com.kmgi.unicorns.core.network.ktor.Service
import com.kmgi.unicorns.core.network.ktor.mappers.UserMapper
import com.kmgi.unicorns.core.network.ktor.models.ProductNetworkModel
import com.kmgi.unicorns.core.network.ktor.models.UserNetworkModel
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class AuthorizationServiceImplPreview(
    configuration: Configuration,
    module: KtorApiModule,
) : Service(configuration, module), AuthorizationService {

    override suspend fun login(identifier: String, password: String): AuthorizationData = module.proceed {
        val result = httpClient.post<AuthorizationDataResponse> {
            url("$hostname/auth/local")
            withUserAgent()
            body = MultiPartFormDataContent(
                formData {
                    append(key = "identifier", value = identifier)
                    append(key = "password", value = password)
                }
            )
        }

        configuration.accessToken = result.accessToken
        configuration.refreshToken = result.refreshToken

        AuthorizationData(
            user = UserMapper<ProductNetworkModel.Compact>().toCommon(result.user),
            accessToken = result.accessToken,
            refreshToken = result.refreshToken
        )
    }

    @Serializable
    class AuthorizationDataResponse(
        val user: UserNetworkModel<ProductNetworkModel.Compact>,
        val accessToken: String,
        val refreshToken: String,
    )

    override suspend fun createUser1(user1: User1) = module.proceed {
        httpClient.post<Unit> {
            url("$hostname/user1")
            withUserAgent()
            withJsonBody(user1)
        }
    }

    override suspend fun createUser2(user2: User2) = module.proceed {
        httpClient.post<Unit> {
            url("$hostname/user2")
            withUserAgent()
            withJsonBody(user2)
        }
    }

    override suspend fun resetPassword(
        code: String,
        password: String,
        passwordConfirmation: String,
    ) = module.proceed<Unit> {
        httpClient.post<HttpResponse> {
            url("$hostname/auth/reset-password")
            withUserAgent()
            withJsonBody(ResetPasswordRequest(
                code = code,
                password = password,
                confirmation = passwordConfirmation
            ))
        }
    }

    @Serializable
    class ResetPasswordRequest(
        val code: String,
        val password: String,
        @SerialName("passwordConfirmation")
        val confirmation: String,
    )
}