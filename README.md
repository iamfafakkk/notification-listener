# 📡 Notification Interceptor

> An Android app that listens to every notification on your device and forwards matching ones to user-defined API endpoints — powered by Room, OkHttp, and Jetpack Compose.

---

## ✨ Features

- 🔔 **Real-time interception** — listens to all device notifications via `NotificationListenerService`
- 📋 **Rule-based routing** — match notifications by package name and forward them to different API endpoints
- 🌐 **Flexible API calls** — supports `POST`, `GET`, `PUT`, `PATCH`, and `DELETE` with custom headers
- 💾 **Persistent rules** — rules saved locally with Room database (survives app restarts)
- 🧪 **Built-in test tool** — send a dummy notification to validate your rules without a real app
- 🎨 **Premium dark UI** — built with Jetpack Compose + Material 3

---

## 🏗️ Architecture

```
com.karuhundeveloper.notification/
│
├── data/
│   ├── local/
│   │   ├── entity/          InterceptRule.kt        ← Room @Entity
│   │   ├── dao/             InterceptRuleDao.kt     ← CRUD + Flow
│   │   ├── converter/       Converters.kt           ← List/Map TypeConverters
│   │   └──                  AppDatabase.kt          ← Room singleton
│   └── model/
│       └──                  NotificationPayload.kt  ← API request body
│
├── network/
│   ├──                      NotificationPusher.kt   ← OkHttp call builder
│   └──                      PushResult.kt           ← Sealed result type
│
├── service/
│   └──                      AppNotificationListener.kt ← NotificationListenerService
│
└── ui/
    ├── theme/               Theme.kt                ← Material3 dark theme
    ├── component/           MethodBadge.kt          ← Shared composables
    ├── screen/
    │   ├──                  MainScreen.kt           ← Rules list
    │   └──                  AddRuleScreen.kt        ← Add/create rule form
    ├── viewmodel/           RulesViewModel.kt       ← AndroidViewModel
    └──                      MainActivity.kt         ← NavHost entry point
```

---

## 🛠️ Tech Stack

| Layer | Library | Version |
|---|---|---|
| UI | Jetpack Compose + Material 3 | BOM `2024.09.00` |
| Navigation | Navigation Compose | `2.8.1` |
| ViewModel | Lifecycle ViewModel Compose | `2.8.6` |
| Database | Room | `2.6.1` |
| Networking | OkHttp | `4.12.0` |
| JSON | Gson | `2.10.1` |
| Min SDK | 24 (Android 7.0) | |
| Target SDK | 36 | |
| Language | Kotlin `2.0.21` + KSP | |

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 11
- Android device or emulator running API 24+

### Build & Run

```bash
# Clone the repo
git clone https://github.com/your-username/NotificationInterceptor.git
cd NotificationInterceptor

# Open in Android Studio and let Gradle sync, or build from CLI:
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

---

## 🔐 Permissions Setup

The app requires two special permissions that must be granted **manually** by the user.

### 1. Notification Listener Access *(required for interception)*

The app will show a red warning banner on the main screen if this is not granted.

1. Tap the **Grant** button in the banner, or go to:
   **Settings → Apps → Special app access → Notification access**
2. Find **Notification Interceptor** and toggle it **ON**
3. Return to the app — the banner disappears automatically

### 2. Post Notifications *(required for the dummy test button)*

On Android 13+ (API 33), the app will prompt for this permission the first time you tap **Send Dummy**.

---

## 📖 How to Use

### Creating a Rule

1. Tap **＋ Add Rule** on the main screen
2. Fill in the fields:

   | Field | Example | Notes |
   |---|---|---|
   | Rule Name | `WhatsApp → Slack` | Human-readable label |
   | Target Apps | `com.whatsapp, com.telegram.messenger` | Comma-separated package names |
   | API URL | `https://hooks.slack.com/services/…` | Must start with `http://` or `https://` |
   | HTTP Method | `POST` | Choose from GET / POST / PUT / PATCH / DELETE |
   | Custom Headers | `{"Authorization": "Bearer xyz"}` | Optional flat JSON object |

3. Tap **Save** — the rule appears in the list immediately

### Testing the Pipeline

1. Create a rule with **Target Apps** set to this app's own package name:
   `com.karuhundeveloper.notification`
2. Tap **Send Dummy** on the main screen
3. The service intercepts the notification and calls your API
4. Check Logcat (tag: `AppNotificationListener`) to see the push result

---

## 🌐 API Payload

Every matched notification is forwarded as a JSON body:

```json
{
  "app_name":            "WhatsApp",
  "notification_title":  "John Doe",
  "notification_text":   "Hey, are you free tonight?",
  "timestamp_ms":        1720432800000
}
```

For **GET** requests, the same fields are sent as URL query parameters instead of a body.  
Custom headers from the rule (e.g. `Authorization`, `X-Source`) are attached to every request.

---

## 🧩 Data Model

```kotlin
@Entity(tableName = "intercept_rules")
data class InterceptRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ruleName: String,
    val targetApps: List<String>,           // stored as JSON array
    val apiUrl: String,
    val apiMethod: String,                  // "POST", "GET", etc.
    val customHeaders: Map<String, String>  // stored as JSON object
)
```

---

## 📡 Push Result Handling

`NotificationPusher.push()` returns a sealed `PushResult` — no exceptions propagate to the caller:

```kotlin
when (result) {
    is PushResult.Success           -> // HTTP 2xx  ✅
    is PushResult.HttpError         -> // HTTP 4xx / 5xx  ⚠️
    is PushResult.NetworkError      -> // No connectivity / timeout  ❌
    is PushResult.UnsupportedMethod -> // Unknown HTTP method  ❌
}
```

---

## 📝 Logcat

Filter in Android Studio by tag to see real-time pipeline activity:

```
tag:AppNotificationListener
```

| Emoji | Meaning |
|---|---|
| ✅ | Notification successfully delivered to API |
| ⚠️ | Server returned a non-2xx status |
| ❌ | Network failure or unsupported method |

---

## 🗺️ Roadmap

- [ ] Rule enable/disable toggle
- [ ] Edit existing rules
- [ ] Per-rule push history and success rate counter
- [ ] Retry with exponential backoff
- [ ] Hilt dependency injection
- [ ] Rule import / export (JSON file)
- [ ] Filter by notification title / body keyword (regex support)
- [ ] Webhook test ping from rule card

---

## 🤝 Contributing

Pull requests are welcome! For major changes, please open an issue first to discuss what you'd like to change.

---

## 📄 License

```
MIT License — Copyright (c) 2026 karuhundeveloper

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is furnished
to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
```
