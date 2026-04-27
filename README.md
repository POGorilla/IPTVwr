# IPTV Streaming Platform

A web-based IPTV streaming application built with Spring Boot. Browse free live TV channels from around the world, manage personal playlists, and watch HLS streams directly in the browser.

## Overview

This application provides a unified interface for streaming live TV content from two sources: the public **iptv-org** registry (10,000+ channels across 200+ countries) and user-uploaded `.m3u` playlists. Streams are proxied through the backend to bypass browser CORS restrictions, and the player automatically falls back across multiple stream sources when one is unavailable.

## Features

### Browsing & discovery

- Browse channels by country, then by genre
- Automatic country detection based on the user's IP, pinned at the top of the country list
- Per-country search across channel names
- Channel logos and country flags rendered from external CDNs
- Adult content filtered out at the data layer

### Playback

- In-browser HLS playback via HLS.js (with native fallback for Safari)
- Multi-source automatic fallback: when a stream times out, the player tries the next available source
- Manual Prev / Next source switching with live status indicator
- Backend stream proxy that rewrites HLS playlists so segment requests also route through the proxy

### Playlist management

- Multiple named playlists per user
- Default locked **Favorites** playlist created automatically on registration (cannot be renamed or deleted)
- Star button on every channel to toggle favorites — works across both iptv-org and user-uploaded channels
- Deduplication by `(name, category, country)` triple
- Manual channel addition via form (name, category, country, stream URL)
- Per-channel removal
- Bulk import via `.m3u` / `.m3u8` upload
- Export any playlist back to `.m3u` for use in VLC or other players

### Authentication

- Form-based login backed by Spring Security
- BCrypt password hashing
- Per-user data isolation enforced at every controller endpoint
- Session-based authentication with logout

### UI

- Responsive Bootstrap 5 layout
- Shared header / navigation via Thymeleaf fragments
- Custom error pages for 404 and 500 responses

## Technology stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.4 |
| Web | Spring MVC, Thymeleaf |
| Security | Spring Security |
| Persistence | Spring Data JPA, Hibernate |
| Database | PostgreSQL (hosted on Supabase) |
| Frontend | Bootstrap 5.3, HLS.js |
| External APIs | iptv-org, ip-api.com, flagcdn.com |
| Build | Maven |

## Architecture

```
Browser
   |
   | HTTP / HTTPS
   v
Spring Boot Application
   |-- Controllers
   |    |-- AuthController       (register, login)
   |    |-- BrowseController     (iptv-org browse + stream proxy)
   |    |-- ChannelController    (upload, watch user channels)
   |    |-- PlaylistController   (CRUD playlists & channels, export)
   |    '-- FavoritesController  (toggle favorite)
   |
   |-- Services
   |    |-- IptvOrgService       (cached fetch of countries/categories/channels/streams)
   |    |-- M3UParserService     (parse uploaded .m3u files)
   |    |-- GeoLocationService   (IP-to-country lookup)
   |    |-- FavoritesService     (favorite-key bookkeeping)
   |    |-- UserService          (registration, UserDetailsService)
   |    '-- FavoritesInitializer (CommandLineRunner backfill)
   |
   |-- Entities
   |    |-- User
   |    |-- Playlist  (id, name, locked, user)
   |    '-- Channel   (id, name, groupTitle, country, streamUrl, playlist)
   |
   '-- Stream proxy --> upstream HLS source
```

The `IptvOrgService` fetches and caches the entire iptv-org dataset at application startup. Subsequent browse and search operations are served entirely from in-memory collections.

The stream proxy at `/stream?url=...` accepts any upstream URL. For `.m3u8` playlists, it parses the manifest and rewrites segment URLs to also route through the proxy, ensuring CORS compliance for the entire playback chain.

## Getting started

### Prerequisites

- Java 21 or newer
- Maven 3.9+
- PostgreSQL database (a free Supabase instance works)

### Configuration

The application reads database credentials from environment variables:

| Variable | Description |
|---|---|
| `DB_URL` | JDBC connection string, e.g. `jdbc:postgresql://host:5432/postgres` |
| `DB_USER` | Database username |
| `DB_PASS` | Database password |

