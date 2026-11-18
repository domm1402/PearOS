# pearOS Studio

pearOS Studio is the first Android app in the pearOS project: an OS design and source workspace with a desktop-inspired interface.

## Current starter release
- Dark pearOS Studio interface
- Project overview and starter configuration editor
- File picker for importing existing OS/project files
- Appearance concept screen
- Build preview explainer
- GitHub Actions workflow that builds and uploads a debug APK

## Build
Open this repository in Android Studio and run the app, or use the **Android APK** workflow under GitHub Actions. The workflow uploads `pearOS-debug-apk`.

## Important scope note
This APK is an OS creation workspace, not a replacement operating system. A bootable Android-based OS requires a device-specific Android source tree, kernel, vendor blobs, device configuration and bootloader support. Importing an image into the app does not flash or install it.
