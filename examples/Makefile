.PHONY: *

build:
	./mvnw clean verify && \
	(cd gradle-ktor && ./gradlew clean check) && \
	(cd npm-typescript && npm run build)

clean:
	./mvnw clean && \
	(cd gradle-ktor && ./gradlew clean) && \
	(cd npm-typescript && npm run clean)

format:
	(cd gradle-ktor && ./gradlew spotlessApply)
