# Verified endpoints

**How these were verified:** by reading the server source in `manhwa-api.zip`
(`src/routes/index.js`, `services/comicService.js`, `services/decorate.js`, `http/respond.js`,
`engine/scrapers/*`). **They were not called against the live Railway deployment** (no network in the
authoring environment), so run the app once against the live server and check Logcat (debug builds log
requests) to confirm the deployed version matches this ZIP.

Base URL: `BuildConfig.BASE_URL` (default `https://tets-server-production.up.railway.app/`, override with
`-PTINTA_BASE_URL=...`). Optional API key: `-PTINTA_API_KEY=...` -> sent as `x-api-key` (API and images).

Envelope for every JSON endpoint: `{ "success": true, "data": ... }` or
`{ "success": false, "error": { "code", "message" } }`.

| Endpoint | Used by | `data` shape (fields the app reads) |
| --- | --- | --- |
| `GET /api/home?source=all&refresh=` | Home | `popular[]`, `latest[]`, `new[]` of Comic, `scrapedAt` |
| `GET /api/list?source=all&page=` | Explore (All), View All | `items[]`, `page`, `hasNextPage` |
| `GET /api/search?q=&source=all&page=` | Search | `items[]`, `page`, `hasNextPage`, `resolvedQuery?` |
| `GET /api/genres` | Explore chips | `[{ name, slug, category, countHint }]` |
| `GET /api/genre/:genre?source=all&page=` | Explore (genre) | `items[]`, `page`, `hasNextPage` |
| `GET /api/type/:type?source=all&page=` | Explore (type) | `items[]`, `page`, `hasNextPage`; types used: `manhwa`, `manga`, `manhua` |
| `GET /api/comic/:source/:id` | Detail | `comic` + `chapters[]` (**newest first**) |
| `GET /api/comic/:source/:id/chapter/:chapter` | Reader | `comic`, `chapter{ number,title,type,images[{index,image,rawUrl}],novelContent?,prevChapter,nextChapter }`, `chapters[]` |
| `GET /api/image-proxy?url=&source=` | Coil (via `cover` / `images[].image`) | binary image; URLs are already absolute in responses |

Comic: `id, source, sourceId, slug, title, alternativeTitles[], type, status, genres[], author, artist,
cover, rawCover, synopsis, latestChapter, rating{value,count}, updatedAt`. Anything the source does not
provide is `null` / `[]`; the app shows nothing for those fields instead of inventing values.

Chapter list item: `id, comicId, source, sourceId, chapter, title, url, releaseDate`.
For Shinigami, `chapter` is the chapter UUID; for other sources it is the chapter number. The app always
passes the `chapter` string back to the reader endpoint unchanged and takes prev/next from the reader response.

## Not provided by the backend (so not faked in the app)

| Feature | App behaviour |
| --- | --- |
| Login / accounts | No login UI; Profile says accounts are unavailable |
| Bookmarks, reading history | Stored locally (Room) |
| Notifications | No notification icon |
| Recommendations | None; Home shows the server's `popular` and `new` lists under those names |
| Reader counts | Never shown (not in the schema) |
| Sort options | None; order is whatever the server returns. Type and genre filters are real |
| Downloads | Not implemented |

## Error handling
Network/timeout/HTTP/parse failures are mapped in `util/Errors.kt`. Server error codes handled by status:
`401, 404, 429, 5xx (502/504 from sources)`. Retrofit read timeout is 30 s because source scraping can be slow
(server-side timeouts are 9.5-15 s).
