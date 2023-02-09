# User-Endpoints

| Endpoint                 | HTTP Method | Summary                                                               |
| ------------------------ | ----------- | --------------------------------------------------------------------- |
| `/user`                  | POST        | [Request info](#create-a-user) - Create a new user                    |
| `/user`                  | GET         | [Request info](#get-user-data) - Get user info                        |
| `/user`                  | DELETE      | [Request info](#trigger-deletion) - Trigger the user deletion process |
| `/user/delete/:id`       | GET         | [Request info](#deletion-confirmation) - Deletion confirmation        |
| `/user/personalizeddata` | GET         | [Request info](#personalized-data) - Get personalized data            |

## Create a user

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
    -   `appAttest` (string) the app attest from an ios app
-   returns on ok (JSON) 200
    -   `loginKey` (String) the loginKey have to be set as a cookie, it is checked for every request
-   return on missing request elements 400 (Bad Request)
-   return on user exists (mail address is key) 406 (Not acceptable)
-   return on invalid captcha 401 (Unauthorized)

## Get user data

The get currently logged in userdata this endpoint can be used. Just like any other endpoint just pass the loginkey either via the json request body or as cookie the authorize this request. The return contains all user information like the loginKey itself, userid, admin etc.

Detailed request list:

-   Path `/user`
-   HTTP Method `GET`
-   required data: loginKey (cookie or request body)
-   returns (JSON):
    -   `id` (string)
    -   `username` (String)
    -   `firstName` (String)
    -   `lastName` (String)
    -   `mail` (String)
    -   `active` (boolean) (indicates wether the account has been deactivated, see [Deactivate user](#deactivate-user))
    -   `admin`(most presumably false)

## Trigger user deletion

Wanting to delete your user account, you first need this HTTP Endpoint, an confirmation E-Mail is sent and with a php script in the frontend repository, a request is sent to the [confirmation](#deletion-confirmation)

-   Path `/user`
-   HTTP Method `DELETE`
-   required data: loginKey (cookie or request body)
-   returns (JSON):
    -   message: "Confirmation email sent"
-   returns via mail verification: confirm link

## Deletion confirmation

Endpoint to confirm the deletion of the current user account

-   Path `/user/delete/:id`
-   HTTP Method `DELETE`
-   required data: id (confirmation id from mail)
-   returns (Text):
    -   User deleted

## Personalized data

Get all data stored on current user (to comply with certain laws in the EU)

-   Path `/user/personalizeddata`
-   HTTP Method `GET`
-   required data: loginKey (cookie or request body)
-   returns (JSON)
    -   `id` (string) user id
    -   `username` (string) username
    -   `firstName` (string) the first name provided at registration
    -   `lastName` (string) last name
    -   `mail` (string) mail address used for login mails and deletion confirmation
    -   `admin` (boolean) user is admin or system (super) admin
    -   `superAdmin` (boolean) user is system (super) admin
    -   `authenticators` (Authenticator[]) list of webauthn authenticators
        -   TODO missing authenticator fields
    -   `polls` (SimplePoll[])
    -   `sessions` (Session[]) list of all active sessions
        -   `expiration` (DateTime) time the login kez expires
        -   `userAgent` (String) the first user agent the login key was used with
        -   `shortKey` (String) the first 4 characters of the session loginkey (used for [deleting sessions](#logout-session))
    -   `votes` (Vote[]) list of all votes
        -   `id` (int) poll id for the vote
        -   `optionID` (int) the option the vote is for
        -   `votedFor` (int) the vote choice

## Edit User settings

<small>Not going to be implemented in first versions</small>

## Deactivate user

<small>Not going to be implemented in first versions</small>
