#!/bin/bash
set -e

WORKSPACE_DIR="$(pwd)"

echo "==> تثبيت Android SDK..."
mkdir -p "$HOME/android-sdk/cmdline-tools"
cd "$HOME/android-sdk/cmdline-tools"
curl -sSLo tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q tools.zip
mv cmdline-tools latest
rm tools.zip

{
  echo 'export ANDROID_HOME=$HOME/android-sdk'
  echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools'
} >> "$HOME/.bashrc"

export ANDROID_HOME="$HOME/android-sdk"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"

yes | sdkmanager --licenses > /dev/null
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

echo "sdk.dir=$ANDROID_HOME" > "$WORKSPACE_DIR/local.properties"

echo "==> تم التجهيز! جرب: ./gradlew assembleDebug"
