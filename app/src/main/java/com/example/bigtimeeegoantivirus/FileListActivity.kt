
package com.example.bigtimeeegoantivirus

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bigtimeeegoantivirus.FileAdapter
import com.example.bigtimeeegoantivirus.FileItem
import com.example.bigtimeeegoantivirus.FileFetcher

class FileListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_list)

        val category = intent.getStringExtra("category") ?: return
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val fileList = when (category) {
            "images" -> FileFetcher.getImages(this)
            "videos" -> FileFetcher.getVideos(this)
            "audio" -> FileFetcher.getAudio(this)
            "documents" -> FileFetcher.getDocuments(this)
            "large" -> FileFetcher.getLargeFiles(this)
            "hidden" -> FileFetcher.getHiddenFiles()
            "recent" -> FileFetcher.getRecentFiles(this)
            "apps" -> FileFetcher.getInstalledApps(this)
            else -> emptyList()
        }

        recyclerView.adapter = FileAdapter(fileList)
        if (fileList.isEmpty()) {
            Toast.makeText(this, "No files found!", Toast.LENGTH_SHORT).show()
        }
    }
}
