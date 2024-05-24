.PHONY: *

all:
	$(shell pwd)/scripts/all.sh

build:
	$(shell pwd)/scripts/build.sh

clean:
	$(shell pwd)/scripts/clean.sh

compile:
	./gradlew assemble --no-configuration-cache

compile-types:
	$(shell pwd)/scripts/compileTypes.sh

example:
	$(shell pwd)/scripts/example.sh

image:
	$(shell pwd)/scripts/image.sh

local:
	$(shell pwd)/scripts/local.sh

publish:
	./gradlew publish --no-configuration-cache

test:
	$(shell pwd)/scripts/test.sh

update:
	npm install -g @vscode/vsce
