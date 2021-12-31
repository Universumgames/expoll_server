/* eslint-disable require-jsdoc */
/* eslint-disable no-unused-vars */
import assert from "assert"
import axios, { AxiosRequestConfig } from "axios"
import { IUser, PollType, tPollID } from "expoll-lib/interfaces"
import { CreatePollRequest, DetailedPollResponse, EditPollRequest, PollOverview } from "expoll-lib/requestInterfaces"
import { describe, it } from "mocha"
import { isRegularExpressionLiteral } from "typescript"
import { User } from "../entities/entities"
import { key } from "./test-config"

const baseURL = "http://localhost:6060"

const loginUserMail = "programming@universegame.de"
const predefinedKey = key

// eslint-disable-next-line no-undef
const prompt = require("prompt-sync")({ sigint: true })

describe("API test", function () {
    // eslint-disable-next-line no-invalid-this
    this.timeout(0)

    it("Test meta endpoint to check if server is running properly", async () => {
        try {
            await axios.get(baseURL + "/metaInfo")
            assert.ok(true)
        } catch (e) {
            assert.fail("Connection not successful " + e)
        }
    })

    let key = predefinedKey
    let user: IUser

    describe("Test Login procedure", () => {
        if (key == "") {
            const requestKey = prompt("Test loginKey request? ") as string

            if (!["n", "no", "f", "false", "0"].includes(requestKey))
                it("Request loginmail for user", async () => {
                    try {
                        await axios.post(baseURL + "/user/login", { mail: loginUserMail })
                        assert.ok(true)
                    } catch (e) {
                        assert.fail("Loginmal could not be requested " + e)
                    }
                })

            key = prompt("Enter login key: ")
        }

        it("Login with key", async () => {
            try {
                user = (await axios.post(baseURL + "/user/login", { loginKey: key })).data
                // console.log("Login userData: ", user)
                assert.ok(true)
            } catch (e) {
                assert.fail("Login failed " + e)
            }
        })

        it("Get userData", async () => {
            try {
                const u2: IUser = (await axios.get(baseURL + "/user", { params: { loginKey: key } })).data
                // console.log(u2)
                assert.ok(user != undefined || user == u2, "UserData does not match original value")
                if (user == undefined) user = u2
            } catch (e) {
                assert.fail("Retrieval failed: " + e)
            }
        })
    })

    describe("Test poll endpoints", () => {
        it("Get polls unauthorized", async () => {
            try {
                const data = await axios.get("/poll")
                assert.fail("Retrieving polls unauthorized succeeded" + data)
            } catch (e) {
                assert.ok(true)
            }
        })

        let polls = []

        it("Get polls", async () => {
            try {
                const data = (await axios.get(baseURL + "/poll", { params: { loginKey: key } })).data as PollOverview
                polls = data.polls
                // console.log("Starting polls: " + (polls == [] ? "[]" : polls))
                assert.ok(data.polls != undefined)
            } catch (e) {
                assert.fail("Retrieving polls failed " + e)
            }
        })

        const originPoll: CreatePollRequest = {
            name: "testpoll",
            maxPerUserVoteCount: -1,
            description: "testdescription",
            type: PollType.String,
            options: []
        }
        let originPollID: tPollID = ""

        it("Create test poll", async () => {
            try {
                const data = (await axios.post(baseURL + "/poll", originPoll, { params: { loginKey: key } })).data
                originPollID = data.pollID
                if (data.pollID == undefined || data.pollID == "")
                    throw new Error("Response not as expected: PollID non existant or empty")
            } catch (e) {
                assert.fail("Creating poll failed: " + e)
            }
        })

        it("Validate created poll", async () => {
            try {
                const data = (await axios.get(baseURL + "/poll", { params: { loginKey: key, pollID: originPollID } }))
                    .data as DetailedPollResponse
                assert.ok(data.pollID == originPollID, "PollID not identical")
                assert.ok(data.admin.id == user.id, "Admin id not identical")
                assert.ok(data.type == originPoll.type, "Poll type not identical")
                assert.ok(data.description == originPoll.description, "Description not identical")
            } catch (e) {
                assert.fail("Validating created poll failed: " + e)
            }
        })

        it("Delete created poll", async () => {
            try {
                const reqData: EditPollRequest = { pollID: originPollID, delete: true }
                const data = (await axios.put(baseURL + "/poll", reqData, { params: { loginKey: key } })).data
                assert.ok(true)
            } catch (e) {
                assert.fail("Deleting failed: " + e)
            }
        })
    })

    describe("Test Vote Endpoint", () => {})
})
