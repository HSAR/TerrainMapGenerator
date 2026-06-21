package io.hsar.mapgenerator.commandgui

import java.io.File

internal data class AssetSource(
    val path: String,
    val originalFilename: String,
    val extension: String,
    val mediaType: String,
    val embeddedBytes: ByteArray? = null,
) {
    fun file(): File = File(path)

    companion object {
        fun fromFile(file: File): AssetSource =
            AssetSource(
                path = file.absolutePath,
                originalFilename = file.name,
                extension = file.extension.lowercase(),
                mediaType = mediaTypeForExtension(file.extension),
            )

        fun fromEmbeddedBytes(
            fileName: String,
            bytes: ByteArray,
        ): AssetSource {
            val extension = fileName.substringAfterLast('.', "").lowercase()
            return AssetSource(
                path = "embedded/$fileName",
                originalFilename = fileName,
                extension = extension,
                mediaType = mediaTypeForExtension(extension),
                embeddedBytes = bytes,
            )
        }

        fun mediaTypeForExtension(extension: String): String =
            when (extension.lowercase()) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "gif" -> "image/gif"
                "bmp" -> "image/bmp"
                "webp" -> "image/webp"
                "svg" -> "image/svg+xml"
                else -> "application/octet-stream"
            }
    }
}
