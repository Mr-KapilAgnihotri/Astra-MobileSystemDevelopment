package com.yourname.trackr.ui.graph

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.yourname.trackr.R
import com.yourname.trackr.TrackrApplication
import com.yourname.trackr.databinding.ActivityAccelGraphBinding
import com.yourname.trackr.viewmodel.AccelGraphViewModel
import kotlinx.coroutines.launch

class AccelerometerGraphActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccelGraphBinding

    private val viewModel: AccelGraphViewModel by lazy {
        val app = application as TrackrApplication
        val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
        ViewModelProvider(
            this,
            AccelGraphViewModel.Factory(app.sessionRepository, sessionId)
        )[AccelGraphViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccelGraphBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            viewModel.samples.collect { samples ->
                binding.accelChart.setSamples(samples)
                binding.textEmpty.visibility = if (samples.isEmpty()) View.VISIBLE else View.GONE
                binding.textPeak.text = getString(R.string.accel_peak_format, viewModel.peakValue)
                binding.textAverage.text = getString(R.string.accel_average_format, viewModel.averageValue)
            }
        }
    }

    companion object {
        const val EXTRA_SESSION_ID = "session_id"
    }
}
