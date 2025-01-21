package com.example.simplecamera4

import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
                setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
                isClickable = true
                isFocusable = true

                setOnClickListener {
                    val intent = Intent(this@FolderActivity, PhotoListActivity::class.java).apply {
                        putExtra("folderName", folderName)
                    }
                    startActivity(intent)
                    Toast.makeText(this@FolderActivity, "$folderName をクリックしました", Toast.LENGTH_SHORT).show()
                }

                setOnLongClickListener {
                    AlertDialog.Builder(this@FolderActivity)
                        .setTitle("フォルダ削除")
                        .setMessage("フォルダ $folderName を削除しますか？")
                        .setPositiveButton("削除") { dialog, which ->
                            removeFolder(folderName, sharedPreferences)
                            folderLayout.removeView(this)
                            Toast.makeText(this@FolderActivity, "$folderName を削除しました", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("キャンセル", null)
                        .show()
                    true
                }
            }
            folderLayout.addView(folderView)
        }
    }

    private fun removeFolder(folderName: String, sharedPreferences: SharedPreferences) {
        val folders = sharedPreferences.getStringSet("Folders", mutableSetOf())?.toMutableSet()
        folders?.removeIf { it.startsWith(folderName) }
        sharedPreferences.edit().putStringSet("Folders", folders).apply()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}