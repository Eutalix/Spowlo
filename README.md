<div align="center">
<img src="https://user-images.githubusercontent.com/60316747/219976475-6dd606b0-8cb8-4dee-8665-544ae4e08ff2.svg" alt="spotify" width="100" height="100"/>
</div>
<h1 align="center">Spowlo (Community Fork)</h1>
<div align="center">
  
A Spotify songs downloader powered by [spotDL](https://github.com/spotDL/spotify-downloader/) made with Jetpack Compose and Material You.

> **Note:** This is an active fork maintained by **Eutalix**, building upon the excellent work of BobbyESP.

[![Telegram Channel](https://img.shields.io/badge/Telegram-Spowlo-green?style=flat&logo=telegram)](https://t.me/spowlo_chatroom)
![GitHub all releases](https://img.shields.io/github/downloads/Eutalix/Spowlo/total?label=Downloads&logo=github)
![GitHub Repo stars](https://img.shields.io/github/stars/Eutalix/Spowlo?color=informational&label=Stars)
 
   ![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/Eutalix/Spowlo?logo=github&logoColor=%23fff&style=for-the-badge)
  ![GitHub top language](https://img.shields.io/github/languages/top/Eutalix/Spowlo?style=for-the-badge)
</div>

---

## 🚨 CRITICAL NOTICE: DOWNLOADS TEMPORARILY UNAVAILABLE 🚨

**As of January 2026, downloading from Spotify links is currently broken for most users.**

**The Cause:**
1. **API Keys Revoked:** The public Spotify API credentials used by Spowlo (and the underlying `spotDL` engine) have been revoked/banned by Spotify.
2. **Spotify Dashboard Lockout:** Spotify has globally disabled the creation of new Developer Apps. This means you **cannot generate new credentials** to fix the issue manually at this moment. (See discussion: [Reddit Thread](https://www.reddit.com/r/truespotify/comments/1q4sbi3/new_developer_apps_silently_disabled_integrations/)).

**Workaround:**
If you already had a **Spotify Developer App created prior to this week**, you are one of the lucky ones!
1. Go to **Settings > Spotify**.
2. Enable **"Use custom credentials"**.
3. Enter your existing **Client ID** and **Client Secret**.

For everyone else, we are monitoring the situation and waiting for Spotify to lift the restriction on new app creation. **Please do not open new issues regarding "Failed to fetch metadata".**

---

## 🚀 What's New?
This fork aims to keep Spowlo alive and updated. We have introduced a **Unified Dependency Updater** that fixes the "ghost version" bugs and ensures that `spotDL` and `yt-dlp` are always using the latest and greatest versions, directly from PyPI, without needing frequent APK updates.

## 📸 Screenshots

<div align="center">
<div>
<img src="https://user-images.githubusercontent.com/60316747/219976933-f0d72d37-2202-4eed-a152-50e3f346f322.jpg" width="30%" />
<img src="https://user-images.githubusercontent.com/60316747/219976935-01b6457e-8793-463c-8c31-0b2557b636c2.jpg" width="30%" />
<img src="https://user-images.githubusercontent.com/60316747/219976936-6bf56e67-8763-47cf-af8b-ce56ece4caa2.jpg" width="30%" />
</div>
</div>

## ⚠️ Content Warning
Spowlo uses YT Music and YouTube to download the songs. This is because Spotify DRM bypassing can lead to an account ban and legal issues. If YT Music isn't available in your country, don't worry, you can still use YouTube as audio provider or use a VPN. We are working on making a regional bypass so don't matter your region. Thank you for understanding.

## 🔮 Features

- Download songs from Spotify thanks to the [spotDL](https://github.com/spotDL/spotify-downloader/) library.

- Downloading without links, just a search query 

- Download full playlists with just one click.

- Embed synced lyrics into the downloaded songs.

- Easy to use and user-friendly.

- [Material Design 3](https://m3.material.io/) style UI, with dynamic color theme.

- MAD: UI and logic written purely on Kotlin. It's used just an activity and composable destinations and deep links thanks to the navigation library.

## ⬇️Download

For most devices, it is recommended to install the **ARM64-v8a** version of the apks.

- Download the latest stable version from [GitHub releases](https://github.com/Eutalix/Spowlo/releases/latest)

## 🌍 Translation

We are currently setting up a new platform to manage translations. Stay tuned for updates! If you want to help, feel free to open an Issue or Pull Request.

## 📖 Credits & Acknowledgment

A massive thank you to **BobbyESP**, the original creator of Spowlo. His work on the architecture, UI, and the complex Python-Android integration laid the foundation for everything we have here. This fork exists to honor that effort and keep the project serving the community.

### Original Credits by BobbyESP:
- Thanks to [xnetcat](https://github.com/xnetcat) for it's help with some spotDL related things!
- Thanks to [Seal](https://github.com/JunkFood02/Seal) and [JunkFood02](https://github.com/JunkFood02) for some of the code of the app and UI ideas. (Without you, this app would not have existed). I learnt a lot about architectures, coroutines, Jetpack Compose...
- [Philipp Lackner](https://www.youtube.com/c/PhilippLackner). Infinite thanks to you, Philipp. You made me learn infinite things with just a few videos.
- [Material color utilities](https://github.com/material-foundation/material-color-utilities) for having Material You coloring support in any device.
- Katoka, for the app name. (Thank you! Without your moral support I couldn't have done the app hahaha)
- [MoureDev by Brais Moure](https://www.youtube.com/c/MouredevApps)
- [Programación Android by AristiDevs](https://www.youtube.com/c/AristiDevs)

And also thank you all for the internal tests of the app!