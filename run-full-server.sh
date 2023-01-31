# docker compose rm -f ./docker/compose-full-server.yml
export COMPOSE_PROJECT_NAME=expoll
docker compose -f ./docker/compose-full-server.yml up --detach --build --force-recreate