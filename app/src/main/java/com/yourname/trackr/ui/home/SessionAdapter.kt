package com.yourname.trackr.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yourname.trackr.R
import com.yourname.trackr.data.local.SessionEntity
import com.yourname.trackr.databinding.ItemSessionBinding
import com.yourname.trackr.model.CalorieCalculator
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

class SessionAdapter(
    private val weightKgProvider: () -> Float,
    private val onItemClick: (SessionEntity) -> Unit
) : ListAdapter<SessionEntity, SessionAdapter.SessionViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SessionViewHolder(binding, weightKgProvider, onItemClick)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SessionViewHolder(
        private val binding: ItemSessionBinding,
        private val weightKgProvider: () -> Float,
        private val onItemClick: (SessionEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(session: SessionEntity) {
            binding.textType.text = session.type
            binding.imageActivityIcon.setImageResource(iconFor(session.type))
            binding.imageActivityIcon.contentDescription = binding.root.context.getString(
                R.string.content_description_activity_icon_format, session.type
            )
            binding.textDate.text = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM, DateFormat.SHORT
            ).format(Date(session.startTime))
            binding.textDistance.text = String.format("%.2f km", session.distanceMeters / 1000f)
            binding.textDuration.text = formatDuration(session.endTime - session.startTime)

            val calories = CalorieCalculator.estimateCalories(
                session.type, weightKgProvider(), session.endTime - session.startTime
            )
            binding.textCalories.text = String.format("%.0f kcal", calories)

            if (session.effortRating > 0) {
                binding.ratingEffort.rating = session.effortRating.toFloat()
                binding.ratingEffort.contentDescription = binding.root.context.getString(
                    R.string.content_description_effort_rating_format, session.effortRating
                )
                binding.ratingEffort.visibility = View.VISIBLE
            } else {
                binding.ratingEffort.visibility = View.GONE
            }

            binding.root.setOnClickListener { onItemClick(session) }
        }

        private fun iconFor(activityType: String): Int = when (activityType.lowercase()) {
            "walk" -> R.drawable.ic_walk
            "cycle" -> R.drawable.ic_cycle
            else -> R.drawable.ic_run
        }

        private fun formatDuration(millis: Long): String {
            val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(millis)
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SessionEntity>() {
            override fun areItemsTheSame(oldItem: SessionEntity, newItem: SessionEntity) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: SessionEntity, newItem: SessionEntity) =
                oldItem == newItem
        }
    }
}
