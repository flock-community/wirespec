.PHONY: *

build:
	./mvnw clean test && \
	(cd gradle-ktor && ./gradlew clean test) && \
	(cd npm-typescript && npm run build)

clean:
	./mvnw clean && \
	(cd gradle-ktor && ./gradlew clean) && \
	(cd npm-typescript && npm run clean)
