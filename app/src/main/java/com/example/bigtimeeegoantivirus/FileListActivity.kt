package com.example.bigtimeeegoantivirus

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File

class FileListActivity : AppCompatActivity() {
    private lateinit var adapter: FileAdapter
    private lateinit var category: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_list)

        category = intent.getStringExtra("category") ?: return
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = FileAdapter(emptyList()) { fileItem ->
            openFile(fileItem)
        }
        recyclerView.adapter = adapter

        refreshFiles()

        findViewById<FloatingActionButton>(R.id.btnRefresh).setOnClickListener {
            refreshFiles()
        }
    }

    private fun refreshFiles() {
        val fileList = when (category) {
            "images" -> FileFetcher.getImages(this)
            "videos" -> FileFetcher.getVideos(this)
            "audio" -> FileFetcher.getAudio(this)
            "documents" -> FileFetcher.getDocuments(this)
            "large" -> FileFetcher.getLargeFiles(this)
            "hidden" -> FileFetcher.getHiddenFiles(this)
            "recent" -> FileFetcher.getRecentFiles(this)
            "apps" -> FileFetcher.getInstalledApps(this)
            else -> emptyList()
        }

        adapter.updateList(fileList)

        if (fileList.isEmpty()) {
            Toast.makeText(this, "No files found!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFile(fileItem: FileItem) {
        try {
            val file = File(fileItem.path)
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, getMimeType(fileItem.name))
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".jpg", ignoreCase = true) -> "image/jpeg"
            fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
            fileName.endsWith(".png", ignoreCase = true) -> "image/png"
            fileName.endsWith(".gif", ignoreCase = true) -> "image/gif"
            fileName.endsWith(".mp4", ignoreCase = true) -> "video/mp4"
            fileName.endsWith(".mkv", ignoreCase = true) -> "video/x-matroska"
            fileName.endsWith(".avi", ignoreCase = true) -> "video/x-msvideo"
            fileName.endsWith(".mp3", ignoreCase = true) -> "audio/mpeg"
            fileName.endsWith(".wav", ignoreCase = true) -> "audio/wav"
            fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            fileName.endsWith(".doc", ignoreCase = true) -> "application/msword"
            fileName.endsWith(".docx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            fileName.endsWith(".xls", ignoreCase = true) -> "application/vnd.ms-excel"
            fileName.endsWith(".xlsx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            fileName.endsWith(".ppt", ignoreCase = true) -> "application/vnd.ms-powerpoint"
            fileName.endsWith(".pptx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            fileName.endsWith(".txt", ignoreCase = true) -> "text/plain"
            else -> "*/*"
        }
    }
}