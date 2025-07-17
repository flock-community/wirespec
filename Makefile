.PHONY: *

# The first command will be invoked with `make` only and should be `all`
all: build image test example format

build:
	make build-wirespec && make build-site

build-site:
	(cd src/site && make build)

build-wirespec:
	./gradlew build && (cd src/ide/vscode && npm i && npm run build)

clean:
	$(shell pwd)/scripts/clean.sh

compile:
	./gradlew assemble

example:
	$(shell pwd)/scripts/example.sh

format:
	$(shell pwd)/scripts/format.sh

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

verify:
	$(shell pwd)/scripts/verify.sh

yolo:
	$(shell pwd)/scripts/yolo.sh
