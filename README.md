# Wiom CSP App — State-Driven Renderer Architecture

Complete rewrite of the CSP Partner App following the **Amendment Architecture** from `CSP_App_Architecture_Build_Plan.pptx`.

## Architecture

The app is a **state-driven renderer** — it never hardcodes business logic. All display decisions (labels, colors, actions, state machines) come from a schema that the backend provides. This means:

- **Tier 1 changes** (SLA thresholds, timer durations, bonus rates) → SPR hot-config, no deploy
- **Tier 2 changes** (new task states, wallet line items, consequences) → backend deploy only, no app release
- **Tier 3 changes** (genuinely new screens) → app release, but built as generic renderers to prevent future Tier 3s

### 5 Non-Negotiable Build Rules

1. **No business logic in the app** — app asks the API what comes next, never hardcodes state machines
2. **No hardcoded labels, thresholds, or display rules** — all from schema (Hindi + English)
3. **Generic TaskRenderer** — single composable renders any task type in any state
4. **Assurance strip is data-driven** — renders N chips from API array, not hardcoded 4
5. **Queue ordering is never local** — API returns ordered list, app renders in received order

### Key Differences from Previous Prototype

| Previous (`feature/design-spec-audit-v1`) | This Build |
|---|---|
| Hardcoded state machines per task type | Schema-driven: `schema[taskType][state]` → label, color, actions |
| Client-side 7-bucket sort | Server-determined order, zero local `sort()` |
| 2-5s aggressive polling | 30s periodic sync + push-triggered refresh |
| Separate Install/Restore/NetBox screens | Single generic TaskRenderer + TaskDetailRenderer |
| Partial offline (Room fallback) | Full offline-first: SQLite cache + action queue |
| Web + Android dual codebase | Android-only, budget device optimized |

## Tech Stack

- **Kotlin** + **Jetpack Compose** (Material3)
- **Hilt** for dependency injection
- **Room** for SQLite cache (schema + data)
- **Retrofit** + **OkHttp** + **kotlinx.serialization** for networking
- **DataStore** for user preferences
- **Coroutines** + **StateFlow** for async + state management

## Project Structure

```
app/src/main/java/com/wiom/csp/
├── data/
│   ├── db/           # Room database, DAOs, entities
│   ├── remote/       # Retrofit API interfaces, DTOs
│   ├── repository/   # 11 repositories (API-first, cache fallback)
│   ├── sync/         # SyncOrchestrator, ActionQueue
│   └── preferences/  # DataStore user preferences
├── domain/
│   ├── model/        # Schema.kt (contract), DataModels.kt (runtime data)
│   └── schema/       # SchemaResolver — heart of state-driven rendering
├── di/               # Hilt modules (Database, Network, App)
├── mock/             # SeedDataProvider — complete mock schema + seed data
├── ui/
│   ├── renderer/     # Generic renderers (TaskCard, TaskDetail, Chip, Ledger, Timeline, Action)
│   ├── home/         # HomeScreen + HomeViewModel
│   ├── auth/         # Login (OTP flow)
│   ├── onboarding/   # Schema-driven registration form
│   ├── wallet/       # 3-ledger wallet with withdraw/add flows
│   ├── sla/          # 4-domain SLA hub with metric visualization
│   ├── team/         # Technician management
│   ├── support/      # Support case lifecycle
│   ├── deposit/      # NetBox deposit ledger
│   ├── technician/   # Technician sub-app
│   ├── profile/      # Profile + settings
│   ├── policies/     # Terms, privacy, SLA policies
│   ├── navigation/   # SPA-style NavGraph with SavedStateHandle
│   ├── common/       # Shared components (OfflineBanner, Toast, Shimmer, DrillDown, FilterChips)
│   └── theme/        # Dark + Light (State-Color-Check) themes
├── notification/     # EventModal for urgent notifications
└── feedback/         # AudioFeedback + HapticFeedback
```

**63 Kotlin files** | **0 compilation errors** | **18MB APK (dev debug)**

## How to Build

### Prerequisites
- **Java 17** (e.g., `brew install openjdk@17`)
- **Android SDK** with platform 35 and build-tools 34.0.0

### Steps

```bash
# Clone and checkout
git clone https://github.com/satyamdarmora/satyamYfeb20.git
cd satyamYfeb20
git checkout cspappnew-architecture-mukul

# Set Java home (if not default)
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"

# Create local.properties with your SDK path
echo "sdk.dir=/path/to/your/android-sdk" > local.properties

# Build
./gradlew assembleDevDebug

# Install on connected device
adb install -r app/build/outputs/apk/dev/debug/app-dev-debug.apk
```

### Build Flavors

| Flavor | Mock Data | Backend URL | Use Case |
|--------|-----------|-------------|----------|
| `dev` | Yes (SeedDataProvider) | `http://10.0.2.2:4000` | Local development |
| `staging` | No | `http://192.168.1.100:4000` | LAN testing |
| `prod` | No | `https://api.wiom.in` | Production |

The **dev** flavor uses `SeedDataProvider` for all data — no backend needed. 8 seed tasks, 4 technicians, wallet with 10 transactions, full SLA metrics, 3 support cases, 6 NetBox units.

## Test Coverage

350+ test cases documented in `csp-app-test-matrix-v1.md`. Validated against this build:
- ~280 PASS
- ~35 PARTIAL (need runtime verification)
- ~25 IMPROVED by new architecture
- 4 known low-severity bugs in wallet flow (documented)

## OS Coverage

Built against 10 locked Operating Systems:
1. Connection Lifecycle OS v1.1
2. Demand & Allocation OS v1.2
3. Quality OS v1.7
4. Enforcement OS v1.5
5. Compensation OS v1.7
6. Payment & Settlement OS v1.5
7. Exit OS v1.6
8. Asset Custody OS v1.3→v1.4
9. Capacity & Coverage OS v1.1
10. Visibility OS v1.2

Plus schema support for 2 new OSes (Support & Resolution, Capability Intervention) when drafted.
