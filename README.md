# ActiveGo - Practice 5: WearOS-Phone Project

Universidad Tecnologica de la Riviera Maya (UTRM)
Ingenieria en Desarrollo y Gestion de Software Multiplataforma - Smart Devices Development
Team: Yair Gamaliel Guzman Perez / Adriel Solano - TI-91

Multi-device implementation of the **ActiveGo** fitness application, built on top of
the team's *Multi-Device Application Functional Requirements Document* and the
*ActiveGo - Multi-Device UI Design* Figma file.

---

## Scope

The FRD specifies three form factors: mobile phone, smartwatch and smart display.
This practice targets the **smartwatch + phone pair**; the smart display is out of
scope and no part of it is implemented here.

The watch has no physical sensors on the Wear OS emulator, so the readings described
in section 3.1 of the FRD (steps, heart rate, calories) are produced by
`MetricsSimulator`. Everything downstream of that class behaves exactly as it would
with real sensors, because the rest of the app only ever sees an `ActivitySnapshot`.

## The two implemented features

| # | Feature | FRD requirements | Wearable API | Why this API |
|---|---------|------------------|--------------|--------------|
| 1 | Workout Control Sync | FR-01, FR-02 | `MessageClient` | Start / pause / resume / stop are ephemeral events. They only matter at the instant they happen, so fire-and-forget delivery over a live connection is the correct trade-off. |
| 2 | Activity Data Sync | FR-03, FR-04, FR-05, FR-06 | `DataClient` | Today's totals and the user's goals are durable state. The data item is replicated, survives a disconnection and is delivered automatically on reconnection, which satisfies FR-05 with no buffering code of our own. |

Both features are **bidirectional**:

- Feature 1: a workout can be started, paused, resumed and stopped from either device.
- Feature 2: the watch writes activity to the phone; the phone writes goals to the watch.

## Communication contract

Defined once in `WearableConstants.kt` (duplicated byte for byte in both modules).

| Transport | Path | Payload |
|-----------|------|---------|
| MessageClient | `/activego/workout/control` | UTF-8 string: `START:RUNNING`, `START:CYCLING`, `START:STRENGTH`, `PAUSE`, `RESUME`, `STOP` |
| DataClient | `/activego/activity/today` | `steps`, `calories`, `active_minutes`, `heart_rate`, `elapsed_seconds`, `workout_calories`, `distance_meters`, `watch_battery`, `session_id`, `workout_type`, `workout_state`, `updated_at` |
| DataClient | `/activego/goals` | `goal_steps`, `goal_calories`, `goal_active_minutes`, `updated_at` |

Three constraints that the implementation depends on:

1. **`updated_at` is mandatory in every DataMap.** `DataClient` de-duplicates: a
   DataMap that is byte-for-byte identical to the stored one is treated as unchanged
   and `onDataChanged()` never fires on the peer.
2. **Both modules share `applicationId` and signing configuration.** If they differ
   the Wearable APIs will not associate the two apps, and they fail silently.
3. **`MessageClient` does not guarantee delivery.** Anything that must survive a
   disconnection goes through `DataClient`.

## Module layout

```
ActiveGo/
├── mobile/                         Phone app - central hub (FRD section 3.2)
│   └── com/example/activego/
│       ├── WearableConstants.kt    Communication contract
│       ├── DataModel.kt            ActivitySnapshot, Goals, WorkoutCommand
│       ├── MainActivity.kt         SENDS: workout commands + goals
│       ├── data/
│       │   ├── PhoneRepository.kt      In-memory state, single source of truth
│       │   └── PhoneListenerService.kt RECEIVES: commands + activity snapshots
│       └── ui/                     Compose Material 3 screens
└── wear/                           Watch app - data collection (FRD section 3.1)
    └── com/example/activego/
        ├── WearableConstants.kt    Identical copy
        ├── DataModel.kt            Identical copy
        ├── ActiveGoWearApplication.kt  Starts the sensor simulation
        ├── data/
        │   ├── WearRepository.kt       In-memory state
        │   ├── MetricsSimulator.kt     Stand-in for the physical sensors
        │   ├── WearListenerService.kt  RECEIVES: commands + goals
        │   ├── ActivityPayload.kt      Builds the DataClient request (one wire format)
        │   └── Haptics.kt              Vibration confirmation (feature F-01)
        └── presentation/
            └── MainActivity.kt     SENDS: workout commands + activity snapshots
```

The sending half of each feature lives in `MainActivity.kt` on both sides; the
receiving half lives in a `WearableListenerService`. That split is deliberate: Google
Play services can start a listener service even when the app is closed, which is what
makes the reconnection scenario of FR-05 observable without a screen open.

`WearableConstants.kt` and `DataModel.kt` are duplicated instead of extracted into a
shared Gradle module. The duplication is the known cost of keeping the build simple;
the two copies must be edited together.

## Visual design (Figma fidelity)

