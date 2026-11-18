# pearOS Studio

pearOS Studio is the Android companion app for designing and prototyping a custom operating-system experience. Its design language blends calm Apple-like spacing, Samsung-style personalisation and Windows-inspired desktop ideas.

## Current app features
- Dark pearOS Studio workspace with responsive navigation
- Project overview and editable system configuration\n- Local persistence for project name and source across app restarts
- Import supported text source/config files from Android's document picker
- Export the current configuration as JSON
- Interactive phone preview with lock screen, home screen, app icons, dock, notifications and quick-settings panel
- Starter configuration templates: Midnight, Minimal and Desktop
- JSON configuration validation in the build centre
- GitHub Actions workflow that builds and uploads a debug APK

## Build the APK
1. Open the **Actions** tab in this repository.
2. Select **Android APK**.
3. Open the latest successful run.
4. Download the `pearOS-debug-apk` artifact.

The project can also be opened in Android Studio with JDK 17 and Android SDK 35.

## Configuration format
The editor currently works with a pearOS JSON configuration document. It is not yet a general-purpose compiler or Linux/Android kernel editor. Text files with common source/config extensions can be imported into the editor; binary archives and disk images are selected as project assets but are not unpacked, flashed or installed.

## Important OS distinction
This APK runs as an Android application. It does not replace Android, unlock the bootloader, flash partitions or boot a separate operating system. A real pearOS device build needs a target device, compatible Android Open Source Project (AOSP) source, device tree, kernel, vendor components, build host and a supported installation method. Those pieces are device-specific and must be developed and tested before a bootable image can be offered.

## Planned engineering milestones
1. Persistent multi-project workspace and file tree
2. Visual phone-first shell designer with editable widgets and app icons
3. Window manager prototype with resize, move, minimise and maximise
4. Project asset manager and archive extraction for safe source bundles
5. Structured configuration schema, validation and migration
6. Build profiles for a specifically supported device
7. AOSP integration and reproducible device builds after hardware target selection
