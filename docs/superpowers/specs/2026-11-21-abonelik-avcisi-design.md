# Abonelik Avcısı — Tasarım

## Amaç ve neden
Kullanıcının (yalnızca kendisi kullanacak, Android/Realme) düzenli ödemelerini tek yerde görmesi: aylık toplam
ne kadar, hangisi ne zaman yenileniyor, kredi kartı ekstresi ne zaman kesiliyor ve son ödeme ne zaman. Veriler
**elle girilir**; uygulama SMS, bildirim ya da e-posta okumaz. Kullanıcının gerekçesi gizlilik (KVKK hassasiyeti):
uygulamaya yalnızca kendi yazdığı bilgiler girer, finansal mesajları okunmaz.

## Hedefler
1. **Abonelik ekleme/düzenleme/silme:** ad, ücret, para birimi (TL varsayılan; USD/EUR seçilirse elle girilen kur),
   ödeme sıklığı **aylık / yıllık**, yenilenme günü (aylık: ayın günü 1–31; yıllık: gün + ay), bağlı kart (isteğe
   bağlı), renk. Abonelik "duraklatıldı" yapılabilir (toplama girmez, hatırlatma gelmez).
2. **Aylık toplam:** ana ekranın üstünde büyük yazıyla. Aylık = aylık ücret; yıllık = ücret / 12; yabancı para × kur.
   Altında yıllık toplam (aylık toplam × 12) ve etkin abonelik sayısı.
3. **Kartlar:** kart adı (kullanıcının verdiği, ör. "Bonus"), hesap kesim günü (1–31), son ödeme günü (1–31), renk.
   **Kart numarası, son 4 hane ya da banka bilgisi istenmez.**
4. **Kart–abonelik bağlantısı:** kart ekranında ve yaklaşan kesim satırında "bu ekstrede X TL abonelik" = karta bağlı
   etkin aboneliklerden yenilenme tarihi bu ekstre dönemine (önceki kesimden bir gün sonra → bu kesim, dahil) düşenlerin
   toplamı (yıllık olanlar yalnızca o döneme düşerse, tam ücretiyle).
5. **Yaklaşanlar listesi:** önümüzdeki 30 gün içindeki abonelik yenilemeleri, kesim ve son ödeme tarihleri, tarihe göre
   sıralı; "bugün / yarın / N gün" etiketi.
6. **Hatırlatmalar (bildirim):** abonelik yenilenmesinden **1 gün önce**; kart **kesim gününde**; **son ödemeden 2 gün
   önce**. Saat varsayılan **10:00**. Ayarlar ekranından: bildirim saati, abonelik için kaç gün önce (0–7), son ödeme
   için kaç gün önce (0–7), her tür için aç/kapa.
7. **İki sekme:** Abonelikler · Kartlar (+ sağ üstte ayarlar simgesi). Ana ekran düzeni onaylanan taslaktaki gibi:
   aylık toplam kartı → Yaklaşanlar → Tüm abonelikler (＋).
   Onaylanan ana ekran taslağı:
   ```
   ┌─────────────────────────────┐
   │ Aylık toplam                │
   │ 1.284,50 TL                 │
   │ yılda 15.414 TL · 9 abonelik│
   ├─────────────────────────────┤
   │ Yaklaşanlar                 │
   │ 🔴 Netflix      yarın 229,99│
   │ 🟢 Spotify     3 gün  99,00 │
   │ 💳 Bonus kesim 5 gün        │
   │    (bu ekstrede 520 TL ab.) │
   ├─────────────────────────────┤
   │ Tüm abonelikler        ＋   │
   │ YouTube Premium  aylık 79,99│
   │ iCloud 200 GB    aylık 39,99│
   │ Amazon Prime    yıllık 469  │
   ├─────────────────────────────┤
   │ [Abonelikler]  [Kartlar]    │
   └─────────────────────────────┘
   ```
   Ekleme/düzenleme ve Kartlar sekmesinin düzeni geliştiriciye kalır (taslağın sade, kart tabanlı diline uygun).
