# Vote Endpoints

| Method  | Path | Summary        |
| ------- | ---- | -------------- |
| `/vote` | POST | Vote on a poll |

## Vote or replace previous one

To vote on a poll you need the `pollID`, the selected option and wether or not it is selected or not. The user creating is vote is identified with the [loginKey](#login-method). When the user already voted for that poll and the option was already voted for once, the vote gets replaced as long as the maximum number of votes for that poll is not reached.

Detailed request list:

-   path `/vote`
-   HTTP Method `POST`
-   required JSON fields:
    -   `pollID` (String) the poll this vote is directed to
    -   `optionID` (Int) the id of the option from the selectables from the poll
    -   `votedFor` (no: 0, yes: 1, maybe: 2) wether or not the user agrees or disagrees
    -   `userID` (string) if an (poll)admin wants to alter a vote the modified user must be passed, if the user is not an (poll)admin this parameter will be ignored
-   returns (HTTP codes)
    -   `200` Vote was accepted
    -   `406` (Not acceptable) Vote is unacceptable
