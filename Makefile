build:
	./gradlew build
.PHONY: build

run:
	./build/bin/macosX64/releaseExecutable/wireSpec.kexe $(shell pwd) Kotlin,TypeScript
.PHONY: run

clean:
	./gradlew clean
.PHONY: clean
