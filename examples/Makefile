.PHONY: *

build:
	(cd gradle-ktor && ./gradlew check) && \
	(cd maven-preprocessor && ./mvnw verify) && \
	(cd maven-spring-avro && ./mvnw verify) && \
	(cd maven-spring-boot-4-integration && ./mvnw verify) && \
	(cd maven-spring-compile && ./mvnw verify) && \
	(cd maven-spring-convert && ./mvnw verify) && \
	(cd maven-spring-custom && ./mvnw verify) && \
	(cd maven-spring-integration && ./mvnw verify) && \
	(cd maven-spring-kotlin && ./mvnw verify) && \
	(cd maven-spring-native && ./mvnw verify) && \
	(cd maven-wiremock && ./mvnw verify) && \
	(cd npm-typescript && npm ci && npm run build) && \
	(cd rust-petstore && bash gen.sh && cargo build) && \
	(cd scala-zio && sbt compile)

clean:
	(cd gradle-ktor && ./gradlew clean) && \
	(cd maven-preprocessor && ./mvnw clean) && \
	(cd maven-spring-avro && ./mvnw clean) && \
	(cd maven-spring-boot-4-integration && ./mvnw clean) && \
	(cd maven-spring-compile && ./mvnw clean) && \
	(cd maven-spring-convert && ./mvnw clean) && \
	(cd maven-spring-custom && ./mvnw clean) && \
	(cd maven-spring-integration && ./mvnw clean) && \
	(cd maven-spring-kotlin && ./mvnw clean) && \
	(cd maven-spring-native && ./mvnw clean) && \
	(cd maven-wiremock && ./mvnw clean) && \
	(cd npm-typescript && npm run clean) && \
	(cd scala-zio && sbt clean)

format:
	(cd gradle-ktor && ./gradlew spotlessApply) && \
    (cd maven-preprocessor && ./mvnw test-compile -Pformat) && \
	(cd maven-spring-avro && ./mvnw test-compile -Pformat) && \
	(cd maven-spring-boot-4-integration && ./mvnw test-compile -Pformat) && \
	(cd maven-spring-compile && ./mvnw test-compile -Pformat) && \
	(cd maven-spring-convert && ./mvnw test-compile -Pformat) && \
	(cd maven-spring-custom && ./mvnw test-compile -Pformat) && \
	(cd maven-spring-integration && ./mvnw test-compile -Pformat) && \
	(cd maven-spring-native && ./mvnw test-compile -Pformat) && \
	(cd maven-wiremock && ./mvnw test-compile -Pformat) && \
	(cd npm-typescript && npm run format)

yolo:
	(cd gradle-ktor && ./gradlew check -x test) && \
	(cd maven-preprocessor && ./mvnw verify -DskipTests) && \
	(cd maven-spring-avro && ./mvnw verify -DskipTests) && \
	(cd maven-spring-boot-4-integration && ./mvnw verify -DskipTests) && \
	(cd maven-spring-compile && ./mvnw verify -DskipTests) && \
	(cd maven-spring-convert && ./mvnw verify -DskipTests) && \
	(cd maven-spring-custom && ./mvnw verify -DskipTests) && \
	(cd maven-spring-integration && ./mvnw verify -DskipTests) && \
	(cd maven-spring-kotlin && ./mvnw verify -DskipTests) && \
	(cd maven-spring-native && ./mvnw verify -DskipTests) && \
	(cd maven-wiremock && ./mvnw verify -DskipTests)
