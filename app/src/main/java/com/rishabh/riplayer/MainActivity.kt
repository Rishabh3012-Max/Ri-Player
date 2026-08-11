package com.rishabh.riplayer

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var videoList: MutableList<MediaItemData>
    private lateinit var audioList: MutableList<MediaItemData>
    private lateinit var adapter: MediaAdapter
    private lateinit var recyclerView: RecyclerView
    private var currentTab = "video"

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val MANAGE_STORAGE_REQUEST_CODE = 101

        private val VIDEO_EXTENSIONS = setOf(
            "mp4", "mkv", "avi", "mov", "webm", "3gp", "flv", "wmv", "m4v", "ts", "mpg", "mpeg"
        )
        private val AUDIO_EXTENSIONS = setOf(
            "mp3", "m4a", "wav", "flac", "ogg", "aac", "wma", "opus"
        )

        // Common folders that hold user media on almost every Android phone
        private val SCAN_FOLDERS = listOf(
            Environment.DIRECTORY_MOVIES,
            Environment.DIRECTORY_DOWNLOADS,
            Environment.DIRECTORY_DCIM,
            Environment.DIRECTORY_MUSIC,
            "WhatsApp/Media/WhatsApp Video",
            "WhatsApp/Media/WhatsApp Audio",
            "Telegram/Telegram Video",
            "Telegram/Telegram Audio"
        )
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

        findViewById<ImageButton>(R.id.btnStream).setOnClickListener {
            showStreamUrlDialog()
        }
        findViewById<ImageButton>(R.id.btnSearch).setOnClickListener {
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

    override fun onResume() {
        super.onResume()
        // Covers the case where the user just granted "All files access" in Settings and came back
        if (hasFullStorageAccess() && videoList.isEmpty() && audioList.isEmpty()) {
            loadLocalMedia()
        }
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
        findViewById<TextView>(R.id.emptyText).visibility =
            if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun hasFullStorageAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkPermissionsAndLoadMedia() {
        // Runtime media permissions (needed to open/play files even with full access)
        val runtimePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val notGranted = runtimePermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), PERMISSION_REQUEST_CODE)
            return
        }

        // "All files access" needed for direct folder scanning (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            AlertDialog.Builder(this)
                .setTitle("One more permission")
                .setMessage("To find all your video and audio files reliably, Ri Player needs 'All files access'. Tap Allow on the next screen.")
                .setPositiveButton("Continue") { _, _ ->
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = Uri.parse("package:$packageName")
                        startActivityForResult(intent, MANAGE_STORAGE_REQUEST_CODE)
                    } catch (e: Exception) {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        startActivityForResult(intent, MANAGE_STORAGE_REQUEST_CODE)
                    }
                }
                .setNegativeButton("Skip") { _, _ -> loadLocalMedia() }
                .setCancelable(false)
                .show()
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
            checkPermissionsAndLoadMedia()
        }
    }

    private fun loadLocalMedia() {
        videoList.clear()
        audioList.clear()

        // Primary: scan common public folders directly (works even if MediaStore hasn't indexed new files)
        scanFolders()

        // Fallback: also merge in anything MediaStore already knows about, avoiding duplicates
        mergeFromMediaStore()

        showList(if (currentTab == "audio") audioList else videoList)
    }

    private fun scanFolders() {
        val root = Environment.getExternalStorageDirectory()
        for (folder in SCAN_FOLDERS) {
            val dir = File(root, folder)
            scanDirectory(dir, depth = 0)
        }
    }

    private fun scanDirectory(dir: File, depth: Int) {
        if (depth > 4) return
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                scanDirectory(file, depth + 1)
            } else {
                val ext = file.extension.lowercase()
                when {
                    VIDEO_EXTENSIONS.contains(ext) -> addIfNew(videoList, file)
                    AUDIO_EXTENSIONS.contains(ext) -> addIfNew(audioList, file)
                }
            }
        }
    }

    private fun addIfNew(list: MutableList<MediaItemData>, file: File) {
        if (list.any { it.uri.toString().endsWith(file.name) && it.title == file.name }) return
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        list.add(MediaItemData(file.name, uri, if (list === videoList) "video" else "audio", 0L))
    }

    private fun mergeFromMediaStore() {
        loadFromCursor(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            videoList
        )
        loadFromCursor(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            audioList
        )
    }

    private fun loadFromCursor(
        contentUri: Uri,
        nameColumn: String,
        durationColumn: String,
        target: MutableList<MediaItemData>
    ) {
        val projection = arrayOf(MediaStore.MediaColumns._ID, nameColumn, durationColumn)
        contentResolver.query(contentUri, projection, null, null, "$nameColumn ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(nameColumn)
            val durCol = cursor.getColumnIndexOrThrow(durationColumn)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: continue
                if (target.any { it.title == name }) continue // already found via folder scan
                val duration = cursor.getLong(durCol)
                val uri = Uri.withAppendedPath(contentUri, id.toString())
                target.add(MediaItemData(name, uri, if (target === videoList) "video" else "audio", duration))
            }
        }
    }

    private fun playMedia(uri: Uri, title: String) {
        val intent = Intent(this, PlayerActivity::class.java)
        intent.putExtra("media_uri", uri.toString())
        intent.putExtra("media_title", title)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(intent)
    }
}

data class MediaItemData(val title: String, val uri: Uri, val type: String, val durationMs: Long = 0L)
