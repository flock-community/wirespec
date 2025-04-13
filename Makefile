.PHONY: *

# The first command will be invoked with `make` only and should be `all`
all: build image test example

build:
	make build-wirespec && make build-site

build-wirespec:
	./gradlew build && (cd src/ide/vscode && npm i && npm run build)

build-site:
	(cd src/site && make build)

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

yolo:
	$(shell pwd)/scripts/yolo.sh
