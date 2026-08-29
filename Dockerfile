# syntax=docker/dockerfile:1
#
# Root-context build of the backend.
#
# This is a duplicate of backend/Dockerfile, differing only in that every path
# is prefixed with backend/. It exists so the image builds whether the platform
# uses the repository root as its build context or the backend directory:
# Render reports "failed to read dockerfile: open Dockerfile: no such file or
# directory" when Root Directory is unset, and pointing Dockerfile Path at
# backend/Dockerfile does not help, because the context stays at the root and
# every COPY then misses.
#
# Keep the two in step. If you change one, change the other.

# ---- build ----------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Dependencies resolve from the POM alone, so copying it first means a source
# change does not re-download the world on every rebuild.
COPY backend/pom.xml .
RUN mvn -B dependency:go-offline

COPY backend/src ./src

# Tests are not run here. Four suites use Testcontainers, which needs a Docker
# daemon of its own, and an image build has no way to give them one. Run the
# suite in CI, where a daemon is available, rather than pretending an image
# build can gate it.
RUN mvn -B clean package -DskipTests

# ---- runtime --------------------------------------------------------------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Unprivileged: nothing this process does needs root, and a container that
# breaks out as root is a much worse day than one that breaks out as nobody.
RUN groupadd --system buglens && useradd --system --gid buglens buglens

COPY --from=build /build/target/*.jar app.jar
RUN chown buglens:buglens app.jar
USER buglens

EXPOSE 8080

# Shell form so ${PORT} expands at runtime. Render (and most PaaS hosts) inject
# PORT and expect the process to bind to it; falling back to SERVER_PORT keeps
# the variable the application already documents working, then 8080.
#
# exec makes java PID 1, so it receives SIGTERM directly and shuts down
# gracefully instead of being killed after the stop timeout.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar --server.port=${PORT:-${SERVER_PORT:-8080}}"]
