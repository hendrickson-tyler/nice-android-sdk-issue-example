# NICE CXone Chat SDK - Android Sample

This repository contains a minimal Android application demonstrating the initial setup and configuration of the **NICE CXone Chat SDK** for Kotlin-based projects.

The main goal of this project is to showcase how to correctly create the required configuration object (`SocketFactoryConfiguration`) and initialize the central SDK singleton (`ChatInstanceProvider`) within the application lifecycle.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio (latest version recommended)
- Minimum SDK version compatible with the CXone SDK (check your `build.gradle` or SDK documentation)
- A physical Android device or emulator running the target SDK version

### Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/laxmansahni/nice-cxone-mobile-sample-android.git
   cd nice-cxone-mobile-sample-android
   ```

2. **Open in Android Studio:**
   - Open the cloned directory in Android Studio.

3. **Sync Gradle:**
   - Allow Android Studio to download the necessary dependencies.

4. **Run the application:**
   - Build and run the app on your chosen device or emulator.

---

## ⚙️ Core Configuration

The core configuration for the NICE CXone Chat SDK is defined in the `CxoneSampleApplication.kt` file.

### SDK Parameters

The initialization currently uses the following placeholder parameters:

| Parameter | Placeholder | Description |
|------------|--------------|-------------|
| **Environment/Region** | `CXoneEnvironment.<YOUR_REGION>` | The target NICE CXone data center region |
| **Brand ID** | `<YOUR_BRAND_ID>` | Your organization's unique Brand Identifier |
| **Channel ID** | `<YOUR_CHANNEL_ID>` | The specific channel/queue ID for the chat service |

> ⚠️ Replace these placeholder values with your **actual CXone credentials** to connect to your production or testing environment.

---

### Initialization (`CxoneSampleApplication.kt`)

The SDK is initialized early in the application lifecycle within the `onCreate()` method of the custom `Application` class.

```kotlin
// app/src/main/java/com/isos/cxone/CxoneSampleApplication.kt

override fun onCreate() {
    super.onCreate()

    // 1. Create the Configuration
    val config = SocketFactoryConfiguration(
        CXoneEnvironment.<YOUR_REGION>.value, // Replace with actual region
        <YOUR_BRAND_ID>, // Replace with your Brand ID
        "<YOUR_CHANNEL_ID>" // Replace with your Channel ID
    )

    // 2. Create the singleton instance
    ChatInstanceProvider.create(config)

    // 3. Initiate the prepare phase
    ChatInstanceProvider.get().prepare(this)
}
```

This ensures that the `ChatInstanceProvider` singleton is available and the SDK is in the **Prepared** state as soon as possible after the app starts.

---

## 📝 Next Steps for Development

Once the initialization is confirmed, here are the immediate next steps to build out the functional chat application:

1. **Implement Listeners:**
   Implement `ChatInstanceProvider.Listener` in your main `Activity` to track the SDK state changes (`Prepared`, `Connecting`, `Connected`, `Ready`, `ConnectionLost`, etc.).

2. **Connect the Chat:**
   Once the state transitions to `Prepared`, call `ChatInstanceProvider.get().connect()` to establish the WebSocket connection and check for agent availability.

3. **UI Integration:**
   Implement the user interface for sending and receiving messages once the state reaches `Ready`.

4. **Error Handling:**
   Add robust logging and user-facing error messages to handle exceptions reported via `onChatRuntimeException`.

---

© 2025 NICE CXone Android Sample — Maintained by [Laxman Sahni](https://github.com/laxmansahni)
