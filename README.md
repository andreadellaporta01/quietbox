# QuietBox — the model is not the feature

Workshop repo for **"The model is not the feature: designing invisible AI for mobile apps"**
next.app devcon Berlin · Friday 9 October 2026 · Andrea Della Porta & Alessandro Finocchiaro

QuietBox is an inbox with no chatbot. The AI is in it, but you never see it as a feature:

| What you see | Task behind it | Where it runs |
|---|---|---|
| Messages sorted into *Needs you / Someone's waiting / FYI / Quiet* | `triage` (routing) | on-device first, Gemini when unsure |
| A chip with *date · time · amount* under a message | `extract` (extraction) | on-device first, Gemini when unsure |
| "Today: send offline-mode estimate" banner | `Proactive.nudges` (ranking) | pure code over extracted data |
| One-line TL;DR on a long thread | `summarize` (generation + retrieval) | Gemini, on open |
| Three reply chips | `reply` (generation → action) | Gemini, on open |

The cloud model is **Gemini through Firebase AI Logic**, called from shared Kotlin on both Android and iOS.

The **AI X-ray** panel on the right shows every call: which route was picked and why, what was tried, tokens, latency, and whether the result was shown or hidden.

---

## Setup (≈10 minutes)

This is a mobile workshop, so you need the mobile toolchain:

- **JDK 17+** (for the labs, which run as plain JVM tests)
- **Android:** Android Studio or the Android SDK, plus an emulator or a phone
- **iOS (optional):** Xcode 26+ and `brew install xcodegen`

```bash
git clone https://github.com/andreadellaporta01/quietbox && cd quietbox
git checkout start
./gradlew :core:jvmTest              # ✅ setup works if this ends with "20 tests completed, 20 failed"
./gradlew :androidApp:installDebug   # the app on your emulator or phone
cd iosApp && xcodegen && open QuietBox.xcodeproj   # iOS: run the QuietBox scheme on a simulator
```

From an empty Gradle cache, measured on 2026-10-07: `:core:jvmTest` downloads ~730 MB (about 1′40″ on fast Wi-Fi), `:androidApp:installDebug` another ~300 MB, the iOS build another ~400 MB. If the Wi-Fi is struggling, pair with a neighbour while yours finishes.

On `start` the app already runs: every unfinished lab shows up in the X-ray as a failed span ("LAB-1 not done yet") and the inbox stays unsorted until you write it.

### The real model, for free

The app talks to **Gemini through Firebase AI Logic** out of the box. The repo ships the config files of a shared project (`quietbox-berlin`) on the free Gemini Developer API tier: no billing, no API key to paste.

The free tier's quota is **per project**, and the whole room shares this one. When it runs out, cloud calls come back `rate_limited` and the app quietly falls back to the device. That's Lab 2 happening for real, and you can watch it in the X-ray.

### Optional: your own Firebase project (≈5 minutes)

Want a quota all to yourself? Create your own project and swap two files. Nothing else changes.

