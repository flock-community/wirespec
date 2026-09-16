.PHONY: *

# Kotlin/Native target of this machine: `make test` and the examples run its CLI
NATIVE_HOST_Darwin_arm64 := macosArm64
NATIVE_HOST_Darwin_x86_64 := macosX64
NATIVE_HOST_Linux_x86_64 := linuxX64
NATIVE_HOST := $(NATIVE_HOST_$(shell uname -s)_$(shell uname -m))

# The first command will be invoked with `make` only and should be `all`
# Formatting comes first so the build's spotlessCheck passes; examples format as part of their build.
all: format-wirespec build image test example verify

build: build-wirespec build-site

build-site:
	(cd src/site && make build)

# Native builds only the host and linuxX64, whose CLI goes into the Docker image; CI covers the
# other targets. Off a Linux host the linuxX64 tests can't run, so their binaries aren't linked.
build-wirespec:
	./gradlew -Pwirespec.nativeTargets=$(NATIVE_HOST),linuxX64 build $(if $(filter linuxX64,$(NATIVE_HOST)),,-x linuxX64Test) && \
	(cd src/ide/vscode && npm i --prefer-offline --no-audit --no-fund && npm run build)

clean:
	$(shell pwd)/scripts/clean.sh

compile:
	./gradlew assemble

example:
	./gradlew publishToMavenLocal src:plugin:npm:jsNodeProductionLibraryDistribution && ./gradlew buildExamples

format:
	./gradlew spotlessApply formatExamples

format-wirespec:
	./gradlew spotlessApply

image:
	$(shell pwd)/scripts/image.sh

jvm:
	./gradlew jvmTest

local:
	$(shell pwd)/scripts/local.sh

# Fast build: JVM + JS only (no klib/native), no tests, then run examples without
# their own tests. Use this for tight local iteration. Native artifacts can be
# produced by adding e.g. `-Pwirespec.nativeTargets=macosArm64` to the gradle command.
quick:
	./gradlew -x test \
		publishToMavenLocal \
		:src:plugin:npm:jsNodeProductionLibraryDistribution && \
	./gradlew yoloExamples

publish:
	./gradlew -Pwirespec.nativeTargets=macosX64,macosArm64,linuxX64,mingwX64 publish

test:
	$(shell pwd)/scripts/test.sh

update:
	npm install -g @vscode/vsce

verify:
	./gradlew :src:verify:allTests -Pverify

yolo:
	./gradlew src:bom:build compileKotlinJvm -x test && ./gradlew publishToMavenLocal && ./gradlew yoloExamples
