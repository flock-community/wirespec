FROM debian:12-slim

RUN mkdir -p /app/types && \
    apt update && \
    apt install -y curl unzip zip && \
    apt clean && \
    curl -fsSL "https://deb.nodesource.com/setup_22.x" | bash && \
    curl -s "https://get.sdkman.io" | bash

SHELL ["/bin/bash", "-c"]

RUN source "$HOME/.sdkman/bin/sdkman-init.sh" && \
    apt install -y nodejs && \
    apt clean && \
    sdk install kotlin && \
    sdk install java && \
    sdk install scala && \
    npm install -g typescript

COPY src/plugin/cli/build/bin/linuxX64/releaseExecutable/cli.kexe /app/wirespec

WORKDIR /app

ENTRYPOINT ["/bin/bash", "-c"]
