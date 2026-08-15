package com.davemorrissey.labs.subscaleview

/**
 * Native crop-borders detection.
 *
 * Mirrors the companion-object JNI pattern used in ca.mpreg.imagedecoder.ImageDecoder.
 * The native function converts RGBA pixels to grayscale and runs the mihon borders
 * algorithm to find the tightest bounding rect of image content.
 */
class CropBorders private constructor() {
    companion object {
        init {
            System.loadLibrary("ssiv_crop")
        }

        /**
         * Find the crop borders for an RGBA image.
         *
         * @param pixels  Raw RGBA pixel data (4 bytes per pixel, row-major).
         * @param width   Image width in pixels.
         * @param height  Image height in pixels.
         * @return IntArray of [left, top, width, height], or the full image rect if
         *         no border was detected.
         */
        @JvmStatic
        external fun findCropBorders(pixels: ByteArray, width: Int, height: Int): IntArray
    }
}
