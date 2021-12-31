# Expoll - Backendserver

Expoll is a simple, easy to use polling service. Combined with the frontend written with Vuejs and typescript you can easily setup your own server.

## Create docker container

Because I am using docker myself to allow easy upgrade installation on my server, there are plenty of of files regarding this matter. To, for example deploy the full system in docker you first need all three repositories (frontend, lib and this one) in directories called 'client', 'lib' and 'server'. All three directories have to bee in the same root directory to easily run any shell script. When all that is sorted out, you can just run the `run-full-server.sh` file in this repo and custom docker images will be built with the right binding to configuration directories and port management. This script deploy a database, the backend server and the frontend server managed via nginx (reverse proxy for api) and will deploy the website via the port 80 (designed to work behind another nginx server that'll manage all websites on one's server). If you want to enable https you have to go into the nginx configuration by yourself at `./nginx/default.conf` and edit the exposed ports at `./compose-full-server.yml`

## Using ansible

in the near future this repository will supportsetup via [ansible](https://ansible.com)
