#!/usr/bin/env python3
"""Pulls the current persisted query hashes out of the Spotify web player build.

Writes docs/spotify-hashes.json, which the app polls so a Spotify rotation doesn't
need an app update.

    python scripts/refresh-spotify-hashes.py            # update
    python scripts/refresh-spotify-hashes.py --check    # exit 2 when stale
    python scripts/refresh-spotify-hashes.py --dump     # show everything scraped
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import hmac
import json
import re
import struct
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

MANIFEST = Path(__file__).resolve().parent.parent / "docs" / "spotify-hashes.json"

USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"
)
WEB_PLAYER = "https://open.spotify.com/"
PATHFINDER = "https://api-partner.spotify.com/pathfinder/v1/query"
SECRETS_URL = "https://code.thetadev.de/ThetaDev/spotify-secrets/raw/branch/main/secrets/secretDict.json"

OPERATIONS = (
    "searchDesktop",
    "getTrack",
    "getAlbum",
    "queryArtistOverview",
    "queryArtistDiscographyAll",
    "fetchPlaylist",
    "profileAttributes",
    "libraryV3",
    "fetchLibraryTracks",
    "home",
)

CHUNK_HINTS = ("xpui-routes-search", "xpui-routes-artist")

OP_DEF = re.compile(
    r'new\s+[A-Za-z_$][\w$]*\.l\("([^"]+)","[^"]+","([0-9a-f]{64})"'
)
ENTRY_BUNDLE = re.compile(
    r"https://open\.spotifycdn\.com/cdn/build/web-player/web-player\.[0-9a-f]+\.js"
)
CDN_BASE = "https://open.spotifycdn.com/cdn/build/web-player/"


def log(message: str) -> None:
    print(message, flush=True)


def fetch(url: str, headers: dict[str, str] | None = None) -> tuple[int, str]:
    request = urllib.request.Request(
        url,
        headers={
            "User-Agent": USER_AGENT,
            "Accept": "*/*",
            "Accept-Language": "en-US,en;q=0.9",
            **(headers or {}),
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            return response.status, response.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as error:
        return error.code, error.read().decode("utf-8", "replace")


def object_at(text: str, start: int) -> dict[str, str]:
    end = text.index("}", start)
    return dict(re.findall(r'(\d+):"([^"]+)"', text[start:end]))


def bundle_operations(bundle_url: str) -> tuple[dict[str, str], int]:
    status, entry = fetch(bundle_url)
    if status != 200:
        raise RuntimeError(f"bundle fetch failed: HTTP {status}")

    operations = dict(OP_DEF.findall(entry))

    # most of what we need is in the entry bundle, the rest sits in route chunks
    marker = entry.find('u.u=e=>""+(({')
    chunks: list[str] = []
    if marker >= 0:
        names_at = entry.index("{", marker)
        names = object_at(entry, names_at)
        hashes_at = entry.index("+({", names_at) + 3
        hashes = object_at(entry, hashes_at)
        for chunk_id, name in names.items():
            if name in CHUNK_HINTS and chunk_id in hashes:
                chunks.append(f"{CDN_BASE}{name}.{hashes[chunk_id]}.js")

    for chunk_url in chunks:
        status, chunk = fetch(chunk_url)
        if status != 200:
            log(f"  ! chunk {chunk_url.rsplit('/', 1)[-1]} HTTP {status}")
            continue
        operations.update(OP_DEF.findall(chunk))

    return operations, len(chunks)


def totp_secret() -> tuple[int, list[int]]:
    status, body = fetch(SECRETS_URL)
    if status == 200:
        try:
            secrets = json.loads(body)
            version = max(int(key) for key in secrets)
            return version, [int(value) for value in secrets[str(version)]]
        except (ValueError, KeyError, TypeError):
            pass
    raise RuntimeError("couldnt read the totp secret list")


def totp(secret: list[int]) -> str:
    key = "".join(str(value ^ ((index % 33) + 9)) for index, value in enumerate(secret))
    counter = int(time.time()) // 30
    digest = hmac.new(key.encode("ascii"), struct.pack(">Q", counter), hashlib.sha1).digest()
    offset = digest[-1] & 0x0F
    binary = struct.unpack(">I", digest[offset : offset + 4])[0] & 0x7FFFFFFF
    return f"{binary % 1_000_000:06d}"


def web_player_session() -> tuple[str, str, str]:
    status, html = fetch(WEB_PLAYER, {"Accept": "text/html,application/xhtml+xml"})
    if status != 200:
        raise RuntimeError(f"open.spotify.com HTTP {status}")

    config = re.search(
        r'<script id="appServerConfig" type="text/plain">([^<]+)</script>', html
    )
    if not config:
        raise RuntimeError("appServerConfig missing from open.spotify.com")
    raw = config.group(1)
    try:
        decoded = json.loads(base64.b64decode(raw + "=" * (-len(raw) % 4)))
    except ValueError as error:
        raise RuntimeError(f"appServerConfig is not base64 json: {error}") from error
    client_version = decoded.get("clientVersion", "")
    if not client_version:
        raise RuntimeError("appServerConfig has no clientVersion")

    device = re.search(r"sp_t=([0-9a-f]+)", html)
    device_id = device.group(1) if device else ""

    version, secret = totp_secret()
    code = totp(secret)
    status, body = fetch(
        "https://open.spotify.com/api/token?reason=init&productType=web-player"
        f"&totp={code}&totpVer={version}&totpServer={code}",
        {"Referer": WEB_PLAYER, **({"Cookie": f"sp_t={device_id}"} if device_id else {})},
    )
    if status != 200:
        raise RuntimeError(f"token endpoint HTTP {status}: {body[:200]}")
    token_json = json.loads(body)
    token = token_json.get("accessToken", "")
    if not token:
        raise RuntimeError("token endpoint returned no accessToken")

    # optional, we still get useful errors without it
    client_token = ""
    payload = json.dumps(
        {
            "client_data": {
                "client_version": client_version,
                "client_id": token_json.get("clientId", ""),
                "js_sdk_data": {
                    "device_brand": "unknown",
                    "device_model": "unknown",
                    "os": "windows",
                    "os_version": "NT 10.0",
                    "device_id": device_id,
                    "device_type": "computer",
                },
            }
        }
    ).encode()
    request = urllib.request.Request(
        "https://clienttoken.spotify.com/v1/clienttoken",
        data=payload,
        headers={
            "User-Agent": USER_AGENT,
            "Content-Type": "application/json",
            "Accept": "application/json",
            "Origin": "https://open.spotify.com",
            "Referer": WEB_PLAYER,
            **({"Cookie": f"sp_t={device_id}"} if device_id else {}),
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=45) as response:
            granted = json.loads(response.read().decode("utf-8", "replace"))
        client_token = granted.get("granted_token", {}).get("token", "")
    except (urllib.error.URLError, ValueError) as error:
        log(f"  ! no client token ({error}), carrying on")

    return token, client_token, client_version


def is_live(operation: str, digest: str, token: str, client_token: str, client_version: str) -> bool:
    # a dead hash says PersistedQueryNotFound. anything else means the hash is registered,
    # even when the call fails because of auth or missing variables. thats what lets us check
    # account-only queries without an account.
    url = PATHFINDER + "?" + urllib.parse.urlencode(
        {
            "operationName": operation,
            "variables": "{}",
            "extensions": json.dumps({"persistedQuery": {"version": 1, "sha256Hash": digest}}),
        }
    )
    headers = {
        "Authorization": f"Bearer {token}",
        "Spotify-App-Version": client_version,
        "App-Platform": "WebPlayer",
        "Origin": WEB_PLAYER.rstrip("/"),
        "Referer": WEB_PLAYER,
    }
    if client_token:
        headers["Client-Token"] = client_token
    status, body = fetch(url, headers)
    if "PersistedQueryNotFound" in body:
        return False
    if status == 401:
        raise RuntimeError("anonymous token rejected, cant validate anything")
    return True


def read_manifest() -> dict:
    if not MANIFEST.exists():
        return {}
    try:
        return json.loads(MANIFEST.read_text(encoding="utf-8"))
    except ValueError:
        return {}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="fail when the manifest is stale")
    parser.add_argument("--dump", action="store_true", help="print every scraped operation")
    parser.add_argument("--strict", action="store_true", help="fail when hashes cant be validated")
    parser.add_argument("--skip-validation", action="store_true")
    args = parser.parse_args()

    log("Fetching the web player build...")
    status, html = fetch(WEB_PLAYER, {"Accept": "text/html,application/xhtml+xml"})
    if status != 200:
        log(f"open.spotify.com HTTP {status}")
        return 1
    match = ENTRY_BUNDLE.search(html)
    if not match:
        log("web-player bundle url missing from open.spotify.com")
        return 1
    bundle_url = match.group(0)
    bundle_version = bundle_url.rsplit("/", 1)[-1].removesuffix(".js")
    log(f"Build: {bundle_version}")

    scraped, chunk_count = bundle_operations(bundle_url)
    log(f"Scraped {len(scraped)} operations from the entry bundle and {chunk_count} route chunk(s)")

    if args.dump:
        for name in sorted(scraped):
            print(f"{name} = {scraped[name]}")
        return 0

    previous = read_manifest()
    previous_ops = previous.get("operations", {})

    try:
        token, client_token, client_version = web_player_session()
    except (RuntimeError, urllib.error.URLError) as error:
        if args.strict:
            log(f"  ! no session ({error})")
            return 1
        log(f"  ! no session ({error}), writing scraped hashes unvalidated")
        token = client_token = ""
        client_version = previous.get("clientVersion", "")

    operations: dict[str, str] = {}
    stale: list[str] = []
    missing: list[str] = []
    for operation in OPERATIONS:
        existing = previous_ops.get(operation, "")
        scraped_hash = scraped.get(operation, "")

        # keep a hash that still works even if the bundle has a newer one. spotify keeps old
        # persisted queries alive for a while and swapping documents can shift the response shape
        candidate = existing or scraped_hash
        if not candidate:
            missing.append(operation)
            continue

        if token and not args.skip_validation:
            if not is_live(operation, candidate, token, client_token, client_version):
                log(f"  x {operation}: PersistedQueryNotFound")
                stale.append(operation)
                candidate = scraped_hash
                if not candidate or not is_live(
                    operation, candidate, token, client_token, client_version
                ):
                    missing.append(operation)
                    continue

        operations[operation] = candidate

    # only the hashes matter, so metadata is left alone unless something actually changed
    changed = (
        previous_ops != operations
        or not previous.get("webPlayer")
        or not MANIFEST.exists()
    )

    if stale:
        log(f"  replaced {len(stale)} stale hash(es): {', '.join(stale)}")
    if missing:
        log(f"  ! no working hash for: {', '.join(missing)}")

    if args.check:
        if changed or stale:
            log("Manifest is stale.")
            return 2
        log("Manifest is current.")
        return 0

    if changed:
        manifest = {
            "schema": 1,
            "updatedAt": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
            "webPlayer": bundle_version,
            "clientVersion": client_version or previous.get("clientVersion", ""),
            "source": bundle_url,
            "operations": operations,
        }
        MANIFEST.parent.mkdir(parents=True, exist_ok=True)
        MANIFEST.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
        log(f"Wrote {MANIFEST.relative_to(MANIFEST.parent.parent)}")
    else:
        log("Manifest unchanged.")

    return 0 if not missing else 1


if __name__ == "__main__":
    sys.exit(main())
