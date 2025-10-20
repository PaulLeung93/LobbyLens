package io.github.paulleung93.lobbylens.domain.ai

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.sqrt
import org.json.JSONObject

/**
 * A class that encapsulates the logic for facial recognition using a TensorFlow Lite model.
 *
 * This class is responsible for loading the FaceNet TFLite model, loading a database of
 * pre-computed politician embeddings, and comparing a detected face against this database
 * to find the closest match.
 *
 * @param context The application context, used for accessing assets.
 */
class FaceRecognizer(private val context: Context) {

    private lateinit var interpreter: Interpreter
    private lateinit var embeddings: Map<String, FloatArray>

    // Model-specific constants for input and output.
    private val inputImageSize = 112
    private val outputEmbeddingSize = 128

    init {
        loadModel()
        loadEmbeddings()
    }

    /**
     * Loads the TensorFlow Lite model from the assets folder.
     */
    private fun loadModel() {
        try {
            val modelBuffer = loadModelFile()
            val options = Interpreter.Options()
            interpreter = Interpreter(modelBuffer, options)
        } catch (e: IOException) {
            // Handle model loading error
            e.printStackTrace()
        }
    }

    /**
     * Loads the pre-computed embeddings from the `embeddings.json` file in assets.
     */
    private fun loadEmbeddings() {
        try {
            val jsonString = context.assets.open("embeddings.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val tempEmbeddings = mutableMapOf<String, FloatArray>()
            jsonObject.keys().forEach { key ->
                val jsonArray = jsonObject.getJSONArray(key)
                val floatArray = FloatArray(jsonArray.length()) {
                    jsonArray.getDouble(it).toFloat()
                }
                tempEmbeddings[key] = floatArray
            }
            embeddings = tempEmbeddings
        } catch (e: IOException) {
            // Handle embeddings loading error
            e.printStackTrace()
            embeddings = emptyMap()
        }
    }

    /**
     * Recognizes a face by comparing it to the loaded embeddings.
     *
     * @param faceBitmap The bitmap of the detected and cropped face.
     * @return A [Pair] containing the ID of the recognized politician and the confidence score, or null if no match is found.
     */
    fun recognize(faceBitmap: Bitmap): Pair<String, Float>? {
        val faceEmbedding = getEmbedding(faceBitmap)

        var bestMatch: String? = null
        var minDistance = Float.MAX_VALUE

        embeddings.forEach { (id, knownEmbedding) ->
            val distance = calculateL2Distance(faceEmbedding, knownEmbedding)
            if (distance < minDistance) {
                minDistance = distance
                bestMatch = id
            }
        }

        // You may need to tune this threshold based on your model's performance.
        val confidenceThreshold = 1.0f
        return if (bestMatch != null && minDistance < confidenceThreshold) {
            Pair(bestMatch, minDistance)
        } else {
            null
        }
    }

    /**
     * Generates an embedding for a given face bitmap.
     */
    private fun getEmbedding(bitmap: Bitmap): FloatArray {
        val preprocessedBitmap = preprocessBitmap(bitmap)
        val inputBuffer = convertBitmapToByteBuffer(preprocessedBitmap)
        val outputBuffer = ByteBuffer.allocateDirect(outputEmbeddingSize * 4).apply {
            order(ByteOrder.nativeOrder())
        }
        
        interpreter.run(inputBuffer, outputBuffer)
        
        outputBuffer.rewind()
        val embedding = FloatArray(outputEmbeddingSize)
        outputBuffer.asFloatBuffer().get(embedding)
        return embedding
    }

    /**
     * Preprocesses the bitmap to the required size and format for the model.
     */
    private fun preprocessBitmap(bitmap: Bitmap): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, inputImageSize, inputImageSize, true)
    }

    /**
     * Converts a bitmap into a ByteBuffer for TFLite model input.
     */
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(inputImageSize * inputImageSize * 3 * 4).apply {
            order(ByteOrder.nativeOrder())
        }
        val intValues = IntArray(inputImageSize * inputImageSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until inputImageSize) {
            for (j in 0 until inputImageSize) {
                val value = intValues[pixel++]
                // Normalize pixel values to the range [-1, 1] as required by some models like FaceNet.
                byteBuffer.putFloat(((value shr 16 and 0xFF) - 127.5f) / 127.5f)
                byteBuffer.putFloat(((value shr 8 and 0xFF) - 127.5f) / 127.5f)
                byteBuffer.putFloat(((value and 0xFF) - 127.5f) / 127.5f)
            }
        }
        return byteBuffer
    }

    /**
     * Loads the model file from the assets folder into a ByteBuffer.
     */
    @Throws(IOException::class)
    private fun loadModelFile(): ByteBuffer {
        val fileDescriptor = context.assets.openFd("FaceNet.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Calculates the L2 (Euclidean) distance between two embeddings.
     */
    private fun calculateL2Distance(embedding1: FloatArray, embedding2: FloatArray): Float {
        var sum = 0.0f
        for (i in embedding1.indices) {
            val diff = embedding1[i] - embedding2[i]
            sum += diff * diff
        }
        return sqrt(sum)
    }
}