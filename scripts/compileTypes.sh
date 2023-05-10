#!/usr/bin/env bash

dir="$(dirname -- "$0")"

jvm="$dir"/../types/out/jvm
cd "$jvm" && (cd java && javac community/flock/wirespec/generated/*.java)
cd "$jvm" && (cd kotlin && kotlinc community/flock/wirespec/generated/*.kt)
cd "$jvm" && (cd scala && scalac community/flock/wirespec/generated/*.scala)
cd "$jvm" && (cd typescript && tsc --noEmit ./*.ts)

native="$dir"/../types/out/native
cd "$native" && (cd java && javac community/flock/wirespec/generated/*.java)
cd "$native" && (cd kotlin && kotlinc community/flock/wirespec/generated/*.kt)
cd "$native" && (cd scala && scalac community/flock/wirespec/generated/*.scala)
cd "$native" && (cd typescript && tsc --noEmit ./*.ts)

node="$dir"/../types/out/node
cd "$node" && (cd java && javac community/flock/wirespec/generated/*.java)
cd "$node" && (cd kotlin && kotlinc community/flock/wirespec/generated/*.kt)
cd "$node" && (cd scala && scalac community/flock/wirespec/generated/*.scala)
cd "$node" && (cd typescript && tsc --noEmit ./*.ts)
