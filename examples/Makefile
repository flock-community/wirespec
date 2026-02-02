.PHONY: *

build:
	./mvnw verify && \
	(cd gradle-ktor && ./gradlew check ) && \
	(cd npm-typescript && npm ci && npm run build)

clean:
	./mvnw clean && \
	(cd gradle-ktor && ./gradlew clean) && \
	(cd npm-typescript && npm run clean)

format:
	(cd gradle-ktor && ./gradlew spotlessApply) && \
	(cd npm-typescript && npm run format)

yolo:
	./mvnw verify -DskipTests && \
	(cd gradle-ktor && ./gradlew check -x test)

