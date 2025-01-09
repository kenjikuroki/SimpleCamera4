package com.example.simplecamera4

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class FolderActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folder)

        val listView: ListView = findViewById(R.id.listView)
        val folderPath = intent.getStringExtra("FOLDER_PATH")
        val folder = File(folderPath ?: "")

        if (folder.exists() && folder.isDirectory) {
            val files = folder.listFiles()?.map { it.name } ?: emptyList()
            val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, files)
            listView.adapter = adapter
        }
    }
}