.PHONY: *

build:
	(cd maven-preprocessor && ../mvnw verify) && \
	(cd maven-spring-avro && ../mvnw verify) && \
	(cd maven-spring-compile && ../mvnw verify) && \
	(cd maven-spring-convert && ../mvnw verify) && \
	(cd maven-spring-custom && ../mvnw verify) && \
	(cd maven-spring-integration && ../mvnw verify) && \
	(cd maven-spring-boot-4-integration && ../mvnw verify) && \
	(cd gradle-ktor && ./gradlew check) && \
	(cd npm-typescript && npm ci && npm run build)

clean:
	(cd maven-preprocessor && ../mvnw clean) && \
	(cd maven-spring-avro && ../mvnw clean) && \
	(cd maven-spring-compile && ../mvnw clean) && \
	(cd maven-spring-convert && ../mvnw clean) && \
	(cd maven-spring-custom && ../mvnw clean) && \
	(cd maven-spring-integration && ../mvnw clean) && \
	(cd maven-spring-boot-4-integration && ../mvnw clean) && \
	(cd gradle-ktor && ./gradlew clean) && \
	(cd npm-typescript && npm run clean)

format:
	(cd gradle-ktor && ./gradlew spotlessApply) && \
	(cd npm-typescript && npm run format)

yolo:
	(cd maven-preprocessor && ../mvnw verify -DskipTests) && \
	(cd maven-spring-avro && ../mvnw verify -DskipTests) && \
	(cd maven-spring-compile && ../mvnw verify -DskipTests) && \
	(cd maven-spring-convert && ../mvnw verify -DskipTests) && \
	(cd maven-spring-custom && ../mvnw verify -DskipTests) && \
	(cd maven-spring-integration && ../mvnw verify -DskipTests) && \
	(cd maven-spring-boot-4-integration && ../mvnw verify -DskipTests) && \
	(cd gradle-ktor && ./gradlew check -x test)
