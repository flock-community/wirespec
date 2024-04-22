FROM debian:latest

RUN mkdir -p /app/types/

COPY src/plugin/cli/build/bin/linuxX64/releaseExecutable/cli.kexe /app/wirespec

WORKDIR /app

CMD /app/wirespec compile -d $(pwd)/types -l Java -o $(pwd)/types/out/docker/java && \
    /app/wirespec compile -d $(pwd)/types -l Kotlin -o $(pwd)/types/out/docker/kotlin && \
    /app/wirespec compile -d $(pwd)/types -l Scala -o $(pwd)/types/out/docker/scala && \
    /app/wirespec compile -d $(pwd)/types -l TypeScript -o $(pwd)/types/out/docker/typescript && \
    /app/wirespec compile -d $(pwd)/types -l Wirespec -o $(pwd)/types/out/docker/wirespec
