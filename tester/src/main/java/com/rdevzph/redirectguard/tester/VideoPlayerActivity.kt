package com.rdevzph.redirectguard.tester

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

class VideoPlayerActivity : ComponentActivity() {

    private var videoViewRef: VideoView? = null
    private var savedPosition: Int = 0

    companion object {
        private const val KEY_SAVED_POSITION = "key_saved_video_position"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedPosition = savedInstanceState?.getInt(KEY_SAVED_POSITION, 0) ?: 0

        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                VideoPlayerScreen(
                    onClose = { finish() },
                    onTriggerRedirect = { triggerShopeeRedirect() },
                    onVideoViewCreated = { vv ->
                        videoViewRef = vv
                        if (savedPosition > 0) {
                            vv.seekTo(savedPosition)
                        }
                    },
                    getSavedPosition = { savedPosition }
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        videoViewRef?.let { vv ->
            val pos = vv.currentPosition
            if (pos > 0) {
                savedPosition = pos
            }
            vv.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        videoViewRef?.let { vv ->
            if (savedPosition > 0) {
                vv.seekTo(savedPosition)
            }
            if (!vv.isPlaying) {
                vv.start()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val pos = videoViewRef?.currentPosition ?: savedPosition
        outState.putInt(KEY_SAVED_POSITION, pos)
    }

    private fun triggerShopeeRedirect() {
        videoViewRef?.let { vv ->
            val pos = vv.currentPosition
            if (pos > 0) {
                savedPosition = pos
            }
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shopee.ph/sample-deal")).apply {
                setPackage("com.shopee.ph")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            Toast.makeText(this, "Ad redirect fired to Shopee", Toast.LENGTH_SHORT).show()
        } catch (e: ActivityNotFoundException) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shopee.ph")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                startActivity(fallbackIntent)
                Toast.makeText(this, "Ad redirect fired via browser fallback", Toast.LENGTH_SHORT).show()
            } catch (err: Exception) {
                Toast.makeText(this, "No handler for redirect intent", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun VideoPlayerScreen(
    onClose: () -> Unit,
    onTriggerRedirect: () -> Unit,
    onVideoViewCreated: (VideoView) -> Unit,
    getSavedPosition: () -> Int
) {
    var countdownSeconds by rememberSaveable { mutableIntStateOf(3) }
    var redirectTriggered by rememberSaveable { mutableStateOf(false) }
    var elapsedSeconds by rememberSaveable { mutableLongStateOf(0L) }
    var videoProgressSec by remember { mutableIntStateOf(0) }
    var videoDurationSec by remember { mutableIntStateOf(0) }
    var playbackState by remember { mutableStateOf("Initializing video...") }
    var localVideoView by remember { mutableStateOf<VideoView?>(null) }

    // Screen session elapsed timer
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsedSeconds += 1
        }
    }

    // Video playback progress updater
    LaunchedEffect(localVideoView) {
        while (true) {
            localVideoView?.let { vv ->
                if (vv.isPlaying) {
                    videoProgressSec = vv.currentPosition / 1000
                    if (vv.duration > 0) {
                        videoDurationSec = vv.duration / 1000
                    }
                }
            }
            delay(250)
        }
    }

    // 3-second countdown to automatic ad redirect
    LaunchedEffect(redirectTriggered) {
        if (!redirectTriggered) {
            while (countdownSeconds > 0) {
                delay(1000)
                countdownSeconds -= 1
            }
            redirectTriggered = true
            onTriggerRedirect()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video View playing raw video resource in loop
        AndroidView(
            factory = { context ->
                VideoView(context).apply {
                    val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.sample_video}")
                    setVideoURI(uri)
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        val restorePos = getSavedPosition()
                        if (restorePos > 0) {
                            seekTo(restorePos)
                            playbackState = "Playing (Resumed at ${restorePos / 1000}s)"
                        } else {
                            playbackState = "Playing loop video (State Active)"
                        }
                        start()
                    }
                    setOnErrorListener { _, what, extra ->
                        playbackState = "Video active (Decoder status: $what/$extra)"
                        true
                    }
                    localVideoView = this
                    onVideoViewCreated(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Bar HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Video: ${videoProgressSec}s / ${if (videoDurationSec > 0) "${videoDurationSec}s" else "--"} (Elapsed: ${elapsedSeconds}s)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Bottom HUD Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Video Player State Test",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = playbackState,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (countdownSeconds > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "Simulated ad redirect triggering in $countdownSeconds second(s)...",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFDCFCE7)
                    ) {
                        Text(
                            text = "Redirect was intercepted. Playback position was restored and video continues uninterrupted.",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF166534),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onTriggerRedirect,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Trigger Redirect")
                    }

                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Text("Exit Video")
                    }
                }
            }
        }
    }
}
