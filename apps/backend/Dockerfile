FROM gradle:8.14.3-jdk21 AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/home/gradle/.gradle gradle --no-daemon bootJar

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S wavelength && adduser -S wavelength -G wavelength
WORKDIR /app
COPY --from=build /workspace/build/libs/wavelength.jar app.jar
USER wavelength
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
