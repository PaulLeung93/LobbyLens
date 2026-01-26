package io.github.paulleung93.lobbylens.data.repository

import android.graphics.Bitmap
import io.github.paulleung93.lobbylens.data.model.AnnotateImageResponse
import io.github.paulleung93.lobbylens.data.model.CloudVisionResponse
import io.github.paulleung93.lobbylens.data.model.FecCandidate
import io.github.paulleung93.lobbylens.data.model.FecCandidateResponse
import io.github.paulleung93.lobbylens.data.model.WebDetection
import io.github.paulleung93.lobbylens.data.model.WebEntity
import io.github.paulleung93.lobbylens.data.network.CloudVisionService
import io.github.paulleung93.lobbylens.util.ImageUtils
import io.github.paulleung93.lobbylens.util.Result
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Unit tests for VisionRepository.
 * Tests politician identification flow including Cloud Vision API and FEC fallback.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VisionRepositoryTest {

    // Mocked dependencies
    private lateinit var cloudVisionService: CloudVisionService
    private lateinit var fecRepository: FecRepository
    private lateinit var mockBitmap: Bitmap

    // Class under test
    private lateinit var repository: VisionRepository

    // Test dispatcher
    private val testDispatcher = StandardTestDispatcher()

    // Test Data
    private val testCandidate = FecCandidate(
        candidateId = "P80001571",
        name = "BIDEN, JOSEPH R",
        party = "DEM",
        state = "DE",
        officeSought = "P"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        cloudVisionService = mockk(relaxed = true)
        fecRepository = mockk(relaxed = true)
        mockBitmap = mockk(relaxed = true)
        
        // Mock the static ImageUtils object
        mockkObject(ImageUtils)
        coEvery { ImageUtils.bitmapToBase64(any()) } returns "base64String"

        repository = VisionRepository(
            cloudVisionService = cloudVisionService,
            fecRepository = fecRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `identifyPolitician returns Success when Cloud Vision finds entity and FEC matches`() = runTest {
        // Arrange
        val webEntity = WebEntity(entityId = "id", score = 0.9f, description = "Joe Biden")
        val visionResponse = CloudVisionResponse(
            responses = listOf(
                AnnotateImageResponse(
                    webDetection = WebDetection(webEntities = listOf(webEntity)),
                    faceAnnotations = null,
                    error = null
                )
            )
        )
        
        coEvery { cloudVisionService.annotateImage(any(), any()) } returns Response.success(visionResponse)
        coEvery { fecRepository.searchCandidatesByName("Joe Biden") } returns Result.Success(
            FecCandidateResponse(listOf(testCandidate))
        )

        // Act
        val result = repository.identifyPolitician(mockBitmap)

        // Assert
        assertTrue("Expected Success result", result is Result.Success)
        val success = result as Result.Success
        assertEquals("BIDEN, JOSEPH R", success.data.name)
    }

    @Test
    fun `identifyPolitician returns Error when Cloud Vision returns no entities`() = runTest {
        // Arrange
        val visionResponse = CloudVisionResponse(
            responses = listOf(
                AnnotateImageResponse(
                    webDetection = WebDetection(webEntities = emptyList()),
                    faceAnnotations = null,
                    error = null
                )
            )
        )
        coEvery { cloudVisionService.annotateImage(any(), any()) } returns Response.success(visionResponse)

        // Act
        val result = repository.identifyPolitician(mockBitmap)

        // Assert
        assertTrue("Expected Error result", result is Result.Error)
        assertEquals("No entities detected.", (result as Result.Error).exception.message)
    }

    @Test
    fun `identifyPolitician tries name variations when direct match fails`() = runTest {
        // Arrange - "Joe Biden" fails, but "Joseph Biden" (nickname variation) succeeds
        val webEntity = WebEntity(entityId = "id", score = 0.9f, description = "Joe Biden")
        val visionResponse = CloudVisionResponse(
            responses = listOf(
                AnnotateImageResponse(
                    webDetection = WebDetection(webEntities = listOf(webEntity)),
                    faceAnnotations = null,
                    error = null
                )
            )
        )
        
        coEvery { cloudVisionService.annotateImage(any(), any()) } returns Response.success(visionResponse)
        
        // First search fails
        coEvery { fecRepository.searchCandidatesByName("Joe Biden") } returns Result.Success(
            FecCandidateResponse(emptyList())
        )
        
        // Variation search succeeds (assuming "Joseph Biden" is generated)
        coEvery { fecRepository.searchCandidatesByName("Joseph Biden") } returns Result.Success(
            FecCandidateResponse(listOf(testCandidate))
        )

        // Act
        val result = repository.identifyPolitician(mockBitmap)

        // Assert
        assertTrue("Expected Success result", result is Result.Success)
        val success = result as Result.Success
        assertEquals("BIDEN, JOSEPH R", success.data.name)
    }
}
