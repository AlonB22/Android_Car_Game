# Task 2 Readme

## Changes Made
- Fixed left/right control behavior while keeping the same button icons.
- Added a crash shake animation to the road when the player hits an obstacle.
- Deepened the crash sound effect and made it more impactful.
- Reworked High Scores to show a per-entry map (lite mode) instead of a single map.
- Added city lookup (Geocoder) for each score when location is available.
- Improved map lifecycle handling inside the score list so maps render reliably.
- Updated the high score layout to embed a map in each row.

## How to Run
1. Open the project in Android Studio.
2. Set a valid Google Maps API key in `app/src/main/res/values/strings.xml`:
   - Replace the value of `google_maps_key` with your key.
   - Do not commit the real key.
3. Sync Gradle and run the `app` configuration on an emulator or device.

## Notes
- If maps show a blank tile, the API key is missing/invalid or billing is not enabled for the key.
- Location permission is required to save coordinates for the high score map and city label.
