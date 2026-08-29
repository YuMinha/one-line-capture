// 앱 셸만 캐시한다. 오프라인 쓰기 큐는 v1에서 다루지 않는다 (spec.md §6)
const CACHE = 'capture-shell-v1'
const SHELL = ['/', '/index.html', '/manifest.webmanifest', '/icon-192.png', '/icon-512.png']

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(CACHE).then((cache) => cache.addAll(SHELL)))
  self.skipWaiting()
})

self.addEventListener('activate', (event) => {
  // 배포할 때마다 CACHE 이름을 올리면 옛 셸이 남아 새 화면이 안 보이는 사고를 막는다
  event.waitUntil(
    caches.keys()
        .then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
        .then(() => self.clients.claim()),
  )
})

self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url)

  // API는 절대 캐시하지 않는다. 캐시된 목록을 보여주면 방금 저장한 게 사라진 것처럼 보인다
  if (event.request.method !== 'GET' || url.pathname.startsWith('/api/')) {
    return
  }

  // 셸은 네트워크 우선, 실패하면 캐시. 새로고침하면 항상 최신을 받는다
  event.respondWith(
    fetch(event.request)
        .then((response) => {
          const copy = response.clone()
          caches.open(CACHE).then((cache) => cache.put(event.request, copy))
          return response
        })
        .catch(() => caches.match(event.request).then((hit) => hit ?? caches.match('/index.html'))),
  )
})
