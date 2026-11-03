# VEYLORA VPN
**Your privacy. Your Freedom.**
Developer: TK Sadhan · sadhanpatar@gmail.com

Android VPN app (Kotlin + Jetpack Compose) built on the WireGuard tunnel library and Android `VpnService`.

> **Important:** a VPN app is only the *client*. To actually protect users you need your own
> WireGuard server(s). The app cannot work until you add at least one real server config.

## 1. Add your server(s)
Edit `app/src/main/assets/servers.json`. Each entry has a full WireGuard client config
(see the placeholder). Users can also import any `.conf` file from the **Servers** tab.

Quick server setup (Ubuntu VPS): run the open-source script
<https://github.com/angristan/wireguard-install> and create a client; paste the generated
config into `servers.json` (use `\n` for new lines).

> For a public app with many users, do **not** ship one shared client key. Build a small
> backend that creates a unique WireGuard peer per user/device and returns the config to the app.
> Shipping keys inside the APK is fine for testing / private use only.

## 2. Push to GitHub
```bash
git init && git add . && git commit -m "VEYLORA VPN"
git branch -M main
git remote add origin https://github.com/<you>/<repo>.git
git push -u origin main
```
`Build debug APK` runs automatically; download the APK from **Actions → run → Artifacts**.

## 3. Privacy Policy & Terms URLs (Play Store needs a public link)
GitHub repo → **Settings → Pages → Source: GitHub Actions**. After the `pages` workflow runs:
- Privacy Policy: `https://<you>.github.io/<repo>/privacy.html`
- Terms: `https://<you>.github.io/<repo>/terms.html`

The same pages are bundled inside the app (Settings → Privacy Policy / Terms).
**Edit the text if anything is untrue for your setup** (e.g. if your servers keep logs, say so).

## 4. Create a signing key (once) and add GitHub secrets
```bash
keytool -genkeypair -v -keystore veylora-upload.jks -alias veylora -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 veylora-upload.jks > keystore.b64      # macOS: base64 -i veylora-upload.jks
```
Repo → Settings → Secrets and variables → Actions → add:
`KEYSTORE_BASE64` (contents of keystore.b64), `KEYSTORE_PASSWORD`, `KEY_ALIAS` (veylora), `KEY_PASSWORD`.
**Back up the .jks file and passwords safely. Never commit them.**

Then Actions → **Build signed release** → Run workflow → download the `.aab`.

## 5. Play Console checklist
1. Create app → upload the `.aab` (use Play App Signing).
2. **Store listing:** use `store/icon-512.png` and `store/feature-graphic-1024x500.png`; add 2+ phone screenshots.
3. **Privacy policy URL:** the GitHub Pages link above.
4. **App content → Data safety:** declare what you collect (if nothing, say so; if your servers log anything, declare it).
5. **App content → VPN service declaration:** required for apps using `VpnService`. Core functionality = VPN; do not use it for ads, data collection or traffic manipulation.
6. Content rating, target audience (18+ recommended), ads = No.
7. New personal developer accounts must run a closed test (12+ testers for 14 days) before production access.
8. Check Play's current target-API requirement and bump `targetSdk`/`compileSdk` in `app/build.gradle.kts` if needed.
9. Some countries restrict VPN apps; review Play's VPN policy and exclude countries if necessary.

## Project layout
- `app/src/main/java/com/veylora/vpn` – UI (Compose), `VpnManager` (WireGuard `GoBackend`), repository
- `app/src/main/assets` – `servers.json`, `privacy.html`, `terms.html`
- `docs/` – same pages for GitHub Pages
- `.github/workflows` – debug build, signed release, Pages deploy
- `store/` – Play Store icon and feature graphic

Open in Android Studio (Koala+ / JDK 17) or build with `gradle :app:assembleDebug`.
