1. Download the emulator and system image, and create the virtual device (neutral name avd):
export SDK=$HOME/android-sdk
mkdir -p $SDK $HOME/.android
docker run --rm -it -u $(id -u):$(id -g) -e HOME=$HOME \
  -v $SDK:$SDK -v $HOME/.android:$HOME/.android \
  android-dev bash -c "
    yes | sdkmanager --sdk_root=$SDK 'cmdline-tools;latest' 'platform-tools' 'emulator' \
          'system-images;android-36;google_apis;x86_64' &&
    echo no | $SDK/cmdline-tools/latest/bin/avdmanager create avd -n avd \
          -k 'system-images;android-36;google_apis;x86_64' -d pixel_7"
2. Start it:
ANDROID_HOME=$SDK ANDROID_AVD_HOME=$HOME/.android/avd $SDK/emulator/emulator -avd avd
If it reports missing libraries on Manjaro, try sudo pacman -S nss libpulse libxcb xcb-util-cursor libxkbfile.
3. In another terminal, run docker compose run --rm dev adb devices. It should list emulator-5554. Don't run adb on the host at the same time.
