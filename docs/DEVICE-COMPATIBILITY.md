# PureCast — kompatibilita zařízení

PureCast je v této fázi pouze aplikace pro Android. iOS verze se nevyvíjí.

## Odesílající telefony

| Značka telefonu | Cílová podpora | Poznámka |
|---|---|---|
| Samsung Galaxy | Android aplikace | Stejný APK balíček |
| Xiaomi | Android aplikace | Stejný APK balíček |
| Redmi | Android aplikace | Stejný APK balíček |
| Ostatní Android | Obecný režim Android | Aplikace není omezena na tři značky |
| iPhone / iPad | Zatím nepodporováno | Žádný iOS target ani iOS build |

Samsung, Xiaomi a Redmi používají stejnou Android implementaci. PureCast rozpozná výrobce a model pro diagnostiku, ale přenos médií se nespoléhá na proprietární funkce konkrétní značky telefonu.

## Přístup ke galerii Androidu

PureCast vyžádá přístup ke knihovně až po stisknutí tlačítka **Povolit přístup** v sekci Fotky a videa. Aplikace nejprve vysvětlí účel požadavku a následně zobrazí systémové okno Androidu.

- Android 12L a starší: systémové oprávnění ke čtení externího úložiště.
- Android 13: oprávnění pro fotografie a videa.
- Android 14 a novější: plný přístup, přístup pouze k uživatelem vybraným médiím nebo zamítnutí. PureCast používá `READ_MEDIA_VISUAL_USER_SELECTED` a respektuje výběr Androidu.

V sekci Fotky a videa se zobrazuje aktuální rozsah oprávnění. Uživatel může oprávnění změnit nebo otevřít nastavení aplikace a načíst dostupná média z galerie. Při omezeném přístupu Android zpřístupní pouze vybrané položky.

Odmítnutí oprávnění neblokuje běžný Android Photo Picker ani výběr souborů. Tyto dva režimy umožňují odeslat konkrétní položky bez udělení přístupu k celé galerii.

## Organizace galerie

Knihovna umí řadit média podle dostupného data změny, názvu a velikosti souboru. U médií vrácených Android MediaStore může zobrazit také filtry podle alba/složky (bucket display name). U souborů z jiných poskytovatelů Androidu nemusí být datum ani album dostupné; tyto položky zůstávají v knihovně a řazení je nechává na konci, pokud příslušné metadata chybí.

## Televizory

### Samsung Smart TV

Samsung Smart TV používají od modelového roku 2016 platformu Tizen. Modely a jejich mediální dekodéry se ale liší podle modelové skupiny a roku výroby.

PureCast se pokusí použít tyto cesty:

1. **DLNA / UPnP AVTransport** — pokud TV inzeruje ovladatelný MediaRenderer a AVTransport službu.
2. **TV webový prohlížeč** — pokud TV může otevřít lokální HTTP adresu telefonu a její prohlížeč podporuje dané médium.
3. **Neznámá / nepodporovaná cesta** — PureCast zobrazí důvod nebo nejistotu; nesmí předstírat kompatibilitu.

Samsung zveřejňuje modelově specifikované tabulky video kontejnerů, kodeků, rozlišení, snímkové frekvence a bitrate. Podpora MIME typu sama o sobě nepotvrzuje podporu konkrétního kodeku, HDR profilu ani FPS.

### LG webOS

Stejné principy jako u Samsungu: DLNA renderer, pokud ho firmware zpřístupňuje, jinak TV browser fallback. Samotná podpora SmartShare/DLNA jako klienta ještě automaticky neznamená podporu ovladatelného MediaRenderer.

### LG 55UJ7507 — webOS 3.5

Oficiální stránka LG uvádí pro model 55UJ7507 modelový rok 2017, webOS 3.5, internetový prohlížeč a DLNA pro filmy, fotografie a hudbu:
https://www.lg.com/cz/tv-a-soundbars/uhd-tv/55uj7507/

Dokumentace vývojářské platformy LG pro webOS TV 3.5 uvádí v tabulce obrazových formátů podporu JPEG (.jpg/.jpeg), GIF, PNG, BMP a WebP:
https://webostv.developer.lge.com/develop/specifications/video-audio-35

Tato tabulka popisuje platformu webOS TV 3.5; sama o sobě nepotvrzuje, že konkrétní firmware 55UJ7507 přehraje každý soubor i přes DLNA. Proto PureCast tyto formáty používá jako první testovací sadu pro prohlížeč TV. HEIC/HEIF, AVIF, JPEG XL, RAW a další méně běžné typy zůstávají pro tento konkrétní model neověřené, dokud je neotestujeme přímo na televizi.

### Ostatní Smart TV

