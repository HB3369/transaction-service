# ============================================
# STAGE 1: BUILD
# ============================================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

# Metadata
LABEL stage=builder
LABEL description="Build stage for Transaction Service"

# Workspace
WORKDIR /build

# Copia solo pom.xml primero (para cachear dependencias)
# Si pom.xml no cambia, Docker reutiliza esta capa
COPY pom.xml .

# Descarga dependencias (se cachea si pom.xml no cambió)
RUN mvn dependency:go-offline -B

# Ahora copia el código fuente
COPY src ./src

# Compila la aplicación
# -DskipTests: saltamos tests (ya se ejecutaron en CI/CD)
# -B: batch mode (sin colores, más rápido)
RUN mvn clean package -DskipTests -B

# Verifica que el JAR se creó
RUN ls -lh target/*.jar

# ============================================
# STAGE 2: RUNTIME
# ============================================
FROM eclipse-temurin:17-jre-alpine

# Metadata
LABEL maintainer="benas"
LABEL version="0.0.1"
LABEL description="Transaction Service - Production Runtime"

# Instala herramientas útiles (opcional)
# curl: para health checks
# busybox-extras: telnet para debugging
RUN apk add --no-cache curl busybox-extras

# Usuario no-root (seguridad)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Workspace
WORKDIR /app

# Copia SOLO el JAR desde el stage builder
# --from=builder: toma del stage anterior
COPY --from=builder /build/target/*.jar app.jar

# JVM Options (optimizadas para container)
ENV JAVA_OPTS="\
    -Xmx512m \
    -Xms256m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom"

# Expone puerto
EXPOSE 8080

# Health check (Docker verificará que la app está viva)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Ejecuta la app
# exec: para que Java reciba señales de Docker (SIGTERM)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
