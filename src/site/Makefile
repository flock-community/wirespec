.PHONY: *

build:
	(cd docs && npm i && npm run build) && \
	(cd playground && npm i && npm run build)

clean:
	(cd docs && npm run clean) && \
    (cd playground && npm run clean)
