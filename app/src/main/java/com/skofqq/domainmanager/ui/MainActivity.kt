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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.skofqq.domainmanager.data.PrefsStore
import com.skofqq.domainmanager.data.RouterApi
import com.skofqq.domainmanager.ui.theme.DomainManagerTheme

class MainActivity : ComponentActivity() {
    private val prefs by lazy { PrefsStore(this) }
    private val api by lazy { RouterApi(prefs) }
    private val viewModel by viewModels<DomainViewModel> { DomainViewModel.Factory(api) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DomainManagerTheme {
                MainScreen(
                    viewModel = viewModel,
                    onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: DomainViewModel, onOpenSettings: () -> Unit) {
    val domain by viewModel.domain.collectAsState()
    val status by viewModel.statusState.collectAsState()
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Domain Manager") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
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
                placeholder = { Text("example.com") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(onGo = {
                    focusManager.clearFocus()
                    viewModel.loadStatus()
                }),
                modifier = Modifier.fillMaxWidth(),
            )
            FilledTonalButton(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.loadStatus()
                },
                enabled = domain.isNotBlank() && status !is StatusUiState.Loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Check Status")
            }
            DomainStatusCard(
                status = status,
                defaultTarget = viewModel.defaultTarget,
                onAdd = { viewModel.addDomain() },
                onRemove = { viewModel.removeDomain() },
            )
        }
    }
}
