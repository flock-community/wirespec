FROM debian:latest

RUN mkdir -p /app/types/

COPY compiler/build/bin/linuxX64/releaseExecutable/compiler.kexe /app

WORKDIR /app

CMD /app/compiler.kexe $(pwd) Kotlin,TypeScript
