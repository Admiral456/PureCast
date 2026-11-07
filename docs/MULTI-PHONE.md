# Používání PureCast z více telefonů

PureCast se instaluje jako jedna samostatná Android aplikace z jediného APK. Není potřeba žádný doplňkový APK, samostatný receiver ani instalace na televizi.

PureCast používá jeden Android APK pro telefony Samsung, Xiaomi, Redmi i další kompatibilní Android zařízení. Sdílená skupina funguje v rámci jedné místní Wi-Fi sítě.

## Aktuální chování

- Jeden telefon založí skupinu a stane se hostitelem.
- Hostitel předá ostatním adresu ve tvaru `http://IP:PORT` a osmiznakový pozvánkový kód.
- Ostatní telefony zadají adresu a kód v sekci **Sdílená fronta**.
- Každý účastník může do společné fronty přidat vybrané fotografie a videa.
- Hostitel jako jediný může změnit pořadí, odebrat položky a spouštět přehrávání na vybrané DLNA televizi. Ovládací karta přehrávání zůstává viditelná i u média od jiného telefonu a umožňuje přechod na předchozí a další položku.
- Všichni připojení účastníci vidí stejný seznam a jeho stav se obnovuje přibližně každé dvě sekundy.

## Jak funguje přenos

Média se nekopírují na hostitelský telefon. Odesílající telefon vystaví přes svůj lokální HTTP server pouze konkrétní vybraný soubor. Sdílená fronta předá hostiteli jeho název, MIME typ a časově omezenou URL. Hostitel pak pošle tuto URL přímo do DLNA televize.

Originální data zůstávají v odesílajícím telefonu. PureCast je nepřevádí ani nekomprimuje. Televize musí sama podporovat konkrétní kontejner, kodek, rozlišení, FPS a HDR formát.

## Podmínky a omezení

- Všechny telefony a TV musí být ve stejné důvěryhodné Wi-Fi síti. Guest Wi-Fi nebo izolace klientů může spojení zablokovat.
- Hostitelská aplikace musí zůstat spuštěná. Když Android ukončí proces hostitele, skupina skončí.
- Telefon, který přidal médium, musí zůstat dostupný, dokud se jeho položka přehrává. Force-stop, ukončení procesu nebo změna sítě může odkaz přerušit.
- Odkazy na média mají platnost šest hodin. Po vypršení je potřeba položku z telefonu znovu přidat.
- Skupina i fronta jsou pouze v paměti. Po ukončení hostitelské aplikace se relace neobnoví.
- Síťové řízení používá HTTP bez TLS. Připojuj pouze telefony, kterým důvěřuješ, a používej jen soukromou domácí síť.
- Sdílené přehrávání zatím ovládá hostitel přes DLNA. Prohlížeč televize nemá dálkové ovládání sdílené fronty.
- Párování QR kódem zatím není implementováno; adresa a kód se předávají ručně.
- Při explicitním odpojení účastníka hostitel zneplatní jeho token a odebere jeho položky z fronty.

