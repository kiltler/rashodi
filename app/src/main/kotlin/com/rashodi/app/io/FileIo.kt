package com.rashodi.app.io

import android.content.Context
import android.net.Uri

/** Чтение/запись через Storage Access Framework (без широких разрешений на хранилище). */
object FileIo {

    fun readText(context: Context, uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        } ?: throw IllegalStateException("Не удалось открыть файл")

    fun writeText(context: Context, uri: Uri, text: String) {
        writeBytes(context, uri, text.toByteArray(Charsets.UTF_8))
    }

    fun writeBytes(context: Context, uri: Uri, bytes: ByteArray) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
            ?: throw IllegalStateException("Не удалось записать файл")
    }
}
