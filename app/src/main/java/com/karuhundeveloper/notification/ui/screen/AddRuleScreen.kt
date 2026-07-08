package com.karuhundeveloper.notification.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.karuhundeveloper.notification.data.local.entity.InterceptRule
import com.karuhundeveloper.notification.ui.component.MethodBadge
import com.karuhundeveloper.notification.ui.theme.*
import com.karuhundeveloper.notification.ui.viewmodel.RulesViewModel

private val HTTP_METHODS = listOf("POST", "GET", "PUT", "PATCH", "DELETE")
private val gson = Gson()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRuleScreen(
    onNavigateBack: () -> Unit,
    viewModel: RulesViewModel = viewModel()
) {
    // ── Form state ────────────────────────────────────────────────────────────
    var ruleName by remember { mutableStateOf("") }
    var targetApps by remember { mutableStateOf("") }
    var apiUrl by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("POST") }
    var customHeaders by remember { mutableStateOf("") }

    // ── Validation error state ────────────────────────────────────────────────
    var ruleNameError by remember { mutableStateOf<String?>(null) }
    var targetAppsError by remember { mutableStateOf<String?>(null) }
    var apiUrlError by remember { mutableStateOf<String?>(null) }
    var headersError by remember { mutableStateOf<String?>(null) }

    // ── Method dropdown ───────────────────────────────────────────────────────
    var methodDropdownExpanded by remember { mutableStateOf(false) }

    // ── Snackbar ──────────────────────────────────────────────────────────────
    val snackbarHostState = remember { SnackbarHostState() }

    fun validate(): Boolean {
        var ok = true

        ruleNameError = if (ruleName.isBlank()) { ok = false; "Rule name is required" } else null

        targetAppsError = if (targetApps.isBlank()) {
            ok = false; "At least one package name is required"
        } else null

        apiUrlError = when {
            apiUrl.isBlank() -> { ok = false; "API URL is required" }
            !apiUrl.startsWith("http://") && !apiUrl.startsWith("https://") ->
                { ok = false; "URL must start with http:// or https://" }
            else -> null
        }

        headersError = if (customHeaders.isNotBlank()) {
            try {
                val type = object : TypeToken<Map<String, String>>() {}.type
                gson.fromJson<Map<String, String>>(customHeaders, type)
                null
            } catch (e: JsonSyntaxException) {
                ok = false; "Invalid JSON — must be a flat {\"key\": \"value\"} object"
            }
        } else null

        return ok
    }

    fun onSave() {
        if (!validate()) return

        val parsedApps = targetApps
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val parsedHeaders: Map<String, String> = if (customHeaders.isBlank()) {
            emptyMap()
        } else {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(customHeaders, type) ?: emptyMap()
        }

        viewModel.insertRule(
            InterceptRule(
                ruleName = ruleName.trim(),
                targetApps = parsedApps,
                apiUrl = apiUrl.trim(),
                apiMethod = selectedMethod,
                customHeaders = parsedHeaders
            )
        )
        onNavigateBack()
    }

    Scaffold(
        containerColor = NavyDeep,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("New Rule", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        Text("Configure a forwarding rule", fontSize = 11.sp, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(IndigoAccent, CyanAccent)))
                    ) {
                        TextButton(
                            onClick = { onSave() },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyDeep)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Identification ────────────────────────────────────────────────
            FormSection(title = "Identification", icon = Icons.Default.Label) {
                RuleTextField(
                    value = ruleName,
                    onValueChange = { ruleName = it; ruleNameError = null },
                    label = "Rule Name",
                    placeholder = "e.g. WhatsApp → Slack webhook",
                    error = ruleNameError,
                    imeAction = ImeAction.Next
                )
            }

            // ── Target Apps ───────────────────────────────────────────────────
            FormSection(title = "Target Apps", icon = Icons.Default.Apps) {
                RuleTextField(
                    value = targetApps,
                    onValueChange = { targetApps = it; targetAppsError = null },
                    label = "Package Names (comma-separated)",
                    placeholder = "com.whatsapp, com.telegram.messenger",
                    error = targetAppsError,
                    imeAction = ImeAction.Next,
                    capitalization = KeyboardCapitalization.None
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "💡  To test the pipeline, enter \"${
                        androidx.compose.ui.platform.LocalContext.current.packageName
                    }\" as the target app and use the Send Dummy button.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }

            // ── Endpoint ──────────────────────────────────────────────────────
            FormSection(title = "API Endpoint", icon = Icons.Default.Link) {
                RuleTextField(
                    value = apiUrl,
                    onValueChange = { apiUrl = it; apiUrlError = null },
                    label = "API URL",
                    placeholder = "https://hooks.slack.com/services/…",
                    error = apiUrlError,
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                    capitalization = KeyboardCapitalization.None
                )

                Spacer(Modifier.height(8.dp))

                // Method dropdown
                Text(
                    "HTTP METHOD",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IndigoLight,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = methodDropdownExpanded,
                    onExpandedChange = { methodDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedMethod,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodDropdownExpanded)
                        },
                        leadingIcon = {
                            MethodBadge(method = selectedMethod)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ruleTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = methodDropdownExpanded,
                        onDismissRequest = { methodDropdownExpanded = false },
                        containerColor = NavyElevated
                    ) {
                        HTTP_METHODS.forEach { method ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        MethodBadge(method = method)
                                        Spacer(Modifier.width(10.dp))
                                        Text(method, color = TextPrimary)
                                    }
                                },
                                onClick = {
                                    selectedMethod = method
                                    methodDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // ── Custom Headers ────────────────────────────────────────────────
            FormSection(title = "Custom Headers", icon = Icons.Default.Code) {
                RuleTextField(
                    value = customHeaders,
                    onValueChange = { customHeaders = it; headersError = null },
                    label = "Headers JSON (optional)",
                    placeholder = "{\n  \"Authorization\": \"Bearer token\",\n  \"X-Source\": \"android\"\n}",
                    error = headersError,
                    minLines = 4,
                    imeAction = ImeAction.Default,
                    keyboardType = KeyboardType.Ascii,
                    capitalization = KeyboardCapitalization.None
                )
                AnimatedVisibility(
                    visible = headersError == null && customHeaders.isNotBlank(),
                    enter = fadeIn() + expandVertically()
                ) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Valid JSON", fontSize = 11.sp, color = SuccessGreen)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared form sub-components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FormSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    title.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = IndigoLight,
                    letterSpacing = 0.8.sp
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ruleTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor       = TextPrimary,
    unfocusedTextColor     = TextPrimary,
    focusedBorderColor     = IndigoAccent,
    unfocusedBorderColor   = Divider,
    focusedLabelColor      = IndigoLight,
    unfocusedLabelColor    = TextSecondary,
    cursorColor            = IndigoAccent,
    focusedContainerColor  = NavyElevated,
    unfocusedContainerColor= NavyElevated,
    errorBorderColor       = ErrorRed,
    errorLabelColor        = ErrorRed,
    errorTextColor         = TextPrimary
)

@Composable
private fun RuleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    error: String? = null,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder, color = TextSecondary.copy(alpha = 0.5f), fontSize = 12.sp) },
            isError = error != null,
            minLines = minLines,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ruleTextFieldColors(),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction,
                capitalization = capitalization
            )
        )
        AnimatedVisibility(visible = error != null, enter = fadeIn() + expandVertically()) {
            error?.let {
                Row(
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(it, fontSize = 11.sp, color = ErrorRed)
                }
            }
        }
    }
}
