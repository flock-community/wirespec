.PHONY: *

build:
	./mvnw -f maven-preprocessor/pom.xml verify && \
	./mvnw -f maven-spring-avro/pom.xml verify && \
	./mvnw -f maven-spring-compile/pom.xml verify && \
	./mvnw -f maven-spring-convert/pom.xml verify && \
	./mvnw -f maven-spring-custom/pom.xml verify && \
	./mvnw -f maven-spring-integration/pom.xml verify && \
	./mvnw -f maven-spring-boot-4-integration/pom.xml verify && \
	(cd gradle-ktor && ./gradlew check ) && \
	(cd npm-typescript && npm ci && npm run build) && \
	(cd rust-petstore && ./gen.sh && cargo build) && \
	(cd scala-zio && ./gen.sh && sbt test)

clean:
	./mvnw -f maven-preprocessor/pom.xml clean && \
	./mvnw -f maven-spring-avro/pom.xml clean && \
	./mvnw -f maven-spring-compile/pom.xml clean && \
	./mvnw -f maven-spring-convert/pom.xml clean && \
	./mvnw -f maven-spring-custom/pom.xml clean && \
	./mvnw -f maven-spring-integration/pom.xml clean && \
	./mvnw -f maven-spring-boot-4-integration/pom.xml clean && \
	(cd gradle-ktor && ./gradlew clean) && \
	(cd npm-typescript && npm run clean) && \
	(cd rust-petstore && cargo clean) && \
	(cd scala-zio && sbt clean)

format:
	(cd gradle-ktor && ./gradlew spotlessApply) && \
	(cd npm-typescript && npm run format)

yolo:
	./mvnw -f maven-preprocessor/pom.xml verify -DskipTests && \
	./mvnw -f maven-spring-avro/pom.xml verify -DskipTests && \
	./mvnw -f maven-spring-compile/pom.xml verify -DskipTests && \
	./mvnw -f maven-spring-convert/pom.xml verify -DskipTests && \
	./mvnw -f maven-spring-custom/pom.xml verify -DskipTests && \
	./mvnw -f maven-spring-integration/pom.xml verify -DskipTests && \
	./mvnw -f maven-spring-boot-4-integration/pom.xml verify -DskipTests && \
	(cd gradle-ktor && ./gradlew check -x test)
