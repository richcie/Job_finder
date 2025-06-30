package com.uilover.project196.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableField
import androidx.recyclerview.widget.RecyclerView
import com.uilover.project196.R
import com.uilover.project196.databinding.ViewholderFilterOptionBinding

// KRITERIA WAJIB: RecyclerView + Adapter (8/9) - Adapter untuk opsi filter
class FilterOptionAdapter(
    private val items: List<String>,
    private val clickListener: ClickListener,
    initialSelectedItems: List<String> = listOf()
) : RecyclerView.Adapter<FilterOptionAdapter.Viewholder>() {

    private var selectedPositions = mutableSetOf<Int>()
    private lateinit var context: Context






    val selectedCount = ObservableField<Int>(0)
    val hasSelections = ObservableField<Boolean>(false)

    init {

        updateInitialSelections(initialSelectedItems)
        updateReactiveState()
    }

    private fun updateInitialSelections(initialSelectedItems: List<String>) {
        selectedPositions.clear()
        initialSelectedItems.forEach { selectedItem ->
            val index = items.indexOf(selectedItem)
            if (index != -1) {
                selectedPositions.add(index)
            }
        }
    }

    private fun updateReactiveState() {
        selectedCount.set(selectedPositions.size)
        hasSelections.set(selectedPositions.isNotEmpty())
    }

    inner class Viewholder(val binding: ViewholderFilterOptionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterOptionAdapter.Viewholder {
        context = parent.context
        val binding = ViewholderFilterOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Viewholder(binding)
    }

    override fun onBindViewHolder(holder: FilterOptionAdapter.Viewholder, position: Int) {
        val item = items[position]
        holder.binding.optionText.text = item




        holder.binding.root.setOnClickListener {
            toggleSelection(position)
            notifyItemChanged(position)


            clickListener.onSelectionChanged(getSelectedItems())


            updateReactiveState()

            android.util.Log.d("FilterOptionAdapter", "Selection changed: ${getSelectedItems().size} items selected")
        }




        updateItemAppearance(holder, position)
    }

    private fun toggleSelection(position: Int) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position)
        } else {
            selectedPositions.add(position)
        }
    }

    private fun updateItemAppearance(holder: Viewholder, position: Int) {
        val isSelected = selectedPositions.contains(position)

        if (isSelected) {
            holder.binding.optionText.setBackgroundResource(R.drawable.purple_full_corner)
            holder.binding.optionText.setTextColor(ContextCompat.getColor(context, R.color.white))
        } else {
            holder.binding.optionText.setBackgroundResource(R.drawable.grey_full_corner_bg)
            holder.binding.optionText.setTextColor(ContextCompat.getColor(context, R.color.black))
        }
    }

    override fun getItemCount(): Int = items.size





    fun getSelectedItems(): List<String> {
        return selectedPositions.map { items[it] }
    }

    fun clearSelections() {
        val oldSelections = selectedPositions.toSet()
        selectedPositions.clear()


        oldSelections.forEach { notifyItemChanged(it) }


        clickListener.onSelectionChanged(getSelectedItems())


        updateReactiveState()

        android.util.Log.d("FilterOptionAdapter", "All selections cleared")
    }

    fun setSelections(newSelections: List<String>) {
        val oldSelections = selectedPositions.toSet()
        selectedPositions.clear()


        newSelections.forEach { selectedItem ->
            val index = items.indexOf(selectedItem)
            if (index != -1) {
                selectedPositions.add(index)
            }
        }


        val allChangedPositions = oldSelections.union(selectedPositions)
        allChangedPositions.forEach { notifyItemChanged(it) }


        clickListener.onSelectionChanged(getSelectedItems())


        updateReactiveState()

        android.util.Log.d("FilterOptionAdapter", "Selections updated: ${newSelections.size} items")
    }

    fun isSelected(item: String): Boolean {
        val index = items.indexOf(item)
        return index != -1 && selectedPositions.contains(index)
    }

    fun getSelectionCount(): Int = selectedPositions.size

    fun hasAnySelections(): Boolean = selectedPositions.isNotEmpty()




    interface ClickListener {
        fun onSelectionChanged(selectedItems: List<String>)


        fun onSelectionCleared() {

        }

        fun onSelectionAdded(item: String) {

        }

        fun onSelectionRemoved(item: String) {

        }
    }
}