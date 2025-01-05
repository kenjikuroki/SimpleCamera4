package com.example.simplecamera4

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import android.util.Log

class MainActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private lateinit var previewView: PreviewView
    private var remainingPhotos = 27 // フィルムカウンターの変数
    private lateinit var filmCounterTextView: TextView // TextViewの変数

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.viewFinder)
        val takePhotoButton: Button = findViewById(R.id.take_photo_button)
        filmCounterTextView = findViewById(R.id.film_counter) // TextViewの初期化
        updateFilmCounter() // 初期値を表示

        cameraExecutor = Executors.newSingleThreadExecutor()

        // パーミッションが許可されているかを確認
        if (allPermissionsGranted()) {
            startCamera() // カメラの初期化
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        takePhotoButton.setOnClickListener {
            takePhoto()
        }
    }

    // フィルムカウンターの更新
    private fun updateFilmCounter() {
        filmCounterTextView.text = "残り ${remainingPhotos}枚"
    }

    // パーミッションが許可されているか確認するメソッド
    private fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // カメラを初期化するメソッド
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Previewの設定
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            // ImageCaptureの設定
            imageCapture = ImageCapture.Builder().build()

            // カメラの選択とビルド
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                // バインドする前に既存のカメラがバインドされていれば解除する
                cameraProvider.unbindAll()

                // カメラをバインド
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
                startCamera() // パーミッションが許可された場合のみカメラを起動
            } else {
                Toast.makeText(this, "カメラパーミッションが必要です", Toast.LENGTH_SHORT).show()
                finish() // パーミッションが拒否された場合はアクティビティを終了
            }
        }
    }

    private fun takePhoto() {
        // 残り写真が0枚の場合はシャッターを切れないようにする
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
                        remainingPhotos-- // カウンターを減らす（0より大きい場合のみ）
                    }
                    updateFilmCounter() // 表示を更新

                    // 残り写真が0になったらボタンを無効化
                    if (remainingPhotos <= 0) {
                        disableShutterButton() // シャッターボタンを無効化
                        Toast.makeText(this@MainActivity, "フィルムがありません", Toast.LENGTH_SHORT).show() // フィルムがなくなった通知
                    }

                    Toast.makeText(this@MainActivity, "写真が保存されました: ${photoFile.absolutePath}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }


    // シャッターボタンを無効化するメソッド
    private fun disableShutterButton() {
        val takePhotoButton: Button = findViewById(R.id.take_photo_button)
        takePhotoButton.isEnabled = false // ボタンを無効化
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
