package co.edu.uniandes.twnel.view

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import co.edu.uniandes.twnel.R
import co.edu.uniandes.twnel.databinding.ActivityMainBinding
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.imageResource
import org.jetbrains.anko.toast
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val REQUEST_CODE = 12
private const val FILE_NAME = "Photo.jpg"
private lateinit var photoFile: File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraSelector: CameraSelector
    private var imageCapture: ImageCapture? = null
    private lateinit var imgCaptureExecutor: ExecutorService
    private var flagCameraX = false
    private val dialog: AlertDialog by lazy {
        AlertDialog.Builder(this).setMessage("Processing image. Please, wait a moment...").setCancelable(false).create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        imgCaptureExecutor = Executors.newSingleThreadExecutor()

        val requestCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission ->
            if (permission) {
                if (!flagCameraX) {
                    val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    photoFile = getPhotoFile(FILE_NAME)

                    val fileProvider = FileProvider.getUriForFile(this, "co.edu.uniandes.twnel.fileprovider", photoFile)
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)
                    if (takePictureIntent.resolveActivity(this.packageManager) != null)
                        startActivityForResult(takePictureIntent, REQUEST_CODE)
                    else
                        toast("Unable to open camera")
                } else
                    startCamera()
            } else {
                toast("Permission denied")
            }
        }

        binding.btnCamera.setOnClickListener {
            requestCamera.launch(Manifest.permission.CAMERA)
        }

        binding.btnTakePhoto.setOnClickListener{
            dialog.show()
            takePhoto()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                animateFlash()
            }
        }

        binding.switchCam.setOnClickListener {
            flagCameraX = !flagCameraX
            if (flagCameraX) {
                binding.tvLabelCamera.text = getString(R.string.camera_x_api)
                binding.btnTakePhoto.visibility = View.VISIBLE
            }
            else {
                binding.tvLabelCamera.text = getString(R.string.camera_app)
                binding.btnTakePhoto.visibility = View.GONE
            }
        }

    }

    private fun takePhoto() {
        imageCapture?.let{
            //Create a storage location whose fileName is timestamped in milliseconds.
            val fileName = "JPEG_${System.currentTimeMillis()}.jpg"
            val file = File(externalMediaDirs[0],fileName)

            // Save the image in the above file
            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()

            /* pass in the details of where and how the image is taken.(arguments 1 and 2 of takePicture)
            pass in the details of what to do after an image is taken.(argument 3 of takePicture) */

            it.takePicture(
                outputFileOptions,
                imgCaptureExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults){
                        Log.i(TAG,"The image has been saved in ${file.toUri()}")
                        val takenImage = BitmapFactory.decodeFile(file.absolutePath)
                        detectFace(takenImage)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        toast("Error taking photo")
                        Log.d(TAG, "Error taking photo:$exception")
                    }

                })
        }
    }

    private fun startCamera() {
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            imageCapture = ImageCapture.Builder().build()

            val preview = Preview.Builder().build().also{
                it.setSurfaceProvider(binding.preview.surfaceProvider)
            }
            try{
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this,cameraSelector,preview, imageCapture)
            } catch (e: Exception) {
                Log.d(TAG, "Use case binding failed")
            }

        }, ContextCompat.getMainExecutor(this))
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun animateFlash() {
        binding.root.postDelayed({
            binding.root.foreground = ColorDrawable(Color.WHITE)
            binding.root.postDelayed({
                binding.root.foreground = null
            }, 50)
        }, 100)
    }

    private fun getPhotoFile(fileName: String): File {
        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpg", storageDirectory)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            dialog.show()
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
                if (dialog.isShowing)
                    dialog.dismiss()
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
                if (dialog.isShowing)
                    dialog.dismiss()
                binding.ivPreviewPhoto.imageResource = R.drawable.noimageavailable
                binding.ivPreviewPhoto.snackbar("There was an error detecting the face")
                Log.d("Error", e.toString())
            }
    }

    companion object {
        const val TAG = "MainActivity"
    }
}