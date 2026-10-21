package io.github.conorryan.weeklies.data.import

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.concurrent.TimeUnit

/** Minimal response abstraction so tests can fake the transport. */
class HttpResponse(val status: Int, val bodyText: String)

fun interface HttpTransport {
    /**
     * Executes one HTTP request. Implementations should cooperate with
     * coroutine cancellation (cancel the underlying request when the
     * caller's coroutine is canceled).
     */
    suspend fun execute(method: String, url: String, headers: Map<String, String>, body: String?): HttpResponse
}

fun isValidRecipeUrl(urlString: String): Boolean {
    return try {
        val u = URI(urlString.trim())
        u.scheme == "http" || u.scheme == "https"
    } catch (e: Exception) {
        false
    }
}

/** OkHttp-backed transport with the fetch limits the RN app used. */
class OkHttpTransport(
    private val client: OkHttpClient = defaultClient(),
) : HttpTransport {

    override suspend fun execute(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?,
    ): HttpResponse = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .method(method, body?.let { okhttp3.RequestBody.create(null, it) })
            .apply { headers.forEach { (k, v) -> header(k, v) } }
            .build()

        val call = client.newCall(request)
        val guard = currentCoroutineContext().job.invokeOnCompletion { call.cancel() }
        try {
            call.execute().use { response ->
                HttpResponse(status = response.code, bodyText = response.body.string())
            }
        } finally {
            guard.dispose()
        }
    }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .callTimeout(ImportConstants.NATIVE_FETCH_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .build()
    }
}

/**
 * Fetches a recipe page with a hard size cap (5 MB), mirroring the RN
 * streaming reader behavior.
 */
class PageFetcher(private val transport: HttpTransport) {

    suspend fun fetchRecipePageHtml(url: String): String {
        val trimmed = url.trim()
        if (!isValidRecipeUrl(trimmed)) throw RecipeImportErrors.invalidUrl()

        val response = try {
            transport.execute(
                method = "GET",
                url = trimmed,
                headers = mapOf(
                    "Accept" to "text/html,application/xhtml+xml;q=0.9,*/*;q=0.8",
                    "User-Agent" to "Weeklies/1.0 (recipe import)",
                ),
                body = null,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw RecipeImportErrors.fetchFailed()
        }

        if (response.status !in 200..299) {
            throw RecipeImportErrors.fetchFailed(response.status)
        }

        if (response.bodyText.toByteArray(Charsets.UTF_8).size > ImportConstants.NATIVE_MAX_RESPONSE_BYTES) {
            throw RecipeImportErrors.pageTooLarge()
        }
        return response.bodyText
    }
}
