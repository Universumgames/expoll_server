# Authentication Endpoints

| Method            | Path   | Summary                                                           |
| ----------------- | ------ | ----------------------------------------------------------------- |
| `/auth/simple`    | POST   | [Request info](#simple-login) - Basic login via mail verification |
| `/auth/logout`    | DELETE | [Request info](#logout-session) - Logout the current session      |
| `/auth/logoutAll` | DELETE | [Request info](#logout-all) - Logout all sessions                 |
| `/auth/webauthn`  | many   | TODO                                                              |

## Simple Login

Endpoint to either request a login mail or to retrieve a cookie with the loginKey provided.
This endpoint is designed for webclients wanting to send the loginkey in the future over a cookie. The cookie will be provided over this endpoint.

Detailed request list:

-   Path `/auth/simple`
-   HTTP Method `POST`
-   data (JSON body):
    -   `loginKey` if already recieved
    -   `mail` to request a login Mail
-   returns (no key provided):
    -   200 (OK)
    -   An EMail will be sent to the user (if he is registered)
-   returns (loginKey provided):
    -   a cookie

## Logout Session

Logout the current session

-   Path `/auth/logout`
-   HTTP Method `DELETE`
-   required data: loginKey (cookie or request body)
-   optional Data: `shortKey` the first 4 characters of the login key that should be deleted, if no key is provide, the current one is used for deletion
-   returns an empty cookie (if active session is used) and HTTP Status 200

## Logout All

Delete all sessions rom current user

-   Path `/auth/logoutAll`
-   HTTP Method `DELETE`
-   required data: loginKey (cookie or request body)
-   returns an empty cookie and HTTP Status 200

## WebAuthn Endpoints

TODO add webauth endpoints
