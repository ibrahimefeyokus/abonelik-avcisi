# Abonelik Avcısı Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Elle girilen abonelikleri ve kredi kartı tarihlerini tutan, aylık toplamı gösteren, hatırlatma ve widget'ı olan çevrimdışı Android uygulaması.

**Architecture:** Saf Kotlin çekirdek (`core/`: para, tarih, toplam, yaklaşanlar, hatırlatma planlayıcı) birim testli; Room + DataStore veri katmanı; günlük tek alarm + telafi geçişiyle hatırlatma; Compose arayüz; Glance widget. Park asistanının derleme/test/kurulum düzeni kopyalanır.

**Tech Stack:** Kotlin 2.2.20, AGP 8.13.1, Compose BOM 2025.10.01, Room 2.7.2 (KSP), DataStore 1.1.7, Glance 1.1.1, Robolectric 4.16, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-11-21-abonelik-avcisi-design.md`

## Global Constraints
- minSdk 31, targetSdk 36; paket `com.ibrah.abonelik`; ad "Abonelik Avcısı"; kök `abonelik/ (proje klasörü)`.
- **İNTERNET izni yok**; `allowBackup=false` + `data_extraction_rules` (cloud-backup ve device-transfer hariç); bildirimler `VISIBILITY_PRIVATE` + tutarsız public sürüm; release `debuggable=false`.
- Para kuruş `Long`; toplamlar `BigDecimal` ile yuvarlamasız, yalnızca gösterimde HALF_UP kuruşa; biçim "1.284,50 TL".
- Tarih: ayda olmayan gün → ayın son günü; dueDay ≤ statementDay → son ödeme sonraki ay; 29 Şubat → artık olmayan yılda 28 Şubat.
- Varsayılan hatırlatmalar: abonelik 1 gün önce, kesim günü, son ödeme 2 gün önce, 10:00. Telafi penceresi 7 gün; olay tarihi geçmişse gönderilmez; anahtar `kind:itemId:eventDate`; 30 günden eski kayıt silinir.
- Test JVM'i `-Duser.language=en -Duser.country=US`. Önce sanal telefon `Pixel_5_2`, gerçek telefona kurulum yalnızca kullanıcı onayıyla.
- İmza: `~/.android-keys/abonelik.properties` (`ABONELIK_SIGNING_PROPS`), anahtar depoya girmez.

## Review Focus
1. Ay sonu/artık yıl: 31'inde yenilenen aylık abonelik Şubat'ta 28/29'da görünmeli, Mart'ta yeniden 31'de. (Task 3)
2. Telefon günlerce kapalı → açılınca yalnızca olayı geçmemiş kaçırılanlar, birer kez. (Task 5)
3. Bildirim saati değiştirilince bugünün hatırlatması ikinci kez gelmemeli; saat geriye alınınca bugünkü hemen gelmeli. (Task 5)
4. "229,99", "1.284,50", "229.99", " 79 " girişleri doğru; "abc", "-5", "" reddedilir; çok büyük sayı taşmaz. (Task 2)
5. Kart silinince bağlı abonelikler kalır, kartsız olur; ekstre toplamı yeniden hesaplanır. (Task 6)

---

### Task 1: Proje iskeleti
**Files:** `settings.gradle.kts`, `build.gradle.kts`, `gradle/` (wrapper + `libs.versions.toml` park asistanından, play-location/coroutines-play çıkarılır), `gradle.properties` (`-Xmx4g -XX:MaxMetaspaceSize=1g`), `app/build.gradle.kts` (park asistanı kalıbı; imza `abonelik.properties`), `AndroidManifest.xml` (POST_NOTIFICATIONS, RECEIVE_BOOT_COMPLETED, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS), `res/xml/data_extraction_rules.xml`, uyarlanabilir simge (vektör), `ui/theme/Theme.kt` (park temasından), `MainActivity.kt` (boş ekran).
- [ ] Step 1: Dosyaları oluştur, dal `feat/v1`.
- [ ] Step 2: `gradlew assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL; sanal telefonda (`emulator -avd Pixel_5_2`) kur ve aç.
- [ ] Step 3: Commit `chore: proje iskeleti`.

