package com.rashodi.app.io

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Подготовка файлов для выгрузки и системное «Поделиться».
 * Выгрузка в Google Таблицы реализована через ACTION_SEND: пользователь в один тап
 * отправляет .xlsx в приложение «Google Таблицы»/«Диск», которое импортирует файл
 * как полноценную таблицу. Так не нужны ни доступ в интернет внутри приложения,
 * ни OAuth/настройка Google Cloud.
 */
object ShareExport {

    const val MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    const val MIME_CSV = "text/csv"
    const val MIME_JSON = "application/json"

    private const val SHEETS_PKG = "com.google.android.apps.docs.editors.sheets"
    private const val DRIVE_PKG = "com.google.android.apps.docs"

    private fun authority(context: Context) = "${context.packageName}.fileprovider"

    private fun exportsDir(context: Context): File =
        File(context.cacheDir, "exports").apply { mkdirs() }

    fun cacheFile(context: Context, fileName: String, bytes: ByteArray): Uri {
        val file = File(exportsDir(context), fileName)
        file.writeBytes(bytes)
        return FileProvider.getUriForFile(context, authority(context), file)
    }

    fun cacheFile(context: Context, fileName: String, text: String): Uri =
        cacheFile(context, fileName, text.toByteArray(Charsets.UTF_8))

    /** Отправляет файл в Google Таблицы/Диск, если установлены, иначе показывает системный выбор. */
    fun shareToSheets(context: Context, uri: Uri, mime: String, subject: String) {
        val base = baseSendIntent(uri, mime, subject)
        val target = firstInstalled(context, base, listOf(SHEETS_PKG, DRIVE_PKG))
        if (target != null) {
            base.setPackage(target)
            context.startActivity(base.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            shareGeneric(context, uri, mime, subject)
        }
    }

    fun shareGeneric(context: Context, uri: Uri, mime: String, subject: String) {
        val chooser = Intent.createChooser(baseSendIntent(uri, mime, subject), subject)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun baseSendIntent(uri: Uri, mime: String, subject: String): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TITLE, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    private fun firstInstalled(context: Context, intent: Intent, packages: List<String>): String? {
        val pm = context.packageManager
        for (pkg in packages) {
            val probe = Intent(intent).setPackage(pkg)
            if (probe.resolveActivity(pm) != null) return pkg
        }
        return null
    }
}
