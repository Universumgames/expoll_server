# Poll-Endpoints

| Endpoint | HTTP Method | Summary                                                                                                                             |
| -------- | ----------- | ----------------------------------------------------------------------------------------------------------------------------------- |
| `/poll`  | GET         | [Request info](#retrieve-poll-list) - Get summary of polls the user was invited to or get detailed information on one specific poll |
| `/poll`  | POST        | [Request info](#create-a-poll) - Create a new poll                                                                                  |
| `/poll`  | PUT         | [Request info](#edit-a-poll) - Edit a poll                                                                                          |

## Retrieve polls

To retrieve all polls the user has access to or to retrieve data from a specific poll data either no additional data is required or the poll id needs to be send.
When passing no additional data, only essential information is passed, like the name, admin information, number of participants, last updated and the description.
When retrieving a single poll by passing the poll ID all information about the poll is retrieved. The information passed, additional to "basic" information (sent when passing no data), are the available options to select from, all Votes the User made and all Votes by the other users. [Information about voting](#vote-endpoints)

TODO update return fields

Detailed request list:

-   Path `/poll`
-   HTTP Method `GET`
-   Retrieve basic information
    -   required JSON fields: none (besides `loginKey` when not sent as cookie)
    -   returns 401 (Unauthorized) if loginKey is invalid
    -   returns (JSON)
        -   `polls`: List of Poll overviews (SimplePoll[])
            -   `pollID` (String) unique id
            -   `name` (string)
            -   `admin` the poll creator (SimpleUser)
                -   `firstName` (String)
                -   `lastName` (String)
                -   `username` (String)
                -   `id` (string)
            -   `description` (String)
            -   `userCount` (Int) number users voted on this poll
            -   `lastUpdated` (DateTime (specifics not defined yet))
            -   `type` (0: String, 1: Date, 2: DateTime)
            -   `editable` (boolean) indicates wether the poll can be edited or is archived by the admin
-   Retrieve detailed information
    -   required JSON fields:
        -   `pollID` the poll id (may be in query string)
    -   returns 401 (Unauthorized) if loginKey is invalid
    -   returns 400 (Bad Request) if poll was not found in users accessible poll list
    -   returns (JSON)
        -   `pollID` (String) unique id
        -   `name`(string)
        -   `admin` the poll creator (SimpleUser)
            -   `firstName` (String)
            -   `lastName` (String)
            -   `username` (String)
            -   `id` (string)
        -   `description` (String)
        -   `maxPerUserVoteCount` (Non decimal number) - the number of options each user choose simultaneously (-1 is infinity)
        -   `userCount` (Int) number users voted on this poll
        -   `lastUpdated` (DateTime (specifics not defined yet))
        -   `created` (DateTime (specifics not defined yet))
        -   `type` (0: String, 1: Date, 2: DateTime)
        -   `options`: List of options
            -   `optionID` (Int)
            -   `value` (String) when type is String
            -   `dateStart`(Date | null) when type is Date
            -   `dateEnd`(Date | null) when type is Date
            -   `dateTimeStart` (DateTime | null) when type is Datetime
            -   `dateTimeEnd` (DateTime | null) when type is Datetime
        -   `userVotes` List of votes by participating users (SimpleUserVotes[])
            -   `user` (SimpleUser)
                -   `firstName` (String)
                -   `lastName` (String)
                -   `username` (String)
                -   `id` (string)
            -   `votes` The votes from the user (SimpleVote[])
                -   `optionID` (int) the option the user has a vote for
                -   `votedFor` (no: 0, yes: 1, maybe: 2) the vote, if maybe is not allowed, the server automatically converts old values of "maybe" (2) into yes' (1)
        -   `userNotes`List of notes for the users by poll admin (SimpleUserNote[])
            -   `user` (SimpleUser)
                -   `firstName` (String)
                -   `lastName` (String)
                -   `username` (String)
                -   `id` (string)
            -   `note` (String) the note or synonym by the admin for the specified user in that poll
        -   `allowsMaybe` (boolean) wether "maybe" is allowed as voting option
        -   `allowsEditing` (boolean) wether the poll can be edited or is archived by the admin

## Create a Poll

To create any kind of Poll basic settings are needed like the name of the poll and the type of the poll (String, Date, DateTime) and the number of vote each user can choose simultaneously. As admin the current user (identified via [loginKey](#login-method)) is used, which is the only one to edit this poll.

Detailed request list:

-   Path `/poll`
-   HTTP Method `POST`
-   required JSON fields:
    -   `name` (String)
    -   `maxPerUserVoteCount` (Non decimal number) - the number of options each user choose simultaneously (-1 is infinity)
    -   `description` (String)
    -   `type` (0: String, 1: Date, 2: DateTime) (this field cannot be changed later)
    -   `options` Array of following type (must correlate to set value above):
        -   in case of type String
            -   `value`(String)
        -   in case of type Date
            -   `dateStart` (Date)
            -   `dateEnd` (Date) value can be null or not set
        -   in case of DateTime
            -   `dateTimeStart` (DateTime)
            -   `dateTimeEnd`(DateTime) value can be null or not set
    -   `allowsMaybe` (boolean) wether or not "maybe" is allowed as a voting option
    -   `allowsEditing` (boolean) wether or not the poll should be archived immediately after creation (default true)
-   returns (JSON)
    -   `pollID` the polls uuid
-   return 400 if parameters are missing
-   returns 409 if parameters are wrong type
-   returns 413 if the user has "owns"/maintains/created too many polls at the same time

## Edit a poll

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
    -   TODO update missing fields (allowsEditing, allowsMaybe, notes)
-   returns (HTTP Codes)
    -   `200` Changes accepted
    -   `400` Poll not found
    -   `401` User not admin of poll, cannot commit changes
