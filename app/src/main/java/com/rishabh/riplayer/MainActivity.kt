package com.rishabh.riplayer

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.EditText
import android.widget.GridLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var videoList: MutableList<MediaItemData>
    private lateinit var audioList: MutableList<MediaItemData>
    private lateinit var adapter: MediaAdapter
    private lateinit var recyclerView: RecyclerView
    private var currentTab = "video"

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        videoList = mutableListOf()
        audioList = mutableListOf()

        recyclerView = findViewById(R.id.mediaGrid)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        adapter = MediaAdapter(videoList) { item -> playMedia(item.uri, item.title) }
        recyclerView.adapter = adapter

        findViewById<android.widget.ImageButton>(R.id.btnStream).setOnClickListener {
            showStreamUrlDialog()
        }
        findViewById<android.widget.ImageButton>(R.id.btnSearch).setOnClickListener {
            Toast.makeText(this, "Search coming soon", Toast.LENGTH_SHORT).show()
        }

        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_video -> {
                    currentTab = "video"
                    showList(videoList)
                    true
                }
                R.id.nav_audio -> {
                    currentTab = "audio"
                    showList(audioList)
                    true
                }
                R.id.nav_browse -> {
                    Toast.makeText(this, "Folder browsing coming soon", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_playlists -> {
                    Toast.makeText(this, "Playlists coming soon", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_more -> {
                    Toast.makeText(this, "Ri Player - Made by \u0930\u0937HABH", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }

        checkPermissionsAndLoadMedia()
    }

    private fun showStreamUrlDialog() {
        val input = EditText(this)
        input.hint = "Enter stream URL (http, https, rtsp)"
        AlertDialog.Builder(this)
            .setTitle("Play Network Stream")
            .setView(input)
            .setPositiveButton("Play") { _, _ ->
                val url = input.text?.toString()?.trim()
                if (!url.isNullOrEmpty()) {
                    playMedia(Uri.parse(url), "Network Stream")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showList(list: List<MediaItemData>) {
        adapter = MediaAdapter(list) { item -> playMedia(item.uri, item.title) }
        recyclerView.adapter = adapter
        findViewById<android.widget.TextView>(R.id.emptyText).visibility =
            if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun checkPermissionsAndLoadMedia() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val notGranted = permission.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            loadLocalMedia()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            loadLocalMedia()
        }
    }

    private fun loadLocalMedia() {
        videoList.clear()
        audioList.clear()
        loadVideos()
        loadAudio()
        showList(videoList)
    }

    private fun loadVideos() {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION
        )
        contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection, null, null,
            MediaStore.Video.Media.DATE_ADDED + " DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol)
                val duration = cursor.getLong(durCol)
                val uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
                videoList.add(MediaItemData(name, uri, "video", duration))
            }
        }
    }

    private fun loadAudio() {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION
        )
        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection, null, null,
            MediaStore.Audio.Media.DATE_ADDED + " DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol)
                val duration = cursor.getLong(durCol)
                val uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString())
                audioList.add(MediaItemData(name, uri, "audio", duration))
            }
        }
    }

    private fun playMedia(uri: Uri, title: String) {
        val intent = Intent(this, PlayerActivity::class.java)
        intent.putExtra("media_uri", uri.toString())
        intent.putExtra("media_title", title)
        startActivity(intent)
    }
}

data class MediaItemData(val title: String, val uri: Uri, val type: String, val durationMs: Long = 0L)
