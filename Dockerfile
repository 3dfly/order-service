# Stage 1: Build
FROM --platform=linux/amd64 gradle:8.4.0-jdk21 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle gradle
RUN gradle dependencies --no-daemon
COPY . .
RUN gradle build -x test --no-daemon

# Stage 2: Run
FROM --platform=linux/amd64 eclipse-temurin:21-jre
WORKDIR /app

# Install system dependencies for 3D printing slicer and PrusaSlicer
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    xz-utils \
    unzip \
    bzip2 \
    build-essential \
    libgl1 \
    libgl1-mesa-dri \
    libglib2.0-0 \
    libxrender1 \
    libxrandr2 \
    libxss1 \
    libxtst6 \
    libgtk-3-0 \
    libdrm2 \
    libxcomposite1 \
    libasound2t64 \
    libfuse2 \
    libglu1-mesa \
    libglx0 \
    libglx-mesa0 \
    libegl1 \
    && rm -rf /var/lib/apt/lists/*

# Install PrusaSlicer 2.7.0 for production slicing
# Uses tar.bz2 format (last stable version with this distribution method)
RUN wget -q https://github.com/prusa3d/PrusaSlicer/releases/download/version_2.7.0/PrusaSlicer-2.7.0+linux-x64-GTK3-202311231454.tar.bz2 -O /tmp/PrusaSlicer.tar.bz2 \
    && mkdir -p /opt/PrusaSlicer \
    && tar -xjf /tmp/PrusaSlicer.tar.bz2 -C /opt/ \
    && rm /tmp/PrusaSlicer.tar.bz2 \
    && ln -s /opt/PrusaSlicer-2.7.0+linux-x64-GTK3-202311231454/bin/prusa-slicer /usr/local/bin/prusa-slicer \
    && chmod +x /usr/local/bin/prusa-slicer

# Optional: Keep smart-slicer.py as backup for development/testing
COPY smart-slicer.py /usr/local/bin/smart-slicer.py
RUN chmod +x /usr/local/bin/smart-slicer.py

# Create directories for printing operations
RUN mkdir -p /tmp/printing-calculations \
    && mkdir -p /app/slicer-configs

# Copy slicer configuration (we'll create this)
COPY slicer-configs/ /app/slicer-configs/

# Copy and set up the application
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080

# Use non-root user for security (but give access to slicer)
RUN addgroup --system spring && adduser --system spring --ingroup spring \
    && chown -R spring:spring /tmp/printing-calculations \
    && chown -R spring:spring /app/slicer-configs \
    && chmod +x /usr/local/bin/prusa-slicer

USER spring:spring
ENTRYPOINT ["java", "-jar", "app.jar"]
