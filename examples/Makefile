.PHONY: *

build:
	./mvnw clean verify && \
	(cd gradle-ktor && ./gradlew clean build) && \
	(cd npm-typescript && npm run build)

clean:
	./mvnw clean && \
	(cd gradle-ktor && ./gradlew clean) && \
	(cd npm-typescript && npm run clean)
