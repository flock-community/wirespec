test:
	$(shell pwd)/scripts/test.sh
.PHONY: test

build:
	$(shell pwd)/scripts/build.sh
.PHONY: build

image:
	$(shell pwd)/scripts/image.sh
.PHONY: image

clean:
	$(shell pwd)/scripts/clean.sh
.PHONY: clean

all:
	$(shell pwd)/scripts/all.sh
.PHONY: all

mac:
	$(shell pwd)/scripts/mac.sh
.PHONY: mac

linux:
	$(shell pwd)/scripts/linux.sh
.PHONY: linux

example:
	$(shell pwd)/scripts/example.sh
.PHONY: example

publish:
	$(shell pwd)/scripts/publish.sh
.PHONY: publish
