# Audit velikosti APK — PureCast 0.10.0

Měření pochází z finálního běhu GitHub Actions:
https://github.com/Admiral456/PureCast/actions/runs/42418890210

## Naměřené instalační balíčky

| Varianta | Velikost APK | DEX soubory | Rozbalený DEX | Komprimovaný DEX |
|---|---:|---:|---:|---:|
| Debug | 11 648 621 B (11,65 MB) | 11 | 29 031 012 B | 11 034 667 B |
| Unminified release | 8 338 902 B (8,34 MB) | 2 | 22 418 220 B | 7 714 609 B |
| R8 release | 1 308 435 B (1,31 MB) | 1 | 2 128 476 B | 1 087 723 B |

R8 release je o 84,3 % menší než unminified release. Obě release varianty vycházejí ze stejného zdrojového kódu; unminified varianta vypíná R8 a zmenšování prostředků. Není to jiná, funkčně omezená edice.

## Co je uvnitř optimalizovaného APK

- DEX kód: 2 128 476 B po rozbalení, 1 087 723 B v APK.
- Android resources.arsc: 95 284 B.
- AndroidManifest.xml: 7 100 B po rozbalení.
- Nativní knihovny: 4 malé kopie `libandroidx.graphics.path.so`, dohromady 37 392 B.
- Žádné zabalené ukázkové fotografie ani videa.

## Proč PureCast nepotřebuje desítky megabajtů vlastních kodeků

PureCast neobsahuje video editor, FFmpeg, VLC ani vlastní dekódovací/enkódovací sadu. Vybraný soubor nepřevádí, nepřekóduje ani nekomprimuje. Telefon jej zpřístupní přes lokální HTTP server a televize si načte původní bajty a dekóduje je sama. Galerie používá Android Photo Picker, náhledy a metadata z Android API. Z tohoto důvodu v APK nejsou kopie uživatelských médií ani velké balíky videokodeků.

To neznamená podporu každého kodeku na každé televizi. Kompatibilita rozlišení, FPS, zvuku a HDR závisí na konkrétním televizoru a jeho dekodéru.

## Co se ověřuje v CI

Před nahráním APK automatický skript kontroluje, že balíček obsahuje hlavní soubory Android aplikace a že DEX stále obsahuje kritické značky pro:

- vytvoření/připojení skupiny a endpointy přidávání, odebírání a řazení položek,
- lokální mediální cesty `/media/` a `/tv/`,
- DLNA příkazy `SetAVTransportURI`, `GetPositionInfo` a `GetTransportInfo`,
- českou obrazovku galerie, sdílené fronty a základní ovládání přehrávání.

CI vytváří R8 mapu a usage report pro interní kontrolu, ale nepublikuje je jako další uživatelské stažení. Mapa ukazuje, jak R8 přejmenoval a zmenšil třídy; usage report ukazuje odstraněné nebo optimalizované členy. Důležité funkce se ověřují také samostatným smoke testem na výsledném APK.

Tento smoke test potvrzuje přítomnost kritických částí v balíčku, nikoli skutečné přehrání na fyzickém telefonu nebo televizi. To je potřeba ověřit samostatným hardwarovým testem.

## Doporučení

Pro instalaci použij jediný artifact `PureCast-Android`, který obsahuje `PureCast.apk`. Interní debug a R8 varianty nejsou samostatné uživatelské downloady.


## Jediný distribuovaný soubor

Od verze 0.9.0 workflow nahrává pouze jeden instalační balíček s názvem `PureCast-Android`. Uvnitř je samostatný `PureCast.apk`, zvolený z plnohodnotné unminified release varianty. Debug a R8 APK se sestavují a ověřují pouze interně, ale nejsou dalšími uživatelskými downloady. K instalaci není potřeba žádná doprovodná aplikace ani data balíček.


Verze 0.10.0 přidává katalog MIME typů pro moderní obrazové formáty a systémový výběr souborů. Neobsahuje dodatečné dekódovací knihovny ani převodník; náhledy využívají systémový ImageDecoder, zatímco přenos stále posílá originální soubor.
