import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.util.Log
import java.io.File
import com.example.simplecamera4.R


class PhotoListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_list)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val folderName = intent.getStringExtra("folderName")
        val photoLayout: LinearLayout = findViewById(R.id.photoLayout)

        if (folderName != null) {
            loadPhotos(folderName, photoLayout)
        } else {
            Toast.makeText(this, "フォルダ名が見つかりません", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPhotos(folderName: String, photoLayout: LinearLayout) {
        // 内部ストレージの指定ディレクトリを取得
        val appDataDir = File(filesDir, "data/$folderName")
        Log.d("PhotoListActivity", "App Data Dir: ${appDataDir.absolutePath}")

        if (appDataDir.exists() && appDataDir.isDirectory) {
            val photos = appDataDir.listFiles { file -> file.isFile && file.extension.equals("jpg", true) }
            if (photos != null && photos.isNotEmpty()) {
                photos.forEach { photo ->
                    val bitmap = BitmapFactory.decodeFile(photo.absolutePath)
                    val imageView = ImageView(this).apply {
                        setImageBitmap(bitmap)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        setPadding(16, 16, 16, 16)
                        isClickable = true
                        isFocusable = true
                        setOnClickListener {
                            Toast.makeText(this@PhotoListActivity, "写真: ${photo.name}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    photoLayout.addView(imageView)
                }
            } else {
                Toast.makeText(this, "フォルダ内に写真がありません", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "フォルダが見つかりませんでした", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
