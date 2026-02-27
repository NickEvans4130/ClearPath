package com.clearpath.overpass

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val PRIMARY_ENDPOINT  = "https://overpass-api.de/api/interpreter"
private const val FALLBACK_ENDPOINT = "https://overpass.kumi.systems/api/interpreter"

class OverpassClient {

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis  = 90_000
            connectTimeoutMillis  = 15_000
            socketTimeoutMillis   = 90_000
        }
    }

    /**
     * Fetch all surveillance nodes within [bbox] (minLat, minLon, maxLat, maxLon).
     * Tries primary endpoint, falls back to secondary on failure.
     */
    suspend fun fetchSurveillance(
        minLat: Double, minLon: Double,
        maxLat: Double, maxLon: Double,
    ): String {
        val query = buildQuery(minLat, minLon, maxLat, maxLon)
        return try {
            postQuery(PRIMARY_ENDPOINT, query)
        } catch (e: Exception) {
            postQuery(FALLBACK_ENDPOINT, query)
        }
    }

    private suspend fun postQuery(endpoint: String, query: String): String {
        val response = client.post(endpoint) {
            contentType(ContentType.Application.FormUrlEncoded)
            header("User-Agent", "ClearPath/1.0 (offline-route-planner)")
            setBody("data=${query.encodeUrl()}")
        }
        return response.bodyAsText()
    }

    private fun buildQuery(
        minLat: Double, minLon: Double,
        maxLat: Double, maxLon: Double,
    ): String {
        val bbox = "$minLat,$minLon,$maxLat,$maxLon"
        return """
            [out:json][timeout:60];
            (
              node["man_made"="surveillance"]($bbox);
              node["man_made"="camera"]($bbox);
              node["surveillance"]($bbox);
            );
            out body;
        """.trimIndent()
    }

    fun close() = client.close()
}

private fun String.encodeUrl(): String = java.net.URLEncoder.encode(this, "UTF-8")
