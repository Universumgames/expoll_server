# Administration Endpoints

All Routes beginning with `/admin` can only be performed as an admin. Either by being promoted to one or by setting the `superAdminMail` to the needed user's mail address. If a non-admin user performs a request to such endpoints the HTTP Code 401 (Unauthorized) will be returned.

| Method         | Path | Summary                                             |
| -------------- | ---- | --------------------------------------------------- |
| `/admin/users` | GET  | [Request info](#retrieve-user-list) - Get all users |
| `/admin/users` | POST | [Request info](#edit-user) - Edit and delete users  |
| `/admin/polls` | GET  | [Request info](#retrieve-poll-list) - Get all polls |

TODO add user deletion
TODO add mail regex settings

## Retrieve User List

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

## Edit user

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

## Retrieve Poll list

Retrieve a list of all created polls:

Detailed request list:

-   Path `/admin/polls`
-   HTTP Method `GET`
-   required JSON fields: as always, the loginKey
-   returns JSON:
-   `polls`:
    -   See [Retrieve Poll overview](#retrieve-polls)
