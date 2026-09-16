package com.yourname.trackr.ui.weekly

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.yourname.trackr.TrackrApplication
import com.yourname.trackr.databinding.ActivityWeeklyStatsBinding
import com.yourname.trackr.viewmodel.WeeklyStatsViewModel
import kotlinx.coroutines.launch

class WeeklyStatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWeeklyStatsBinding

    private val viewModel: WeeklyStatsViewModel by lazy {
        val app = application as TrackrApplication
        ViewModelProvider(this, WeeklyStatsViewModel.Factory(app.sessionRepository))[WeeklyStatsViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeeklyStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            viewModel.weeklyDistances.collect { days ->
                binding.weeklyChart.setData(days)
            }
        }
    }
}
