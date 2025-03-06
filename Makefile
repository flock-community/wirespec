.PHONY: *

define measure_time
	d=$$(date +%s); $(2); echo "Command:$(1) took $$(($$(date +%s)-d)) seconds"
endef

# The first command will be invoked with `make` only and should be `all`
all:
	@$(call measure_time, all, make build image test example)

build:
	@$(call measure_time, build, $(shell pwd)/scripts/build.sh)

clean:
	@$(call measure_time, clean, $(shell pwd)/scripts/clean.sh)

compile:
	@$(call measure_time, compile, ./gradlew assemble)

compile-types:
	@$(call measure_time, compile-types, $(shell pwd)/scripts/compileTypes.sh)

example:
	@$(call measure_time, example, $(shell pwd)/scripts/example.sh)

jvm:
	@$(call measure_time, jvm, ./gradlew jvmTest)

format:
	@$(call measure_time, format, $(shell pwd)/scripts/format.sh)

image:
	@$(call measure_time, image, $(shell pwd)/scripts/image.sh)

local:
	@$(call measure_time, local, $(shell pwd)/scripts/local.sh)

publish:
	@$(call measure_time, publish, ./gradlew publish)

test:
	@$(call measure_time, test, $(shell pwd)/scripts/test.sh)

update:
	@$(call measure_time, update, npm install -g @vscode/vsce)