Aplikace může použít DLNA/UPnP u kompatibilních televizorů dalších značek. Zařízení bez DLNA rendereru lze zkusit přes webový prohlížeč, pokud ho mají a podporují příslušný formát.

## Více telefonů

- PureCast lze nainstalovat na více telefonech se systémem Android.
- Každý telefon může nezávisle vybrat a odeslat svoje fotografie a videa.
- Více telefonů může používat stejnou televizi postupně.
- Běžný DLNA renderer přehrává jednu aktuální položku. Nový příkaz od jiného telefonu může právě přehrávanou položku nahradit.
- Sdílená skupinová fronta umožňuje více telefonům přidat vlastní média; hostitel řídí pořadí a DLNA přehrávání.

## Obrazové formáty a jejich omezení

PureCast rozpoznává obrazové soubory podle MIME typu i běžných přípon a předává jejich původní bajty bez převodu. Kromě JPG/JPEG a PNG zahrnuje GIF, APNG, WebP, HEIC/HEIF, AVIF, JPEG XL, TIFF, BMP, SVG, ICO, JPEG 2000, OpenEXR, DDS, KTX/KTX2, FITS, QOI, DNG a běžné RAW přípony fotoaparátů (ARW, CR2/CR3/CRW, NEF, NRW, ORF, PEF, RAF, RW2, SRW a další).

| Skupina formátů | Chování PureCast | Omezení |
|---|---|---|
| JPG/JPEG, PNG, GIF, BMP, WebP | Rozpoznání, náhled podle Androidu a originální přenos | TV musí formát přijmout |
| HEIC/HEIF | Rozpoznání, pokus o systémový náhled a originální přenos | Dekodér závisí na verzi Androidu a zařízení |
| AVIF | Rozpoznání, systémový náhled na podporovaném Androidu a originální přenos | 10bitové varianty nejsou zaručené na každém zařízení |
| APNG, TIFF, JPEG 2000, SVG, ICO | Rozpoznání MIME a originální přenos | Místní náhled a TV podpora závisí na systémovém dekodéru/prohlížeči |
| JPEG XL a RAW z fotoaparátů | Rozpoznání přípony/MIME a originální přenos | Nativní náhled Androidu nebo dekódování TV nemusí být dostupné |
| OpenEXR, DDS, KTX/KTX2, FITS a QOI | Rozpoznání MIME a originální přenos | Specializované formáty; běžné televizory je nemusí umět zobrazit |

Výběr přes **Procházet soubory a další formáty** používá systémový správce souborů a doplňuje běžný Photo Picker. Díky tomu lze vybrat také soubory, které správce fotek v telefonu nezobrazuje. PDF a jiné nerozpoznané dokumenty jsou při importu přeskočeny a nelze je přidat do přehrávací fronty.

Android API 28+ používá ImageDecoder pro systémově podporované náhledy a čtení rozměrů s omezenou cílovou velikostí; při chybě se zkusí BitmapFactory. Neznámý nebo nepodporovaný obrazový dekodér nebrání samotnému přenosu: PureCast se pokusí poslat originál, ale televize může formát odmítnout. V režimu TV prohlížeče aplikace zobrazuje výsledek načtení obrázku a případné rozlišení dekódovaného obrázku.

## Co aplikace ověřuje

- Značku a model odesílajícího telefonu.
- Značku/model televize z údajů UPnP, pokud je TV poskytne.
- UPnP MediaRenderer a AVTransport.
- DLNA ConnectionManager protocolInfo / deklarované MIME profily.
- Technická metadata souboru: rozlišení, dostupné FPS, video/audio MIME typ, bitrate, délku a některá HDR metadata.
- Webový prohlížeč může vracet vlastní odhad podpory MIME typu pomocí HTMLMediaElement.canPlayType().

## Omezení a pravidla

- CI build neověřuje skutečné televizory.
- Podpora značky není zárukou podpory každého modelu.
- Protokolová kompatibilita není totéž co dekódovací kompatibilita.
- PureCast nepřekóduje, nezmenšuje, nepřekomprimuje ani tajně nesnižuje kvalitu.
- Pokud zařízení soubor nezvládne, aplikace má zobrazit chybu nebo neověřený stav.

## Ověření na fyzických zařízeních

Před označením modelu jako ověřeného je potřeba otestovat:
- vyhledání zařízení a UPnP popis,
- fotografii JPEG a další skutečně použité obrazové formáty,
- běžné MP4 H.264/AAC,
- vysoké rozlišení, vyšší FPS, HEVC a HDR podle podporované specifikace daného modelu,
- pauzu, pokračování, stop a seek,
- streamování při běhu aplikace na pozadí,
- výpadek a opětovné připojení Wi-Fi.
