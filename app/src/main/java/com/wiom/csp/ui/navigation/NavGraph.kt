package com.wiom.csp.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiom.csp.data.preferences.UserPreferences
import com.wiom.csp.domain.model.AppNotification
import com.wiom.csp.domain.schema.SchemaResolver
import com.wiom.csp.notification.EventModal
import com.wiom.csp.ui.auth.LoginScreen
import com.wiom.csp.ui.common.ConfirmationToast
import com.wiom.csp.ui.common.OfflineBanner
import com.wiom.csp.ui.deposit.DepositScreen
import com.wiom.csp.ui.deposit.DepositViewModel
import com.wiom.csp.ui.home.HomeScreen
import com.wiom.csp.ui.home.HomeViewModel
import com.wiom.csp.ui.onboarding.OnboardingScreen
import com.wiom.csp.ui.policies.PoliciesScreen
import com.wiom.csp.ui.profile.ProfileScreen
import com.wiom.csp.ui.sla.SlaScreen
import com.wiom.csp.ui.sla.SlaViewModel
import com.wiom.csp.ui.support.SupportScreen
import com.wiom.csp.ui.support.SupportViewModel
import com.wiom.csp.ui.taskdetail.TaskDetailScreen
import com.wiom.csp.ui.team.TeamScreen
import com.wiom.csp.ui.team.TeamViewModel
import com.wiom.csp.ui.technician.TechnicianScreen
import com.wiom.csp.ui.theme.WiomCspTheme
import com.wiom.csp.ui.wallet.WalletScreen
import com.wiom.csp.ui.wallet.WalletViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Auth/Nav ViewModel ──────────────────────────────────────────────

enum class AuthState { LOADING, LOGGED_OUT, ONBOARDING, LOGGED_IN }

@HiltViewModel
class NavViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val userPreferences: UserPreferences,
    val schemaResolver: SchemaResolver
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState.LOADING)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _hindi = MutableStateFlow(false)
    val hindi: StateFlow<Boolean> = _hindi.asStateFlow()

    private val _darkTheme = MutableStateFlow(true)
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    var selectedTaskId: String?
        get() = savedStateHandle["selectedTaskId"]
        set(value) { savedStateHandle["selectedTaskId"] = value }

    var activeSection: String?
        get() = savedStateHandle["activeSection"]
        set(value) { savedStateHandle["activeSection"] = value }

    var menuOpen: Boolean
        get() = savedStateHandle["menuOpen"] ?: false
        set(value) { savedStateHandle["menuOpen"] = value }

    init { checkAuth() }

    private fun checkAuth() {
        viewModelScope.launch {
            val token = userPreferences.getToken()
            if (token == null) {
                _authState.value = AuthState.LOGGED_OUT
            } else if (!userPreferences.isProfileComplete()) {
                _authState.value = AuthState.ONBOARDING
            } else {
                _authState.value = AuthState.LOGGED_IN
                loadPrefs()
            }
        }
    }

    private suspend fun loadPrefs() {
        _hindi.value = userPreferences.getLanguage() == "hi"
        _darkTheme.value = userPreferences.getTheme() == "DARK"
    }

    fun onLoginSuccess() {
        viewModelScope.launch {
            if (!userPreferences.isProfileComplete()) {
                _authState.value = AuthState.ONBOARDING
            } else {
                _authState.value = AuthState.LOGGED_IN
                loadPrefs()
            }
        }
    }

    fun onOnboardingComplete() {
        viewModelScope.launch {
            userPreferences.setProfileComplete(true)
            _authState.value = AuthState.LOGGED_IN
            loadPrefs()
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            val d = !_darkTheme.value
            _darkTheme.value = d
            userPreferences.setTheme(if (d) "DARK" else "LIGHT")
        }
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val h = !_hindi.value
            _hindi.value = h
            userPreferences.setLanguage(if (h) "hi" else "en")
        }
    }
}

// ── NavGraph composable ─────────────────────────────────────────────

