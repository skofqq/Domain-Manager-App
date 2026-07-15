package com.skofqq.domainmanager.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.skofqq.domainmanager.data.PrefsStore
import com.skofqq.domainmanager.data.RouterApi
import com.skofqq.domainmanager.ui.theme.DomainManagerTheme
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
    private val prefs by lazy { PrefsStore(this) }
    private val api by lazy { RouterApi(prefs) }
    private val viewModel by viewModels<SettingsViewModel> { SettingsViewModel.Factory(prefs, api) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DomainManagerTheme {
                SettingsScreen(viewModel = viewModel, onBack = ::finish)
            }
        }
    }
}

private val TARGET_OPTIONS = listOf("both", "mihomo", "magitrickle")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val testResult by viewModel.testResult.collectAsState()
    var tokenVisible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { SectionLabel("Router") }
            item {
                OutlinedTextField(
                    value = viewModel.host,
                    onValueChange = { viewModel.host = it },
                    label = { Text("Host") },
                    placeholder = { Text("192.168.1.1") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = viewModel.port,
                    onValueChange = { viewModel.port = it },
                    label = { Text("Port") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { HorizontalDivider() }
            item { SectionLabel("Auth") }
            item {
                OutlinedTextField(
                    value = viewModel.token,
                    onValueChange = { viewModel.token = it },
                    label = { Text("Token") },
                    singleLine = true,
                    visualTransformation = if (tokenVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { tokenVisible = !tokenVisible }) {
                            Text(if (tokenVisible) "Hide" else "Show")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { HorizontalDivider() }
            item { SectionLabel("Default target") }
            items(TARGET_OPTIONS.size) { i ->
                val option = TARGET_OPTIONS[i]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(option, style = MaterialTheme.typography.bodyLarge)
                    RadioButton(
                        selected = viewModel.target == option,
                        onClick = { viewModel.target = option },
                    )
                }
            }
            item { HorizontalDivider() }
            item {
                Button(
                    onClick = {
                        viewModel.save()
                        scope.launch { snackbarHostState.showSnackbar("Saved") }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Save") }
            }
            item {
                OutlinedButton(
                    onClick = { viewModel.testConnection() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Test connection") }
            }
            testResult?.let { result ->
                item {
                    val ok = result.startsWith("Connected")
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (ok) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        Text(
                            text = result,
                            modifier = Modifier.padding(12.dp),
                            color = if (ok) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp),
    )
}
