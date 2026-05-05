package com.example.vulpinetasks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.vulpinetasks.backend.SubTaskDto
import com.example.vulpinetasks.databinding.ItemSubtaskBinding

class SubtasksAdapter(
    private var subtasks: List<SubTaskDto>,
    private val onToggleComplete: (Int, Boolean) -> Unit,
    private val onDeleteSubtask: (Int) -> Unit
) : RecyclerView.Adapter<SubtasksAdapter.ViewHolder>() {

    fun updateList(newList: List<SubTaskDto>) {
        subtasks = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSubtaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(subtasks[position], position)
    }

    override fun getItemCount() = subtasks.size

    inner class ViewHolder(private val binding: ItemSubtaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(subtask: SubTaskDto, position: Int) {
            binding.checkboxSubtask.text = subtask.title
            binding.checkboxSubtask.isChecked = subtask.isCompleted

            binding.checkboxSubtask.setOnCheckedChangeListener { _, isChecked ->
                onToggleComplete(position, isChecked)
            }

            binding.btnDeleteSubtask.setOnClickListener {
                onDeleteSubtask(position)
            }
        }
    }
}