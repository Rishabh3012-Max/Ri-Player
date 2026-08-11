package com.rishabh.riplayer

import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.google.android.material.slider.Slider
import java.util.Locale
import java.util.concurrent.TimeUnit

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private var equalizer: Equalizer? = null
    private var currentSpeed = 1.0f
    private val speedOptions = floatArrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    private val aspectModes = intArrayOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT,
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
        AspectRatioFrameLayout.RESIZE_MODE_FILL
    )
    private var aspectIndex = 0

    private lateinit var playerView: PlayerView
    private lateinit var seekBar: SeekBar
    private lateinit var timeCurrent: TextView
    private lateinit var timeTotal: TextView
    private lateinit var speedBadge: TextView
    private lateinit var btnPlayPause: ImageButton

    private val handler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        playerView = findViewById(R.id.playerView)
        seekBar = findViewById(R.id.seekBar)
        timeCurrent = findViewById(R.id.timeCurrent)
        timeTotal = findViewById(R.id.timeTotal)
        speedBadge = findViewById(R.id.speedBadge)
        btnPlayPause = findViewById(R.id.btnPlayPause)

        val videoTitle = findViewById<TextView>(R.id.videoTitle)
        // Handles both: (1) launched internally with "media_uri" extra,
        // and (2) launched externally via "Open with" -> data is in intent.data
        val externalUri: Uri? = if (intent.action == android.content.Intent.ACTION_VIEW) intent.data else null
        val uriString = intent.getStringExtra("media_uri") ?: externalUri?.toString()
        val title = intent.getStringExtra("media_title")
            ?: externalUri?.lastPathSegment
            ?: "Ri Player"
        videoTitle.text = title

        if (uriString != null) {
            player = ExoPlayer.Builder(this).build().also { exo ->
                playerView.player = exo
                val mediaItem = MediaItem.fromUri(Uri.parse(uriString))
                exo.setMediaItem(mediaItem)
                exo.prepare()
                exo.playWhenReady = true

                exo.addListener(object : Player.Listener {
                    override fun onAudioSessionIdChanged(audioSessionId: Int) {
                        setupEqualizer(audioSessionId)
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        btnPlayPause.setImageResource(
                            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                        )
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            timeTotal.text = formatTime(exo.duration)
                            seekBar.max = exo.duration.toInt().coerceAtLeast(0)
                        }
                    }
                })
            }
        }

        btnPlayPause.setOnClickListener {
            player?.let { p -> if (p.isPlaying) p.pause() else p.play() }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) timeCurrent.text = formatTime(progress.toLong())
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {
                player?.seekTo(sb?.progress?.toLong() ?: 0L)
            }
        })

        findViewById<ImageButton>(R.id.btnRepeat).setOnClickListener { btn ->
            player?.let { p ->
                p.repeatMode = if (p.repeatMode == Player.REPEAT_MODE_ONE)
                    Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
                btn.alpha = if (p.repeatMode == Player.REPEAT_MODE_ONE) 1.0f else 0.6f
            }
        }

        findViewById<ImageButton>(R.id.btnAspect).setOnClickListener {
            aspectIndex = (aspectIndex + 1) % aspectModes.size
            playerView.resizeMode = aspectModes[aspectIndex]
        }

        findViewById<ImageButton>(R.id.btnSubtitle).setOnClickListener {
            android.widget.Toast.makeText(this, "No subtitle tracks found", android.widget.Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageButton>(R.id.btnMore).setOnClickListener { anchor ->
            showMoreMenu(anchor)
        }

        playerView.setOnClickListener { toggleOverlays() }

        handler.post(progressRunnable)
    }

    private fun toggleOverlays() {
        val topOverlay = findViewById<LinearLayout>(R.id.topOverlay)
        val bottomOverlay = findViewById<LinearLayout>(R.id.bottomOverlay)
        val visible = topOverlay.visibility == View.VISIBLE
        val newVisibility = if (visible) View.GONE else View.VISIBLE
        topOverlay.visibility = newVisibility
        bottomOverlay.visibility = newVisibility
        btnPlayPause.visibility = newVisibility
    }

    private fun showMoreMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "Playback Speed")
        popup.menu.add(0, 2, 1, "Equalizer")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> { showSpeedDialog(); true }
                2 -> { showEqualizerDialog(); true }
                else -> false
            }
        }
        popup.show()
    }

    private fun showSpeedDialog() {
        val labels = speedOptions.map { "${it}x" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Playback Speed")
            .setItems(labels) { _, which ->
                currentSpeed = speedOptions[which]
                player?.playbackParameters = PlaybackParameters(currentSpeed)
                speedBadge.text = String.format(Locale.US, "%.2fx", currentSpeed)
                speedBadge.visibility = if (currentSpeed == 1.0f) View.GONE else View.VISIBLE
            }
            .show()
    }

    private fun showEqualizerDialog() {
        val eq = equalizer
        if (eq == null) {
            android.widget.Toast.makeText(this, "Equalizer not ready yet, try again in a moment", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val scroll = androidx.core.widget.NestedScrollView(this)
        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(32, 16, 32, 16)
        scroll.addView(container)

        val bands = eq.numberOfBands
        for (i in 0 until bands) {
            val band = i.toShort()
            val label = TextView(this)
            label.text = "Band ${i + 1}"
            container.addView(label)
            val slider = Slider(this).apply {
                valueFrom = -1500f
                valueTo = 1500f
                value = eq.getBandLevel(band).toFloat()
                addOnChangeListener { _, value, _ ->
                    eq.setBandLevel(band, value.toInt().toShort())
                }
            }
            container.addView(slider)
        }

        AlertDialog.Builder(this)
            .setTitle("Equalizer")
            .setView(scroll)
            .setPositiveButton("Done", null)
            .show()
    }

    private fun setupEqualizer(audioSessionId: Int) {
        try {
            equalizer = Equalizer(0, audioSessionId).apply { enabled = true }
        } catch (e: Exception) {
            // Equalizer not available on this session; ignore gracefully
        }
    }

    private fun updateProgress() {
        val p = player ?: return
        if (p.duration > 0) {
            seekBar.max = p.duration.toInt()
            seekBar.progress = p.currentPosition.toInt()
            timeCurrent.text = formatTime(p.currentPosition)
            timeTotal.text = formatTime(p.duration)
        }
    }

    private fun formatTime(ms: Long): String {
        if (ms <= 0) return "00:00"
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms)
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%02d:%02d", m, s)
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(progressRunnable)
        equalizer?.release()
        player?.release()
        player = null
    }
}
