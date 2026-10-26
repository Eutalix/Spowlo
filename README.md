<div align="center">
<img src="https://user-images.githubusercontent.com/60316747/219976475-6dd606b0-8cb8-4dee-8665-544ae4e08ff2.svg" alt="spotify" width="100" height="100"/>
</div>
<h1 align="center">Spowlo (Community Fork)</h1>
<div align="center">
  
A Spotify songs downloader powered by [spotDL](https://github.com/spotDL/spotify-downloader/) made with Jetpack Compose and Material You.

> **Note:** This is a community fork maintained by **SomeRedstone** and **Eutalix**, building upon the excellent work of BobbyESP.

[![Telegram Channel](https://img.shields.io/badge/Telegram-Spowlo-green?style=flat&logo=telegram)](https://t.me/spowlo_chatroom)
![GitHub all releases](https://img.shields.io/github/downloads/Eutalix/Spowlo/total?label=Downloads&logo=github)
![GitHub Repo stars](https://img.shields.io/github/stars/Eutalix/Spowlo?color=informational&label=Stars)
 
   ![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/Eutalix/Spowlo?logo=github&logoColor=%23fff&style=for-the-badge)
  ![GitHub top language](https://img.shields.io/github/languages/top/Eutalix/Spowlo?style=for-the-badge)
</div>

## 👀 A new era
Spowlo is back, now as a joint effort between **SomeRedstone** and **Eutalix**.

The download pipeline no longer depends on Spotify's official Web API credentials. Metadata is fetched straight from Spotify's web player through a WebView and passed to spotDL, so downloads keep working even though the old shared keys were revoked.
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

Spowlo is distributed as an **ARM64-v8a** APK. 32-bit devices are not supported.

- Download the latest stable version from [GitHub releases](https://github.com/Eutalix/Spowlo/releases/latest)

## 🌍 Translation

We are currently setting up a new platform to manage translations. Stay tuned for updates! If you want to help, feel free to open an Issue or Pull Request.

## 📖 Credits & Acknowledgment

"A massive thank you to **BobbyESP**, the original creator of Spowlo. His work on the architecture, UI, and the complex Python-Android integration laid the foundation for everything we have here. This fork exists to honor that effort and keep the project serving the community."  
-Eutalix

"Spowlo is an incredible project that I find myself coming back to every now and then. This fork is a culmination of months of thought and attempts. <3

I'd also like to thank Eutalix, whose fork kept Spowlo alive after BobbyESP stepped down. We maintain the project together now, and it's quite the honor"
-SomeRedstone

### Original Credits by BobbyESP:
- Thanks to [xnetcat](https://github.com/xnetcat) for it's help with some spotDL related things!
- Thanks to [Seal](https://github.com/JunkFood02/Seal) and [JunkFood02](https://github.com/JunkFood02) for some of the code of the app and UI ideas. (Without you, this app would not have existed). I learnt a lot about architectures, coroutines, Jetpack Compose...
- [Philipp Lackner](https://www.youtube.com/c/PhilippLackner). Infinite thanks to you, Philipp. You made me learn infinite things with just a few videos.
- [Material color utilities](https://github.com/material-foundation/material-color-utilities) for having Material You coloring support in any device.
- Katoka, for the app name. (Thank you! Without your moral support I couldn't have done the app hahaha)
- [MoureDev by Brais Moure](https://www.youtube.com/c/MouredevApps)
- [Programación Android by AristiDevs](https://www.youtube.com/c/AristiDevs)

And also thank you all for the internal tests of the app!