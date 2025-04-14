package com.example.bigtimeeegoantivirus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FileAdapter(
    private var fileList: List<FileItem>,
    private val onItemClick: (FileItem) -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    fun updateList(newList: List<FileItem>) {
        fileList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = fileList[position]
        holder.fileName.text = file.name
        holder.fileSize.text = "Size: ${file.size}"

        if (file.isSafe) {
            holder.scanStatus.setImageResource(R.drawable.ic_safe)
            holder.scanStatus.setColorFilter(holder.itemView.context.getColor(android.R.color.holo_green_light))
        } else {
            holder.scanStatus.setImageResource(R.drawable.ic_warning)
            holder.scanStatus.setColorFilter(holder.itemView.context.getColor(android.R.color.holo_red_light))
        }

        holder.itemView.setOnClickListener {
            onItemClick(file)
        }
    }

    override fun getItemCount(): Int = fileList.size

    class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileName: TextView = view.findViewById(R.id.file_name)
        val fileSize: TextView = view.findViewById(R.id.file_size)
        val scanStatus: ImageView = view.findViewById(R.id.scan_status)
    }
}