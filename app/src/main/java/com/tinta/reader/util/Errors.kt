package com.tinta.reader.util

import com.tinta.reader.data.remote.dto.EnvelopeDto
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Error terstruktur yang aman ditampilkan ke pengguna. */
class AppException(
    val kind: Kind,
    override val message: String,
    val code: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {
    enum class Kind { Network, Timeout, Server, NotFound, Parse, RateLimited, Unauthorized, Unknown }
}

/** Ubah throwable apa pun menjadi AppException; CancellationException dilempar ulang. */
fun Throwable.toAppException(): AppException = when (this) {
    is CancellationException -> throw this
    is AppException -> this
    is SocketTimeoutException -> AppException(AppException.Kind.Timeout, Strings.errTimeout, cause = this)
    is UnknownHostException -> AppException(AppException.Kind.Network, Strings.errOffline, cause = this)
    is IOException -> AppException(AppException.Kind.Network, Strings.errNetwork, cause = this)
    is SerializationException -> AppException(AppException.Kind.Parse, Strings.errParse, cause = this)
    is IllegalArgumentException -> AppException(AppException.Kind.Parse, Strings.errParse, cause = this)
    is HttpException -> fromHttp(this)
    else -> AppException(AppException.Kind.Unknown, Strings.errUnknown, cause = this)
}

private fun fromHttp(e: HttpException): AppException {
    val code = e.code()
    val kind = when (code) {
        401 -> AppException.Kind.Unauthorized
        404 -> AppException.Kind.NotFound
        429 -> AppException.Kind.RateLimited
        504 -> AppException.Kind.Timeout
        in 500..599 -> AppException.Kind.Server
        else -> AppException.Kind.Unknown
    }
    val message = when (kind) {
        AppException.Kind.Unauthorized -> Strings.errUnauthorized
        AppException.Kind.NotFound -> Strings.errNotFound
        AppException.Kind.RateLimited -> Strings.errRateLimited
        AppException.Kind.Timeout -> Strings.errSourceTimeout
        AppException.Kind.Server -> Strings.errSource
        else -> Strings.errUnknown
    }
    return AppException(kind, message, code = "HTTP $code", cause = e)
}

/** Membuka envelope { success, data }. Gagal bila success=false atau data kosong. */
fun <T> EnvelopeDto<T>.unwrap(): T {
    if (!success) {
        val text = error?.message?.takeIf { it.isNotBlank() } ?: Strings.errSource
        throw AppException(AppException.Kind.Server, text, code = error?.code)
    }
    return data ?: throw AppException(AppException.Kind.Parse, Strings.errParse)
}
