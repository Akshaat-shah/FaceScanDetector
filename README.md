# Face Metrics

An Android application that uses the device's front camera to detect faces and display real-time facial metrics.

![Face Metrics App](generated-icon.png)

## Features

- Real-time face detection using the front-facing camera
- Facial metrics visualization including:
  - Interpupillary distance
  - Head orientation (pitch, roll, yaw)
  - Face quality score
  - Facial landmark positions
- Clean, intuitive user interface
- On-device processing with no data transmission
- Supports portrait and landscape orientations

## Requirements

- Android device with Android 5.0 (Lollipop) or higher
- Front-facing camera
- Camera permissions granted to the app

## Installation

See [APK Installation Guide](resources/ApkInstallationGuide.md) for detailed instructions on installing the app on your Android device.

## Usage

1. Launch the Face Metrics app
2. Grant camera permission when prompted
3. Position your face in the camera view
4. Real-time metrics will be displayed on the screen:
   - Left panel: Positional metrics
   - Right panel: Quality metrics
   - Bottom panel: Orientation data

For more detailed usage instructions, see [USAGE.md](USAGE.md).

## Development Setup

### Prerequisites

- Android Studio Arctic Fox (2021.3.1) or newer
- JDK 11 or higher
- Android SDK with minimum API level 21

### Building from Source

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/face-metrics.git
   ```

2. Open the project in Android Studio

3. Sync Gradle files and build the project:
   ```
   ./gradlew build
   ```

4. Run the app on an emulator or physical device:
   ```
   ./gradlew installDebug
   ```

## Technical Information

- Built with Kotlin
- Uses CameraX API for camera integration
- Face detection via ML Kit
- MVVM architecture
- Material Design components

For more technical details, see [DESIGN.md](DESIGN.md).

## Privacy

This application processes all data on-device. No facial data or images are transmitted or stored permanently. Camera access is used exclusively for real-time processing.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Google ML Kit for face detection capabilities
- CameraX library for simplified camera integration
- Material Design components for UI elements