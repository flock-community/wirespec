FROM debian:latest

RUN mkdir -p /app/types/

COPY src/plugin/cli/build/bin/linuxX64/releaseExecutable/cli.kexe /app/wirespec

WORKDIR /app

CMD /app/wirespec compile -d $(pwd)/types -l Java -l Kotlin -l Scala -l TypeScript
