# syntax=docker/dockerfile:1.7

ARG BUILD_IMAGE=maven:3.9.15-eclipse-temurin-25-noble
ARG RUNTIME_IMAGE=gcr.io/distroless/base-debian13:nonroot

FROM ${BUILD_IMAGE} AS deps
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
	./mvnw -B -q \
    -Dcompact.constructor.javadoc.param.check.skip=true \
    -Dspotless.check.skip=true \
    -Dpmd.skip=true \
    -Dcpd.skip=true \
    -Dspotbugs.skip=true \
    -Djacoco.skip=true \
    -Dmaven.test.skip=true \
    dependency:go-offline

FROM deps AS build

COPY config/ config/
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
	./mvnw -B -q \
    -Dcompact.constructor.javadoc.param.check.skip=true \
    -Dspotless.check.skip=true \
    -Dpmd.skip=true \
    -Dcpd.skip=true \
    -Dspotbugs.skip=true \
    -Djacoco.skip=true \
    -Dmaven.test.skip=true \
    package

FROM ${BUILD_IMAGE} AS jre
WORKDIR /workspace

COPY --from=build /workspace/target/bet-settler-0.0.1-SNAPSHOT.jar app.jar

RUN mkdir -p /workspace/extracted \
	&& cd /workspace/extracted \
	&& jar -xf ../app.jar \
	&& MODULES="$(jdeps \
		--ignore-missing-deps \
		--multi-release 25 \
		--print-module-deps \
		--recursive \
		--class-path 'BOOT-INF/lib/*' \
		BOOT-INF/classes)" \
	&& jlink \
		--add-modules "${MODULES},jdk.crypto.ec" \
		--strip-debug \
		--strip-java-debug-attributes \
		--no-header-files \
		--no-man-pages \
		--compress=zip-6 \
		--output /opt/bet-settler-jre

FROM ${RUNTIME_IMAGE}
WORKDIR /app

ENV JAVA_HOME=/opt/bet-settler-jre
ENV PATH="${JAVA_HOME}/bin:${PATH}"

COPY --from=jre /opt/bet-settler-jre /opt/bet-settler-jre
COPY --from=build /workspace/target/bet-settler-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["/opt/bet-settler-jre/bin/java", "-jar", "/app/app.jar"]

