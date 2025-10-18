package com.example.dailydrift.uii.hydration

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dailydrift.R
import com.example.dailydrift.data.model.WaterEntry
import com.example.dailydrift.databinding.ItemWaterEntryBinding
import java.text.SimpleDateFormat
import java.util.*

class WaterEntryAdapter(
    private val entries: MutableList<WaterEntry>,
    private val onDelete: (WaterEntry) -> Unit
) : RecyclerView.Adapter<WaterEntryAdapter.WaterEntryViewHolder>() {

    inner class WaterEntryViewHolder(private val binding: ItemWaterEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: WaterEntry) {
            binding.tvAmount.text = "${entry.amountMl} ml"
            binding.tvTime.text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(entry.createdAt))
            
            binding.btnDelete.setOnClickListener {
                onDelete(entry)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WaterEntryViewHolder {
        val binding = ItemWaterEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WaterEntryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WaterEntryViewHolder, position: Int) {
        holder.bind(entries[position])
    }

    override fun getItemCount(): Int = entries.size

    fun replace(newEntries: List<WaterEntry>) {
        entries.clear()
        entries.addAll(newEntries)
        notifyDataSetChanged()
    }
}
