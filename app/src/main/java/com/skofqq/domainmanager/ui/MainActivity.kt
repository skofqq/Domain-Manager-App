package com.skofqq.domainmanager.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
        installSplashScreen()
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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Domain Manager") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
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
            }
            item {
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
            }
            item {
                DomainStatusCard(
                    status = status,
                    defaultTarget = viewModel.defaultTarget,
                    onAdd = { viewModel.addDomain() },
                    onRemove = { viewModel.removeDomain() },
                )
            }
        }
    }
}
