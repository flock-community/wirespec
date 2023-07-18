#!/usr/bin/env bash

dir="$(dirname -- "$0")"

jvm="$dir"/../types/out/jvm
cd "$jvm" && (cd java && javac community/flock/wirespec/generated/*.java)
cd "$jvm" && (cd java && javac community/flock/openapi/generated/*.java)
cd "$jvm" && (cd kotlin && kotlinc community/flock/wirespec/generated/*.kt)
cd "$jvm" && (cd kotlin && kotlinc community/flock/openapi/generated/*.kt)
cd "$jvm" && (cd scala && scalac community/flock/wirespec/generated/*.scala)
cd "$jvm" && (cd scala && scalac community/flock/openapi/generated/*.scala)
cd "$jvm" && (cd typescript && tsc --noEmit community/flock/wirespec/generated/*.ts)
cd "$jvm" && (cd typescript && tsc --noEmit community/flock/openapi/generated/*.ts)

native="$dir"/../types/out/native
cd "$native" && (cd java && javac community/flock/wirespec/generated/*.java)
cd "$native" && (cd java && javac community/flock/openapi/generated/*.java)
cd "$native" && (cd kotlin && kotlinc community/flock/wirespec/generated/*.kt)
cd "$native" && (cd kotlin && kotlinc community/flock/openapi/generated/*.kt)
cd "$native" && (cd scala && scalac community/flock/wirespec/generated/*.scala)
cd "$native" && (cd scala && scalac community/flock/openapi/generated/*.scala)
cd "$native" && (cd typescript && tsc --noEmit community/flock/wirespec/generated/*.ts)
cd "$native" && (cd typescript && tsc --noEmit community/flock/openapi/generated/*.ts)

node="$dir"/../types/out/node
cd "$node" && (cd java && javac community/flock/wirespec/generated/*.java)
cd "$node" && (cd java && javac community/flock/openapi/generated/*.java)
cd "$node" && (cd kotlin && kotlinc community/flock/wirespec/generated/*.kt)
cd "$node" && (cd kotlin && kotlinc community/flock/openapi/generated/*.kt)
cd "$node" && (cd scala && scalac community/flock/wirespec/generated/*.scala)
cd "$node" && (cd scala && scalac community/flock/openapi/generated/*.scala)
cd "$node" && (cd typescript && tsc --noEmit community/flock/wirespec/generated/*.ts)
cd "$node" && (cd typescript && tsc --noEmit community/flock/openapi/generated/*.ts)
