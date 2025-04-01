package com.example.bigtimeeegoantivirus

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import java.io.File

object FileFetcher {

    fun getImages(context: Context) = fetchMediaFiles(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
    fun getVideos(context: Context) = fetchMediaFiles(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
    fun getAudio(context: Context) = fetchMediaFiles(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)

    fun getDocuments(context: Context): List<FileItem> {
        val documentExtensions = listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt")
        return fetchFilesByExtension(documentExtensions)
    }

    fun getLargeFiles(context: Context): List<FileItem> {
        return fetchFilesBySize(50 * 1024 * 1024) // Files larger than 50MB
    }

    fun getHiddenFiles(): List<FileItem> {
        return fetchFilesInDirectory("/storage/emulated/0/") { it.name.startsWith(".") }
    }

    fun getRecentFiles(context: Context): List<FileItem> {
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        return fetchFilesByDate(sevenDaysAgo)
    }

    fun getInstalledApps(context: Context): List<FileItem> {
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA).map {
            FileItem(it.loadLabel(pm).toString(), "Unknown Size", isSafe = true)

        }
    }

    private fun fetchMediaFiles(context: Context, uri: Uri): List<FileItem> {
        val files = mutableListOf<FileItem>()
        val cursor: Cursor? = context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)

        cursor?.use {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            while (it.moveToNext()) {
                val path = it.getString(columnIndex)
                val file = File(path)
                files.add(FileItem(file.name, "${file.length() / 1024} KB", isSafe = true)) // or false based on your logic

            }
        }
        return files
    }

    private fun fetchFilesByExtension(extensions: List<String>): List<FileItem> {
        return fetchFilesInDirectory("/storage/emulated/0/") { file ->
            extensions.any { file.name.endsWith(it, ignoreCase = true) }
        }
    }

    private fun fetchFilesBySize(minSize: Long): List<FileItem> {
        return fetchFilesInDirectory("/storage/emulated/0/") { it.length() > minSize }
    }

    private fun fetchFilesByDate(minDate: Long): List<FileItem> {
        return fetchFilesInDirectory("/storage/emulated/0/") { it.lastModified() > minDate }
    }

    private fun fetchFilesInDirectory(directoryPath: String, filter: (File) -> Boolean): List<FileItem> {
        return File(directoryPath).listFiles()?.filter(filter)?.map {
            FileItem(it.name, "${it.length() / 1024} KB", isSafe = true)

        } ?: emptyList()
    }
}