The UI is built from the team's Figma file *ActiveGo - Multi-Device UI Design*,
watch page and the second iteration of the phone page (the lime one, which matches
the watch and the smart display).

| Element | Source |
|---------|--------|
| Colours | Exact Figma values: background `#090B09`, cards `#121612`, borders `#232A23`, tracks `#1C221C`, lime `#CCFF00`, green `#30D158`, red `#FF3B30`, grey `#8B958B` |
| Fonts | **Outfit** (numbers, titles, buttons) and **Geist** (labels), bundled in `res/font` as static TTFs |
| Icons | Every icon was exported from the Figma nodes as SVG and converted 1:1 to Android vector drawables (`ic_<name>_<size>.xml`). They are Lucide icons drawn with Figma's 2 px stroke at the exact size used in each frame |
| Avatar | Generic silhouette avatar (`DefaultAvatar`) in the Figma greys, with the user name *Yair Guzman*, instead of the stock photo and name of the mock-up |
| Rings and bars | Drawn in Compose with the Figma geometry (watch ring 186 dp with a 5 % band; phone rings 140 / 110 / 80 dp with 12 dp bands), because their fill depends on live data |

The watch frames are laid out on the 198 x 198 dp Figma canvas and scaled to the
real screen, so proportions hold on small and large round watches.

### Screens

| Figma frame | Implemented as |
|-------------|----------------|
| `watch-home` | `WatchHomeScreen` (tap the step count to start a workout, tap the heart / battery row for the summary) |
| `watch-start-workout` | `WatchStartWorkoutScreen` |
| `watch-active-workout` | `WatchActiveWorkoutScreen`: lime play (start / resume), blue pause, red stop. Picking a workout opens it in `READY`; nothing starts until play is pressed |
| `watch-daily-summary` | `WatchDailySummaryScreen` (tap to go back) |
| `home-dashboard` | `HomeDashboardScreen` |
| `start-workout` | `StartWorkoutScreen` (Workout tab while idle) |
| `active-workout` | `ActiveWorkoutScreen` (Workout tab): same lime play / blue pause / red stop controls as the watch; *Start Session* opens it in `READY` and play starts the session |
| `workout-history` | `WorkoutHistoryScreen`, filled from finished sessions (FR-07) |
| `fitness-goals` | `FitnessGoalsScreen`; the `+` button opens the goal editor that syncs to the watch |
| `settings-device-pairing` | `SettingsScreen` on the Profile tab, with the real watch link and sync time |

### Deliberate differences from the mock-up

- The iOS status bar and home indicator drawn in the phone frames are replaced by
  Android's own system bars (edge to edge, dark).
- `GPS Connected` on the live screen shows the real watch link (`Watch Connected`
  / `Watch Offline`); the emulator has no GPS.
- The second goal is `Daily Active Time` instead of `Weekly Active Time`, so it
  matches the daily value the watch measures.
- The smart panel card is listed as *Not paired*: the smart display is outside
  this practice.
- Both devices use the same three session controls (lime play, blue pause, red stop) and wait in `READY` until play is pressed. On the watch, (lime play, blue pause, red stop) instead of the Figma pause / stop pair. The blue (`#0A84FF`) is not in the Figma file; it is the system blue of the same palette as the design's red and green.
- The simulated day starts from a mid-day baseline (about 7,800 steps, 470 kcal, 26 active minutes), like the numbers in the Figma mock-ups, so the rings and bars have realistic proportions on first launch. Workout values (timer, distance, session calories) always start at zero.
- `watch-reminder` is not implemented; reminders (F-07) are not one of the two
  features of this practice.

## Running it

1. Open the project in Android Studio and let Gradle sync.
2. In Device Manager create a phone emulator (API 30+) and a Wear OS emulator, then
   pair them with **Pair Wearable** on the phone emulator.
3. Run the `mobile` configuration on the phone and the `wear` configuration on the watch.
4. On the watch home screen, confirm it reads `PHONE LINKED`.

### Demonstration script

| Step | Action | Expected result |
|------|--------|-----------------|
| 1 | Watch: tap the step count, pick *Outdoor Run*, then press the lime play button | The watch shows `READY` until play is pressed; then it vibrates, the timer starts and the phone jumps to the live screen by itself |
| 2 | Phone: tap the blue pause button (or the blue pause button on the watch) | Watch vibrates and shows `PAUSED`; its timer freezes |
| 3 | Watch: tap the lime play button | Both devices resume |
| 4 | Phone: Goals tab, `+`, change the targets, *Save & Sync* | Watch vibrates; its ring and daily summary recalculate against the new goals |
| 5 | Turn off the watch emulator's connection and keep the workout running | Watch keeps counting; phone Profile tab shows `Disconnected` and the sync time stops |
| 6 | Restore the connection | The phone receives the accumulated data without any action (FR-05) |
| 7 | Stop the workout on either device | The session appears in the phone's History tab |

Steps 5 and 6 are the evidence for FR-05 and are worth capturing in the report.
