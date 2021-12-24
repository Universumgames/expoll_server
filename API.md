# API - Documentation

## Endpoints

Configured on our server, the API is accessible via the `/api` Endpoint

| Endpoint      | HTTP Method | Summary                                                                               |
| :------------ | :---------: | ------------------------------------------------------------------------------------- |
| `/user`       |      -      | [Link](#user-endpoints)                                                               |
| `/user`       |    POST     | [Details](#create-a-user) - Creating a user                                           |
| `/user`       |     GET     | [Details](#get-user-data) - Get user data                                             |
| `/user`       |     PUT     | [Details](#edit-user-settings) - Edit own user                                        |
| `/user/login` |    POST     | [Details]() - Login via loginKey or request login mail                                |
| `user`        |   DELETE    | Deactivate a useraccount - serves no purpose so far                                   |
| `/poll`       |      -      | [Link](#poll-endpoints)                                                               |
| `/poll`       |     GET     | [Details](#retrieve-polls) - Get all/specific polls the user created or has access to |
| `/poll`       |    POST     | [Details](#create-a-poll) - Creating a new Poll                                       |
| `/poll`       |     PUT     | [Details](#edit-a-poll) - Editing an existing Poll                                    |
| `/vote`       |      -      | [Link](#vote-endpoints)                                                               |
| `/vote`       |    POST     | [Details](#vote-or-replace-previous-one) - Vote on a poll                             |
| `/vote`       |   DELETE    | [Details](#revokedelete-vote) - Revoke vote on poll                                   |

## Return code overview

-   `200` OK
-   `400` (Bad request) parameters missing/invalid
-   `401` (Unauthorized) LoginKey is invalid
-   `406` (Not acceptable) Vote is not acceptable / user already exists (mail or username)
-   `409` (Conflict) wrong parameter type

## Login Method

Login is handled not with password but with a login key which is received at signup and sent per mail when requested via username or the users mail address.
This loginKey is required to either be send in the request body or as cookie.

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
-   returns on ok (JSON) 200
    -   `loginKey` (String) the loginKey have to be set as a cookie, it is checked for every request
-   return on missing request elements 400 (Bad Request)
-   return on user exists (mail address is key) 406 (Not acceptable)

### Get user data

The get currently logged in userdata this endpoint can be used. Just like any other endpoint just pass the loginkey either via the json request body or as cookie the authorize this request. The return contains all user information like the loginKey itself, userid, admin etc.

Detailed request list:

-   Path `/user`
-   HTTP Method `GET`
-   required data: loginKey (cookie or request body)
-   returns (JSON):
    -   `loginKey` (String)
    -   `id` (Int)
    -   `username` (String)
    -   `firstName` (String)
    -   `lastName` (String)
    -   `mail` (String)
    -   `active` (boolean) (indicates wether the account has been deactivated, see [Deactivate user](#deactivate-user))
    -   `admin`(most presumably false)

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
        -   `polls`: List of Polls
            -   `pollID` (String) unique id
            -   `admin` the poll creator
                -   `firstName` (String)
                -   `lastName` (String)
                -   `username` (String)
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
            -   `admin` the poll creator
                -   `firstName` (String)
                -   `lastName` (String)
                -   `username` (String)
            -   `description` (String)
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

### Create a Poll

To create any kind of Poll basic settings are needed like the name of the poll and the type of the poll (String, Date, DateTime) and the number of vote each user can choose simultaneously. As admin the current user (identified via [loginKey](#login-method)) is used, which is the only one to edit this poll.

Detailed request list:

-   Path `/poll`
-   HTTP Method `POST`
-   required JSON fields:
    -   `name` (String)
    -   `maxPerUserVoteCount` (Non decimal number) - the number of options each user choose simultaneously (chose a number <= 0 to set to infinity)
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

### Edit a poll

To edit a poll, being using an invite link, edit description, name, options or to remove and edit user votes you have to be the polls admin (except using the invite link). In the request you have to specify what you want to change, you can either change on thing at a time or summarize all changes in one request.

Detailed request list:

-   Path `/poll`
-   HTTP Method `PUT`
-   require JSON field:
    -   `inviteLink` (String) when you are trying to join a poll to vote, this parameter must be set to the pollID the user wants to join
    -   `pollID` (String) the poll you want to change (the user must be the admin of that poll) (must always be set, except for an invite)
    -   `name` (string) the poll name, if you want to change that
    -   `description`(string) the polls description, if you want to change that
    -   `userRemove` (array of userID's) the users ids you want to remove from the poll
    -   `votes` (array of following), if you change any vote
        -   `userID` (string) the user the vote is from
        -   `optionID` (number) the option you want to change
        -   `votedFor` (boolean) the state you want that vote to change to
    -   `options` (array of following), if you want to add or remove an option
        -   `optionID` the optionID (if removing an option)
        -   and the new value (see the needed parameters from the options array at [Creating a poll](#create-a-poll))
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
-   returns (HTTP codes)
    -   `200` Vote was accepted
    -   `406` (Not acceptable) Vote is unacceptable

### Revoke/Delete vote

<small>Not going to be implemented in first version</small>
