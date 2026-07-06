# syntax=docker/dockerfile:1

# Build the rust-petstore example entirely inside Docker.
# The host needs no Rust/cargo toolchain — only Docker.
#
# The one prerequisite is the prebuilt Wirespec native CLI at ./cli.kexe
# (linuxX64). Produce it from the repo root with:
#   ./gradlew -Pwirespec.enableNative=true :src:plugin:cli:linuxX64Binaries
# then copy build/bin/linuxX64/releaseExecutable/cli.kexe here (gen.sh does
# the equivalent for local builds).
#
#   docker build -t wirespec-petstore .
#   docker run --rm -p 8080:8080 wirespec-petstore

########## Stage 1: generate Rust sources from the OpenAPI spec ##########
# cli.kexe is an x86-64 ELF, so pin this stage to linux/amd64 (runs under
# emulation on arm64 hosts). The output is plain .rs text, copied into the
# native builder stage below.
FROM --platform=linux/amd64 debian:bookworm-slim AS generator
WORKDIR /gen
COPY petstore.json cli.kexe ./
RUN chmod +x cli.kexe && \
    ./cli.kexe convert OpenAPIV2 -i /gen/petstore.json -o /gen/out -l Rust -p '' --shared

########## Stage 2: compile the client and server binaries ##########
FROM rust:1-slim-bookworm AS builder
# reqwest (used by the client binary) links against OpenSSL.
RUN apt-get update && \
    apt-get install -y --no-install-recommends pkg-config libssl-dev && \
    rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY Cargo.toml Cargo.lock ./
COPY src ./src
# Replace any host-generated sources with the freshly generated ones.
RUN rm -rf src/gen
COPY --from=generator /gen/out ./src/gen
RUN cargo build --release --bin server --bin client

########## Stage 3: minimal runtime image ##########
FROM debian:bookworm-slim AS runtime
# libssl3 + CA certs for the reqwest-based client binary.
RUN apt-get update && \
    apt-get install -y --no-install-recommends libssl3 ca-certificates && \
    rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /app/target/release/server /usr/local/bin/server
COPY --from=builder /app/target/release/client /usr/local/bin/client
EXPOSE 8080
CMD ["server"]
