import nodeconfig from "config"

export const config = {
    mailServer: nodeconfig.get<string>("mailServer"),
    mailPort: nodeconfig.get<number>("mailPort"),
    mailSecure: nodeconfig.get<boolean>("mailSecure"),
    mailUser: nodeconfig.get<string>("mailUser"),
    mailPassword: nodeconfig.get<string>("mailPassword"),
    serverPort: nodeconfig.get<number>("serverPort"),
    frontEndPort: nodeconfig.get<number>("frontEndPort"),
    loginLinkURL: nodeconfig.get<string>("loginLinkURL"),
    superAdminMail: nodeconfig.get<string>("superAdminMail"),
    database: {
        type: nodeconfig.get<string>("database.type"),
        host: nodeconfig.get<string>("database.host"),
        port: nodeconfig.get<number>("database.port"),
        rootPW: nodeconfig.get<string>("database.rootPW")
    },
    maxPollCountPerUser: nodeconfig.get<number>("maxPollCountPerUser"),
    recaptchaAPIKey: nodeconfig.get<string>("recaptchaAPIKey"),
    serverVersion: nodeconfig.get<string>("serverVersion"),
    webauthn: {
        rpName: nodeconfig.get<string>("webauthn.rpName"), // Human-readable title for your website
        rpID: nodeconfig.get<string>("webauthn.rpID"), // A unique identifier for your website
        origin: nodeconfig.get<string>("webauthn.origin")
        // The URL at which registrations and authentications should occur
    },
    shareURLPrefix: nodeconfig.get<string>("shareURLPrefix"),
    notifications: {
        bundleID: nodeconfig.get<string>("notifications.bundleID"),
        teamID: nodeconfig.get<string>("notifications.teamID"),
        apnsKeyID: nodeconfig.get<string>("notifications.apnsKeyID"),
        apnsKeyPath: nodeconfig.get<string>("notifications.apnsKeyPath"),
        apnsURL: nodeconfig.get<string>("notifications.apnsURL")
    },
    testUser: {
        firstName: nodeconfig.get<string>("testUser.firstName"),
        lastName: nodeconfig.get<string>("testUser.lastName"),
        email: nodeconfig.get<string>("testUser.email"),
        username: nodeconfig.get<string>("testUser.username"),
        loginKey: nodeconfig.get<string>("testUser.loginKey")
    },
    minimumRequiredClientVersion: nodeconfig.get<string>("minimumRequiredClientVersion")
}

console.log("Running server with config as following:\n", config)
