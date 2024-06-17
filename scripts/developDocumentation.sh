#!/bin/bash

cd ..

./scripts/openAPIBundle.sh src/main/resources/openapi/openapi_v4.yaml src/main/resources/openapi/openapi.yaml > /dev/null
echo "Documentation updated: $(date)"

while sleep 5; do
  ./scripts/openAPIBundle.sh src/main/resources/openapi/openapi_v4.yaml src/main/resources/openapi/openapi.yaml > /dev/null
  echo "Documentation updated: $(date)"
done
