# API - Documentation

## Endpoints

Configured on our server, the API is accessible via the `/api`Endpoint

| Endpoint | HTTP Method | Summary                                                                               |
| :------- | :---------: | ------------------------------------------------------------------------------------- |
| `/user`  |      -      | [Link](#user-endpoints)                                                               |
| `/user`  |    POST     | [Details](#create-a-user) - Creating a user                                           |
| `/user`  |     PUT     | [Details](#edit-user-settings) - Edit own user                                        |
| `/poll`  |      -      | [Link](#poll-endpoints)                                                               |
| `/poll`  |     GET     | [Details](#retrieve-polls) - Get all/specific polls the user created or has access to |
| `/poll`  |    POST     | [Details](#create-a-poll) - Creating a new Poll                                       |
| `/poll`  |     PUT     | [Details](#edit-a-poll) - Editing an existing Poll                                    |
| `/vote`  |      -      | [Link](#vote-endpoints)                                                               |
| `/vote`  |    POST     | [Details](#vote-or-replace-previous-one) - Vote on a poll                             |

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
-   returns (JSON)
    -   `loginKey` (String) the loginKey have to be set as a cookie, it is checked for every request

### Edit User settings

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
    -   required JSON fields: none
    -   returns (JSON)
        -   List of Polls
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
        -   `pollID` the poll id
        -   returns (JSON)
            -   `pollID` (String) unique id
            -   `admin` the poll creator
                -   `firstName` (String)
                -   `lastName` (String)
                -   `username` (String)
            -   `description` (String)
            -   `userCount` (Int) number users voted on this poll
            -   `lastUpdated` (DateTime (specifics not defined yet))
            -   `type` (0: String, 1: Date, 2: DateTime)
            -   List of options
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
    -   `pollKey` the polls uuid

### Edit a poll

<small>Not going to be implemented in first version</small>

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
