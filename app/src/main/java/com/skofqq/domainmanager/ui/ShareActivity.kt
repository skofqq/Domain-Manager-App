package com.skofqq.domainmanager.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skofqq.domainmanager.data.PrefsStore
import com.skofqq.domainmanager.data.RouterApi
import com.skofqq.domainmanager.ui.theme.DomainManagerTheme
import com.skofqq.domainmanager.util.extractDomain

class ShareActivity : ComponentActivity() {
    private val prefs by lazy { PrefsStore(this) }
    private val api by lazy { RouterApi(prefs) }
    private val viewModel by viewModels<DomainViewModel> { DomainViewModel.Factory(api) }

    override fun onCreate(savedInstanceState: Bundle?) {
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
            DomainManagerTheme {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add to router") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = domain,
                onValueChange = {
                    viewModel.setDomain(it)
                    viewModel.resetStatus()
                },
                label = { Text("Domain") },
                singleLine = true,
                trailingIcon = {
                    if (domain.isNotBlank()) {
                        IconButton(onClick = { viewModel.loadStatus() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload status")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            DomainStatusCard(
                status = status,
                defaultTarget = viewModel.defaultTarget,
                onAdd = { viewModel.addDomain() },
                onRemove = { viewModel.removeDomain() },
            )
            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            ) {
                Text("Done")
            }
        }
    }
}
