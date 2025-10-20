package io.github.paulleung93.lobbylens.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data models for parsing responses from the ProPublica Congress API.
 * These classes correspond to the JSON structure returned by the ProPublica endpoints.
 */

/**
 * Represents the top-level response from the ProPublica `getMembers` endpoint.
 */
data class ProPublicaMemberResponse(
    val status: String,
    val copyright: String,
    val results: List<ProPublicaMemberResult>
)

/**
 * Represents the 'results' object within the main response, containing the list of members.
 */
data class ProPublicaMemberResult(
    val congress: String,
    val chamber: String,
    @SerializedName("num_results")
    val numResults: Int,
    val offset: Int,
    val members: List<ProPublicaMember>
)

/**
 * Represents a single member of Congress as returned by the ProPublica API.
 * This class holds the essential legislator data that will be used throughout the app.
 */
data class ProPublicaMember(
    val id: String, // This is the unique ProPublica ID for the member (e.g., "B001288")
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String,
    val party: String,
    val state: String,
    val title: String,
    @SerializedName("next_election")
    val nextElection: String? = null,
    // Note: Campaign finance details will be fetched from a different endpoint.
    // We will add models for that data next.
)
