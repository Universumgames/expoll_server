# Expoll - Backendserver

Expoll is a simple, easy to use polling service. Combined with the frontend written with Vuejs and typescript you can easily setup your own server.

## Features

-   simple passwordless login via username and EMail verification
-   simple login via webauthn (WebAuthn is a standard for biometrical authentication)
-   simple, easy to use, the API Documentation is either as [YAML File](src/main/resources/openapi/documentation.yaml) or on our website via [Swagger]()
-   user management

## installation

### Easy way Using ansible

This project supports the installation of the backend and frontend via [ansible](https://ansible.com) and docker. To install this on your own server you have to edit the `ansible/inventory` file and add your own host to the `[servers]` section. Then you can run the following command:

```sh
ansible-playbook -i ansible/inventory ansible/main.yml
```

or, when you are already in the ansible directory:

```sh
ansible-playbook main.yml
```

The script requires a certain directory structure to be present. The following directories are required:

-   [`<dirX>/client/`](https://git.mt32.net/universum/expoll_client) - containing the frontend vue project
-   [`<dirX>/server/`](https://git.mt32.net/universum/expoll_server) - containing the backend server

The script will automatically upload both directories to the server, with your custom configuration files, and builds the required docker containers and starts the servers.

### Create docker container

Because I am using docker myself to allow easy upgrade installation on my server, there are plenty of of files regarding this matter. To, for example deploy the full system in docker you first need all three repositories (frontend, lib and this one) in directories called 'client', 'lib' and 'server'. All three directories have to bee in the same root directory to easily run any shell script. When all that is sorted out, you can just run the `run-full-server.sh` file in this repo and custom docker images will be built with the right binding to configuration directories and port management. This script deploy a database, the backend server and the frontend server managed via nginx (reverse proxy for api) and will deploy the website via the port 80 (designed to work behind another nginx server that'll manage all websites on one's server). If you want to enable https you have to go into the nginx configuration by yourself at `./nginx/default.conf` and edit the exposed ports at `./compose-full-server.yml`

[Project Generation url](https://start.ktor.io/#/plugins?name=expoll_server&website=expoll.mt32.net&artifact=net.mt32.expoll.expoll_server&kotlinVersion=1.8.10&ktorVersion=2.2.4&buildSystem=GRADLE_KTS&engine=NETTY&configurationIn=CODE&addSampleCode=true&plugins=routing%2Cauth%2Ccontent-negotiation%2Ckotlinx-serialization%2Cauth-jwt%2Cdefault-headers%2Cktor-sessions%2Cstatus-pages%2Cexposed%2Cktor-jackson%2Copenapi%2Ccompression)