1. [console.firebase.google.com](https://console.firebase.google.com) → **Create project** (no Google Analytics needed, no billing).
2. **AI Services → AI Logic → Get started → Gemini Developer API**.
3. Add an **Android app** with package `dev.quietbox` → download `google-services.json` into `androidApp/`.
   Add an **iOS app** with bundle id `dev.quietbox.app` → download `GoogleService-Info.plist` into `iosApp/QuietBox/`.
4. **Security → App Check → APIs → Firebase AI Logic → Unenforced.** New projects enforce it by default, and without a registered debug token every call fails with 403.
5. Rebuild the app. `./gradlew :core:eval -Pengine=firebase` reads the same `google-services.json`, so the eval uses your project too.

> If your first call says `genai config not found`, the project is still provisioning. Wait two or three minutes and try again.

App Check is **off only for the workshop**. In production, App Check (Play Integrity / App Attest) is what stops someone else from spending your quota with the key inside your app. Firebase makes it mandatory from 2 November 2026.

### Engines

```bash
./gradlew :core:eval                       # mock: deterministic, offline, behaves like a model (including a bad answer)
./gradlew :core:eval -Pengine=firebase     # Gemini via Firebase AI Logic
./gradlew :core:eval -Pengine=offline      # no network: everything on-device
./gradlew :core:eval -Pengine=chaos        # 30% server errors, 20% broken JSON, +0.9 s
./gradlew :androidApp:installDebug -Pquietbox.engine=mock   # the app without the network
```

---

## The labs

Each lab is one `TODO` inside a `// region LAB-N` block, plus a test class that tells you when you're done.

```bash
./gradlew :core:jvmTest --tests '*Lab1*'
```

| Lab | File | You build | Done when |
|---|---|---|---|
| 1 · Route | `core/.../ai/Router.kt` | the decision of *where* a task runs: privacy, offline, battery, budget, confidence | `Lab1RouterTest` green |
| 2 · Fall back | `core/.../ai/Pipeline.kt` | the tier loop: try, record an `Attempt`, stop at the first valid answer | `Lab2FallbackTest` green |
| 3 · Guard | `core/.../tasks/Extract.kt` | `validate()`: reject anything the message doesn't literally support | `Lab3GuardrailTest` green |
| 4 · Nudge | `core/.../engine/Proactive.kt` | proactive banners: at most two, soonest first, never nag | `Lab4ProactiveTest` green |

After each lab, run `./gradlew :core:eval` (also with `-Pengine=offline` and `-Pengine=chaos`) and watch the scoreboard change.

Stuck? Every lab has a checkpoint you can jump to:

```bash
git stash && git checkout lab-1-done    # also lab-2-done, lab-3-done, solved
```

---

## Map of the code

```
core/   KMP, no UI. Everything that matters.
  ai/         AiTask contract, TaskSpec (budgets), Router, Pipeline, TokenBudget
  tasks/      Triage · Extract · Summarize · SmartReply   (one file per task: prompt, schema, validator, on-device path)
  local/      on-device building blocks: Sensitivity (privacy), Retriever, text heuristics
  cloud/      CloudModel · FirebaseCloud (Gemini) · MockCloud · FlakyCloud (chaos) · ProxyCloud
  engine/     InboxEngine (when each task fires) · Proactive (nudges)
  telemetry/  Span per call, the data behind the X-ray
  eval/       golden-set scoreboard  →  ./gradlew :core:eval
app/        Compose Multiplatform UI (Android · iOS), MVI on KMP ViewModels, Koin
  mvi/        MviViewModel: Intent → Result → pure reduce → State, one-shot Effects on a Channel
  session/    AiSession: the one place that builds a Pipeline (cloud, conditions, budget, telemetry)
  inbox/      InboxContract (State · Intent · Result · Effect) · InboxViewModel · InboxScreen
  detail/     DetailContract · DetailViewModel (summary + replies, cancelled when you leave) · DetailScreen
  xray/       XRayContract · XRayViewModel (the demo levers) · XRayScreen
  di/         Koin modules: cloud (Firebase / mock / proxy) · data · presentation
androidApp/ Android shell (one Activity)
iosApp/     iOS shell (one SwiftUI file, xcodegen)
proxy/      optional: a server-side proxy, for when the key must never ship in the app
```

**Why MVI on top of the AI layer.** Every AI result is a value with an outcome (shown, hidden, failed). The screens never call a model: they send an `Intent`, and a pure reducer turns *what happened* into the next `State`. That is how "the app shows less, never something wrong" stays true in the UI too: there is no code path where a failed call becomes an error dialog. Reducers and ViewModels are tested in `app/src/commonTest` (`./gradlew :app:iosSimulatorArm64Test`).

The "on-device model" in this repo uses heuristics, on purpose. The architecture doesn't care what sits behind `AiTask.onDevice()`: a regex, Gemini Nano through ML Kit GenAI, Apple's Foundation Models, or a TFLite classifier. The contract is the same: return a value **and a confidence**, or `null`.

## Optional: the server-side proxy

Firebase AI Logic is the client-side pattern: the app calls the model directly, and App Check keeps other clients out. When the provider key must never reach the device, put a proxy in front instead:

```bash
export ANTHROPIC_API_KEY=sk-ant-...      # yours
export QUIETBOX_TOKEN=any-shared-secret
./gradlew :proxy:run                     # :8787, structured outputs, low effort for background tasks
QUIETBOX_PROXY_URL=http://localhost:8787 QUIETBOX_TOKEN=any-shared-secret ./gradlew :core:eval -Pengine=proxy
```
