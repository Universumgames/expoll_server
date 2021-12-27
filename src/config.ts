import nodeconfig from "config"

export const config = {
    mailServer: nodeconfig.get<string>("mailServer"),
    mailPort: nodeconfig.get<number>("mailPort"),
    mailSecure: nodeconfig.get<boolean>("mailSecure"),
    mailUser: nodeconfig.get<string>("mailUser"),
    mailPassword: nodeconfig.get<string>("mailPassword"),
    serverPort: nodeconfig.get<number>("serverPort"),
    frontEndPort: nodeconfig.get<number>("frontEndPort"),
    superAdminMail: nodeconfig.get<string>("superAdminMail")
}

console.log("Running server with config as following:\n", config)