8. **Ana ekran widget'ı (kullanıcı isteği):** Glance ile, varsayılan 2x2 (4x2'ye büyütülebilir). Üstte "Aylık toplam"
   ve tutar; altında yaklaşan ilk 2 öğe (ad + "yarın / N gün"; 4x2'de tutar da). Dokununca uygulama açılır. Veri her
   değiştiğinde ve günlük hatırlatma alarmında güncellenir. Hiç abonelik yoksa "Abonelik ekle" yazar.
9. **Kurulum bandı:** bildirim izni yoksa ya da Realme pil kısıtlaması açıksa üstte "Hatırlatmalar için kurulum eksik"
   bandı (park asistanındaki yaklaşım).

## Kapsam dışı (ilk sürüm)
SMS/bildirim/e-posta okuma; internetten kur çekme; aboneliği uygulamadan iptal etme; deneme süresi takibi; fiyat geçmişi/grafikler; yedekleme/dışa aktarma; Play Store; iPhone.

## Anti-hedefler
- Hassas veri toplamak: kart numarası, banka adı zorunlu alanı, internet erişimi yok.
- Kaçırılan hatırlatma: telefon yeniden başlasa ya da Realme uygulamayı kapatsa bile ertesi hatırlatma gelmeli.
- Yanlış toplam: kuruş yuvarlama hataları; 31'inde yenilenen aboneliğin kısa ayda kaybolması.

## Kısıtlar
- Android minSdk 31, targetSdk 36. Paket `com.ibrah.abonelik`, uygulama adı "Abonelik Avcısı", proje kökü
  `abonelik/ (proje klasörü)`.
- **İNTERNET izni yok.** `allowBackup=false` + veri aktarım kuralları (yedek ve cihaz aktarımı hariç). Bildirimler kilit
  ekranında gizli (tutar görünmez). Release derlemesi hata ayıklamaya kapalı.
- Para tutarları **kuruş cinsinden Long** saklanır; arayüzde Türkçe biçim ("1.284,50 TL"); girişte virgül ve nokta kabul.
- **Tarih kuralları:** ayın günü o ayda yoksa (ör. 31 Şubat) ayın son günü kullanılır. Son ödeme günü kesim gününden
  küçük ya da eşitse son ödeme bir sonraki aydadır. Yıllık 29 Şubat → artık olmayan yıllarda 28 Şubat.
- **Test düzeni:** önce Android Studio sanal telefonu (Pixel_5_2), sonra kullanıcının onayıyla gerçek telefon.
- Park asistanı altyapısı yeniden kullanılır: derleme ayarları (AGP 8.13.1, Kotlin 2.2.20, Compose BOM, Room, Robolectric,
  test JVM'i İngilizce yerel ayar), tema, bildirim kanalı, kurulum ekranı ve Realme pil yönlendirmesi, `.gitignore`.
- İmza: depo dışında yeni anahtar `~/.android-keys/abonelik.jks` + `abonelik.properties`. GitHub'a yükleme yalnızca
  kullanıcı isterse.

## Yaklaşımlar
1. **Yerel Kotlin + Jetpack Compose, park asistanı altyapısıyla — SEÇİLDİ.** Hazır bildirim/kurulum/tema/test düzeni;
   tamamen çevrimdışı.
2. Flutter vb. çoklu platform: iPhone gerekmiyor, hazır altyapı kullanılamaz. Elendi.
3. Hazır bütçe uygulaması uyarlamak: çoğu internete çıkıyor/reklamlı; gizlilik hedefine ters. Elendi.

## Kullanıcının kararları
Elle giriş; aylık/yıllık seçimi; aylık toplam; kredi kartı hatırlatıcısı (kesim + son ödeme); aboneliklerin karta
bağlanması; yıllık = 1/12; kart numarası yok; TL + elle kur; 1 gün / kesim günü / 2 gün önce, 10:00; önce sanal telefon.

## Geliştiricinin kararları (bu belgede verilenler)
- **Veri modeli (Room):** `Subscription(id, name, amountKurus: Long, currency: TRY|USD|EUR, fxRateMilli: Long?,
  period: MONTHLY|YEARLY, day: Int, month: Int?, cardId: String?, color: Int, paused: Boolean)`;
  `Card(id, name, statementDay: Int, dueDay: Int, color: Int)`; ayarlar DataStore/SharedPreferences'ta.
  Kart silinince bağlı aboneliklerin kartı boşalır (abonelik silinmez).
- **Hesaplama saf Kotlin'de** (`core/`): sonraki tarih, ekstre dönemi, aylık toplam, yaklaşanlar — `java.time` ile,
  birim testli; saat dilimi cihazınki.
- **Hatırlatma motoru:** her gün ayarlanan saatte tek bir alarm (AlarmManager `setAndAllowWhileIdle`, birkaç dakika
  sapma kabul) o günün hatırlatmalarını hesaplayıp bildirir ve ertesi günü kurar; `BOOT_COMPLETED` ve uygulama
  açılışında yeniden kurulur. Tam zamanlı alarm izni gerekmez.
- **Kaçırılan hatırlatmalar (kullanıcı kararı):** alarm çalıştığında, telefon açıldığında (`BOOT_COMPLETED`) ve
  uygulama açıldığında bir "telafi" geçişi yapılır: hatırlatma günü **son 7 gün içinde** olup henüz gönderilmemiş ve
  **hatırlattığı tarih (yenilenme / kesim / son ödeme) henüz geçmemiş** hatırlatmalar geç de olsa gönderilir; tarihi
  geçmişler gönderilmez. Bugünün hatırlatması saatinden önce gönderilmez. Her hatırlatma için gönderim kaydı
  (tür + öğe kimliği + hatırlattığı tarih) saklanır; aynı hatırlatma iki kez gönderilmez. 30 günden eski kayıtlar
  silinir.
- **Yuvarlama:** toplamlar yuvarlamasız hesaplanır (yıllık/12 ve kur çevrimi dahil), yalnızca gösterirken kuruşa
  yuvarlanır; böylece "aylık × 12" gerçek yıllık tutarla tutarlı kalır.
- **Yabancı para:** kur her abonelikte ayrı saklanır, kullanıcının girdiği değerdir; toplamda TL'ye çevrilir, satırda orijinal tutar + "≈ TL" gösterilir.

## Geliştiriciye kalanlar
Renk paleti, simgeler, ekranların piksel ayrıntıları, boş durum metinleri, ayarlar ekranının düzeni, bildirim metinleri.
