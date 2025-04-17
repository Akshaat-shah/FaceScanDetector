# Face Metrics - Usage Guide

This guide explains how to use the Face Metrics Android application effectively.

## Getting Started

### Installation

1. Install the APK file on your Android device
2. Open the app from your app drawer
3. Grant camera permission when prompted (this is required for the app to function)

### Basic Usage

When you first open the app, you'll see the camera preview with your face. Position your face so it's clearly visible in the frame.

## Understanding the Interface

The interface is designed to be minimal and non-intrusive, displaying relevant information around the edges of the screen:

```
┌───────────────────────────────────────────┐
│                                           │
│  ┌───────────┐               ┌───────────┐│
│  │ Position  │               │  Quality  ││
│  │ Metrics:  │               │  Metrics: ││
│  │           │               │           ││
│  │ Distance: │      Face     │ Quality:  ││
│  │ 65mm      │    Detection  │ 0.87      ││
│  │           │      Area     │           ││
│  │ Face Size:│               │ Smiling:  ││
│  │ 134x142px │               │ Yes (0.9) ││
│  │           │               │           ││
│  │ Center:   │               │ Eyes:     ││
│  │ X: +2mm   │               │ Open      ││
│  │ Y: -4mm   │               │           ││
│  └───────────┘               └───────────┘│
│                                           │
│                                           │
│                                           │
│                                           │
│  ┌─────────────────────────────────────┐  │
│  │          Orientation Data:          │  │
│  │   Pitch: -2.3°  Roll: 1.5°  Yaw: 0° │  │
│  └─────────────────────────────────────┘  │
│                                           │
└───────────────────────────────────────────┘
```

### Metrics Panels

#### Left Panel: Position Metrics
- **Distance**: Estimated distance between eyes (interpupillary distance)
- **Face Size**: Width and height of detected face in pixels
- **Center Offset**: How far your face is from the center of the frame

#### Right Panel: Quality Metrics
- **Quality Score**: Overall face detection confidence (0-1)
- **Smile Detection**: Whether you're smiling and confidence score
- **Eye State**: Whether eyes are open or closed
- **Glasses**: Detection if you're wearing glasses

#### Bottom Panel: Orientation Data
- **Pitch**: Head tilting up (+) or down (-)
- **Roll**: Head tilting sideways (clockwise or counterclockwise)
- **Yaw**: Head turning left or right

### Visual Elements

- **Green Bounding Box**: Outlines the detected face
- **Landmark Points**: Small dots indicating detected facial features
- **Center Crosshair**: Reference point for ideal face positioning

## Tips for Best Results

1. **Lighting**: Use in well-lit environments for more accurate detection
2. **Distance**: Keep your face about 30-50cm from the camera
3. **Position**: Try to center your face in the frame for the most accurate metrics
4. **Stability**: Hold the device steady for more consistent readings
5. **Glasses**: The app works with glasses, but may have slightly reduced accuracy
6. **Facial Hair**: Dense facial hair may affect some metrics

## Troubleshooting

### Face Not Detected
- Check lighting conditions
- Make sure your face is fully visible in the frame
- Clean your camera lens if it appears blurry

### Metrics Seem Inaccurate
- Improve lighting conditions
- Hold the device more steadily
- Keep your face at the recommended distance

### App Crashes or Freezes
- Restart the app
- Make sure your device meets the minimum requirements
- Check if a newer version of the app is available

## Privacy Information

All processing happens on your device. No images or facial data are stored or transmitted over the internet.