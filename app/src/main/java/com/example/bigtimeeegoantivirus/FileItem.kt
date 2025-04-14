package com.example.bigtimeeegoantivirus

data class FileItem(
    val name: String,
    val size: String,
    val isSafe: Boolean,
    val lastModified: Long = 0L,
    val path: String = ""
)