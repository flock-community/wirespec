.PHONY: *

build:
	(cd docs && npm i --prefer-offline --no-audit --no-fund && npm run build) && \
	(cd playground && npm i --prefer-offline --no-audit --no-fund && npm run build)

# Deletes build output only, installed node_modules stay
clean:
	(cd docs && npm run clean:docusaurus && npm run clean:build) && \
	(cd playground && npm run clean:build)
