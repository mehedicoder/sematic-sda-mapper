FROM node:20-alpine

WORKDIR /app

COPY package.json .
RUN npm install

COPY server.js .

EXPOSE 4334 3000 8081

CMD ["npm", "start"]
