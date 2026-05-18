FROM nginx:1.24-alpine

LABEL maintainer="yuemo"

RUN rm -rf /usr/share/nginx/html/*

COPY dist/ /usr/share/nginx/html/

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
