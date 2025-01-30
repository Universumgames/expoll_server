#!/bin/bash

cd ..

python3 ./scripts/openAPIBundle.py src/main/resources/openapi/openapi_v4.yaml src/main/resources/openapi/openapi.yaml > /dev/null
echo "Documentation updated: $(date)"

while sleep 5; do
  python3 ./scripts/openAPIBundle.py src/main/resources/openapi/openapi_v4.yaml src/main/resources/openapi/openapi.yaml > /dev/null
  echo "Documentation updated: $(date)"
done
