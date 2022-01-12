# API - Documentation

## Endpoints

Configured on our server, the API is accessible via the `/api` Endpoint

| Endpoint       | HTTP Method | Summary                                                                |
| :------------- | :---------: | ---------------------------------------------------------------------- |
| `/user`        |      -      | [Link](#user-endpoints)                                                |
| `/user`        |    POST     | [Details](#create-a-user) - Creating a user                            |
| `/user`        |     GET     | [Details](#get-user-data) - Get user data                              |
| `/user`        |     PUT     | [Details](#edit-user-settings) - Edit own user (Coming soon)           |
| `/user/login`  |    POST     | [Details](#login) - Login via loginKey or request login mail           |
| `/user`        |   DELETE    | Deactivate a user account (Coming soon)                                |
| `/poll`        |      -      | [Link](#poll-endpoints)                                                |
| `/poll`        |     GET     | [Details](#retrieve-polls) - Get poll overview or detailed information |
| `/poll`        |    POST     | [Details](#create-a-poll) - Creating a new Poll                        |
| `/poll`        |     PUT     | [Details](#edit-a-poll) - Editing an existing Poll                     |
| `/vote`        |      -      | [Link](#vote-endpoints)                                                |
| `/vote`        |    POST     | [Details](#vote-or-replace-previous-one) - Vote on a poll              |
| `/admin`       |      -      | [Link](#administration-endpoints)                                      |
| `/admin/users` |      -      | [Link](#user-management)                                               |
| `/admin/users` |     GET     | [Details](#retrieve-user-list) - retrieving all registered users       |
| `/admin/users` |     PUT     | Edit user account - Coming soon                                        |
| `/admin/polls` |     GET     | [Details](#retrieve-poll-list) - Retrieve all existing polls           |

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

## Detailed information about object structure

Detailed information about inner object structure, request and response objects can be found in the [lib](https://git.mt32.net/mt32/expoll_lib) repository.

## Return code overview

-   `200` OK
-   `400` (Bad request) parameters missing/invalid
-   `401` (Unauthorized) LoginKey invalid/expired, Captcha not accepted
-   `406` (Not acceptable) Vote is not acceptable / user already exists (mail or username)
-   `409` (Conflict) wrong parameter type
-   `413` (Payload too large) maximum number of polls created by the user is exceeded

## Login Method

Login is handled not with password but with a login key which is received at signup and sent per mail when requested via username or the users mail address.
This loginKey is required to either be send in the request body or as cookie.

Detailed request list:

-   Path `/user/login`
-   HTTP Method `POST`
-   required fields (JSON)
    -   `mail` (string) the users mail address if a login-mail/loginKey is needed
    -   `loginKey` (string) the received loginKey
-   returns
    -   200 (OK) and userdata
    -   400 (Bad Request) missing loginKey/mail or invalid mail
    -   401 (Unauthorized) passed loginKey invalid

## User-Endpoints

### Create a user

To create a User the full name and the users mail address is needed. When creating a new User the loginKey, a replacement for passwords, is sent back.
The data is passed in the request Body in JSON format.

Detailed request list:

-   Path `/user`
-   HTTP Method `POST`
-   required JSON fields:
    -   `firstName` (String)
    -   `lastName` (String)
    -   `mail` (String)
    -   `username` (String)
    -   `captcha` (string) the google recaptcha token
-   returns on ok (JSON) 200
    -   `loginKey` (String) the loginKey have to be set as a cookie, it is checked for every request
-   return on missing request elements 400 (Bad Request)
-   return on user exists (mail address is key) 406 (Not acceptable)
-   return on invalid captcha 401 (Unauthorized)

### Get user data

The get currently logged in userdata this endpoint can be used. Just like any other endpoint just pass the loginkey either via the json request body or as cookie the authorize this request. The return contains all user information like the loginKey itself, userid, admin etc.

Detailed request list:

-   Path `/user`
-   HTTP Method `GET`
-   required data: loginKey (cookie or request body)
-   returns (JSON):
    -   `id` (Int)
    -   `username` (String)
    -   `firstName` (String)
    -   `lastName` (String)
    -   `mail` (String)
    -   `active` (boolean) (indicates wether the account has been deactivated, see [Deactivate user](#deactivate-user))
    -   `admin`(most presumably false)

### Login

Endpoint to either request a login mail or to retrieve a cookie with the loginKey provided.
This endpoint is designed for webclients wanting to send the loginkey in the future over a cookie. The cookie will be provided over this endpoint.

Detailed request list:

-   Path `/user/login`
-   HTTP Method `POST`
-   optional data (JSON body): `loginKey` if already recieved
-   returns (not key provided):
    -   200 (OK)
    -   An EMail will be sent to the user (if he is registered)
-   returns (loginKey provided):
    -   a cookie

### Edit User settings

<small>Not going to be implemented in first version</small>

### Deactivate user

<small>Not going to be implemented in first version</small>

## Poll-Endpoints

### Retrieve polls

To retrieve all polls the user has access to or to retrieve data from a specific poll data either no additional data is required or the poll id needs to be send.
When passing no additional data, only essential information is passed, like the name, admin information, number of participants, last updated and the description.
When retrieving a single poll by passing the poll ID all information about the poll is retrieved. The information passed, additional to "basic" information (sent when passing no data), are the available options to select from, all Votes the User made and all Votes by the other users. [Information about voting](#vote-endpoints)

Detailed request list:

-   Path `/poll`
-   HTTP Method `GET`
-   Retrieve basic information
    -   required JSON fields: none (besides `loginKey` when not sent as cookie)
    -   returns 401 (Unauthorized) if loginKey is invalid
    -   returns (JSON)
        -   `polls`: List of Poll overviews
            -   `pollID` (String) unique id
            -   `name` (string)
            -   `admin` the poll creator
                -   `firstName` (String)
                -   `lastName` (String)
                -   `username` (String)
                -   `id` (number)
            -   `description` (String)
            -   `userCount` (Int) number users voted on this poll
            -   `lastUpdated` (DateTime (specifics not defined yet))
            -   `type` (0: String, 1: Date, 2: DateTime)
-   Retrieve detailed information
    -   required JSON fields:
        -   `pollID` the poll id (may be in query string)
    -   returns 401 (Unauthorized) if loginKey is invalid
    -   returns 400 (Bad Request) if poll was not found in users accessible poll list
    -   returns (JSON)
        -   `pollID` (String) unique id
        -   `name`(string)
        -   `admin` the poll creator
            -   `firstName` (String)
            -   `lastName` (String)
            -   `username` (String)
        -   `description` (String)
        -   `maxPerUserVoteCount` (Non decimal number) - the number of options each user choose simultaneously (-1 is infinity)
        -   `userCount` (Int) number users voted on this poll
        -   `lastUpdated` (DateTime (specifics not defined yet))
        -   `created` (DateTime (specifics not defined yet))
        -   `type` (0: String, 1: Date, 2: DateTime)
        -   `options`: List of options
            -   `optionID` (Int)
            -   `value` (String) when type is String
            -   `dateStart`(Date) when type is Date
            -   `dateEnd`(Date | null) when type is Date
            -   `dateTimeStart` (DateTime) when type is Datetime
            -   `dateTimeEnd` (DateTime) when type is Datetime
        -   List of votes
            -   `optionID` (Int) the id of the option from the selectables from the poll
            -   `votedFor` (boolean) wether or not the user agrees or disagrees
            -   `userID`
        -   List of participating users
            -   `firstName`(String)
            -   `lastName` (String)
            -   `username` (String)
            -   `id` (String)
            -   `admin`(boolean) if user is any kind of admin

### Create a Poll

To create any kind of Poll basic settings are needed like the name of the poll and the type of the poll (String, Date, DateTime) and the number of vote each user can choose simultaneously. As admin the current user (identified via [loginKey](#login-method)) is used, which is the only one to edit this poll.

Detailed request list:

-   Path `/poll`
-   HTTP Method `POST`
-   required JSON fields:
    -   `name` (String)
    -   `maxPerUserVoteCount` (Non decimal number) - the number of options each user choose simultaneously (-1 is infinity)
    -   `description` (String)
    -   `type` (0: String, 1: Date, 2: DateTime)
    -   `options` Array of following type (must correlate to set value above):
        -   in case of type String
            -   `value`(String)
        -   in case of type Date
            -   `dateStart` (Date)
            -   `dateEnd` (Date) value can be null or not set
        -   in case of DateTime
            -   `dateTimeStart` (DateTime)
            -   `dateTimeEnd`(DateTime) value can be null or not set
-   returns (JSON)
    -   `pollID` the polls uuid
-   return 400 if parameters are missing
-   returns 409 if parameters are wrong type
-   returns 413 if the user has "owns"/maintains/created too many polls at the same time

### Edit a poll

To edit a poll, being using an invite link, edit description, name, options or to remove and edit user votes you have to be the polls admin (except using the invite link). In the request you have to specify what you want to change, you can either change on thing at a time or summarize all changes in one request.

Detailed request list:

-   Path `/poll`
-   HTTP Method `PUT`
-   require JSON field:
    -   `inviteLink` (String) when you are trying to join a poll to vote, this parameter must be set to the pollID the user wants to join
    -   `leave`(boolean) the inverse to the former (`inviteLink`), if the user wants to leave the poll
    -   `pollID` (String) the poll you want to change (the user must be the admin of that poll) (must always be set, except for an invite)
    -   `name` (string) the poll name, if you want to change that
    -   `description`(string) the polls description, if you want to change that
    -   `maxPerUserVoteCount` (Non decimal number) - the number of options each user choose simultaneously (-1 is infinity)
    -   `userRemove` (array of userID's) the users ids you want to remove from the poll
    -   `votes` (array of following), if you change any vote
        -   `userID` (string) the user the vote is from
        -   `optionID` (number) the option you want to change
        -   `votedFor` (boolean) the state you want that vote to change to
    -   `options` (array of following), if you want to add or remove an option (xor: only one of the two can be inside one of the array elements, otherwise only the deletion will be acknowledged)
        -   `optionID` the optionID (if removing an option)
        -   and the new value (see the needed parameters from the options array at [Creating a poll](#create-a-poll))
    -   `delete` (boolean) this is to delete the poll irreversibly (!!!!!!!!!!!)
-   returns (HTTP Codes)
    -   `200` Changes accepted
    -   `400` Poll not found
    -   `401` User not admin of poll, cannot commit changes

## Vote Endpoints

### Vote or replace previous one

To vote on a poll you need the `pollID`, the selected option and wether or not it is selected or not. The user creating is vote is identified with the [loginKey](#login-method). When the user already voted for that poll and the option was already voted for once, the vote gets replaced as long as the maximum number of votes for that poll is not reached.

Detailed request list:

-   path `/vote`
-   HTTP Method `POST`
-   required JSON fields:
    -   `pollID` (String) the poll this vote is directed to
    -   `optionID` (Int) the id of the option from the selectables from the poll
    -   `votedFor` (boolean) wether or not the user agrees or disagrees
    -   `userID` (string) if an (poll)admin wants to alter a vote the modified user must be passed, if the user is not an (poll)admin this parameter will be ignored
-   returns (HTTP codes)
    -   `200` Vote was accepted
    -   `406` (Not acceptable) Vote is unacceptable

## Administration Endpoints

All Routes beginning with `/admin` can only be performed as an admin. Either by being promoted to one or by setting the `superAdminMail` to the needed user's mail address. If a non-admin user performs a request to such endpoints the HTTP Code 401 (Unauthorized) will be returned.

### User management

#### Retrieve User List

Retrieving a list of all registered users.

Detailed request list:

-   Path `/admin/users`
-   HTTP Method `GET`
-   required JSON fields: as always, the loginkey in some form
-   returns JSON:
    -   `users`an array of userdata
        -   `id` (number) userid
        -   `username` (string)
        -   `firstName` (string)
        -   `lastName` (string)
        -   `mail` (string)
        -   `active` (boolean)
        -   `admin` (boolean) user is any kind of admin
-   returns HTTP Codes
    -   200 OK
    -   401 (Unauthorized) User is not an admin

### Edit user

Edit any user as an admin. Edit name, mail, promote to admin and delete the user. Deleting a user deletes al votes and polls the user created.

Detailed request list:

-   Path `/admin/users`
-   HTTP Method `PUT`
-   required JSON field (besides an admin loginKey):
    -   `userID`(number) the user to edit
    -   `delete`(boolean) - optional, when the user should be deleted, all other values are ignored
    -   `mail` (string) - optional, only pass value if mail address should be changed
    -   `admin` (boolean) - optional, pass true when user should be promoted, false if demoted (demoting works only if user is superAdmin (configurable in config file))
    -   `firstName` (string) - optional, only pass value if first name should be changed
    -   `lastName` (string) - optional, only pass value if last name should be changed
    -   `username` (string) - optional, only pass value if username should be changed
-   returns
    -   Success:
        -   HTTP Status: 200
    -   or default error codes, see [Error codes](#return-code-overview)

### Poll management

#### Retrieve Poll list

Retrieve a list of all created polls:

Detailed request list:

-   Path `/admin/polls`
-   HTTP Method `GET`
-   required JSON fields: as always, the loginKey
-   returns JSON:
-   `polls`:
    -   See [Retrieve Poll overview](#retrieve-polls)
