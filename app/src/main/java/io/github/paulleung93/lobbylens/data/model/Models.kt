package io.github.paulleung93.lobbylens.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents the top-level response for a legislator lookup from the OpenSecrets API.
 */
data class LegislatorResponse(
    @SerializedName("response") val response: LegislatorsContainer
)

/**
 * Container for the list of legislators within the API response.
 */
data class LegislatorsContainer(
    @SerializedName("legislator") val legislators: List<Legislator>
)

/**
 * Data class representing a legislator, containing their attributes.
 * The actual data is nested within the '@attributes' object in the JSON response.
 */
data class Legislator(
    @SerializedName("@attributes") val attributes: LegislatorAttributes
)

/**
 * Contains the detailed attributes of a single legislator.
 */
data class LegislatorAttributes(
    @SerializedName("cid") val cid: String,
    @SerializedName("firstlast") val firstLast: String,
    @SerializedName("party") val party: String,
    @SerializedName("office") val office: String,
)

/**
 * Represents the top-level response for an industry lookup.
 */
data class IndustryResponse(
    @SerializedName("response") val response: IndustriesContainer
)

/**
 * Container for the industry data within the API response.
 */
data class IndustriesContainer(
    @SerializedName("industries") val industries: Industries
)

/**
 * Holds the list of contributing industries.
 */
data class Industries(
    @SerializedName("industry") val industryList: List<Industry>
)

/**
 * Data class representing a single contributing industry.
 * The actual data is nested within the '@attributes' object in the JSON response.
 */
data class Industry(
    @SerializedName("@attributes") val attributes: IndustryAttributes
)

/**
 * Contains the detailed attributes of a contributing industry, including donation amounts.
 */
data class IndustryAttributes(
    @SerializedName("industry_name") val industryName: String,
    @SerializedName("industry_code") val industryCode: String,
    @SerializedName("total") val total: String
)
