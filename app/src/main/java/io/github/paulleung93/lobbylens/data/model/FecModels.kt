package io.github.paulleung93.lobbylens.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents the top-level response from the FEC's /candidates/search endpoint.
 */
data class FecCandidateResponse(
    @SerializedName("results")
    val results: List<FecCandidate>
)

/**
 * Represents a single candidate returned from the FEC API.
 *
 * @property candidateId The unique ID for the candidate (e.g., "H8CA05035").
 * @property name The full name of the candidate.
 * @property officeSought The office the candidate is running for (e.g., "H" for House, "S" for Senate).
 */
data class FecCandidate(
    @SerializedName("candidate_id")
    val candidateId: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("office_sought")
    val officeSought: String?
)

/**
 * Represents the top-level response from the FEC's /receipts/by_employer endpoint.
 */
data class FecEmployerContributionResponse(
    @SerializedName("results")
    val results: List<FecEmployerContribution>
)

/**
 * Represents campaign contributions aggregated by a specific employer.
 *
 * @property employer The name of the employer.
 * @property total The total amount contributed by employees of this employer.
 * @property count The number of individual contributions.
 */
data class FecEmployerContribution(
    @SerializedName("employer")
    val employer: String,

    @SerializedName("total")
    val total: Double,

    @SerializedName("count")
    val count: Int
)
