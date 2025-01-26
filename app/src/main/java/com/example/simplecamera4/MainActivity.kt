package com.example.simplecamera4

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
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
    private lateinit var openFolderButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.viewFinder)
        takePhotoButton = findViewById(R.id.take_photo_button)
        filmCounterTextView = findViewById(R.id.film_counter)
        filmChangeButton = findViewById(R.id.film_change_button)
        openFolderButton = findViewById(R.id.open_folder_button)
        updateFilmCounter()

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera(previewView)
            takePhotoButton.isEnabled = true
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        takePhotoButton.setOnClickListener {
            takePhoto()
        }

        filmChangeButton.setOnClickListener {
            replaceFilm()
        }

        openFolderButton.setOnClickListener {
            openPhotoFolder()
        }

        filmChangeButton.isEnabled = false
    }

    private fun replaceFilm() {
        remainingPhotos = 27
        updateFilmCounter()
        Toast.makeText(this, "フィルムが交換されました", Toast.LENGTH_SHORT).show()
        takePhotoButton.isEnabled = true
        filmChangeButton.isEnabled = false

        val folderName = "PhotoFolder_${System.currentTimeMillis()}"
        val folderPath: File = createPhotoFolder(folderName)
        val creationDate = System.currentTimeMillis()
        saveFolderInfo(folderName, creationDate)
    }

    private fun createPhotoFolder(folderName: String): File {
        val folder = File(filesDir, "data/$folderName") // "com/example/simplecamera4" は不要
        if (!folder.exists()) {
            folder.mkdirs()
        }
        return folder
    }


    private fun saveFolderInfo(folderName: String, creationDate: Long) {
        val sharedPreferences: SharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val folders = sharedPreferences.getStringSet("Folders", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        folders.add("$folderName,$creationDate")
        sharedPreferences.edit {
            putStringSet("Folders", folders)
            apply()
        }
    }

    private fun openPhotoFolder() {
        val intent = Intent(this, FolderActivity::class.java)
        startActivity(intent)
    }

    private fun updateFilmCounter() {
        filmCounterTextView.text = "残り ${remainingPhotos}枚"
        filmChangeButton.isEnabled = remainingPhotos < 27
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
                takePhotoButton.isEnabled = true
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

        // 写真撮影後のフォルダ作成
        val folderPath: File
        if (remainingPhotos == 27) {
            // フィルム交換後、新しいフォルダを作成
            val folderName = "PhotoFolder_${System.currentTimeMillis()}"
            folderPath = createPhotoFolder(folderName)
            saveFolderInfo(folderName, System.currentTimeMillis())
        } else {
            // それ以外の時には直近のフォルダに保存
            folderPath = File(filesDir, "data") // 直近のフォルダ
        }

        // 写真ファイルの保存先を指定
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(Date())
        val photoFile = File(folderPath, "$timestamp.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraApp", "写真撮影に失敗しました: ${exc.message}", exc)
                    Toast.makeText(this@MainActivity, "写真撮影に失敗しました: ${exc.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d("MainActivity", "写真が保存されました: ${photoFile.absolutePath}")
                    if (remainingPhotos > 0) {
                        remainingPhotos--
                    }
                    updateFilmCounter()

                    if (remainingPhotos <= 0) {
                        takePhotoButton.isEnabled = false
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

