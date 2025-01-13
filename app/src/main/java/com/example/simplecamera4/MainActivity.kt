package com.example.simplecamera4

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.os.Environment
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.graphics.BitmapFactory;
import java.io.FileOutputStream


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

        openFolderButton.setOnClickListener {
            openPhotoFolder()
        }

        filmChangeButton.isEnabled = false // 初期状態ではフィルム交換ボタンを無効化
    }

private fun replaceFilm() {
    remainingPhotos = 27
    updateFilmCounter()
    Toast.makeText(this, "フィルムが交換されました", Toast.LENGTH_SHORT).show()
    takePhotoButton.isEnabled = true // フィルム交換後は撮影可能にする
    filmChangeButton.isEnabled = false // 交換後はフィルム交換ボタンを無効化

    val folderName = "PhotoFolder_${System.currentTimeMillis()}"
    val folderPath = createPhotoFolder(folderName)
    val creationDate = System.currentTimeMillis()
    saveFolderInfo(folderName, creationDate)
}

private fun createPhotoFolder(folderName: String): String {
    val folder = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), folderName)
    if (!folder.exists()) {
        folder.mkdirs()
    }
    return folder.absolutePath
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

                    // 保存された画像にエモいエフェクトを適用
                    val originalBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    val retroBitmap = CameraUtil.addRetroEffect(originalBitmap)

                    // エモい画質を保存
                    val retroFile = File(externalMediaDirs.firstOrNull(), "retro_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(retroFile).use { fos ->
                        retroBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                    }

                    Toast.makeText(this@MainActivity, "エモい写真が保存されました: ${retroFile.absolutePath}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }




    object CameraUtil {

        // 写真にエモい画質を追加するメソッド
        fun addRetroEffect(originalImage: Bitmap): Bitmap {
            // コントラスト調整
            var image = adjustContrast(originalImage)

            // フィルムノイズを追加
            image = addFilmNoise(image)

            // 色温度調整
            image = adjustColorTemperature(image)

            // 日付追加（右下に表示）
            image = addDateToImage(image)

            return image
        }

        // コントラストを強調
        private fun adjustContrast(image: Bitmap): Bitmap {
            val mutableImage = image.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableImage)
            val paint = Paint()

            // コントラストを強調するためにカラーを調整
            val contrastMatrix = android.graphics.ColorMatrix()
            contrastMatrix.setSaturation(1.2f) // 色の強さを強調
            paint.colorFilter = android.graphics.ColorMatrixColorFilter(contrastMatrix)

            canvas.drawBitmap(image, 0f, 0f, paint)
            return mutableImage
        }

        // フィルムノイズを加える
        private fun addFilmNoise(image: Bitmap): Bitmap {
            val mutableImage = image.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableImage)
            val paint = Paint()

            // ノイズを追加
            for (x in 0 until image.width) {
                for (y in 0 until image.height) {
                    val randomNoise = (Math.random() * 40).toInt() // ノイズ強度
                    val pixelColor = image.getPixel(x, y)
                    val red = Math.min(255, android.graphics.Color.red(pixelColor) + randomNoise)
                    val green = Math.min(255, android.graphics.Color.green(pixelColor) + randomNoise)
                    val blue = Math.min(255, android.graphics.Color.blue(pixelColor) + randomNoise)
                    mutableImage.setPixel(x, y, android.graphics.Color.rgb(red, green, blue))
                }
            }

            return mutableImage
        }

        // 色温度を調整
        private fun adjustColorTemperature(image: Bitmap): Bitmap {
            val mutableImage = image.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableImage)
            val paint = Paint()

            // 色温度調整（黄色みを加える）
            val colorMatrix = android.graphics.ColorMatrix()
            colorMatrix.setScale(1f, 1f, 0.9f, 1f) // 青色成分を少し減らす
            paint.colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)

            canvas.drawBitmap(image, 0f, 0f, paint)
            return mutableImage
        }

        // 右下に日付を追加
        private fun addDateToImage(originalImage: Bitmap): Bitmap {
            val dateFormat = SimpleDateFormat("yyyy.MM.dd")
            val currentDate = dateFormat.format(Date())

            val mutableImage = originalImage.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableImage)
            val paint = Paint()
            paint.color = android.graphics.Color.WHITE
            paint.textSize = 40f
            paint.textAlign = Paint.Align.RIGHT
            paint.isAntiAlias = true

            val x = mutableImage.width - 20
            val y = mutableImage.height - 20
            canvas.drawText(currentDate, x.toFloat(), y.toFloat(), paint)

            return mutableImage
        }
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