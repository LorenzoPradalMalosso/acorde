FROM node:24-alpine AS build
WORKDIR /workspace/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
COPY assets/branding/Acorde_Brand_Kit/tokens/acorde.css /workspace/assets/branding/Acorde_Brand_Kit/tokens/acorde.css
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

FROM nginx:1.27-alpine
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/frontend/dist /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=20s --timeout=4s --retries=5 CMD wget -q --spider http://127.0.0.1/ || exit 1
