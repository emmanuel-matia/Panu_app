// PANU Service Worker (sw.js) — Mode Gratuit & Mode Hors-ligne PANU
// Permet de consulter les templates, d'accéder au Studio hors-ligne et de synchroniser au retour du réseau.
const CACHE_NAME = 'panu-offline-v3';
const STUDIO_TEMPLATES_CACHE = 'panu-studio-templates-v3';
const OFFLINE_URL = '/';

const PRECACHE_ASSETS = [
  '/',
  '/index.html',
  '/manifest.json',
  '/icons/icon-192x192.png',
  '/icons/icon-512x512.png',
  '/assets/panu-icon-192.png',
  '/assets/panu-icon-512.png'
];

// Installation : Mise en cache immédiate des ressources et du Studio hors-ligne
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(PRECACHE_ASSETS);
    }).then(() => self.skipWaiting())
  );
});

// Activation : Nettoyage des anciens caches
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames.map((cache) => {
          if (cache !== CACHE_NAME && cache !== STUDIO_TEMPLATES_CACHE) {
            return caches.delete(cache);
          }
        })
      );
    }).then(() => self.clients.claim())
  );
});

// Interception des requêtes réseau : Stale-While-Revalidate pour les templates & Studio, Network-First pour les API
self.addEventListener('fetch', (event) => {
  if (event.request.method !== 'GET') return;

  const url = new URL(event.request.url);

  if (url.pathname.endsWith('.apk') || url.pathname === '/download') {
    return;
  }

  // Mise en cache prioritaire des miniatures et templates du Studio PANU hors-ligne
  if (
    url.pathname.includes('/rest/v1/canvas_projects') ||
    url.hostname.includes('images.unsplash.com')
  ) {
    event.respondWith(
      caches.open(STUDIO_TEMPLATES_CACHE).then((cache) =>
        cache.match(event.request).then((cached) => {
          const networkFetch = fetch(event.request)
            .then((response) => {
              if (response && response.status === 200) {
                cache.put(event.request, response.clone());
              }
              return response;
            })
            .catch(() => cached);
          return cached || networkFetch;
        })
      )
    );
    return;
  }

  event.respondWith(
    fetch(event.request)
      .then((networkResponse) => {
        if (networkResponse && networkResponse.status === 200 && networkResponse.type === 'basic') {
          const responseToCache = networkResponse.clone();
          caches.open(CACHE_NAME).then((cache) => {
            cache.put(event.request, responseToCache);
          });
        }
        return networkResponse;
      })
      .catch(() => {
        return caches.match(event.request).then((cachedResponse) => {
          if (cachedResponse) {
            return cachedResponse;
          }
          if (event.request.mode === 'navigate') {
            return caches.match(OFFLINE_URL);
          }
          return new Response('Mode Hors-ligne PANU Studio actif', {
            status: 200,
            headers: new Headers({ 'Content-Type': 'text/plain; charset=utf-8' })
          });
        });
      })
  );
});

// Synchronisation automatique en arrière-plan dès le retour du réseau
self.addEventListener('sync', (event) => {
  if (event.tag === 'sync-panu-offline-creations') {
    event.waitUntil(
      self.clients.matchAll().then((clients) => {
        clients.forEach((client) => client.postMessage({ type: 'PANU_SYNC_OFFLINE_DRAFTS' }));
      })
    );
  }
});
