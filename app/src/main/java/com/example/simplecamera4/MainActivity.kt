package com.example.simplecamera4

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private lateinit var previewView: PreviewView
    private var remainingPhotos = 27
    private lateinit var filmCounterTextView: TextView
    private lateinit var filmChangeButton: Button
    private lateinit var takePhotoButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.viewFinder)
        takePhotoButton = findViewById(R.id.take_photo_button)
        filmCounterTextView = findViewById(R.id.film_counter)
        filmChangeButton = findViewById(R.id.film_change_button)
        updateFilmCounter()

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera(previewView)
            takePhotoButton.isEnabled = true // パーミッション取得後、撮影ボタンを有効化
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        takePhotoButton.setOnClickListener {
            takePhoto()
        }

        filmChangeButton.setOnClickListener {
            replaceFilm()
        }

        filmChangeButton.isEnabled = false // 初期状態ではフィルム交換ボタンを無効化
    }

    private fun replaceFilm() {
        remainingPhotos = 27
        updateFilmCounter()
        Toast.makeText(this, "フィルムが交換されました", Toast.LENGTH_SHORT).show()
        takePhotoButton.isEnabled = true // フィルム交換後は撮影可能にする
        filmChangeButton.isEnabled = false // 交換後はフィルム交換ボタンを無効化
    }

    private fun updateFilmCounter() {
        filmCounterTextView.text = "残り ${remainingPhotos}枚"

        // 27枚の時はフィルム交換ボタンを無効にする、26枚以下になったら有効化
        if (remainingPhotos == 27) {
            filmChangeButton.isEnabled = false
        } else {
            filmChangeButton.isEnabled = true
        }
    }

    private fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e("CameraApp", "カメラの初期化に失敗しました: ${exc.message}", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startCamera(previewView)
                takePhotoButton.isEnabled = true // パーミッション取得後、撮影ボタンを有効化
            } else {
                Toast.makeText(this, "カメラパーミッションが必要です", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun takePhoto() {
        if (remainingPhotos <= 0) {
            Toast.makeText(this, "フィルムがありません", Toast.LENGTH_SHORT).show()
            return
        }

        val imageCapture = imageCapture ?: run {
            Log.e("CameraApp", "imageCaptureがnullです")
            Toast.makeText(this, "カメラの準備ができていません", Toast.LENGTH_SHORT).show()
            return
        }

        val photoFile = File(externalMediaDirs.firstOrNull(), "${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraApp", "写真撮影に失敗しました: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    if (remainingPhotos > 0) {
                        remainingPhotos-- // 残り枚数を減らす
                    }
                    updateFilmCounter() // フィルムカウンタを更新

                    if (remainingPhotos <= 0) {
                        takePhotoButton.isEnabled = false // フィルムがなくなったらボタンを無効化
                    }

                    Toast.makeText(this@MainActivity, "写真が保存されました: ${photoFile.absolutePath}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
