.PHONY: *

build:
	./mvnw clean verify && \
	(cd wirespec-gradle-ktor && ./gradlew clean build) && \
	(cd npm-typescript && npm run build)

clean:
	./mvnw clean && \
	(cd wirespec-gradle-ktor && ./gradlew clean) && \
	(cd npm-typescript && npm run clean)