`src/main/resources/application.properties` references these via `${DB_URL}`, `${DB_USER}`, `${DB_PASS}`.

Optional overrides:

```properties
server.port=${PORT:8080}
spring.jpa.hibernate.ddl-auto=update
```

### Running locally

```bash
git clone https://github.com/<your-repo>/iptv.git
cd iptv

export DB_URL="jdbc:postgresql://..."
export DB_USER="..."
export DB_PASS="..."

./mvnw spring-boot:run
```

Open <http://localhost:8080> in your browser.

### Building a JAR

```bash
./mvnw clean package
java -jar target/iptv-0.0.1-SNAPSHOT.jar
```

## Routes

| Method | Path | Description |
|---|---|---|
| `GET` | `/`, `/login` | Login page |
| `GET`, `POST` | `/register` | Account creation |
| `GET` | `/home` | Home dashboard |
| `GET` | `/browse` | Country grid (auto-detected country pinned first) |
| `GET` | `/browse/{code}` | Genre list for a country (with optional `?q=` search) |
| `GET` | `/browse/{code}/{categoryId}` | Channels in a country and genre |
| `GET` | `/browse/watch/{channelId}` | Watch player for an iptv-org channel |
| `GET` | `/stream?url=...` | HLS / direct stream proxy |
| `GET` | `/upload` | Upload form |
| `POST` | `/upload` | Process an `.m3u` upload into a new playlist |
| `GET` | `/playlists` | All playlists for the current user |
| `POST` | `/playlists/new` | Create an empty playlist |
| `GET` | `/playlists/{id}` | View channels in a playlist |
| `GET` | `/playlists/{id}/export` | Download playlist as `.m3u` |
| `POST` | `/playlists/{id}/rename` | Rename (blocked on locked playlists) |
| `POST` | `/playlists/{id}/delete` | Delete (blocked on locked playlists) |
| `POST` | `/playlists/{id}/channels` | Add a channel manually |
| `POST` | `/playlists/{id}/channels/{channelId}/delete` | Remove a single channel |
| `POST` | `/favorites/toggle` | Star / unstar a channel |
| `GET` | `/watch/{id}` | Watch player for a user-owned channel |

## Project structure

```
src/main/
├── java/com/proiect/iptv/
│   ├── IptvApplication.java
│   ├── config/         (RestTemplate bean)
│   ├── controller/     (5 controllers)
│   ├── dto/            (iptv-org DTOs, WatchInfo, GeoResponse)
│   ├── entity/         (User, Playlist, Channel)
│   ├── repository/     (Spring Data JPA interfaces)
│   ├── security/       (SecurityConfig with form login)
│   └── service/        (6 services)
└── resources/
    ├── application.properties
    └── templates/      (Thymeleaf views, Bootstrap layout)
```

## Data sources

This project relies on the following public datasets and APIs:

- [iptv-org/api](https://github.com/iptv-org/api) — channel and stream catalog (CC0 licensed)
- [ip-api.com](https://ip-api.com) — IP geolocation (free for non-commercial use)
- [flagcdn.com](https://flagcdn.com) — country flag images

## Limitations and known issues

- The iptv-org dataset is fetched once at startup; the application must be restarted to pick up upstream changes.
- The stream proxy buffers nothing — long-running connections accumulate as concurrent threads under the default servlet container.
- IP geolocation only works correctly when the application is reachable from the public internet; running locally with a browser-only VPN will report the JVM's actual IP, not the browser's.
- Some upstream streams are geo-restricted, expired, or simply unavailable; the multi-source fallback mitigates but does not eliminate this.

## Roadmap

Possible future enhancements:

- Electronic Program Guide (EPG) integration
- Watch history and "continue watching" surface on the home page
- Drag-and-drop reordering within a playlist
- In-playlist search and group-based filtering for large playlists
- Public / shareable playlists between users
- Scheduled background refresh of the iptv-org dataset
- On-disk caching of the iptv-org JSON to accelerate cold starts

## License

This project was developed as coursework. Stream content is provided by upstream sources under their respective licenses; the iptv-org registry itself is CC0.
