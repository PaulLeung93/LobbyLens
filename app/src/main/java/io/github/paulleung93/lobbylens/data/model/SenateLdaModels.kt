package io.github.paulleung93.lobbylens.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data models for the U.S. Senate Lobbying Disclosure Act (LDA) API.
 * Specifically for LD-203 Contribution Reports.
 */

data class SenateContributionResponse(
    @SerializedName("count")
    val count: Int,
    @SerializedName("next")
    val next: String?,
    @SerializedName("previous")
    val previous: String?,
    @SerializedName("results")
    val results: List<SenateContributionReport>
)

data class SenateContributionReport(
    @SerializedName("filing_uuid")
    val filingUuid: String,
    @SerializedName("filing_year")
    val filingYear: Int,
    @SerializedName("filing_period")
    val filingPeriod: String?,
    @SerializedName("registrant")
    val registrant: SenateRegistrant,
    @SerializedName("contribution_items")
    val contributionItems: List<SenateContribution>? = null
)

data class SenateRegistrant(
    @SerializedName("name")
    val name: String,
    @SerializedName("registrant_id")
    val registrantId: Int
)

data class SenateContribution(
    @SerializedName("contribution_type")
    val type: String,
    @SerializedName("contributor_name")
    val contributorName: String,
    @SerializedName("payee_name")
    val payeeName: String,
    @SerializedName("honoree_name")
    val honoreeName: String,
    @SerializedName("amount")
    val amount: String, // API often returns string for decimals
    @SerializedName("date")
    val date: String
)
