from node

WORKDIR /expoll/api_server
COPY ./server/ .
RUN npm install --legacy-peer-deps
RUN npm run build

EXPOSE 6060
CMD [ "node", "./compiled/index.js" ]
