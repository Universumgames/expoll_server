from node
COPY ../lib /expoll/lib
WORKDIR /expoll/lib
RUN npm install
RUN npm build

WORKDIR /expoll/api_server
COPY . .


RUN npm install
RUN npm run build

EXPOSE 6060
CMD [ "node", "./compiled/index.js" ]
