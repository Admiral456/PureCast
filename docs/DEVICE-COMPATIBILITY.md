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

### Ostatní Smart TV

Aplikace může použít DLNA/UPnP u kompatibilních televizorů dalších značek. Zařízení bez DLNA rendereru lze zkusit přes webový prohlížeč, pokud ho mají a podporují příslušný formát.

## Více telefonů

- PureCast lze nainstalovat na více telefonech se systémem Android.
- Každý telefon může nezávisle vybrat a odeslat svoje fotografie a videa.
- Více telefonů může používat stejnou televizi postupně.
- Běžný DLNA renderer přehrává jednu aktuální položku. Nový příkaz od jiného telefonu může právě přehrávanou položku nahradit.
- Sdílená skupinová fronta s řízením oprávnění není zatím implementována.

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
