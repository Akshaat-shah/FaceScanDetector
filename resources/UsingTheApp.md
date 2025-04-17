# How to Use the Face Metrics Android App

## Overview

The Face Metrics app captures real-time facial data using your device's front camera. This guide explains each metric and how to interpret the displayed values.

## Main Interface

The app displays your camera feed with three key elements:
1. **Face bounding box** (green rectangle) - Shows the detected face area
2. **Left metrics panel** - Technical measurements about the face position and angles
3. **Right metrics panel** - Analysis scores and detection results

## Left Panel Metrics Explained

| Metric | Description | Good Values |
|--------|-------------|------------|
| Sequence | Frame counter that increments with each processed frame | Higher values indicate smooth processing |
| Pitch | Up/down head rotation in degrees | Near 0° is optimal (looking straight) |
| Roll | Side tilt head rotation in degrees | Near 0° is optimal (head not tilted) |
| Yaw | Left/right head rotation in degrees | Near 0° is optimal (facing camera) |
| Quality | Overall face quality score (0-100) | Above 70 is good |
| Range | Estimated distance from camera | 60-120 is typically good |
| IPD | Interpupillary distance (eye separation) | Varies by person |
| bbRow | Bounding box top position | For reference only |
| bbCol | Bounding box left position | For reference only |
| bbW | Bounding box width | Larger values = closer to camera |
| bbH | Bounding box height | Larger values = closer to camera |

## Right Panel Metrics Explained

| Metric | Description | Good Values |
|--------|-------------|------------|
| Fusion | Combined confidence score | Higher is better |
| Face | Face detection confidence (1=detected, 0=none) | 1 indicates a face is detected |
| Depth | Estimated depth measurement | For reference only |
| PeriL | Left eye region quality score | Higher values indicate better quality |
| PeriR | Right eye region quality score | Higher values indicate better quality |
| Glasses | Glasses detection (1=wearing, 0=none) | Informational only |
| Blink | Blink detection (1=blinking, 0=eyes open) | 0 for quality face capture |
| LiveProb | Liveness probability estimate | Higher values suggest a real face |

## Best Practices

For optimal face detection:
- Ensure good, even lighting on your face
- Position your face in the center of the frame
- Keep a neutral expression looking directly at the camera
- Avoid rapid movements
- Hold the device 30-60cm (1-2 feet) from your face

## Privacy Note

All processing is done on-device. No facial data or images are stored or transmitted.