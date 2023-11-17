.PHONY: *

compile:
	$(shell pwd)/scripts/compile.sh

compile-types:
	$(shell pwd)/scripts/compileTypes.sh

build:
	$(shell pwd)/scripts/build.sh

test:
	$(shell pwd)/scripts/test.sh

image:
	$(shell pwd)/scripts/image.sh

clean:
	$(shell pwd)/scripts/clean.sh

all:
	$(shell pwd)/scripts/all.sh

mac:
	$(shell pwd)/scripts/mac.sh

linux:
	$(shell pwd)/scripts/linux.sh

example:
	$(shell pwd)/scripts/example.sh

publish:
	$(shell pwd)/scripts/publish.sh

update:
	$(shell pwd)/scripts/update.sh
