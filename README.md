# Hatırlatıcı

Android hatırlatıcı uygulaması: bildirim gönderir, ana ekran widget'ı vardır ve takvimde günlere not düşmeyi sağlar.

## Özellikler (v0.1)

- Hatırlatıcı ekleme, düzenleme, silme (başlık, not, tarih, saat)
- Zamanı gelince bildirim, bildirimden **Tamamlandı** ve **10 dk ertele**
- Tekrarlama: her gün, hafta, ay, yıl
- Aylık takvim: notu olan günler işaretli, bir güne dokununca o günün notları ve "Not ekle"
- Ana ekran widget'ı: yaklaşan hatırlatıcılar ve hızlı ekleme
- Telefon yeniden başlayınca alarmlar korunur

## APK'yı indirme

Her `main` push'unda GitHub Actions debug APK derler:
**Actions** sekmesi → son "APK derle" çalışması → sayfanın altındaki **Artifacts** bölümünden `hatirlatici-debug-apk` dosyasını indir, zip'i aç, `app-debug.apk`'yı telefona kur
(telefonda "bilinmeyen kaynaklardan yükleme" izni gerekir).

## Teknik yapı

- Kotlin, Jetpack Compose (Material 3), minSdk 26, targetSdk 35
- Room (veritabanı), AlarmManager (tam zamanlı alarm), Glance (widget)
- Paketler:
  - `data`: `Reminder` varlığı, DAO, veritabanı, `ReminderRepository` (tek giriş noktası)
  - `alarm`: alarm kurma, bildirimler, alarm ve açılış (boot) alıcıları
  - `widget`: Glance ana ekran widget'ı
  - `ui`: ekranlar (liste, takvim, düzenleme) ve ViewModel'ler
  - `AppContainer`: basit bağımlılık kabı

Yerelde derlemek için: `./gradlew assembleDebug` (JDK 17 ve Android SDK gerekir).

## Yol haritası

- Kategoriler ve renkler, arama
- Konuma veya özel günlere göre hatırlatma
- Yedekleme ve geri yükleme
- Farklı widget boyutları, takvim widget'ı
