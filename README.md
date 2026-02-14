# TikTok VASP — Scrolling Behavior Research Platform

A custom Android application built for the **VASP Lab** at the **University of Alberta** to study user scrolling behavior in a TikTok-style video feed. The app replicates the core TikTok experience while capturing granular behavioral data for psychology research.

---

## Purpose

Researchers configure timed sessions where participants scroll through short-form videos. The app silently records swipe kinematics, engagement actions, watch durations, and randomized interruption responses — then exports everything as structured CSV files for analysis.

## Key Features

| Area | Details |
|---|---|
| **Video Feed** | Vertical-swipe ViewPager2 feed with ExoPlayer, looping playback, and auto-advancement |
| **Swipe Analytics** | Full touch-path capture (coordinates, timestamps, pressure) with derived velocity, acceleration, jerk, straightness, and smoothness metrics |
| **Engagement Tracking** | Like, comment, bookmark, and share toggles per video — all logged per viewing instance |
| **Random Interruptions** | Configurable gray-screen pauses (15–30 s) at random intervals (30–60 s) that block all input |
| **Physical Units** | Pixel measurements are converted to meters via device DPI for cross-device comparability |
| **Session Management** | Researcher-facing landing screen to set participant ID, video folder, duration, and toggle features |
| **Data Export** | Automatic CSV export on session end: `play_by_play` (one row per video view) and `session_data` (aggregate per video) plus legacy JSON/CSV swipe dumps |
| **Swipe Pattern PNGs** | Optional auto-generated images of each swipe path for visual inspection |

## Architecture

```
com.example.tiktokvasp
├── adapters/          VideoAdapter — RecyclerView.Adapter + ExoPlayer lifecycle
├── components/        Compose UI: top bar, bottom bar, overlays, analytics viz
├── model/             Video data class
├── repository/        MediaStore-backed video loading
├── screens/           Landing, Main (feed), Debug, EndOfExperiment
├── tracking/          UserBehaviorTracker, SwipeAnalyticsService, SessionManager, DataExporter
├── util/              TikTokSwipeDetector, PhysicalUnitsConverter, StableVelocityTracker
└── viewmodel/         MainViewModel, LandingViewModel, DebugViewModel
```

**Tech stack:** Kotlin · Jetpack Compose + XML Views · Media3 ExoPlayer · ViewPager2 · Accompanist · MVVM with StateFlow

## Getting Started

1. Clone the repo and open in Android Studio.
2. Build with **compileSdk 34** / **minSdk 24**.
3. Place `.mp4` video files in a folder on the device's external storage.
4. Grant storage permissions when prompted.
5. On the landing screen, enter a participant ID, select the video folder, configure session duration, and tap **Start Session**.

Exported data is written to the device at:
```
Android/data/com.example.tiktokvasp/files/Documents/TikTokVasp/<participantId>/<category>/
```

## Output Schema (Play-by-Play CSV)

Each row represents one video viewing instance:

| Column | Description |
|---|---|
| Video Number | Consistent index from the original (unshuffled) video list |
| Watch Duration (ms) | Actual watch time, excluding any interruption overlay time |
| Watch Percentage | Ratio of watch duration to video duration (can exceed 1.0 on rewatch) |
| Watch Count | Number of full loops during this viewing session |
| Liked / Shared / Commented | Engagement toggles captured at time of exit |
| Interruption Occurred | Whether a random pause was shown during this view |
| Interruption Duration (ms) | Length of the pause |
| Time Since Last Interruption (ms) | Gap between consecutive interruption starts |
| Exit Swipe Velocity (m/s) | Physical velocity of the swipe that left this video |
| Exit Swipe Distance (m) | Physical distance of the exit swipe |

## Acknowledgements

This project is designed for academic research at the University of Alberta. The UI layout is referenced from TikTok's design. Some icons are derived from [this Figma community pack](https://www.figma.com/design/yUNfBmJC9SKCp1DHSsH9sJ/TikTok-UI-2024--Community-). Claude was used in places for debugging and refactoring.
