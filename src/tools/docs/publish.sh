npm install
npm run build
docker buildx build --platform linux/amd64 -t europe-docker.pkg.dev/wirespec-421108/wirespec-docker/wirespec-docs .
docker push europe-docker.pkg.dev/wirespec-421108/wirespec-docker/wirespec-docs
gcloud run deploy wirespec-docs --image=europe-docker.pkg.dev/wirespec-421108/wirespec-docker/wirespec-docs --project=wirespec-421108 --region=europe-west4
