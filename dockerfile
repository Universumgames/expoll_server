from node
WORKDIR /expoll/api_server
COPY . .

RUN npm install
RUN npm run build

EXPOSE 6060
CMD [ "node", "./compiled/index.js" ]
