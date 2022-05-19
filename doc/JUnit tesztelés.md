# Unit tesztelés

### Bevezetés 

A Pinto könyvtár magját az RDFMapper osztály képezi. Ez szolgál arra, hogy a programunkban megtalálható Java-objektumokat RDF gráfba képezze le. Így a
tesztelés középpontjában is ennek az osztálynak kell állnia. Az RDFMapper épít a Java által nyújtott futásidejű reflection-re, amellyel képes lekérdezni az
objektumok attribútumainak (illetve metódusainak) nevét.

### Meglévő tesztek
A Pinto fejlesztői készítettek JUnit teszteket a könyvtárhoz, ezért a tesztesetek kiegészítésénél azzal kezdtük a munkát, hogy megvizsgáljuk a meglévőeket, és tájékozódunk
a könyvtár elvárt működéséről. A könyvtár dokumentációja igen csekély (mindössze 1 sornyi példakódot tartalmaz), viszont a tesztek elemzésével a szükséges mértékben
meg tudtuk ismerni a könyvtár elemeit - elsősorban az RDFMapper osztály metódusaira koncentrálva.

Azt tapasztaltuk, hogy sok teszteset áll rendelkezésre. Ezek különféle példa-osztályokat alakítanak Java bean formából RDF gráfba, és vissza. A példa-osztályok
jó része rendelkezik attribútumokkal vagy kollekciókkal (többnyire a Set, List és Map interfészeket megvalósító osztályokat várnak), primitív és referencia
típusokból egyaránt. Olyan példaosztályok is léteznek, amelyeket az RDFMapper nem tud gráffá alakítani - ezek a rossz bemenetre adott választ hivatottak tesztelni. Az ilyen
osztályok nem rendelkeznek alapértelmezett konstruktorral, vagy absztraktak.

> Az eredetileg meglévő tesztek által lefedett kód mennyiségét az IntelliJ IDEA fejlesztői környezetben megtalálható lefedettség-vizsgálóval mértük. 

A vizsgálat azt mutatta, hogy az RDFMapper osztály metódusainak sorait több, mint 70%-ban használják az eredeti tesztek. A tesztek által nem érintett kód legnagyobb része az RDF -> Java Bean
irányú átalakításnál van; olyan esetekre tér ki, mint a WildcardTypeImpl osztályhoz tartozó objektumok visszaalakítása, és a nem létrehozható / nem talált osztályok kezelése.
Mivel a dokumentációból nem derül ki, hogy ezeknek a kódrészleteknek mi a rendeltetése (vagyis: mi célból kezel a könyvtár WildcardTypeImpl-ből származó objektumokat),
és a futásukkor mi lenne az elvárt viselkedés, a kódfedettséget itt nem tudtuk érdemben növelni. Maradt tehát az a megközelítés, hogy a meglévő tesztekkel, és a tesztfájl
meglévő funkcionalitásának használatával fogjuk bővíteni a tesztkészletet.

Találtunk olyan teszteseteket, amelyek az ```@Ignored``` annotációval vannak jelölve. Ezek közül néhánynál a függvény nevéből következtettünk a tesztelendő célra, és megvalósítottuk.

> Megjegyzés: a következő szekciókban szereplő IRI rövidítés alatt az URI-k általánosítását értjük.

### A teszteink

A ```testWriteEnumSet()``` teszt az eredeti kódban ```@Ignored``` annotációval van jelölve, az implementáció üres. Megvalósítottuk a tesztet, mivel volt hozzá egy TestEnum típus, és
egy ClassWithEnumSet osztály, ami egy enum kollekciót tartalmaz.
A teszteset első megvalósítása a mostani ```testWriteEnumSetMixed()``` függvényben található. Az eredeti kódban megtalálható ClassWithEnumSet osztályból létrehozunk egy példányt,
amelynek enum-kollekciójához hozzáadunk egy TestEnum.Foo, egy TestEnum.Bar, és egy TestEnum.Baz példányt. A meglévő tesztesetek alapján arra számítottunk, hogy ez az eset
nem igényel majd különösebb figyelmet. Ellenben azt láttuk, hogy a ClassWithEnumSet példány gráfba alakítása nem sikeres. A Stack trace-en az RDFMapper.Write() és az
RDFMapper.setValue() függvények egymás utáni hívását láttuk, ami egy idő után verem túlcsorduláshoz vezetett.