@Composable
fun WiomNavGraph(deepLinkIntent: Intent? = null) {
    val navVm: NavViewModel = hiltViewModel()
    val homeVm: HomeViewModel = hiltViewModel()

    val authState by navVm.authState.collectAsState()
    val hindi by navVm.hindi.collectAsState()
    val darkTheme by navVm.darkTheme.collectAsState()
    val homeState by homeVm.uiState.collectAsState()

    var selectedTaskId by rememberSaveable { mutableStateOf(navVm.selectedTaskId) }
    var activeSection by rememberSaveable { mutableStateOf(navVm.activeSection) }
    var menuOpen by rememberSaveable { mutableStateOf(navVm.menuOpen) }

    LaunchedEffect(selectedTaskId) { navVm.selectedTaskId = selectedTaskId }
    LaunchedEffect(activeSection) { navVm.activeSection = activeSection }
    LaunchedEffect(menuOpen) { navVm.menuOpen = menuOpen }

    // Deep link
    LaunchedEffect(deepLinkIntent) {
        deepLinkIntent?.data?.let { uri ->
            val segs = uri.pathSegments ?: return@let
            if (segs.size >= 2) when (segs[0]) {
                "task" -> selectedTaskId = segs[1]
                "section" -> activeSection = segs[1]
            }
        }
    }

    // Back handler chain
    BackHandler(enabled = selectedTaskId != null || activeSection != null || menuOpen) {
        when {
            selectedTaskId != null -> selectedTaskId = null
            activeSection != null -> activeSection = null
            menuOpen -> menuOpen = false
        }
    }

    WiomCspTheme(darkTheme = darkTheme) {
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues())
        ) {
            when (authState) {
                AuthState.LOADING -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                AuthState.LOGGED_OUT -> {
                    LoginScreen(onLoginSuccess = { navVm.onLoginSuccess() })
                }

                AuthState.ONBOARDING -> {
                    OnboardingScreen(
                        schema = navVm.schemaResolver,
                        hindi = hindi,
                        onComplete = { navVm.onOnboardingComplete() }
                    )
                }

                AuthState.LOGGED_IN -> {
                    // Main content
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (homeState.isOffline) {
                            OfflineBanner()
                        }
                        HomeScreen(
                            tasks = homeState.tasks,
                            assurance = homeState.assurance,
                            isOffline = homeState.isOffline,
                            isLoading = homeState.isLoading,
                            isRefreshing = homeState.isRefreshing,
                            capabilityResetActive = homeState.capabilityResetActive,
                            schema = homeVm.getSchemaResolver(),
                            hindi = hindi,
                            activeFilter = homeState.activeFilter,
                            fadingTasks = homeState.fadingTasks,
                            onFilterChange = { homeVm.setFilter(it) },
                            onTaskClick = { selectedTaskId = it },
                            onTaskAction = { id, action -> homeVm.handleTaskAction(id, action) },
                            onChipClick = { chip ->
                                if (chip == "sla_standing") activeSection = "sla"
                            },
                            onRefresh = { homeVm.refresh() },
                            onMenuClick = { menuOpen = true }
                        )
                    }

                    // Menu drawer
                    AnimatedVisibility(
                        visible = menuOpen,
                        enter = slideInHorizontally(tween(300)) { -it },
                        exit = slideOutHorizontally(tween(300)) { -it }
                    ) {
                        SecondaryMenu(
                            hindi = hindi,
                            onSectionSelect = { activeSection = it; menuOpen = false },
                            onClose = { menuOpen = false }
                        )
                    }

                    // Section overlays
                    AnimatedVisibility(
                        visible = activeSection != null,
                        enter = slideInHorizontally(tween(300)) { it },
                        exit = slideOutHorizontally(tween(300)) { it }
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            when (activeSection) {
                                "wallet" -> {
                                    val vm: WalletViewModel = hiltViewModel()
                                    val w by vm.wallet.collectAsState()
                                    w?.let {
                                        WalletScreen(
                                            wallet = it,
                                            schema = navVm.schemaResolver,
                                            hindi = hindi,
                                            onBack = { activeSection = null },
                                            onWithdraw = { amt -> vm.withdraw(amt) },
                                            onAddMoney = { amt, m -> vm.addMoney(amt, m) }
                                        )
                                    }
                                }
                                "team" -> {
                                    val vm: TeamViewModel = hiltViewModel()
                                    val techs by vm.technicians.collectAsState()
                                    TeamScreen(
                                        technicians = techs,
                                        schema = navVm.schemaResolver,
                                        hindi = hindi,
                                        onBack = { activeSection = null },
                                        onAddTechnician = { n, p -> vm.addTechnician(n, p) }
                                    )
                                }
                                "support" -> {
                                    val vm: SupportViewModel = hiltViewModel()
                                    val cases by vm.cases.collectAsState()
                                    SupportScreen(
                                        cases = cases,
                                        schema = navVm.schemaResolver,
                                        hindi = hindi,
                                        onBack = { activeSection = null },
                                        onCreateCase = { s, d, t -> vm.createCase(s, d, t) },
                                        onReply = { id, txt -> vm.replyToCase(id, txt) }
                                    )
                                }
                                "netbox" -> {
                                    val vm: DepositViewModel = hiltViewModel()
                                    val dep by vm.deposit.collectAsState()
                                    dep?.let {
                                        DepositScreen(
                                            deposit = it,
                                            schema = navVm.schemaResolver,
                                            hindi = hindi,
                                            onBack = { activeSection = null }
                                        )
                                    }
                                }
                                "sla" -> {
                                    val vm: SlaViewModel = hiltViewModel()
                                    val sla by vm.sla.collectAsState()
                                    sla?.let {
                                        SlaScreen(
                                            sla = it,
                                            schema = navVm.schemaResolver,
                                            hindi = hindi,
                                            onBack = { activeSection = null }
                                        )
                                    }
                                }
                                "profile" -> {
                                    ProfileScreen(
                                        userName = homeState.wallet?.let { "CSP Partner" } ?: "Partner",
                                        partnerId = "CSP-MH-1001",
                                        hindi = hindi,
                                        onBack = { activeSection = null },
                                        onLogout = {
                                            // handled via preferences clear + nav reset
                                        },
                                        onToggleTheme = { navVm.toggleTheme() },
                                        onToggleLanguage = { navVm.toggleLanguage() },
                                        isDarkTheme = darkTheme
                                    )
                                }
                                "policies" -> {
                                    PoliciesScreen(
                                        hindi = hindi,
                                        onBack = { activeSection = null }
                                    )
                                }
                                "technician" -> {
                                    TechnicianScreen(
                                        schema = navVm.schemaResolver,
                                        hindi = hindi,
                                        onBack = { activeSection = null }
                                    )
                                }
                            }
                        }
                    }

                    // Task detail overlay
                    AnimatedVisibility(
                        visible = selectedTaskId != null,
                        enter = slideInHorizontally(tween(300)) { it },
                        exit = slideOutHorizontally(tween(300)) { it }
                    ) {
                        selectedTaskId?.let { taskId ->
                            TaskDetailScreen(
                                taskId = taskId,
                                schema = navVm.schemaResolver,
                                hindi = hindi,
                                technicians = emptyList(),
                                onBack = { selectedTaskId = null },
                                onAction = { id, action, payload ->
                                    homeVm.handleTaskAction(id, action, payload)
                                }
                            )
                        }
                    }

                    // Confirmation toast
                    homeState.confirmMessage?.let { msg ->
                        ConfirmationToast(
                            message = msg,
                            onDismiss = { homeVm.dismissConfirmation() }
                        )
                    }

                    // Urgent notification modal
                    homeState.urgentNotification?.let { notif ->
                        EventModal(
                            notification = notif,
                            schema = navVm.schemaResolver,
                            hindi = hindi,
                            onDismiss = { homeVm.dismissUrgentNotification() }
                        )
                    }
                }
            }
        }
    }
}

// ── Secondary Menu ──────────────────────────────────────────────────

@Composable
private fun SecondaryMenu(
    hindi: Boolean,
    onSectionSelect: (String) -> Unit,
    onClose: () -> Unit
) {
    val sections = listOf(
        "wallet" to if (hindi) "वॉलेट" else "Wallet",
        "team" to if (hindi) "टीम" else "Team",
        "support" to if (hindi) "सहायता" else "Support",
        "netbox" to if (hindi) "नेटबॉक्स" else "NetBox",
        "sla" to if (hindi) "SLA" else "SLA",
        "profile" to if (hindi) "प्रोफ़ाइल" else "Profile",
        "policies" to if (hindi) "नीतियाँ" else "Policies",
        "technician" to if (hindi) "तकनीशियन" else "Technician"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(24.dp)) {
            androidx.compose.material3.Text(
                text = if (hindi) "मेनू" else "Menu",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            sections.forEach { (id, label) ->
                androidx.compose.material3.TextButton(
                    onClick = { onSectionSelect(id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    androidx.compose.material3.Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
