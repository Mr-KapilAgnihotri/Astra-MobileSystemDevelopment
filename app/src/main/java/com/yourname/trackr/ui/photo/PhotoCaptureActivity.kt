package com.yourname.trackr.ui.photo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import com.yourname.trackr.R
import com.yourname.trackr.TrackrApplication
import com.yourname.trackr.databinding.ActivityPhotoCaptureBinding
import com.yourname.trackr.ui.home.HomeActivity
import com.yourname.trackr.viewmodel.PhotoViewModel
import java.io.File

/**
 * Last stop after a session is saved: offers to attach a photo via the system
 * camera app (through a FileProvider-issued content:// Uri), then returns to
 * Home. Only ever receives session_id (a Long) via the Intent, per the
 * "primitive extras only" rule - the photo Uri is written back through
 * SessionRepository, never by handing a whole SessionEntity around.
 */
class PhotoCaptureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoCaptureBinding
    private var photoUri: Uri? = null
    private var hasPhoto = false

    private val sessionId: Long by lazy { intent.getLongExtra(EXTRA_SESSION_ID, -1L) }

    private val viewModel: PhotoViewModel by lazy {
        val app = application as TrackrApplication
        ViewModelProvider(this, PhotoViewModel.Factory(app.sessionRepository, sessionId))[PhotoViewModel::class.java]
    }

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            hasPhoto = true
            binding.imagePreview.setImageURI(photoUri)
            binding.imagePreview.visibility = View.VISIBLE
            updateButtonLabels()
            viewModel.savePhotoUri(photoUri.toString())
        }
        // On failure/cancel just stay on this screen - Take Photo/Skip are still available.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonTakePhoto.setOnClickListener { launchCamera() }
        binding.buttonSecondary.setOnClickListener {
            if (!hasPhoto) {
                viewModel.savePhotoUri(null)
            }
            goHome()
        }
    }

    private fun launchCamera() {
        val photosDir = File(filesDir, "photos").apply { mkdirs() }
        val file = File(photosDir, "session_$sessionId.jpg")
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        photoUri = uri
        takePicture.launch(uri)
    }

    private fun updateButtonLabels() {
        binding.buttonTakePhoto.text = getString(R.string.retake_photo)
        binding.buttonSecondary.text = getString(R.string.continue_label)
    }

    private fun goHome() {
        Toast.makeText(this, R.string.session_saved, Toast.LENGTH_SHORT).show()
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }

    companion object {
        const val EXTRA_SESSION_ID = "session_id"
    }
}
