.PHONY: *

# The first command will be invoked with `make` only and should be `all`
all: build image test example format verify

build: build-wirespec build-site

build-site:
	(cd src/site && make build)

build-wirespec:
	./gradlew -Pwirespec.enableNative=true build && (cd src/ide/vscode && npm i && npm run build)

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

# Fast build: JVM + JS only (no klib/native), no tests, then run examples without
# their own tests. Use this for tight local iteration. Native artifacts can be
# produced by adding `-Pwirespec.enableNative=true` to the gradle command.
quick:
	./gradlew --no-configuration-cache -x test \
		publishToMavenLocal \
		:src:plugin:npm:jsNodeProductionLibraryDistribution && \
	(cd examples && make yolo)

publish:
	./gradlew -Pwirespec.enableNative=true publish

test:
	$(shell pwd)/scripts/test.sh

update:
	npm install -g @vscode/vsce

verify:
	./gradlew :src:verify:test -Pverify

yolo:
	$(shell pwd)/scripts/yolo.sh
