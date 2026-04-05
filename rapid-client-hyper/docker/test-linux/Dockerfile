# Pre-baked Corretto JDK + Rust + Gradle distribution (via wrapper) for JNI tests.
# Build context must be the Gradle project root (see buildTestLinuxImage in build.gradle).
#
#   ./gradlew :rapid-client-hyper:buildTestLinuxImage
#
# Override platform when building the image to match how you run tests, e.g.:
#   ./gradlew :rapid-client-hyper:buildTestLinuxImage -PtestLinuxPlatform=linux/amd64
FROM amazoncorretto:17-al2-jdk

RUN set -eux; \
    yum install -y gcc curl >/dev/null 2>&1; \
    curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs \
        | sh -s -- -y --profile minimal --default-toolchain 1.89.0 --no-modify-path; \
    yum clean all; \
    rm -rf /var/cache/yum

ENV PATH="/root/.cargo/bin:${PATH}"
WORKDIR /work

# Warm Gradle wrapper dist into /root/.gradle so testLinux runs do not re-download it
# (/work is bind-mounted at runtime; user home is not).
COPY gradlew /work/gradlew
COPY gradle/wrapper /work/gradle/wrapper
RUN chmod +x /work/gradlew && cd /work && ./gradlew --version