### Task 2: Para
**Files:** Create `core/Money.kt`; Test `core/MoneyTest.kt`
**Produces:** `object Money { fun parse(text: String): Long?; fun format(kurus: BigDecimal): String; fun format(kurus: Long): String; fun parseRate(text: String): Long? }`, `enum class Currency(val symbol: String) { TRY("TL"), USD("$"), EUR("€") }`.
- [ ] Step 1: Testler: `parse("229,99")=22999`, `parse("1.284,50")=128450`, `parse("229.99")=22999`, `parse(" 79 ")=7900`, `parse("1.000")=100000`, `parse("abc")/("-5")/("")/("99999999999")=null` (üst sınır 1.000.000 TL); `format(128450L)="1.284,50 TL"`, `format(BigDecimal("1234.5"))="12,35 TL"`; `parseRate("34,25")=34250` (kur ×1000).
- [ ] Step 2: RED. Step 3: Uygula (tek nokta + 3 hane → binlik; virgül → ondalık). Step 4: GREEN. Step 5: Commit `feat: para ayrıştırma ve biçim`.

### Task 3: Takvim ve toplamlar
**Files:** Create `core/Model.kt`, `core/Schedule.kt`, `core/Totals.kt`; Tests `ScheduleTest.kt`, `TotalsTest.kt`
**Produces:**
- `enum Period { MONTHLY, YEARLY }`; `data class Sub(id, name, amountKurus: Long, currency: Currency, fxRateMilli: Long?, period: Period, day: Int, month: Int?, cardId: String?, color: Int, paused: Boolean)`; `data class Card(id, name, statementDay: Int, dueDay: Int, color: Int)`.
- `Schedule.nextRenewal(s: Sub, from: LocalDate): LocalDate` (≥ from), `Schedule.nextStatement(c: Card, from): LocalDate`, `Schedule.dueAfter(c: Card, statement: LocalDate): LocalDate`, `Schedule.period(c: Card, statement: LocalDate): ClosedRange<LocalDate>` (önceki kesim+1 .. kesim).
- `Totals.monthlyKurus(subs): BigDecimal`, `Totals.tryKurus(s: Sub): BigDecimal`, `Totals.onStatement(card, subs, statement): BigDecimal`.
- [ ] Step 1: Testler: gün 31 → 2026-02-28, 2028-02-29, 2026-03-31; yıllık 29 Şubat → 2027-02-28; `from` yenileme günüyse aynı gün; kesim 25 / son ödeme 5 → sonraki ayın 5'i; kesim 5 / son ödeme 15 → aynı ay; dönem (kesim 25 Kasım) = 26 Ekim..25 Kasım; aylık toplam 229,99 + yıllık 1.200 + 10 USD × 34,25 = 672,49 TL; duraklatılan girmez; ekstrede yalnızca dönemdeki yenilemeler, yıllık tam ücretle.
- [ ] Step 2: RED. Step 3: Uygula (`java.time`, ay sonuna kırpma). Step 4: GREEN. Step 5: Commit `feat: takvim ve toplam hesapları`.

### Task 4: Yaklaşanlar
**Files:** Create `core/Upcoming.kt`; Test `UpcomingTest.kt`
**Produces:** `sealed interface UpcomingItem { val date: LocalDate }` — `Renewal(sub, date)`, `Statement(card, date, subsKurus: BigDecimal)`, `Due(card, date)`; `Upcoming.list(subs, cards, today, days = 30)` (tarih sırası; eşitlikte Due, Statement, Renewal); `relativeLabel(date, today)` → "bugün" / "yarın" / "3 gün".
- [ ] Steps: test (sıralama, 30 gün sınırı, duraklatılan yok, etiketler) RED → uygula → GREEN → commit `feat: yaklaşanlar`.

### Task 5: Hatırlatma planlayıcı (saf)
**Files:** Create `core/Reminders.kt`; Test `RemindersTest.kt`
**Produces:** `enum Kind { SUB, STATEMENT, DUE }`; `data class ReminderSettings(time: LocalTime = 10:00, subDaysBefore = 1, dueDaysBefore = 2, subOn = true, statementOn = true, dueOn = true)`; `data class Reminder(kind, itemId, eventDate, remindDate) { val key = "$kind:$itemId:$eventDate" }`; `ReminderPlanner.due(subs, cards, settings, now: LocalDateTime, sent: Set<String>): List<Reminder>`; `AlarmTime.next(now, time): LocalDateTime`.
- [ ] Step 1: Testler: bugün 09:59 → bugünkü yok, 10:00 → var; 3 gün kapalı → olayı bugün olan kesim hatırlatması gelir, olayı dün olan gelmez; 8 gün önceki gelmez; `sent` içindeki gelmez; kapatılan tür gelmez; duraklatılan gelmez; `subDaysBefore=0` → yenileme günü; `AlarmTime.next` iki durum.
- [ ] Steps: RED → uygula → GREEN → commit `feat: hatırlatma planlayıcı`.

