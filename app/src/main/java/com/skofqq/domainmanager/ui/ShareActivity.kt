package com.skofqq.domainmanager.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.BackEventCompat
import androidx.activity.SystemBarStyle
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.HistoryStore
import com.skofqq.domainmanager.data.PrefsStore
import com.skofqq.domainmanager.data.RouterApi
import com.skofqq.domainmanager.ui.theme.DomainManagerTheme
import com.skofqq.domainmanager.util.extractDomain
import kotlinx.coroutines.CancellationException

class ShareActivity : AppCompatActivity() {
    private val prefs by lazy { PrefsStore(this) }
    private val api by lazy { RouterApi(prefs, applicationContext) }
    private val viewModel by viewModels<DomainViewModel> {
        DomainViewModel.Factory(api, HistoryStore.get(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val sharedText = intent?.getStringExtra(Intent.EXTRA_TEXT)
                ?: intent?.getStringExtra(Intent.EXTRA_SUBJECT)
                ?: ""
            val guessed = extractDomain(sharedText) ?: ""
            viewModel.setDomain(guessed)
            if (guessed.isNotBlank()) viewModel.loadStatus()
        }
        setContent {
            val darkTheme = isDarkTheme(prefs.themeMode)
            LaunchedEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                )
            }
            DomainManagerTheme(darkTheme = darkTheme, useDynamicColor = prefs.useDynamicColor) {
                ShareScreen(viewModel = viewModel, onDone = ::finish)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(viewModel: DomainViewModel, onDone: () -> Unit) {
    val domain by viewModel.domain.collectAsState()
    val status by viewModel.statusState.collectAsState()

    var backProgress by remember { mutableFloatStateOf(0f) }
    var swipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }

    PredictiveBackHandler { events ->
        try {
            events.collect { event ->
                backProgress = event.progress
                swipeEdge = event.swipeEdge
            }
            onDone()
        } catch (e: CancellationException) {
            backProgress = 0f
            throw e
        }
    }

    val scale = lerp(1f, 0.88f, backProgress)
    val translationX = lerp(
        0f,
        if (swipeEdge == BackEventCompat.EDGE_LEFT) -100f else 100f,
        backProgress,
    )
    val cornerRadius = lerp(0f, 32f, backProgress).dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.translationX = translationX
                alpha = lerp(1f, 0.85f, backProgress)
            }
            .clip(RoundedCornerShape(cornerRadius)),
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.share_title)) },
                    navigationIcon = {
                        IconButton(onClick = onDone) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.close),
                            )
                        }
                    },
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = domain,
                        onValueChange = {
                            viewModel.setDomain(it)
                            viewModel.resetStatus()
                        },
                        label = { Text(stringResource(R.string.domain)) },
                        singleLine = true,
                        trailingIcon = {
                            if (domain.isNotBlank()) {
                                IconButton(onClick = { viewModel.loadStatus() }) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = stringResource(R.string.reload_status),
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    DomainStatusCard(
                        status = status,
                        defaultTarget = viewModel.defaultTarget,
                        onAdd = { viewModel.addDomain() },
                        onRemove = { viewModel.removeDomain() },
                    )
                }
                item {
                    OutlinedButton(
                        onClick = onDone,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        Text(stringResource(R.string.done))
                    }
                }
            }
        }
    }
}
