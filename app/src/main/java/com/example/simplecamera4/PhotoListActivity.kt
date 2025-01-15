package com.example.simplecamera4

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.util.Log
import java.io.File

class PhotoListActivity : AppCompatActivity() {

    private val REQUEST_CODE_PERMISSION = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_list) // レイアウトリソースを指定

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // フォルダ名を受け取る
        val folderName = intent.getStringExtra("folderName")
        Log.d("PhotoListActivity", "Folder Name: $folderName") // フォルダ名をログに表示

        val photoLayout: LinearLayout = findViewById(R.id.photoLayout)

        // パーミッションの確認とリクエスト
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE_PERMISSION)
        } else {
            loadPhotos(folderName, photoLayout)
        }
    }

    // フォルダ内の写真を読み込む関数
    private fun loadPhotos(folderName: String?, photoLayout: LinearLayout) {
        folderName?.let {
            // フォルダのパスを取得
            val folderPath = File(getExternalFilesDir(null), it)
            Log.d("PhotoListActivity", "Folder Path: ${folderPath.absolutePath}") // フォルダパスをログに表示

            // フォルダが存在し、かつディレクトリであれば処理を行う
            if (folderPath.exists() && folderPath.isDirectory) {
                val photos = folderPath.listFiles()
                if (photos != null && photos.isNotEmpty()) {
                    Log.d("PhotoListActivity", "Number of photos: ${photos.size}") // 写真の数をログに表示
                    photos.forEach { photo ->
                        // .jpgファイルのみを対象に画像ビューを作成
                        if (photo.isFile && photo.extension.equals("jpg", ignoreCase = true)) {
                            val imageView = ImageView(this).apply {
                                setImageURI(Uri.fromFile(photo))  // 画像を読み込んで表示
                                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                                setPadding(16, 16, 16, 16)
                                setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
                                isClickable = true
                                isFocusable = true
                                setOnClickListener {
                                    Toast.makeText(this@PhotoListActivity, "写真: ${photo.name}", Toast.LENGTH_SHORT).show()
                                }
                            }
                            photoLayout.addView(imageView)
                        }
                    }
                } else {
                    Log.d("PhotoListActivity", "No photos found in folder.")
                    Toast.makeText(this, "フォルダ内に写真がありません", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.d("PhotoListActivity", "Folder does not exist or is not a directory.")
                Toast.makeText(this, "フォルダが見つかりませんでした", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 上に戻るボタンが押された時の処理
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    // パーミッションリクエスト結果の処理
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // パーミッションが許可された場合、写真を読み込む
            val folderName = intent.getStringExtra("folderName")
            val photoLayout: LinearLayout = findViewById(R.id.photoLayout)
            loadPhotos(folderName, photoLayout)
        } else {
            Toast.makeText(this, "パーミッションが許可されていません", Toast.LENGTH_SHORT).show()
        }
    }
}
