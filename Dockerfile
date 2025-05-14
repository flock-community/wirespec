FROM debian:12-slim

RUN mkdir -p /app/types && \
    apt update && \
    apt install -y curl unzip zip python3-mypy && \
    apt clean && \
    curl -fsSL "https://deb.nodesource.com/setup_22.x" | bash && \
    curl -s "https://get.sdkman.io" | bash

SHELL ["/bin/bash", "-c"]

RUN source "$HOME/.sdkman/bin/sdkman-init.sh" && \
    apt install -y nodejs && \
    apt clean && \
    sdk install kotlin && \
    sdk install java && \
    npm install -g typescript

RUN apt update && \
    apt install -y python3 python3-pip && \
    apt clean

COPY src/plugin/cli/build/bin/linuxX64/releaseExecutable/cli.kexe /app/wirespec
COPY scripts/docker/compileTypes.sh /app/compileTypes.sh

WORKDIR /app

ENTRYPOINT ["/bin/bash", "-c"]
