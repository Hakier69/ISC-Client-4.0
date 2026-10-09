# ISC-Client (Fabric 1.21.8, client-side)

E + Z (together) opens the ISC-Client GUI. The GUI has its own look (dark/purple theme) with the ISC Client logo (`assets/isc_client/textures/gui/logo.png`) at the top. ESC closes it.

## Modules (GUI: three columns)
- **Flight** - speed slider x1-x20 and Anti-Kick (briefly cancels the flight so it keeps working).
- **NoFall** - no fall damage (the server is told you are on the ground while falling).
- **Fake Lag** - other players see you freeze and then jump (movement packets are held and sent later), you play normally.
  - *Lag* - how long the packets are held, *Every* - how long everything is normal between two lags.
  - *See server place* - draws a second you where the server thinks you are while the lag is on.

## Build on GitHub (no local setup)
1. Create a repo and upload the CONTENTS of this folder (build.gradle, src/, .github/ ... must be in the repo root).
2. Open the "Actions" tab -> workflow "Build mod". It runs on every push (or press "Run workflow").
3. When it is green, open the run and download the "ISC-Client" artifact (a zip with the mod .jar inside).
4. Put the .jar in `.minecraft/mods` together with Fabric API.

Release: push a tag like `v0.1.0` and the jar is attached to a GitHub Release automatically.

## Build locally
JDK 21 + Gradle 8.14: `gradle build` -> `build/libs/isc-client-0.1.0.jar`
