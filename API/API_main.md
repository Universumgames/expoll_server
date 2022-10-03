# API - Documentation

## Endpoint overview

Configured on our server, the API is accessible via the `/api` Endpoint, but the backend itself, without any proxy configuration (like nginx) is accessible via the `/` (root) directory.

| Endpoint         | Summary                                                                                                                   |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------- |
| `/user`          | [User Endpoints](./User_API.md) - Register, Login and manage currently logged in user                                     |
| `/poll`          | [Poll Endpoints](./Poll_API.md) - Create, edit and manage polls                                                           |
| `/vote`          | [Vote Endpoints](./Voting_API.md) - Create and edit votes                                                                 |
| `/admin`         | [Admin Endpoints](./Admin_API.md) - Manage users and polls                                                                |
| `/auth`          | [Authentication Endpoints](./Authentication_API.md) - Create and manage webauthn credentials                              |
| `/test`          | [Test Endpoint](#test-endpoint) Test Endpoint, for checking that server is running                                        |
| `/metaInfo`      | [Meta Information](#meta-info) Endpoint, review your Reverse Proxy Settings to see, what HTTP Headers the server receives |
| `/serverInfo`    | [Server Info](#server-info) Retrieve basic server information (like backend version, port, base link, login mail sender)  |
| `/notifications` | [Notification Endpoints](./Notification_API.md) - Create and manage notification preferences                              |

## Config files

The configuration files are in the `./config/` directory with the default configuration being in the `./config/default.json` file. By creating a new file in this directory ending with `.json` these values can be overwritten.
Inside the config following values can be changed:

-   `mailServer` (string): the mailserver to login in to to send mail
-   `mailPort` (int) the port of the mailserver
-   `mailSecure` (boolean) ssl connection to mail server
-   `mailUser` (string) the mail user
-   `mailPassword` (string) the password to login
-   `serverPort` (int) the port the api server listens to
-   `frontEndPort` (int) the port the frontend server runs at (important for mail login)
-   `superAdminMail` (string) the mail address of the user that, if newly created will be promoted to an admin, can be changed down the line, even though this change will not be saved to the database (automatically)
-   `database`
    -   `type` (string) the database type
    -   `host` (string) the ip/hostname of the database
    -   `port` (number) the port the database is accessible through
    -   `rootPW` (string) the root password to the database
-   `maxPollCountPerUser` (number) restrict the number of polls each user can create
-   `recaptchaAPIKey` the api key to use google recaptcha
-   `webauthn`
    -   `rpName` (string) the name of the relying party
    -   `rpID` (string) the unique identifier of the relying party
    -   `origin` (string) the origin from where the user can login

## Detailed information about object structure

Detailed information about inner object structure, request and response objects can be found in the [lib](https://git.mt32.net/mt32/expoll_lib) repository.

## Return code overview

```ts
export enum ReturnCode {
    OK = 200,
    BAD_REQUEST = 400,
    MISSING_PARAMS = 400,
    INVALID_PARAMS = 400,
    UNAUTHORIZED = 401,
    INVALID_LOGIN_KEY = 401,
    INVALID_CHALLENGE_RESPONSE = 401,
    CAPTCHA_INVALID = 401,
    FORBIDDEN = 403,
    CHANGE_NOT_ALLOWED = 403,
    NOT_ACCEPTABLE = 406,
    USER_EXISTS = 406,
    CONFLICT = 409,
    INVALID_TYPE = 409,
    PAYLOAD_TOO_LARGE = 413,
    TOO_MANY_POLLS = 413,
    UNPROCESSABLE_ENTITY = 422,
    INTERNAL_SERVER_ERROR = 500,
    NOT_IMPLEMENTED = 501
}
```

## Login Method

Login is handled not with password but with a login key which is received at signup and sent per mail when requested via username or the users mail address.
This loginKey is required to either be send in the request body or as cookie.

Detailed request list:

-   Path `/user/login`
-   HTTP Method `POST`
-   required fields (JSON) (or)
    -   `mail` (string) the users mail address if a login-mail/loginKey is needed
    -   `loginKey` (string) the received loginKey
-   returns
    -   200 (OK) and userdata
    -   400 (Bad Request) missing loginKey/mail or invalid mail
    -   401 (Unauthorized) passed loginKey invalid

## Test endpoint

When backend is running, this endpoint returns a 200 OK with the message "Test Successful".

-   Path `/test`
-   HTTP Method `GET`
-   required JSON fields: none
-   returns JSON: `message` (string) "Test Successful"

## Meta Info

Returns meta information about the request, it's purpose is to test the proxy settings.

-   Path `/metaInfo`
-   HTTP Method `GET`
-   returns JSON: request data

## Server Info

Returns backend server information. Partially the configuration of the server is returned.

-   Path `/serverInfo`
-   HTTP Method `GET`
-   required JSON fields: none
-   returns JSON:
    -   `version` (string) the version of the backend server
    -   `serverPort` (int) the port the backend is running on (the proxy should redirect to)
    -   `frontendPort` (int) the port the frontend is running on (for login mail sending)
    -   `loginLinkBase` (string) the base of the login link
    -   `mailSender` (string) the mail address the mails are sent from
-   example response:
    ```json
    {
        "version": "2.4.0",
        "serverPort": 6060,
        "frontendPort": 80,
        "loginLinkBase": "expoll.mt32.net",
        "mailSender": "no-reply@universegame.de"
    }
    ```
