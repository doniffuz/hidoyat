# HIDOYAT — 1-BOSQICH (loyiha strukturasi)

## Arxitektura (qisqa)
- Bitta app moduli, ichida paketlar (MVVM): core/ (tema, util), data/ (Room, DataStore, remote, repository), domain/ (modellar), feature/* (har bir ekran: UI + ViewModel).
- Offline-first: asosiy ma lumot qurilmada, internet faqat qo shimcha.
- Hozircha ruxsatlar yo q, keraksiz kutubxona yo q.
- minSdk 26 (Android 8+), targetSdk 34, Kotlin 1.9.24, Compose BOM 2024.06, AGP 8.5.2, Gradle 8.9, JDK 17.

## Termux
pkg update && pkg install unzip git openjdk-17 gradle
unzip HIDOYAT_stage1.zip && cd HIDOYAT
git init && git add . && git commit -m "1-bosqich: loyiha strukturasi"
git branch -M main
git remote add origin https://github.com/LOGIN/hidoyat.git
git push -u origin main

## Build
Termux da Android SDK bo lmasa: GitHub ga push qiling, Actions > Android CI > artifact hidoyat-debug-apk ni yuklab oling.
Android Studio da ham to g ridan-to g ri ochiladi.
Eslatma: gradle-wrapper.jar kiritilmagan; Android Studio yoki gradle wrapper buyrug i yaratadi.

## CHECKPOINT 1
Tayyor: Gradle (Kotlin DSL, version catalog), manifest, Application, MainActivity (Compose), tema (yashil/oltin/krem, dark+light), 3 tilda satrlar (uz lotin, kirill, rus), ikonka, paket papkalari, .gitignore, GitHub Actions.
Keyingi: 2-BOSQICH — premium dizayn tizimi.
