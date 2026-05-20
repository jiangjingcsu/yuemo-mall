FROM node:20-alpine AS builder

WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm ci --prefer-offline --no-audit --progress=false || npm install --prefer-offline --no-audit --progress=false

COPY . .
RUN npm run build

FROM nginx:1.29.8-alpine

LABEL maintainer="yuemo"
LABEL description="御魔商城 Frontend — Nginx SPA static server"

COPY --from=builder /app/dist /usr/share/nginx/html

COPY nginx.conf /etc/nginx/conf.d/default.conf:ro

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD wget -q --spider http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
