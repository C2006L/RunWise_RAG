FROM node:22-alpine AS build
WORKDIR /build
COPY shared ./shared
COPY runwise-web/package*.json ./runwise-web/
COPY runwise-admin/package*.json ./runwise-admin/
RUN cd runwise-web && npm ci && cd ../runwise-admin && npm ci
COPY runwise-web ./runwise-web
COPY runwise-admin ./runwise-admin
RUN cd runwise-web && VITE_USE_MOCK=false VITE_API_BASE=/api npm run build \
    && cd ../runwise-admin && VITE_API_BASE_URL=/api/admin npm run build -- --base=/admin/

FROM nginx:1.28-alpine
COPY deploy/nginx.conf /etc/nginx/nginx.conf
COPY deploy/proxy.conf /etc/nginx/proxy.conf
COPY --from=build /build/runwise-web/dist /usr/share/nginx/html
COPY --from=build /build/runwise-admin/dist /usr/share/nginx/html/admin
USER nginx
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
