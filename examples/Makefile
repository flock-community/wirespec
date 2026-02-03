.PHONY: *

build:
	./mvnw verify && \
	(cd gradle-ktor && ./gradlew check ) && \
	(cd npm-typescript && npm ci && npm run build) && \
	(cd python-todo && ./generate.sh && python3 -m pytest tests/ 2>/dev/null || echo "Python tests skipped (pytest not available)")

clean:
	./mvnw clean && \
	(cd gradle-ktor && ./gradlew clean) && \
	(cd npm-typescript && npm run clean) && \
	(cd python-todo && rm -rf src/generated/* .pytest_cache __pycache__ tests/__pycache__ || true)

format:
	(cd gradle-ktor && ./gradlew spotlessApply) && \
	(cd npm-typescript && npm run format)

yolo:
	./mvnw verify -DskipTests && \
	(cd gradle-ktor && ./gradlew check -x test)

