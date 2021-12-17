# API - Documentation

## Endpoints

Configured on our server, the API is accessible via the `/api`Endpoint

| Endpoint | HTTP Method | Summary                                                                                             |
| -------- | ----------- | --------------------------------------------------------------------------------------------------- |
| `/user`  | -           | [Link](#user-endpoints)                                                                             |
| `/user`  | POST        | [Details](#create-a-user) - Creating a user with Full Name and a unique, not yet used, mail address |
| `/user`  | PUT         | [Details](#edit-user-settings) - Edit own user                                                      |
| `/poll`  | -           | [Link](#poll-endpoints)                                                                             |
| `/poll`  | POST        | [Details](#create-a-poll) - Creating a new Poll                                                     |
| `/poll`  | PUT         | [Details](#edit-a-poll) - Editing an existing Poll                                                  |

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
