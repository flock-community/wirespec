compile:
	./gradlew build
.PHONY: compile

test:
	./compiler/build/bin/macosX64/releaseExecutable/compiler.kexe $(shell pwd) Kotlin,TypeScript
.PHONY: test

build:
	docker build -t wire-spec .
.PHONY: build

run:
	docker run --rm -it -v $(shell pwd)/types:/app/types wire-spec
.PHONY: run

clean:
	./gradlew clean && docker rmi wire-spec
.PHONY: clean
