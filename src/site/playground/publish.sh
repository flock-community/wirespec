npm install
npm run build
docker buildx build --platform linux/amd64 -t europe-docker.pkg.dev/wirespec-421108/wirespec-docker/wirespec-playground .
docker push europe-docker.pkg.dev/wirespec-421108/wirespec-docker/wirespec-playground