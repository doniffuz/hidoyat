package uz.hidoyat.app.core.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri

/** Tanlangan rasmni kvadrat, kichraytirilgan avatarga aylantiradi (kutubxonasiz). */
object AvatarImage {
    private const val SIZE = 512

    fun decode(resolver: ContentResolver, uri: Uri): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            while (bounds.outWidth / (sample * 2) >= SIZE && bounds.outHeight / (sample * 2) >= SIZE) {
                sample *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val raw = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null

            val rotation = resolver.openInputStream(uri)?.use { readRotation(it) } ?: 0
            val side = minOf(raw.width, raw.height)
            val matrix = Matrix().apply {
                postRotate(rotation.toFloat())
                postScale(SIZE.toFloat() / side, SIZE.toFloat() / side)
            }
            Bitmap.createBitmap(
                raw, (raw.width - side) / 2, (raw.height - side) / 2, side, side, matrix, true
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun readRotation(stream: java.io.InputStream): Int = try {
        when (
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
        ) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (e: Exception) {
        0
    }
}
