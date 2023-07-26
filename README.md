# IntentTracker

An Xposed module to track intents of android applications

## Features

- Track startActivity intent
- Get stack trace
- Support app Parcelables

## Usage

1. `./gradlew deployCLIDebug`
2. Activate the module and open your target app
3. `adb shell /data/local/tmp/itc`
