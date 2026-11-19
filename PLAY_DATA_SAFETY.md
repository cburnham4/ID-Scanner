# Google Play Data Safety — ID Scanner

**App:** ID Scanner (`letshangllc.idscanner`)  
**Version:** 3.0.0 (versionCode 100)  
**Target SDK:** 36  
**Last updated:** November 2026

Use this document when filling out the Play Console Data safety form and related permission declarations.

---

## Overview

ID Scanner reads a US driver’s license PDF417 barcode with the device camera, parses name / age / address fields **on device**, and shows whether the holder meets a user-set minimum age. It displays AdMob banners (after UMP consent). It does **not** create accounts, sync ID data to a backend, or upload government ID images.

---

## Government ID / sensitive personal data (important)

| Question | Answer |
|----------|--------|
| Does scanned ID data leave the device? | **No** — barcode bytes and parsed fields (name, DOB, address, expiration, etc.) stay in process memory / UI only. There is no app server upload of ID content. |
| Are ID images stored? | **No** — the camera preview is used for barcode recognition; the app does not save ID photos to disk or gallery. |
| Shared with third parties? | **ID payload is not shared.** AdMob may collect advertising identifiers and app/device signals for ads (separate from ID contents). |

Play policy treats government ID numbers and similar credentials as sensitive. Declare **Personal info → Government ID** (or equivalent) only if you later change the app to collect/share that data. With the current on-device-only design, ID fields are processed ephemerally for age verification UI and are not transmitted.

---

## Data collection summary

| Data type | Collected | Shared | Purpose |
|-----------|-----------|--------|---------|
| Government ID / license fields (name, DOB, address, etc.) | Processed on device only (ephemeral) | No | App functionality (age check UI) |
| Camera (live preview / frames) | Used locally for barcode scan | No | App functionality |
| App interactions | Yes (via AdMob) | Yes (AdMob) | Advertising |
| Advertising ID / device or other IDs | Yes (via AdMob) | Yes (AdMob) | Advertising |
| Approximate location | Possibly (AdMob/UMP, depending on consent & partners) | Yes (ad partners) | Advertising |

---

## Permissions that need Play declarations

| Permission | Why | Play notes |
|------------|-----|------------|
| `CAMERA` | Scan PDF417 barcode on the back of an ID | Declare camera use; prominent in-app disclosure / runtime request before scan. Not used for unrelated background capture. |
| `INTERNET` | Load AdMob ads / UMP consent | Standard |
| `ACCESS_NETWORK_STATE` | Ads SDK connectivity checks | Standard |

`ACCESS_WIFI_STATE` was removed as unused by the app itself.

---

## Third-party SDKs

### Google Mobile Ads (AdMob)
- **Data:** Advertising ID, app interactions, device info (per Google’s policies / user consent)
- **Purpose:** Banner ads (HTTPS)
- **Privacy:** https://policies.google.com/privacy

### Google User Messaging Platform (UMP)
- **Purpose:** GDPR/consent before personalized ads
- **Privacy:** https://policies.google.com/privacy

### IDScan PDF417 scanner + DL parser (local AAR/JAR + `.so`)
- **Data:** Camera frames and barcode payload processed on device
- **Network:** None from these libraries in this integration
- License keys are required at runtime (see `license_keys.xml` TODOs)

Amazon Mobile Ads was **removed** (legacy HTTP / unsupported).

---

## Security

- **In transit:** Ad / consent traffic uses HTTPS via Google SDKs. ID scan data is not sent over the network by the app.
- **At rest:** No persistent storage of scanned ID fields in this codebase.
- **Deletion:** Nothing to delete for ID scans beyond clearing app storage / uninstall. Ad-related identifiers follow device / Google controls.

---

## Children’s privacy / Families

This app is **not** directed at children under 13 (COPPA) or under 16 (GDPR).  
UMP is configured with `setTagForUnderAgeOfConsent(false)`. Do not enroll in Designed for Families while serving personalized ads without reviewing Families ads policy.

---

## Play Console form — suggested answers

**Does your app collect or share any of the required user data types?**  
Yes (at least Device or other IDs via AdMob). Treat government ID fields as **not collected/shared** if you only process them on device and do not store/transmit them; re-evaluate if that changes.

**Is all of the user data collected by your app encrypted in transit?**  
Yes (for network-collected ad/analytics data via Google SDKs).

**Do you provide a way for users to request that their data is deleted?**  
Yes — uninstall / clear app data; contact developer email on the Play listing for ad-related requests. No cloud ID archive exists in this app.

### Data types to declare (ads)

1. **Device or other IDs** — Collected: Yes; Shared: Yes (Google/AdMob); Purpose: Advertising; Optional via consent / device settings  
2. **App interactions** — Collected: Yes; Shared: Yes (AdMob); Purpose: Advertising  

---

## Owner follow-ups before publish

1. Replace TODO AdMob app ID + banner unit ID in `app/src/main/res/values/ad_config.xml` (publisher `ca-app-pub-8223005482588566`).
2. Replace TODO scanner/parser license keys in `app/src/main/res/values/license_keys.xml` (or restore private `keys.xml`).
3. Complete Data safety + Camera permission declarations to match this document.
4. Note: native parser libs are only `armeabi-v7a` / `x86` (no `arm64-v8a`); test on target devices.
