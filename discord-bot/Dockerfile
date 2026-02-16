FROM node:20-alpine

WORKDIR /app

COPY discord-bot/package*.json ./
RUN npm install --production

COPY discord-bot/ .

EXPOSE 3000

CMD ["node", "index.js"]
