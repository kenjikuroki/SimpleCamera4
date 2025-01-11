package com.example.simplecamera4

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.widget.ArrayAdapter
import android.widget.ListView
import java.io.File

class FolderActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folder)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val listView: ListView = findViewById(R.id.listView)
        val folderPath = intent.getStringExtra("FOLDER_PATH")
        val folder = File(folderPath ?: "")

        if (folder.exists() && folder.isDirectory) {
            val files = folder.listFiles()?.map { it.name } ?: emptyList()
            val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, files)
            listView.adapter = adapter
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}