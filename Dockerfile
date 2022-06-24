FROM debian:latest

RUN mkdir -p /app/types/

COPY build/bin/linuxX64/debugExecutable/wireSpec.kexe /app

WORKDIR /app

CMD /app/wireSpec.kexe $(pwd) Kotlin,TypeScript
