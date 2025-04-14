package com.example.bigtimeeegoantivirus

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.DecimalFormat
import kotlin.math.max
import kotlin.math.log10

object FileFetcher {

    fun getImages(context: Context) = fetchMediaFiles(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

    fun getVideos(context: Context) = fetchMediaFiles(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)

    fun getAudio(context: Context) = fetchMediaFiles(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)

    fun getDocuments(context: Context): List<FileItem> {
        val extensions = listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt")
        return fetchFilesByExtension(context, extensions)
    }

    fun getLargeFiles(context: Context): List<FileItem> {
        return fetchFilesBySize(context, 50 * 1024 * 1024) // Files > 50MB
    }

    fun getHiddenFiles(context: Context): List<FileItem> {
        return fetchFilesInDirectory(context, getStorageRoot(context)) {
            it.isHidden && it.isFile
        }
    }

    fun getRecentFiles(context: Context): List<FileItem> {
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        return fetchFilesByDate(context, sevenDaysAgo)
    }

    fun getInstalledApps(context: Context): List<FileItem> {
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA).map {
            try {
                val appInfo = pm.getApplicationInfo(it.packageName, 0)
                val size = File(appInfo.sourceDir).length()
                FileItem(
                    it.loadLabel(pm).toString(),
                    formatSize(size),
                    isSafe = true,
                    path = appInfo.sourceDir
                )
            } catch (e: Exception) {
                FileItem(
                    it.loadLabel(pm).toString(),
                    "Unknown Size",
                    isSafe = true,
                    path = ""
                )
            }
        }
    }

    private fun getStorageRoot(context: Context): String {
        return context.getExternalFilesDir(null)?.parentFile?.parentFile?.parentFile?.absolutePath
            ?: Environment.getExternalStorageDirectory().absolutePath
    }

    private fun fetchMediaFiles(context: Context, uri: Uri): List<FileItem> {
        val files = mutableListOf<FileItem>()
        val projection = arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.DATA
        )

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val pathIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)

            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIndex)
                val size = cursor.getLong(sizeIndex)
                val modified = cursor.getLong(dateIndex) * 1000 // Convert to milliseconds
                val path = cursor.getString(pathIndex)

                files.add(FileItem(
                    name,
                    formatSize(size),
                    isSafe = true,
                    lastModified = modified,
                    path = path
                ))
            }
        }

        return files
    }

    private fun fetchFilesByExtension(context: Context, extensions: List<String>): List<FileItem> {
        return fetchFilesInDirectory(context, getStorageRoot(context)) { file ->
            file.isFile && extensions.any { ext ->
                file.name.endsWith(".$ext", ignoreCase = true)
            }
        }
    }

    private fun fetchFilesBySize(context: Context, minSize: Long): List<FileItem> {
        return fetchFilesInDirectory(context, getStorageRoot(context)) {
            it.isFile && it.length() > minSize
        }
    }

    private fun fetchFilesByDate(context: Context, minDate: Long): List<FileItem> {
        return fetchFilesInDirectory(context, getStorageRoot(context)) {
            it.isFile && it.lastModified() > minDate
        }
    }

    private fun fetchFilesInDirectory(
        context: Context,
        directoryPath: String,
        filter: (File) -> Boolean
    ): List<FileItem> {
        val dir = File(directoryPath)
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        val files = mutableListOf<FileItem>()
        dir.walk().maxDepth(10).forEach { file ->
            try {
                if (filter(file)) {
                    files.add(FileItem(
                        file.name,
                        formatSize(file.length()),
                        isSafe = true,
                        lastModified = file.lastModified(),
                        path = file.absolutePath
                    ))
                }
            } catch (e: Exception) {

            }
        }

        return files.sortedByDescending { it.lastModified }
    }

    private fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"

        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        val adjustedSize = size / Math.pow(1024.0, digitGroups.toDouble())

        return "%.1f %s".format(adjustedSize, units[digitGroups])
    }
}