FROM 192.168.1.53/base-images/node:20-alpine AS builder

WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm ci --prefer-offline --no-audit --progress=false || npm install --prefer-offline --no-audit --progress=false

COPY . .
RUN npm run build

FROM 192.168.1.53/base-images/nginx:stable-alpine

LABEL maintainer="yuemo"
LABEL description="月魔商城 Frontend — Nginx SPA static server"

ARG API_PROXY_TARGET=http://192.168.1.56
ENV API_PROXY_TARGET=${API_PROXY_TARGET}

COPY --from=builder /app/dist /usr/share/nginx/html

COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
RUN sed -i "s|http://192.168.1.56|${API_PROXY_TARGET}|g" /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD wget -q --spider http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
