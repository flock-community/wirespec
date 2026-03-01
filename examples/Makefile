.PHONY: *

build:
	(cd maven-preprocessor && mvn verify) && \
	(cd maven-spring-avro && mvn verify) && \
	(cd maven-spring-compile && mvn verify) && \
	(cd maven-spring-convert && mvn verify) && \
	(cd maven-spring-custom && mvn verify) && \
	(cd maven-spring-integration && mvn verify) && \
	(cd maven-spring-boot-4-integration && mvn verify) && \
	(cd gradle-ktor && ./gradlew check) && \
	(cd npm-typescript && npm ci && npm run build)

clean:
	(cd maven-preprocessor && mvn clean) && \
	(cd maven-spring-avro && mvn clean) && \
	(cd maven-spring-compile && mvn clean) && \
	(cd maven-spring-convert && mvn clean) && \
	(cd maven-spring-custom && mvn clean) && \
	(cd maven-spring-integration && mvn clean) && \
	(cd maven-spring-boot-4-integration && mvn clean) && \
	(cd gradle-ktor && ./gradlew clean) && \
	(cd npm-typescript && npm run clean)

format:
	(cd gradle-ktor && ./gradlew spotlessApply) && \
	(cd npm-typescript && npm run format)

yolo:
	(cd maven-preprocessor && mvn verify -DskipTests) && \
	(cd maven-spring-avro && mvn verify -DskipTests) && \
	(cd maven-spring-compile && mvn verify -DskipTests) && \
	(cd maven-spring-convert && mvn verify -DskipTests) && \
	(cd maven-spring-custom && mvn verify -DskipTests) && \
	(cd maven-spring-integration && mvn verify -DskipTests) && \
	(cd maven-spring-boot-4-integration && mvn verify -DskipTests) && \
	(cd gradle-ktor && ./gradlew check -x test)
