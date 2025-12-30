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
    val officeSought: String?,

    @SerializedName("state")
    val state: String?,

    @SerializedName("party")
    val party: String?,

    @SerializedName("principal_committees")
    val principalCommittees: List<FecCommittee>? = null
)

/**
 * Represents a committee (principally a campaign committee) associated with a candidate.
 */
data class FecCommittee(
    @SerializedName("committee_id")
    val committeeId: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("committee_type")
    val committeeType: String?
)

/**
 * Represents the top-level response from the FEC's /candidate/{candidate_id}/history endpoint.
 */
data class FecCandidateHistoryResponse(
    @SerializedName("results")
    val results: List<FecCandidateHistory>
)

/**
 * Represents a historical record for a candidate in a specific two-year period.
 */
data class FecCandidateHistory(
    @SerializedName("candidate_id")
    val candidateId: String,

    @SerializedName("two_year_period")
    val twoYearPeriod: Int,

    @SerializedName("principal_committees")
    val principalCommittees: List<FecCommittee>? = null
)

/**
 * Represents the top-level response from the FEC's /candidate/{candidate_id}/committees/history endpoint.
 */
data class FecCommitteeHistoryResponse(
    @SerializedName("results")
    val results: List<FecCommitteeHistory>
)

/**
 * Represents a historical record of a committee assignment for a candidate.
 */
data class FecCommitteeHistory(
    @SerializedName("committee_id")
    val committeeId: String,

    @SerializedName("designation")
    val designation: String?,

    @SerializedName("designation_full")
    val designationFull: String?,

    @SerializedName("cycle")
    val cycle: Int,

    @SerializedName("name")
    val name: String
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
    val count: Int,

    // Not from API, but populated manually by repository to differentiate source type.
    // "Employer" or "PAC"
    var type: String = "Employer"
)

/**
 * Represents the top-level response from the FEC's /schedules/schedule_a/ endpoint.
 */
data class FecScheduleAResponse(
    @SerializedName("results")
    val results: List<FecScheduleA>
)

data class FecScheduleA(
    @SerializedName("contributor_name")
    val contributorName: String,

    @SerializedName("contribution_receipt_amount")
    val amount: Double,
    
    @SerializedName("entity_type")
    val entityType: String?
)

/**
 * Represents the top-level response from the FEC's /schedules/schedule_a/by_contributor endpoint.
 */
data class FecContributorResponse(
    @SerializedName("results")
    val results: List<FecContributor>
)

/**
 * Represents aggregated contributions by a specific contributor (Individual or Committee).
 */
data class FecContributor(
    @SerializedName("contributor_name")
    val contributorName: String,

    @SerializedName("contributor_id")
    val contributorId: String?,

    @SerializedName("total")
    val total: Double,

    @SerializedName("count")
    val count: Int
)
