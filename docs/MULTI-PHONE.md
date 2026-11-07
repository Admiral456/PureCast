# Používání PureCast z více telefonů

PureCast je možné nainstalovat na více Android telefonech v jedné domácí síti. Samsung, Xiaomi a Redmi používají stejný Android APK.

## Současná verze

Každý telefon funguje jako nezávislý odesílatel:
- vybírá svoje vlastní soubory,
- poskytuje originální soubor přes lokální HTTP server telefonu,
- vyhledá kompatibilní DLNA televizory,
- může odeslat soubor do stejné televize jako jiný telefon.

Televizor s jedním DLNA AVTransport obvykle zpracovává jednu aktivní položku. Když jiný telefon odešle nový příkaz, může přehrávání nahradit. To je standardní chování jednoho rendereru, ne chyba značky telefonu.

## Budoucí sdílená fronta

Koordinovaná fronta může být přidána v další etapě:
1. Jeden telefon založí lokální skupinu.
2. Ostatní telefony se připojí pomocí QR kódu nebo krátkého kódu.
3. Každý uživatel přidá vybranou položku do společné fronty.
4. Host nebo oprávněný člen rozhoduje, co se přehraje.
5. Všichni členové vidí stav fronty.

Sdílení se má odehrávat v domácí síti a pouze pro explicitně vybrané soubory. Celá galerie telefonu se nesdílí.