A TestEnum típusnak 3 különböző értéke van. A TestEnum.Foo-t érvénytelen IRI annotáció azonosítja, a TestEnum.Bar-on érvényes az IRI annotáció, a TestEnum.Baz-on pedig
nincsen annotációval megadott IRI. Szétbontottuk a ```testWriteEnumSetMixed()``` esetet 3 alesetre annak reményében, hogy jobb képet kapunk majd az előzőleg leírt hibáról.

A ```testWriteEnumSetValidIRI()``` függvény egy TestEnum.Bar-t ad hozzá a ClassWithEnumSet példány kollekciójához, majd megpróbálja átalakítani a példányt RDF gráffá. Azt tapasztaltuk,
hogy a teszt sikertelen, továbbra is stack overflow hibával szembesülünk.

A ```testWriteEnumSetInvalidIRI()``` függvény annyiban tér el a ```testWriteEnumSetValidIRI()```-től, hogy egy TestEnum.Foo példányt adunk hozzá a ClassWithEnumSet kollekciójához.
Erre a hibalehetőségre vonatkozóan nem látunk eldobott kivételt, a stack trace továbbra is megegyezik a ```testWriteEnumSetMixed()``` tesztnél látottal.

Szintén ```@Ignored``` annotációval volt jelölve egy ```testCharBeanTypeWithLongString()``` függvény, ahol az elvárt eredmény egy RDFMappingException kivétel eldobása volt.
Itt arra gondoltunk, hogy amikor túl hosszú karakterlánc kiírására teszünk kísérletet, akkor az RDFMapper-nek kivételt kell dobnia. A működés ellenőrzéséhez
írtunk egy ```buildLongString()``` függvényt, ami egy 100 millió karakterből álló stringet generál le, és ad vissza. Majd egy rendelkezésre álló példa-objektumban elhelyeztünk
egy ilyen karakterláncot. Az RDFMapper még ilyen hosszú karakterláncot tartalmazó objektum esetében sem dobott kivételt.

A ```testLongUri()``` tesztet az előző teszt mintájára készítettük el. Itt egy 00 millió karakter hosszú IRI-vel rendelkező beant kell RDF gráfba alakítania az RDFMapper-nek.
Az elvárt működés az, hogy nem keletkezik kivétel.

A ```testReadIdentical()``` tesztben azt ellenőrizzük, hogy egy objektumot azonos formában kapunk-e vissza, ha gráffá alakítjuk, majd visszaolvassuk. Az elvárt működés az, hogy
a visszaolvasott objektum minden attribútumában megegyezik az eredetivel.

A ```testWriteMapWithInvalidURI()``` függvényben egy olyan Java objektumot szeretnénk gráffá alakítani, ami egy mappel rendelkezik. A map más tesztekben is használatos BadCompany
típusú objektumokat tartalmaz, amikhez érvénytelen formátumú IRI tartozik. A teszt lényege, hogy az RDFMapper objektumnak kivételt kell dobnia, amikor megpróbálja kiírni
az mappel rendelkező objektumot.

A ```testURIMapping()``` szintén egy olyan teszteset, aminek üres volt az implementációja, és ```@Ignored``` annotációval volt megjelölve. Az implementáció helyén volt egy komment, ami
két tesztesetet írt le. Az elsőben egy ```@RdfsType``` annotációval jelölt objektumot kell gráfból visszaalakítani annak kipróbálására, hogy figyelembe veszi-e az RDFMapper a
visszaalakításnál az annotációt. A másodikban egy összetett objektummal kell ugyanezt elvégezni (ahol a tartalmazott objektum osztályán is van ```@RdfsType``` annotáció), és a
tartalmazott objektum típusát kell ellenőrizni. Ha a tartalmazott objektum megfelelő típusúként lett visszaalakítva, akkor sikeres a teszt. A mi tapasztalatunk az, hogy
az első teszteset sikeres, a második viszont nem.

A ```testMultipleSubjectsNoIdProvided()``` tesztben beolvasunk egy RDF gráfot úgy, hogy az előálló objektumnak nem adunk IRI-t. Ez egy RDFMappingException
kivétel eldobását eredményezi. Nem találtunk hasonló tesztesetet, amiben azt ellenőrizzük, hogy IRI megadása esetén nem kapunk kivételt, ezért írtunk egyet. A teszteset
neve ```testMultipleSubjectsWithIdProvided()```.




