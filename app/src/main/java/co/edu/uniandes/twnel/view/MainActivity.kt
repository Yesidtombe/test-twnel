package co.edu.uniandes.twnel.view

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import co.edu.uniandes.twnel.R
import co.edu.uniandes.twnel.databinding.ActivityMainBinding
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.imageResource
import org.jetbrains.anko.toast
import java.io.File

private const val REQUEST_CODE = 12
private const val FILE_NAME = "Photo.jpg"
private lateinit var photoFile: File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val requestCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission ->
            if (permission) {
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                photoFile = getPhotoFile(FILE_NAME)

                //takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoFile)
                val fileProvider = FileProvider.getUriForFile(this, "co.edu.uniandes.twnel.fileprovider", photoFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)
                if (takePictureIntent.resolveActivity(this.packageManager) != null)
                    startActivityForResult(takePictureIntent, REQUEST_CODE)
                else
                    toast("Unable to open camera")
            } else {
                toast("Permission denied")
            }
        }

        binding.btnCamera.setOnClickListener {
            requestCamera.launch(Manifest.permission.CAMERA)
        }

    }

    private fun getPhotoFile(fileName: String): File {
        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpg", storageDirectory)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            //val imageThumbnail = data?.extras?.get("data") as Bitmap
            val takenImage = BitmapFactory.decodeFile(photoFile.absolutePath)
            detectFace(takenImage)
        } else
            super.onActivityResult(requestCode, resultCode, data)
    }

    private fun detectFace(imagebit: Bitmap) {

        val image = FirebaseVisionImage.fromBitmap(imagebit)

        val options = FirebaseVisionFaceDetectorOptions.Builder()
            .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
            .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
            .build()

        val detector = FirebaseVision.getInstance()
            .getVisionFaceDetector(options)

        detector.detectInImage(image)
            .addOnSuccessListener { faces ->
                Log.d("Faces detected", faces.size.toString())
                if (faces.size > 0) {
                    binding.ivPreviewPhoto.setImageBitmap(imagebit)
                    toast("Face detected")
                }
                else {
                    binding.ivPreviewPhoto.imageResource = R.drawable.noimageavailable
                    binding.ivPreviewPhoto.snackbar("No face detected. Please, try take a photo again")
                }
            }
            .addOnFailureListener { e ->
                binding.ivPreviewPhoto.imageResource = R.drawable.noimageavailable
                binding.ivPreviewPhoto.snackbar("There was an error detecting the face")
                Log.d("Error", e.toString())
            }
    }
}