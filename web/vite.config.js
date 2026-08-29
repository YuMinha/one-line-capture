import { defineConfig, loadEnv } from 'vite'

export default defineConfig(({ mode }) => {
  // 환경변수는 레포 루트 .env 하나로 관리한다. 백엔드와 프론트가 따로 놀지 않게
  const env = loadEnv(mode, '..', '')

  return {
    envDir: '..',
    server: {
      // 개발 중에도 API를 같은 출처로 부른다. 이러면 CORS 설정이 아예 필요 없고,
      // 배포 때 Caddy가 하는 일(/api → api 컨테이너)과 모양이 같아진다
      proxy: {
        '/api': {
          target: `http://localhost:${env.API_PORT || 8080}`,
          changeOrigin: true,
        },
      },
    },
  }
})
