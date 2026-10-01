# QuietBox — the model is not the feature

Workshop repo for **"The model is not the feature: designing invisible AI for mobile apps"**
next.app devcon Berlin 2026 · Andrea Della Porta & Alessandro Finocchiaro

QuietBox is an inbox with no chatbot. The AI is in it, but you never see it as a feature:

| What you see | Task behind it | Where it runs |
|---|---|---|
| Messages sorted into *Needs you / Someone's waiting / FYI / Quiet* | `triage` (routing) | on-device first, cloud when unsure |
| A chip with *date · time · amount* under a message | `extract` (extraction) | on-device first, cloud when unsure |
| "Today: send offline-mode estimate" banner | `Proactive.nudges` (ranking) | pure code over extracted data |
| One-line TL;DR on a long thread | `summarize` (generation + retrieval) | cloud, on open |
| Three reply chips | `reply` (generation → action) | cloud, on open |

The **AI X-ray** panel on the right shows every call: which route was picked and why, what was tried, tokens, latency, and whether the result was shown or hidden.

---

## Setup (≈5 minutes)

You need **JDK 17+** and nothing else. No Android SDK, no Xcode, no API key.

```bash
git clone <URL-ON-THE-SLIDE> quietbox && cd quietbox
git checkout start
./gradlew :core:jvmTest          # ✅ setup works if this ends with "20 tests completed, 20 failed"
./gradlew :app:run               # the desktop app (the same Compose code that runs on Android and iOS)
```

The first build downloads about 400 MB. If the Wi-Fi is struggling, ask us for the USB stick.

Optional: if you have the Android SDK, `./gradlew :androidApp:installDebug` installs the app on a device. If you have Xcode, run `cd iosApp && xcodegen && open QuietBox.xcodeproj`.

### Engines

Every command uses the **mock** engine by default. It is deterministic, works offline, and behaves like a real model would, including the occasional bad answer.

To use the real model through our proxy (the token is on the slide):

```bash
export QUIETBOX_ENGINE=proxy QUIETBOX_PROXY_URL=<on the slide> QUIETBOX_TOKEN=<on the slide>
./gradlew :app:run
./gradlew :core:eval -Pengine=proxy
```

The API key stays on the proxy and never reaches the app. That is part of the lesson.

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
  cloud/      CloudModel · MockCloud · ProxyCloud · FlakyCloud (chaos)
  engine/     InboxEngine (when each task fires) · Proactive (nudges)
  telemetry/  Span per call, the data behind the X-ray
  eval/       golden-set scoreboard  →  ./gradlew :core:eval
app/        Compose Multiplatform UI (desktop · Android · iOS)
androidApp/ Android shell (one Activity)
iosApp/     iOS shell (one SwiftUI file, xcodegen)
proxy/      Ktor server calling Claude (structured outputs, effort, refusal fallbacks)
```

The "on-device model" in this repo uses heuristics, on purpose. The architecture doesn't care what sits behind `AiTask.onDevice()`: a regex, Gemini Nano through ML Kit GenAI, Apple's Foundation Models, or a TFLite classifier. The contract is the same: return a value **and a confidence**, or `null`.

## Running the proxy yourself

```bash
export ANTHROPIC_API_KEY=sk-ant-...      # yours
export QUIETBOX_TOKEN=any-shared-secret
./gradlew :proxy:run                     # :8787, model claude-opus-5-5, structured outputs, low effort for background tasks
```
