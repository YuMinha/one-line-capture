# 빌드 시점에 VITE_ 변수가 코드에 박힌다. 런타임 환경변수로는 못 바꾼다
FROM node:24-alpine AS build
WORKDIR /app
# 의존성 설치를 소스 복사보다 위에 둔다. 소스만 고치면 이 레이어가 재사용된다
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
