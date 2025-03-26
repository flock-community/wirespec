.PHONY: *

# The first command will be invoked with `make` only and should be `all`
all: format build image test example

build:
	$(shell pwd)/scripts/build.sh

ci: build image test example

clean:
	$(shell pwd)/scripts/clean.sh

compile:
	./gradlew assemble

compile-types:
	$(shell pwd)/scripts/compileTypes.sh

example:
	$(shell pwd)/scripts/example.sh

jvm:
	./gradlew jvmTest

format:
	$(shell pwd)/scripts/format.sh

image:
	$(shell pwd)/scripts/image.sh

local:
	$(shell pwd)/scripts/local.sh

publish:
	./gradlew publish

test:
	$(shell pwd)/scripts/test.sh

update:
	npm install -g @vscode/vsce
