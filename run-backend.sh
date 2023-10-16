export COMPOSE_PROJECT_NAME=expoll
docker compose -f ./docker/compose-backend.yml up --detach --build --force-recreate