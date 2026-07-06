# ─── Stage 1: Build the Android APK ────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-jammy AS builder

LABEL maintainer="Law-Glance Team"
LABEL description="Android build environment for Law Glance app"

# Install required system tools
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    unzip \
    git \
    && rm -rf /var/lib/apt/lists/*

# ── Android SDK Setup ──────────────────────────────────────────────────────────
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH="${PATH}:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools"

# Download Android command-line tools
RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools \
 && wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdline-tools.zip \
 && unzip -q /tmp/cmdline-tools.zip -d /tmp/cmdline-tools \
 && mv /tmp/cmdline-tools/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/latest \
 && rm /tmp/cmdline-tools.zip

# Accept licenses and install SDK components (API 35 matches compileSdk in build.gradle.kts)
RUN yes | sdkmanager --licenses \
 && sdkmanager \
    "platform-tools" \
    "platforms;android-35" \
    "build-tools;35.0.0"

# ── Build the App ──────────────────────────────────────────────────────────────
WORKDIR /project

# Copy the entire Android project
COPY . .

# Inject the OPENAI_API_KEY into gradle.properties at build time
ARG OPENAI_API_KEY=""
RUN sed -i "s|^OPENAI_API_KEY=.*|OPENAI_API_KEY=${OPENAI_API_KEY}|" gradle.properties

# Make gradlew executable and build the debug APK
RUN chmod +x gradlew \
 && ./gradlew assembleDebug --no-daemon --stacktrace

# ─── Stage 2: Artifact Export ──────────────────────────────────────────────────
# This stage is a minimal image that just holds the built APK.
# Use `docker cp` or a volume mount to extract it:
#   docker build --target export -t law-glance-apk .
#   docker run --rm -v $(pwd)/output:/output law-glance-apk
FROM alpine:3.19 AS export

WORKDIR /output

COPY --from=builder /project/app/build/outputs/apk/debug/app-debug.apk ./law-glance-debug.apk

# Default command: list what's in /output
CMD ["ls", "-lh", "/output"]
