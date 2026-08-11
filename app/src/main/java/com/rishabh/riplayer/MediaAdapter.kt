package com.rishabh.riplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.TimeUnit

class MediaAdapter(
    private val items: List<MediaItemData>,
    private val onClick: (MediaItemData) -> Unit
) : RecyclerView.Adapter<MediaAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.mediaTitle)
        val duration: TextView = view.findViewById(R.id.durationBadge)
        val thumb: ImageView = view.findViewById(R.id.thumbImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.duration.text = formatDuration(item.durationMs)
        holder.thumb.setImageResource(android.R.color.darker_gray)
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size

    private fun formatDuration(ms: Long): String {
        if (ms <= 0) return "--:--"
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms)
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
        else String.format("%02d:%02d", m, s)
    }
}
