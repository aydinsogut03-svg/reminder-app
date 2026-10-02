# Hatırlatıcı

Android hatırlatıcı uygulaması: bildirim gönderir, ana ekran widget'ı vardır ve takvimde günlere not düşmeyi sağlar.

## Özellikler (v0.7)

- Hatırlatıcı ekleme, düzenleme, silme (başlık, not, tarih, saat)
- Zamanı gelince bildirim, bildirimden **Tamamlandı** ve **10 dk ertele**
- Tekrarlama: her gün, hafta, ay, yıl
- Açılış ekranı kalemli takvim. Uyarılar sekmesi: sıradaki uyarı ve geri sayım, kalem/ses/yeni hızlı düğmeleri, **Yaklaşan** ve **Geçmiş** (kaçırılan + tamamlanan) sekmeleri
- Aylık takvim: notu olan günler işaretli; altında hep açık yazı alanı var, kalemle yazınca seçili güne kendiliğinden eklenir ("10'da dişçi" gibi saat yazılırsa o saate kurulur)
- Ana ekran widget'ı: 7 günlük şerit ve kalem alanı; bir güne dokununca küçük bir kalem penceresi açılır, yazılan o güne varsayılan saatle (ya da yazılan saatle) eklenir. Altında yaklaşan hatırlatıcılar
- Telefon yeniden başlayınca alarmlar korunur
- Sesle ekleme: "yarın saat 9'da annemi ara" gibi Türkçe cümlelerden başlık ve zamanı internetsiz anlar (`util/TurkishReminderParser.kt`)
- Kalemle yazma: S Pen ya da parmakla el yazısı, ML Kit Digital Ink ile cihaz içinde Türkçe metne çevrilir (`ui/ink`, `ai/HandwritingRecognizer.kt`)
- Gemini Nano: destekleyen telefonlarda (ör. Galaxy S25) cümleleri cihaz içinde anlar, bir cümleden birden fazla hatırlatıcı çıkarır; olmazsa yerel ayrıştırıcıya düşer (`ai/`)
- Sesli hatırlatma: zamanı gelince başlık ve not Türkçe okunur (telefon sessizdeyken okumaz)
- Paylaşım: karta basılı tut ya da düzenleme ekranındaki paylaş düğmesi; WhatsApp, e-posta (takvim dosyası .ics ekli), telefonun takvimine ekleme, diğer uygulamalar
- Widget: boyuta göre küçük/orta/büyük düzen, ✓ ile uygulamayı açmadan tamamlama, kaçırılan uyarı sayısı
- Ayarlar: kalemle kendiliğinden kaydetme süresi, widget'ı ana ekrana ekleme, e-posta alıcısı, paylaşım imzası, tümünü .ics olarak dışa aktarma, tamamlananları temizleme, tema, erteleme süresi, varsayılan saat, tamamlananları gizleme, sesle direkt kaydetme
- Telefon entegrasyonu: Paylaş menüsünden metni hatırlatıcı yapma, simgeye basılı tutunca "Sesle ekle" ve "Yeni" kısayolları, widget'ta mikrofon düğmesi
- Tasarım: beyaz zemin, pastel renkli gölgesiz kartlar, günlere göre gruplu liste, özet kartları, kaydırarak tamamla/sil (geri alınabilir), hızlı zaman seçimleri, her hatırlatıcıya renk, açık ve karanlık tema

## APK'yı indirme

Her `main` push'unda GitHub Actions debug APK derler ve **Releases → Son sürüm** sayfasına `hatirlatici.apk` olarak koyar. Telefondan indirip kur ("bilinmeyen kaynaklardan yükleme" izni gerekir). Tüm sürümler aynı anahtarla imzalandığı için yeni APK eskisinin üzerine kurulur.

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
