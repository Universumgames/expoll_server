from node

WORKDIR /expoll/api_server
COPY ./server/ .
RUN npm install --legacy-peer-deps
RUN npm run build

EXPOSE 6060
ARG NODE_ENV=production
CMD [ "node", "./compiled/index.js" ]
