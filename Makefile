compile:
	$(shell pwd)/scripts/compile.sh
.PHONY: compile

test:
	$(shell pwd)/scripts/test.sh
.PHONY: test

build:
	$(shell pwd)/scripts/build.sh
.PHONY: build

run:
	$(shell pwd)/scripts/run.sh
.PHONY: run

clean:
	$(shell pwd)/scripts/clean.sh
.PHONY: clean

all:
	$(shell pwd)/scripts/all.sh
.PHONY: all
