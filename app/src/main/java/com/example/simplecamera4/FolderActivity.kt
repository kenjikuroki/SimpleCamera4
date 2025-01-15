package com.example.simplecamera4

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent

class FolderActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folder)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val folderLayout: LinearLayout = findViewById(R.id.folderLayout)
        val sharedPreferences: SharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val folders = sharedPreferences.getStringSet("Folders", emptySet())

        folders?.forEach { folderInfo ->
            val (folderName, creationDate) = folderInfo.split(",")
            val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            val dateText = dateFormat.format(Date(creationDate.toLong()))

            val folderView = TextView(this).apply {
                text = "$folderName\nフィルム交換日: $dateText"
                textSize = 18f
                setPadding(16, 16, 16, 16)
                setBackgroundResource(android.R.drawable.dialog_holo_light_frame) // 背景を設定
                isClickable = true
                isFocusable = true

                // クリックイベントを設定
                setOnClickListener {
                    // クリックしたフォルダの名前を取得
                    val folderName = folderName

                    // フォルダに対応する写真リストのアクティビティに遷移
                    val intent = Intent(this@FolderActivity, PhotoListActivity::class.java).apply {
                        putExtra("folderName", folderName)  // フォルダ名をIntentに渡す
                    }
                    startActivity(intent)  // 写真リストアクティビティを開始
                    Toast.makeText(this@FolderActivity, "$folderName をクリックしました", Toast.LENGTH_SHORT).show()
                }

            }
            folderLayout.addView(folderView)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
