./gradlew :src:plugin:cli:linkReleaseExecutableMacosArm64

src/plugin/cli/build/bin/macosArm64/releaseExecutable/cli.kexe compile -i ./verify/todo.ws -o ./verify/python/gen -l python --shared --ir

docker run --rm \
  -v "$(pwd)/verify/python/gen:/app/gen" \
  python:3.12-slim \
  bash -c "pip install mypy && cd /app && python -m mypy --disable-error-code=empty-body gen/"
