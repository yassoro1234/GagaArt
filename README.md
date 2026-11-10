# Gaga Art – Professional 3D Drawing & Painting Studio

**Gaga Art** is a premium digital art studio for Android, featuring a modern 3D interface, powerful drawing tools, layers, and a smooth creative experience.

> This repository contains the foundation of the application (Stages 1–3).

## Current Features

### ✅ Stage 1 – Main Interface & Visual Identity
- Premium color palette and typography
- Light / Dark theme support
- 3D-style rounded buttons with soft shadows
- Attractive Home screen with logo, New Drawing, Gallery, Settings, etc.

### ✅ Stage 2 – Drawing Canvas & Touch Input
- Large interactive canvas
- Smooth stroke drawing with quadratic smoothing
- Pinch-to-zoom, pan, and rotation support
- Full-screen focused drawing workspace

### ✅ Stage 3 – Brushes, Pencils & Erasers
- Multiple professional brush types:
  - Pencil, Pen, Marker
  - Paint Brush, Airbrush, Watercolor, Oil Paint
  - Calligraphy, Spray, Glow, Pattern
  - Smudge, Blur, Dodge, Burn
- Adjustable size, opacity, hardness
- Normal / Soft eraser
- Undo / Redo (up to 30 steps)
- Clear canvas

### ✅ Layers System (basic)
- Add / select layers
- Visibility toggle
- Layer panel UI

## Project Structure

```
GagaArt/
├── app/
│   └── src/main/java/com/gagaart/app/
│       ├── engine/          # Drawing engine, brushes, layers, tools
│       ├── ui/
│       │   ├── canvas/      # DrawingCanvas
│       │   ├── components/  # Premium buttons
│       │   ├── screens/     # Home + Drawing screens
│       │   └── theme/       # Colors, Typography, Theme
│       └── MainActivity.kt
├── .github/workflows/       # CI – builds APK automatically
└── README.md
```

## How to Build

### Requirements
- Android Studio Ladybug (2024.2+) or newer
- JDK 17
- Android SDK 35

### Local Build
```bash
./gradlew assembleDebug
```
APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### GitHub Actions
Every push to `main` / `master` / `develop` automatically builds a **Debug APK**.  
You can download it from the **Actions** tab → Artifacts.

You can also trigger a Release build manually from the Actions tab.

## Next Stages (Planned)

- [ ] Stage 4 – Full color system (color wheel, HEX, palettes)
- [ ] Stage 5 – Advanced selection tools + complete layer management
- [ ] Stage 6 – Text, shapes & stickers
- [ ] Stage 7 – Save / Load projects + Export PNG/JPEG
- [ ] Stage 8 – Performance optimization & polish

## Technical Notes

- Built with **Kotlin + Jetpack Compose**
- Custom drawing engine using Android `Bitmap` + `Canvas`
- Layer system with compositing
- Designed for phones and tablets (minSdk 26)

## License

Private / All rights reserved – Gaga Art project.
