package com.reschange.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.reschange.app.databinding.ItemPresetBinding

class PresetAdapter(
    private val presets: List<DevicePreset>,
    private val onApply: (DevicePreset) -> Unit,
    private val onCustom: () -> Unit
) : RecyclerView.Adapter<PresetAdapter.VH>() {

    inner class VH(private val b: ItemPresetBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(preset: DevicePreset) {
            b.tvName.text = preset.name
            if (preset.isCustom) {
                b.tvResolution.text = "Tap to set your own"
                b.tvDpi.text = ""
                b.btnApply.text = "Configure"
                b.btnApply.setOnClickListener { onCustom() }
            } else {
                b.tvResolution.text = "${preset.width}×${preset.height}"
                b.tvDpi.text = "${preset.dpi} dpi"
                b.btnApply.text = "Apply"
                b.btnApply.setOnClickListener { onApply(preset) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemPresetBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(presets[position])

    override fun getItemCount() = presets.size
}
