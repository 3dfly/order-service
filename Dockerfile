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

# Install system dependencies for 3D printing slicer
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    xz-utils \
    unzip \
    build-essential \
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
    && rm -rf /var/lib/apt/lists/*

# Install Python and create smart STL analyzer for production use
RUN apt-get update && apt-get install -y python3 python3-pip \
    && pip3 install numpy-stl --break-system-packages \
    && rm -rf /var/lib/apt/lists/*

# Create production-ready STL analyzer script
COPY smart-slicer.py /usr/local/bin/smart-slicer.py
RUN chmod +x /usr/local/bin/smart-slicer.py \
    && ln -s /usr/local/bin/smart-slicer.py /usr/local/bin/prusa-slicer

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
