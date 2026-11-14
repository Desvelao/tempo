FROM eclipse-temurin:17-jdk-jammy
ARG CMDLINE_TOOLS_BUILD=15859902
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV GRADLE_USER_HOME=/gradle
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

RUN apt-get update && apt-get install -y --no-install-recommends curl unzip git \
 && rm -rf /var/lib/apt/lists/*
RUN mkdir -p $ANDROID_HOME/cmdline-tools /gradle && chmod 777 /gradle \
 && curl -fsSL -o /tmp/clt.zip \
    https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_BUILD}_latest.zip \
 && unzip -q /tmp/clt.zip -d $ANDROID_HOME/cmdline-tools \
 && mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest \
 && rm /tmp/clt.zip
RUN yes | sdkmanager --licenses >/dev/null \
 && sdkmanager "platform-tools" "platforms;android-36" "platforms;android-37.0" "build-tools;36.0.0" \
 && chmod -R a+rwX $ANDROID_HOME
WORKDIR /workspace
