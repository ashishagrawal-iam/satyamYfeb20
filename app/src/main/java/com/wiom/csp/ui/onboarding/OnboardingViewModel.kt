package com.wiom.csp.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiom.csp.data.preferences.UserPreferences
import com.wiom.csp.data.repository.AuthRepository
import com.wiom.csp.domain.model.FormFieldSchema
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val prefs: UserPreferences
) : ViewModel() {

    private val _formState = MutableStateFlow<Map<String, String>>(emptyMap())
    val formState: StateFlow<Map<String, String>> = _formState.asStateFlow()

    private val _fieldErrors = MutableStateFlow<Map<String, String?>>(emptyMap())
    val fieldErrors: StateFlow<Map<String, String?>> = _fieldErrors.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    private val _submitError = MutableStateFlow<String?>(null)
    val submitError: StateFlow<String?> = _submitError.asStateFlow()

    private val _submitSuccess = MutableStateFlow(false)
    val submitSuccess: StateFlow<Boolean> = _submitSuccess.asStateFlow()

    private var initialized = false

    /**
     * Initialize form fields with empty defaults. Only runs once.
     */
    fun initFields(fields: List<FormFieldSchema>) {
        if (initialized) return
        initialized = true
        val initial = mutableMapOf<String, String>()
        fields.forEach { field ->
            initial[field.id] = when (field.type) {
                "checkbox" -> "false"
                else -> ""
            }
        }
        _formState.value = initial
    }

    /**
     * Update a single form field value and validate it.
     */
    fun updateField(fieldId: String, value: String, allFields: List<FormFieldSchema>) {
        _formState.value = _formState.value.toMutableMap().apply { put(fieldId, value) }
        _submitError.value = null

        val field = allFields.find { it.id == fieldId } ?: return
        val error = validateField(field, value)
        _fieldErrors.value = _fieldErrors.value.toMutableMap().apply { put(fieldId, error) }
    }

    /**
     * Validate a single field against its schema rules.
     * Returns null if valid, or an error message string.
     */
    private fun validateField(field: FormFieldSchema, value: String): String? {
        // Checkbox: required means must be "true"
        if (field.type == "checkbox") {
            if (field.required && value != "true") {
                return "This field is required"
            }
            return null
        }

        // Required check
        if (field.required && value.isBlank()) {
            return "This field is required"
        }

        // If empty and not required, skip further validation
        if (value.isBlank()) return null

        // minLength check
        if (field.minLength != null && value.length < field.minLength) {
            return "Minimum ${field.minLength} characters required"
        }

        // maxLength check
        if (field.maxLength != null && value.length > field.maxLength) {
            return "Maximum ${field.maxLength} characters allowed"
        }

        // Regex validation
        if (field.validation != null) {
            try {
                val regex = Regex(field.validation)
                if (!regex.matches(value)) {
                    return "Invalid format"
                }
            } catch (_: Exception) {
                // If regex itself is invalid, skip this check
            }
        }

        return null
    }

    /**
     * Check if the entire form is valid (all required fields filled, no errors).
     */
    fun isFormValid(fields: List<FormFieldSchema>): Boolean {
        val state = _formState.value
        return fields.all { field ->
            val value = state[field.id] ?: ""
            validateField(field, value) == null
        }
    }

    /**
     * Validate all fields and submit if valid.
     */
    fun submit(fields: List<FormFieldSchema>) {
        // Validate all fields first
        val state = _formState.value
        val errors = mutableMapOf<String, String?>()
        var hasError = false

        fields.forEach { field ->
            val value = state[field.id] ?: ""
            val error = validateField(field, value)
            errors[field.id] = error
            if (error != null) hasError = true
        }

        _fieldErrors.value = errors

        if (hasError) {
            _submitError.value = "Please fix the errors above"
            return
        }

        viewModelScope.launch {
            _submitting.value = true
            _submitError.value = null

            try {
                // Submit profile data — currently stores locally since
                // the onboarding endpoint may not exist yet. The AuthRepository
                // already handles token persistence; here we just mark profile complete.
                prefs.setProfileComplete(true)
                _submitSuccess.value = true
            } catch (e: Exception) {
                _submitError.value = e.message ?: "Registration failed. Please try again."
            } finally {
                _submitting.value = false
            }
        }
    }
}
