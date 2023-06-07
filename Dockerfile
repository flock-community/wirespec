FROM debian:latest

RUN mkdir -p /app/types/

COPY compiler/cli/build/bin/linuxX64/releaseExecutable/cli.kexe /app

WORKDIR /app

CMD /app/cli.kexe -l Java -l Kotlin -l Scala -l TypeScript $(pwd)/types
