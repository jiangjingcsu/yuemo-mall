FROM node:20-alpine

LABEL maintainer="yuemo"
LABEL description="月魔商城 Frontend — Vite SPA static server"

RUN npm install -g serve@14

WORKDIR /app

COPY dist/ /app/

USER node

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD node -e "require('http').get('http://localhost:80/', (r) => {process.exit(r.statusCode === 200 ? 0 : 1)})"

CMD ["serve", "-s", "/app", "-l", "80", "--no-clipboard"]
