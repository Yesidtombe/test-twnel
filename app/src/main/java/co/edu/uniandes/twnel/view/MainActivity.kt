package co.edu.uniandes.twnel.view

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import co.edu.uniandes.twnel.R
import co.edu.uniandes.twnel.databinding.ActivityMainBinding
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        detectFace()
    }

    private fun detectFace() {
        val bitmap = BitmapFactory.decodeResource(
            this.applicationContext.resources,
            R.drawable.face
        )

        val image = FirebaseVisionImage.fromBitmap(bitmap)

        val options = FirebaseVisionFaceDetectorOptions.Builder()
            .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
            .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
            .build()

        val detector = FirebaseVision.getInstance()
            .getVisionFaceDetector(options)

        detector.detectInImage(image)
            .addOnSuccessListener { faces ->
                Log.d("Faces detected", faces.size.toString())
                for (i in faces.indices)
                    draw(faces[0], bitmap)
            }
            .addOnFailureListener { e ->
                Log.d("Error", e.toString())
            }
    }

    private fun draw(
        face: FirebaseVisionFace,
        myBitmap: Bitmap
    ) {

        val tempBitmap = Bitmap.createBitmap(myBitmap.width, myBitmap.height, Bitmap.Config.RGB_565)
        val canvas = Canvas(tempBitmap)

        canvas.drawBitmap(myBitmap, 0f, 0f, null)

        val selectedColor = Color.RED
        val facePositionPaint = Paint()
        facePositionPaint.color = selectedColor

        val boxPaint = Paint()
        boxPaint.color = selectedColor
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = BOX_STROKE_WIDTH

        val x = face.boundingBox.centerX().toFloat()
        val y = face.boundingBox.centerY().toFloat()

        val xOffset = face.boundingBox.width() / 2.0f
        val yOffset = face.boundingBox.height() / 2.0f
        val left = x - xOffset
        val top = y - yOffset
        val right = x + xOffset
        val bottom = y + yOffset
        canvas.drawRect(left, top, right, bottom, boxPaint)

        val contour = face.getContour(FirebaseVisionFaceContour.ALL_POINTS)
        for (point in contour.points) {
            val px = point.x
            val py = point.y
            canvas.drawCircle(px, py, FACE_POSITION_RADIUS, facePositionPaint)
        }

        binding.ivPreviewPhoto.setImageDrawable(BitmapDrawable(resources, tempBitmap))
    }

    companion object {
        private const val FACE_POSITION_RADIUS = 10.0f
        private const val BOX_STROKE_WIDTH = 15.0f
    }
}