package com.yourname.trackr.ui.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.yourname.trackr.R
import com.yourname.trackr.TrackrApplication
import com.yourname.trackr.data.local.SessionEntity
import com.yourname.trackr.databinding.ActivitySessionDetailBinding
import com.yourname.trackr.model.CalorieCalculator
import com.yourname.trackr.model.toTrackPoints
import com.yourname.trackr.ui.WeatherPresenter
import com.yourname.trackr.ui.fadeIn
import com.yourname.trackr.ui.graph.AccelerometerGraphActivity
import com.yourname.trackr.ui.setUpBasicMap
import com.yourname.trackr.ui.showStaticRoute
import com.yourname.trackr.viewmodel.DetailViewModel
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

class SessionDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySessionDetailBinding
    private val routeColor: Int by lazy { ContextCompat.getColor(this, R.color.trackr_accent) }

    private val viewModel: DetailViewModel by lazy {
        val app = application as TrackrApplication
        val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
        ViewModelProvider(
            this,
            DetailViewModel.Factory(app.sessionRepository, sessionId)
        )[DetailViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySessionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.mapView.setUpBasicMap()

        binding.buttonDelete.setOnClickListener { viewModel.deleteSession() }
        binding.buttonViewAccelData.setOnClickListener {
            val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
            val graphIntent = Intent(this, AccelerometerGraphActivity::class.java).apply {
                putExtra(AccelerometerGraphActivity.EXTRA_SESSION_ID, sessionId)
            }
            startActivity(graphIntent)
        }

        lifecycleScope.launch {
            viewModel.session.collect { session -> session?.let { bindSession(it) } }
        }
        lifecycleScope.launch {
            viewModel.deleted.collect { deleted -> if (deleted) finish() }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        binding.mapView.onPause()
        super.onPause()
    }

    private fun bindSession(session: SessionEntity) {
        binding.textType.text = session.type
        binding.textDate.text = DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM, DateFormat.SHORT
        ).format(Date(session.startTime))
        binding.textDistance.text = getString(R.string.distance_km_format, session.distanceMeters / 1000f)
        binding.textSteps.text = getString(R.string.steps_format, session.stepCount)
        binding.textDuration.text = formatDuration(session.endTime - session.startTime)

        val weightKg = (application as TrackrApplication).userPrefs.weightKg
        val calories = CalorieCalculator.estimateCalories(
            session.type, weightKg, session.endTime - session.startTime
        )
        binding.textCalories.text = getString(R.string.calories_format, calories)

        val points = session.pathJson.toTrackPoints().map { GeoPoint(it.lat, it.lng) }
        binding.mapView.showStaticRoute(points, routeColor)

        if (session.photoUri != null) {
            binding.imagePhoto.setImageURI(Uri.parse(session.photoUri))
            binding.imagePhoto.visibility = View.VISIBLE
        } else {
            binding.imagePhoto.visibility = View.GONE
        }

        val tempC = session.weatherTempC
        val weatherCode = session.weatherWeatherCode
        if (tempC != null && weatherCode != null) {
            val presentation = WeatherPresenter.present(weatherCode)
            binding.weatherCard.textWeatherTemp.text = getString(R.string.temp_c_format, tempC)
            binding.weatherCard.textWeatherCondition.text = presentation.label
            binding.weatherCard.imageWeatherIcon.setImageResource(presentation.iconRes)
            binding.weatherCard.root.fadeIn()
        } else {
            binding.weatherCard.root.visibility = View.GONE
        }

        binding.statsRow.fadeIn()
    }

    private fun formatDuration(millis: Long): String {
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    companion object {
        const val EXTRA_SESSION_ID = "session_id"
    }
}
