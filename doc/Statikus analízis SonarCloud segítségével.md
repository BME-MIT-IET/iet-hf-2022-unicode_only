# Statikus analízis SonarCloud segítségével

A projekt hibáinak és javítandó gyengeségeinek felderítéséhez a SonarCloud platformot használtuk.

---
## A SonarCloud-ról röviden
A SonarCloud egy felhőalapú kódelemző szolgáltatás, amely 25 különböző programozási nyelvben észleli a kódminőségi problémákat, folyamatosan biztosítva a kód karbantarthatóságát, megbízhatóságát és biztonságát.

A statikus elemzést azért nevezzük statikusnak, mert nem támaszkodik a kód tényleges futtatására (a futó kód elemzését dinamikus elemzésnek nevezzük). Ennek eredményeképpen a SonarCloud az automatizált teszteléstől és az emberi kód-felülvizsgálattól eltérő, további ellenőrzési réteget kínál.

A problémák korai felismerése biztosítja, hogy kevesebb probléma jut át a folyamat későbbi szakaszaiba, és végső soron hozzájárul a termelési kód általános minőségének növeléséhez.

Bővebben: [sonarcloud.io](https://docs.sonarcloud.io/#what-is-sonarcloud)

---

## Konfigurálás
A projekt alapvetően nem volt SonarCloud kompatibilis. Ennek oka a Java 8 illetve a régebbi Gradle volt. A probléma megoldásához a következő lépéseket tettük. 

- A SonarCloud dokumentációjának részletes elolvasása
- A legkönyebben kivitelezhető kompatibilis konfiguráció megtalálása
- A projekt build paramétereinek megváltoztatása hogy Java 11-es jdk-val forduljon
- A megfelelő gradle verzió megtalálása és használata

A fentebb említett lépések végül sikerrel zárultak. A probléma megoldása több időt vett igénybe mint elsőre gondoltuk, de sikerült abszolválni. 
A sikerhez hozzájárultak olyan értelmetlen és bonyolult próbák is mint például, hogy Gradle helyett Maven-t használjunk, stb.

## Ellenőrzés futtatása és eredmények
Mivel a projekt megismerése után az első dolgunk a [Github Actions](https://github.com/BME-MIT-IET/iet-hf-2022-unicode_only/blob/master/doc/Github%20Actions.md) beállítása volt így adódott a gondolat hogy a master ág commitolásával lefut az ellenőrzés.

Ez a próbálkozás kudarccal zárult, ugyanis a Sonar Cloud csak akkor ellenőrizte le a fileokat ha azok egy pull-requestben voltak és történt bennük változás az előző állapot óta. Így minden filet módosítottunk és sikeresen lefuttauk az első tesztet, hogy teljese képet kapjunk a projektről: 
![](https://github.com/BME-MIT-IET/iet-hf-2022-unicode_only/blob/master/doc/img/first_sonar_run.png)

A legfontosabb hibákat és javításaikat a [hibajegyen](https://github.com/BME-MIT-IET/iet-hf-2022-unicode_only/issues/8) lehetet követni

---

## SonarCloud hibák javítása

Az eredeti projektben (master branch) található hibák a következők voltak a projekt elkezdésének idejében:
[link](https://sonarcloud.io/project/issues?resolved=false&severities=MAJOR&pullRequest=7&id=BME-MIT-IET_iet-hf-2022-unicode_only)

Ezeknek a hibáknak a lehető legnagyobb mértékű kijavítását is felvettük a feladatok közé.
Különböző nehézségű és súlyosságú hibák voltak, melyre egy megközelítőleges időintervallumot és a hiba leírását kaptuk meg. 
A code smellek többségét sikerül is az adott időn belül javítani, mellyel nagy mértékben 
javítottuk a kód helyességét.

A legtöbb code smell fajtája:
- Constructor has 8 parameters, which is greater than 7 authorized.

Itt a kód bonyolultsága miatt kellett egyszerűsíteni a konstruktort, melyet itt úgy oldottunk meg, hogy
összevontunk összetartozó adatokat egy új struktúrában.

- Provide the parametrized type for this generic.

Ebben az errorban legtöbbször a <?> rész maradt le (unbounded wildcard), illetve ennek különböző esetei.

- Replace this usage of "Long.class.isInstance()" with "instanceof Long"

Itt konkrét megoldást kapunk magára a hibára, ezeket volt a legkönnyebb kijavítani.

- Refactor this repetition that can lead to a stack overflow for large inputs.

A regex kifejezés javítása volt még a feladat, mivel nagy input esetén stack overflow lehet.
Itt a 'vagy'-ot kellett eltávolítani a kifejezésből.