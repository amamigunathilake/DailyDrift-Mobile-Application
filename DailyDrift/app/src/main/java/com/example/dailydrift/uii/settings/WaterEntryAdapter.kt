package com.example.dailydrift.uii.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dailydrift.data.model.WaterEntry
import com.example.dailydrift.databinding.ItemWaterEntryBinding
import java.text.SimpleDateFormat
import java.util.*

class WaterEntryAdapter(
    private val items: MutableList<WaterEntry>,
    private val onLongDelete: (WaterEntry) -> Unit
) : RecyclerView.Adapter<WaterEntryAdapter.VH>() {

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemWaterEntryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun replace(newItems: List<WaterEntry>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class VH(private val b: ItemWaterEntryBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(e: WaterEntry) {
            b.tvAmount.text = "${e.amountMl} ml"
            b.tvTime.text = timeFormat.format(Date(e.createdAt))
            b.root.setOnLongClickListener {
                onLongDelete(e); true
            }
        }
    }
}