### Task 6: Veri katmanı
**Files:** Create `data/Db.kt` (Room v1, `exportSchema=true`, şema `app/schemas`), `data/Repository.kt`, `data/SettingsStore.kt` (DataStore); Test `RepositoryTest.kt` (Robolectric, bellek içi Room)
**Produces:** `Repository.get(ctx)`: `subs: Flow<List<Sub>>`, `cards: Flow<List<Card>>`, `saveSub/deleteSub/saveCard/deleteCard(id)` (kart silme tek işlemde bağlı aboneliklerin `cardId`'sini null yapar), `sentKeys()`, `markSent(keys, on)`, `pruneSent(before)`; `SettingsStore.flow: Flow<ReminderSettings>`, `update(f)`.
- [ ] Steps: test (kaydet/oku, kart silme → abonelik kalır cardId null, sent kayıtları, prune) RED → uygula → GREEN → commit `feat: veri katmanı`.

### Task 7: Hatırlatma çalıştırıcı (Android)
**Files:** Create `reminders/ReminderText.kt` (saf), `reminders/DailyAlarm.kt`, `reminders/ReminderReceiver.kt`, `reminders/BootReceiver.kt`, `reminders/CatchUp.kt`, `notify/Notifications.kt`; Test `ReminderTextTest.kt`
- `ReminderText.of(r, sub, card, statementKurus, today): Pair<String, String>` → ("Netflix yarın yenileniyor", "229,99 TL"), ("Bonus ekstresi bugün kesiliyor", "Bu ekstrede 520,00 TL abonelik"), ("Bonus son ödeme 2 gün sonra", "Son ödeme: 7 Aralık"). Public sürüm: "Abonelik Avcısı · hatırlatma".
- `CatchUp.run(ctx)`: depo + ayarlar → planlayıcı → bildirim → `markSent` → `pruneSent(today−30)` → widget güncelle → `DailyAlarm.schedule(AlarmTime.next(...))` (`setAndAllowWhileIdle`, RTC_WAKEUP). Alarm alıcısı (`goAsync`), BootReceiver (`BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`), `MainActivity.onResume` ve ayar saati değişimi çağırır.
- [ ] Steps: ReminderTextTest RED → GREEN; Android parçaları; derle; sanal telefonda bildirimi doğrula; commit `feat: hatırlatmalar`.

### Task 8: Arayüz
**Files:** Create `ui/AppNav.kt`, `ui/HomeScreen.kt`, `ui/SubEditor.kt` (tam ekran form), `ui/CardsScreen.kt`, `ui/CardEditor.kt`, `ui/SettingsScreen.kt`, `ui/SetupBanner.kt` (park asistanı `BatterySettings`), `ui/Forms.kt` (saf doğrulama); Test `FormsTest.kt`
**Produces:** `SubForm(...).validate(): Map<String, String>` ("Ad gerekli", "Geçerli bir tutar gir", "Kur gerekli", "Gün 1–31 olmalı", "Ay seç"), `toSub(id)`; `CardForm.validate()` ("Ad gerekli", "Gün 1–31 olmalı").
- [ ] Steps: FormsTest RED → GREEN; ekranlar; sanal telefonda ekleme/düzenleme/silme, toplam, kart bağlama, ayarlar; commit `feat: arayüz`.

### Task 9: Widget
**Files:** Create `widget/SubsWidget.kt` (Glance, `SizeMode.Responsive` 2x2 / 4x2), `widget/WidgetUpdater.kt`, `res/xml/subs_widget_info.xml`; manifest alıcısı; Repository yazmaları ve CatchUp sonrası `WidgetUpdater.update`.
- İçerik: "Aylık toplam" + tutar; `Upcoming.list` ilk 2 (ad + etiket; 4x2'de tutar); boşsa "Abonelik ekle"; dokununca uygulama.
- [ ] Steps: derle; sanal telefonda widget ekle, güncellendiğini gör; commit `feat: widget`.

### Task 10: Doğrulama ve teslim
- [ ] Bağımsız son gözden geçirme; bulgular tek düzeltme geçişinde (her biri RED→GREEN).
- [ ] İmza anahtarı (depo dışı), `assembleRelease`, sanal telefonda release denemesi.
- [ ] Kullanıcı onayıyla gerçek telefona kurulum.
