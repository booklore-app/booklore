# BookLore iOS App — Scope & Plan

Status: **superseded, not started.** See
`bookshelf` repo's `docs/companion-reader-app-plan.md` (scoped
2026-08-07) for the direction actually chosen: a fork of
`advplyr/audiobookshelf-app`, generic OPDS (not BookLore's native API),
cross-platform iOS+Android from one codebase (not iOS-only), and
Bookshelf-branded rather than BookLore-branded. The `audiobookshelf-app`
fork evaluation below carried forward into that plan; the native
SwiftUI/Readium direction and the BookLore-native-API choice did not.

Original scoping notes kept below for reference (scoped 2026-08-02).

## Why

Existing OPDS-compatible iOS readers tried so far have been unsatisfying
(Panels is comic-focused and subscription-based; general knowledge of
current free options on iOS is unreliable/stale and shouldn't be trusted
without checking the App Store fresh). Decided to scope a purpose-built
companion app instead of continuing to search.

## Before starting: check for a forkable existing project

Worth a real search before writing anything from scratch — there may be an
existing open-source OPDS/self-hosted-library iOS client (or an existing
Komga/Kavita/Calibre-Web client) close enough to fork and adapt, which
would be much faster than building from zero. Check:
- GitHub search for OPDS/Komga/Kavita Swift/SwiftUI iOS clients
- Whether any existing client's architecture would accept a BookLore
  backend with reasonable changes (auth flow, API shape)

If nothing suitable turns up, proceed with the from-scratch plan below.

### Candidate found: `advplyr/audiobookshelf-app` (evaluated 2026-08-02)

- **Stack**: NuxtJS + Capacitor (web app in a native shell/WebView), not
  native Swift. iOS + Android from one codebase.
- **License**: GPL-3.0. No conflict with BookLore's AGPL-3.0 — AGPL's
  network-copyleft clause applies to the *server*, not to client apps that
  merely talk to it over the API (same pattern as GitLab/Mastodon mobile
  clients). A fork would just need to stay GPL-3.0 itself.
- **Maturity**: actively maintained, 2,596+ commits. Audiobookshelf serves
  both audiobooks and ebooks, so the reader UI already covers this exact
  use case (library browsing, EPUB/PDF/audio playback, progress sync,
  auth, settings all already built).
- **Their own iOS status**: still TestFlight-beta-only, capped at Apple's
  10k tester limit — separate concern from whether a fork could go through
  full App Store review under a new identity.
- **Tradeoff vs. the native SwiftUI/Readium plan below**: much faster
  start (mature app to adapt vs. building from zero) but WebView-based
  rather than fully native feel/performance, and still requires ripping
  out and rebuilding the entire API layer (auth, book/library models,
  progress sync calls) to match BookLore's endpoints instead of
  Audiobookshelf's — a real refactor, just smaller than from-scratch.

**Decision not yet made** — pick this up when back from the trip.

## Decisions made

- **Distribution**: full public App Store release (not TestFlight-only or
  sideload). Requires an Apple Developer Program membership ($99/yr,
  enrolled under the user's own Apple ID — not something that can be done
  on their behalf). TestFlight is still the right intermediate step before
  public submission regardless.
- **v1 (MVP) scope**: reading-focused only. No upload-from-phone, no
  comics/audiobooks/shelves in v1 — just browse library → read → progress
  syncs back to BookLore.

## Architecture

- **Data layer**: BookLore's own JWT REST API (the same one the Angular
  frontend uses), **not OPDS**. OPDS is a good fit for generic third-party
  readers (KOReader etc.) but is a limited Atom-feed protocol; the native
  API gives structured JSON for libraries/books/covers/progress, which is
  what a purpose-built app needs.
- **Reading engine**: [Readium Swift Toolkit](https://readium.org) for
  EPUB (handles parsing/pagination/rendering — not building an EPUB engine
  from scratch). **PDFKit** (Apple's native framework) for PDF instead of
  routing PDFs through Readium too.
- **Auth**: JWT + refresh token stored in iOS Keychain. Login screen needs
  a **server URL field** (self-hosted, not a fixed backend) — same
  established pattern as other self-hosted-client App Store apps
  (Tailscale, Infuse, etc.), shouldn't hit review friction as a category.
- **UI**: SwiftUI throughout.
- **Offline**: cache downloaded book files locally; lightweight local
  metadata store (SwiftData) so the library list works without a live
  connection.

## Confirmed relevant backend endpoints (checked against actual code, 2026-08-02)

- Auth: `AuthenticationController` (JWT login/refresh, same as web)
- Reading progress: `POST /api/v1/books/{id}/progress`,
  `POST /api/v1/books/{id}/reset-progress` (`BookController.java`) — the
  same endpoint the web EPUB reader itself uses, cleaner fit than
  reverse-engineering the KOReader hash-based sync protocol
  (`/api/koreader/**`), which also exists but is a different, more
  awkward protocol designed for a different client
- Libraries/books/authors: `LibraryController`, `BookController`,
  `AuthorController`
- Covers/media: `BookCoverController`, `BookMediaController`
- Bookmarks/notes/reviews exist too (`BookMarkController`,
  `BookNoteController`/`BookNotesV2Controller`, `BookReviewController`) —
  out of scope for v1, noted for later

## MVP feature list

1. Login (server URL + credentials)
2. Library list → book grid/list with covers, search
3. Book detail (metadata, description, "Read" button)
4. EPUB reader (Readium) — pagination, font/theme controls (reading UX
   quality is the whole point, given what's been tried so far)
5. PDF reader (PDFKit)
6. Reading progress synced via the native progress endpoint above — save
   on close/periodically, restore on open, so web and app stay in sync
7. Downloaded-book caching for offline reading
8. Settings: server URL, logout

## Build order

1. Xcode project setup + login/networking layer
2. Library browsing
3. EPUB reading (Readium integration)
4. Progress sync
5. PDF support + offline caching
6. App Store polish (icon, screenshots, privacy nutrition labels) →
   TestFlight → submit for review

## Open items / decisions still needed

- Repo location: separate repo suggested (`X2-Consult/booklore-ios` or
  similar) — different toolchain entirely (Swift/Xcode) from this monorepo
- Minimum iOS version: default suggestion is iOS 17+ (matches the test
  device already in use)
- Apple Developer Program enrollment — user action, not something that can
  be started on their behalf
- Revisit the production admin-permissions issue noted in `CHANGELOG.md`
  before relying on the production instance for app development/testing
