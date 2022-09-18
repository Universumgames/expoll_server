TEAM_ID="98ZXK38P8L"
TOKEN_KEY_FILE_NAME="config/AuthKey_58V5WA3RLA.p8"
AUTH_KEY_ID="58V5WA3RLA"
TOPIC="de.universegame.expoll-ios"
DEVICE_TOKEN="18b0797729bc221f88ae01c4ded1936133aa4d734efb9eb223a114465be202b8"
APNS_HOST_NAME=api.sandbox.push.apple.com

JWT_ISSUE_TIME=$(date +%s)
JWT_HEADER=$(printf '{ "alg": "ES256", "kid": "%s" }' "${AUTH_KEY_ID}" | openssl base64 -e -A | tr -- '+/' '-_' | tr -d =)
JWT_CLAIMS=$(printf '{ "iss": "%s", "iat": %d }' "${TEAM_ID}" "${JWT_ISSUE_TIME}" | openssl base64 -e -A | tr -- '+/' '-_' | tr -d =)
JWT_HEADER_CLAIMS="${JWT_HEADER}.${JWT_CLAIMS}"
JWT_SIGNED_HEADER_CLAIMS=$(printf "${JWT_HEADER_CLAIMS}" | openssl dgst -binary -sha256 -sign "${TOKEN_KEY_FILE_NAME}" | openssl base64 -e -A | tr -- '+/' '-_' | tr -d =)
AUTHENTICATION_TOKEN="${JWT_HEADER}.${JWT_CLAIMS}.${JWT_SIGNED_HEADER_CLAIMS}"

curl -v --header "apns-topic: $TOPIC" --header "apns-push-type: alert" --header "authorization: bearer $AUTHENTICATION_TOKEN" --data '{"aps":{"alert":"abcs"}}' --http2 https://${APNS_HOST_NAME}/3/device/${DEVICE_TOKEN}