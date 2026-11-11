# Image de production du frontend : build Vite puis nginx (fichiers statiques + proxy /api vers le backend).
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# Adresse publique du fournisseur OIDC ; l'API est servie sur le même domaine (/api), d'où VITE_API_URL vide.
ARG VITE_OIDC_AUTHORITY
ARG VITE_OIDC_CLIENT_ID=faro-frontend
ARG VITE_API_URL=
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
