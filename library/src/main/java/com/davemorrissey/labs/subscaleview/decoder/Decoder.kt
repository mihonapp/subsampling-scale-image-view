package com.davemorrissey.labs.subscaleview.decoder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import ca.mpreg.imagedecoder.ImageDecoder
import com.davemorrissey.labs.subscaleview.CropBorders
import com.davemorrissey.labs.subscaleview.provider.InputProvider

class Decoder(
    private val cropBorders: Boolean,
) : ImageRegionDecoder {

    // Pixel data copied into JVM heap memory (RGBA, 4 bytes per pixel).
    // We copy eagerly because DecodeResult.image is a direct ByteBuffer backed by
    // native memory that gets g_free'd when the DecodeResult is finalized. Holding
    // a duplicate() of that buffer would leave us with a dangling pointer after GC.
    private var pixels: ByteArray? = null
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0

    // When cropBorders is enabled these track the detected crop rect so that
    // init() can report the cropped dimensions to SSIV while decodeRegion()
    // translates tile coordinates back into the full-image pixel buffer.
    private var cropLeft: Int = 0
    private var cropTop: Int = 0
    private var cropWidth: Int = 0
    private var cropHeight: Int = 0

    @Volatile
    private var recycled = false

    /**
     * Initialise the decoder. Opens the stream, decodes the full image once to obtain
     * dimensions and a safe copy of the pixel data, then returns those dimensions.
     * If cropBorders is enabled the native border-detection is run and the cropped
     * dimensions are returned so SSIV lays out tiles against the trimmed size.
     */
    override fun init(context: Context, provider: InputProvider): Point {
        val decoder = provider.openStream().use { inputStream ->
            checkNotNull(inputStream) { "InputProvider returned null stream" }
            ImageDecoder.new(inputStream)
        }

        val result = decoder.decode(page = 0)
        imageWidth = result.width
        imageHeight = result.height

        // Copy native pixel data into a JVM ByteArray before the DecodeResult can be
        // finalized (which would g_free the underlying buffer the ByteBuffer points into).
        val buf = result.image
        buf.rewind()
        val copy = ByteArray(buf.remaining())
        buf.get(copy)
        pixels = copy

        // Default crop rect = full image
        cropLeft = 0
        cropTop = 0
        cropWidth = imageWidth
        cropHeight = imageHeight

        if (cropBorders) {
            val borders = CropBorders.findCropBorders(copy, imageWidth, imageHeight)
            val l = borders[0];
            val t = borders[1]
            val w = borders[2];
            val h = borders[3]
            // Only apply if the detected rect is non-empty and smaller than the full image
            if (w > 0 && h > 0 && (l != 0 || t != 0 || w != imageWidth || h != imageHeight)) {
                cropLeft = l
                cropTop = t
                cropWidth = w
                cropHeight = h
            }
        }

        return Point(cropWidth, cropHeight)
    }

    /**
     * Decode a region of the image with the given sample size.
     *
     * [sRect] is expressed in the cropped coordinate space that SSIV knows about.
     * We translate it back into the full pixel buffer by adding [cropLeft]/[cropTop],
     * then extract the region stepping [sampleSize] source pixels per output pixel.
     *
     * @param sRect      Source image rectangle to decode (in cropped coordinates).
     * @param sampleSize Sample size (1 = full res, 2 = half res, etc.).
     * @return The decoded region bitmap.
     */
    override fun decodeRegion(sRect: Rect, sampleSize: Int): Bitmap {
        val src = checkNotNull(pixels) { "Decoder not initialized or recycled" }
        check(!recycled) { "Decoder has been recycled" }

        // Translate from cropped-image coords to full-image coords
        val srcLeft = (sRect.left + cropLeft).coerceAtLeast(0)
        val srcTop = (sRect.top + cropTop).coerceAtLeast(0)
        val srcRight = (sRect.right + cropLeft).coerceAtMost(imageWidth)
        val srcBottom = (sRect.bottom + cropTop).coerceAtMost(imageHeight)

        val regionWidth = srcRight - srcLeft
        val regionHeight = srcBottom - srcTop
        val outWidth = (regionWidth + sampleSize - 1) / sampleSize
        val outHeight = (regionHeight + sampleSize - 1) / sampleSize

        val outPixels = IntArray(outWidth * outHeight)
        val stride = imageWidth * 4  // 4 bytes per RGBA pixel

        for (outY in 0 until outHeight) {
            val sy = srcTop + outY * sampleSize
            if (sy >= imageHeight) break
            for (outX in 0 until outWidth) {
                val sx = srcLeft + outX * sampleSize
                if (sx >= imageWidth) continue

                val i = sy * stride + sx * 4
                val r = src[i].toInt() and 0xFF
                val g = src[i + 1].toInt() and 0xFF
                val b = src[i + 2].toInt() and 0xFF
                val a = src[i + 3].toInt() and 0xFF

                outPixels[outY * outWidth + outX] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        return Bitmap.createBitmap(outPixels, outWidth, outHeight, Bitmap.Config.ARGB_8888)
    }

    /**
     * Returns true if the decoder has been successfully initialized and not yet recycled.
     */
    override fun isReady(): Boolean {
        return pixels != null && !recycled
    }

    /**
     * Release resources held by this decoder.
     */
    override fun recycle() {
        recycled = true
        pixels = null
    }
}
