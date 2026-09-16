package com.yourname.trackr.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yourname.trackr.R
import com.yourname.trackr.TrackrApplication
import com.yourname.trackr.data.UserPrefs
import com.yourname.trackr.data.local.SessionEntity
import com.yourname.trackr.databinding.ActivityHomeBinding
import com.yourname.trackr.databinding.DialogWeightPromptBinding
import com.yourname.trackr.ui.detail.SessionDetailActivity
import com.yourname.trackr.ui.tracking.TrackingActivity
import com.yourname.trackr.ui.weekly.WeeklyStatsActivity
import com.yourname.trackr.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var sessionAdapter: SessionAdapter

    private val userPrefs: UserPrefs by lazy { (application as TrackrApplication).userPrefs }

    private val viewModel: HomeViewModel by lazy {
        val app = application as TrackrApplication
        ViewModelProvider(this, HomeViewModel.Factory(app.sessionRepository))[HomeViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        sessionAdapter = SessionAdapter(
            weightKgProvider = { userPrefs.weightKg },
            onItemClick = { session -> openSessionDetail(session) }
        )
        binding.recyclerSessions.layoutManager = LinearLayoutManager(this)
        binding.recyclerSessions.adapter = sessionAdapter

        binding.buttonStartSession.setOnClickListener { startTrackingSession() }

        lifecycleScope.launch {
            viewModel.sessions.collect { sessions ->
                sessionAdapter.submitList(sessions)
                binding.textEmpty.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        if (!userPrefs.hasBeenPromptedForWeight) {
            showWeightDialog(isFirstRun = true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_weekly_stats -> {
                startActivity(Intent(this, WeeklyStatsActivity::class.java))
                true
            }
            R.id.action_settings -> {
                showWeightDialog(isFirstRun = false)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showWeightDialog(isFirstRun: Boolean) {
        val dialogBinding = DialogWeightPromptBinding.inflate(layoutInflater)
        if (!isFirstRun) {
            dialogBinding.editWeightKg.setText(userPrefs.weightKg.toString())
        }

        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.weight_dialog_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val entered = dialogBinding.editWeightKg.text?.toString()?.toFloatOrNull()
                if (entered != null && entered > 0f) {
                    userPrefs.weightKg = entered
                }
                userPrefs.hasBeenPromptedForWeight = true
                sessionAdapter.notifyDataSetChanged()
            }
            .setCancelable(!isFirstRun)

        if (isFirstRun) {
            builder.setNegativeButton(R.string.skip) { _, _ -> userPrefs.hasBeenPromptedForWeight = true }
        } else {
            builder.setNegativeButton(R.string.cancel, null)
        }

        builder.show()
    }

    private fun startTrackingSession() {
        val intent = Intent(this, TrackingActivity::class.java).apply {
            putExtra(TrackingActivity.EXTRA_ACTIVITY_TYPE, "Run")
        }
        startActivity(intent)
    }

    private fun openSessionDetail(session: SessionEntity) {
        val intent = Intent(this, SessionDetailActivity::class.java).apply {
            putExtra(SessionDetailActivity.EXTRA_SESSION_ID, session.id)
        }
        startActivity(intent)
    }
}
