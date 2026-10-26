# check=skip=FromPlatformFlagConstDisallowed

# The CLI is only built for linuxX64
FROM --platform=linux/amd64 debian:13-slim

COPY src/plugin/cli/build/bin/linuxX64/releaseExecutable/cli.kexe /app/wirespec

WORKDIR /app

ENTRYPOINT ["/bin/bash", "-c"]
