package com.wiom.csp.ui.onboarding

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wiom.csp.domain.model.FormFieldSchema
import com.wiom.csp.domain.schema.SchemaResolver

/**
 * Schema-driven registration form. Renders fields from [SchemaResolver.onboardingFields],
 * grouped by section. All validation rules come from [FormFieldSchema].
 */
@Composable
fun OnboardingScreen(
    schema: SchemaResolver,
    hindi: Boolean,
    onComplete: () -> Unit
) {
    val viewModel: OnboardingViewModel = hiltViewModel()
    val formState by viewModel.formState.collectAsState()
    val fieldErrors by viewModel.fieldErrors.collectAsState()
    val submitting by viewModel.submitting.collectAsState()
    val submitError by viewModel.submitError.collectAsState()
    val submitSuccess by viewModel.submitSuccess.collectAsState()

    val fields = remember(schema) { schema.onboardingFields() }
    val focusManager = LocalFocusManager.current

    // Initialize form fields in ViewModel
    viewModel.initFields(fields)

    // Navigate on success
    if (submitSuccess) {
        onComplete()
    }

    val sections = remember(fields) {
        fields.groupBy { it.section }.toSortedMap(
            compareBy {
                listOf("business", "location", "identity", "bank", "agreement").indexOf(it)
            }
        )
    }

    val sectionLabels = mapOf(
        "business" to (if (hindi) "\u0935\u094D\u092F\u093E\u092A\u093E\u0930 \u0935\u093F\u0935\u0930\u0923" else "Business Details"),
        "location" to (if (hindi) "\u0938\u094D\u0925\u093E\u0928" else "Location"),
        "identity" to (if (hindi) "\u092A\u0939\u091A\u093E\u0928" else "Identity"),
        "bank" to (if (hindi) "\u092C\u0948\u0902\u0915 \u0935\u093F\u0935\u0930\u0923" else "Bank Details"),
        "agreement" to (if (hindi) "\u0938\u092E\u091D\u094C\u0924\u093E" else "Agreement")
    )

    val allValid = remember(formState, fieldErrors) {
        viewModel.isFormValid(fields)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = if (hindi) "\u092A\u0902\u091C\u0940\u0915\u0930\u0923" else "Registration",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (hindi) "\u0905\u092A\u0928\u093E \u092A\u094D\u0930\u094B\u092B\u093C\u093E\u0907\u0932 \u092A\u0942\u0930\u093E \u0915\u0930\u0947\u0902" else "Complete your profile to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Submit error banner
        if (submitError != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = submitError ?: "",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Render sections
        val allFields = fields
        var fieldIndex = 0

        sections.forEach { (section, sectionFields) ->
            val sectionTitle = sectionLabels[section] ?: section.replaceFirstChar { it.uppercase() }
            Text(
                text = sectionTitle,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            )

            sectionFields.forEach { field ->
                val isLast = fieldIndex == allFields.size - 1
                val imeAction = if (isLast) ImeAction.Done else ImeAction.Next
                val currentValue = formState[field.id] ?: ""
                val currentError = fieldErrors[field.id]

                FormField(
                    field = field,
                    hindi = hindi,
                    value = currentValue,
                    error = currentError,
                    imeAction = imeAction,
                    onValueChange = { viewModel.updateField(field.id, it, fields) },
                    onImeNext = { focusManager.moveFocus(FocusDirection.Down) },
                    onImeDone = { focusManager.clearFocus() }
                )
                Spacer(modifier = Modifier.height(8.dp))
                fieldIndex++
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Submit button
        Button(
            onClick = { viewModel.submit(fields) },
            enabled = allValid && !submitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
            )
        ) {
            if (submitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (hindi) "\u091C\u092E\u093E \u0915\u0930 \u0930\u0939\u0947 \u0939\u0948\u0902..." else "Submitting...")
            } else {
                Text(
                    text = if (hindi) "\u092A\u0902\u091C\u0940\u0915\u0930\u0923 \u092A\u0942\u0930\u093E \u0915\u0930\u0947\u0902" else "Complete Registration",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Renders a single form field based on its [FormFieldSchema.type].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormField(
    field: FormFieldSchema,
    hindi: Boolean,
    value: String,
    error: String?,
    imeAction: ImeAction,
    onValueChange: (String) -> Unit,
    onImeNext: () -> Unit,
    onImeDone: () -> Unit
) {
    val label = if (hindi) field.labelHi else field.label
    val requiredSuffix = if (field.required) " *" else ""
    val displayLabel = "$label$requiredSuffix"

    when (field.type) {
        "text" -> {
            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    val constrained = if (field.maxLength != null) {
                        newValue.take(field.maxLength)
                    } else {
                        newValue
                    }
                    onValueChange(constrained)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(displayLabel) },
                isError = error != null,
                supportingText = if (error != null) {
                    { Text(text = error, color = MaterialTheme.colorScheme.error) }
                } else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = imeAction
                ),
                keyboardActions = KeyboardActions(
                    onNext = { onImeNext() },
                    onDone = { onImeDone() }
                ),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )
        }

        "phone" -> {
            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    val digitsOnly = newValue.filter { it.isDigit() }
                    val constrained = if (field.maxLength != null) {
                        digitsOnly.take(field.maxLength)
                    } else {
                        digitsOnly
                    }
                    onValueChange(constrained)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(displayLabel) },
                isError = error != null,
                supportingText = if (error != null) {
                    { Text(text = error, color = MaterialTheme.colorScheme.error) }
                } else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = imeAction
                ),
                keyboardActions = KeyboardActions(
                    onNext = { onImeNext() },
                    onDone = { onImeDone() }
                ),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )
        }

        "number" -> {
            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    val digitsOnly = newValue.filter { it.isDigit() }
                    val constrained = if (field.maxLength != null) {
                        digitsOnly.take(field.maxLength)
                    } else {
                        digitsOnly
                    }
                    onValueChange(constrained)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(displayLabel) },
                isError = error != null,
                supportingText = if (error != null) {
                    { Text(text = error, color = MaterialTheme.colorScheme.error) }
                } else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = imeAction
                ),
                keyboardActions = KeyboardActions(
                    onNext = { onImeNext() },
                    onDone = { onImeDone() }
                ),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )
        }

        "select" -> {
            var expanded by rememberSaveable { mutableStateOf(false) }
            val options = field.options ?: emptyList()
            val selectedLabel = options.find { it.id == value }?.let {
                if (hindi) it.labelHi else it.label
            } ?: ""

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedLabel,
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    label = { Text(displayLabel) },
                    isError = error != null,
                    supportingText = if (error != null) {
                        { Text(text = error, color = MaterialTheme.colorScheme.error) }
                    } else null,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    shape = RoundedCornerShape(10.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (hindi) option.labelHi else option.label,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            onClick = {
                                onValueChange(option.id)
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        }

        "checkbox" -> {
            val checked = value == "true"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { onValueChange(if (it) "true" else "false") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 48.dp)
                )
            }
        }
    }
}
