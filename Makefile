.PHONY: *

# The first command will be invoked with `make` only and should be `all`
all: build image test example

build:
	$(shell pwd)/scripts/build.sh

clean:
	$(shell pwd)/scripts/clean.sh

compile:
	./gradlew assemble

compile-types:
	$(shell pwd)/scripts/compileTypes.sh

example:
	$(shell pwd)/scripts/example.sh

image:
	$(shell pwd)/scripts/image.sh

jvm:
	./gradlew jvmTest

local:
	$(shell pwd)/scripts/local.sh

publish:
	./gradlew publish

test:
	$(shell pwd)/scripts/test.sh

update:
	npm install -g @vscode/vsce
