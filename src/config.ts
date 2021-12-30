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
    maxPollCountPerUser: nodeconfig.get<number>("maxPollCountPerUser")
}

console.log("Running server with config as following:\n", config)
