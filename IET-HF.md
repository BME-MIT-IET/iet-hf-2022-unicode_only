A pinto egy Java nyelven írt alkalmazás amely a bemenetére kapott java beaneket RDF-re fordítja és ez fordítva is működik.

Egy példa a használatára.

Adott az alábbi Java Bean:
```java
public static final class Person {
private String mName;

    public Person() {
    }

    public Person(final String theName) {
        mName = theName;
    }

    public String getName() {
        return mName;
    }

    public void setName(final String theName) {
        mName = theName;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mName);
    }

    @Override
    public boolean equals(final Object theObj) {
        if (theObj == this) {
            return true;
        }
        else if (theObj instanceof Person) {
            return Objects.equal(mName, ((Person) theObj).mName);
        }
        else {
            return false;
        }
    }
}
```
Ezt az alábbi módon konvertáljuk RDF-re:

```java
Graph aGraph = RDFMapper.create().writeValue(new Person("Michael Grove"));
```
Az aGraph objektum hármasa az alábbi:

```java
<tag:complexible:pinto:f97658c7048377a026111c7806bd7280> <tag:complexible:pinto:name> "Michael Grove"^^<http://www.w3.org/2001/XMLSchema#string> .
```
Ha megvan ez az RDF akkor vissza lehet konvertálni a Person objektummá:
```java
final Person aPerson RDFMapper.create().readValue(aGraph, Person.class)
```




Unit tesztelés

A Pinto könyvtár magját az RDFMapper osztály képezi. Ez szolgál arra, hogy a programunkban megtalálható Java-objektumokat RDF gráfba képezze le. Így a 
tesztelés középpontjában is ennek az osztálynak kell állnia. Az RDFMapper épít a Java által nyújtott futásidejű reflection-re, amellyel képes lekérdezni az
objektumok attribútumainak (illetve metódusainak) nevét.

A Pinto fejlesztői készítettek JUnit teszteket a könyvtárhoz, ezért a tesztesetek kiegészítésénél azzal kezdtük a munkát, hogy megvizsgáljuk a meglévőeket, és tájékozódunk
a könyvtár elvárt működéséről. A könyvtár dokumentációja igen csekély (mindössze 1 sornyi példakódot tartalmaz), viszont a tesztek elemzésével a szükséges mértékben
meg tudtuk ismerni a könyvtár elemeit - elsősorban az RDFMapper osztály metódusaira koncentrálva.

Azt tapasztaltuk, hogy sok teszteset áll rendelkezésre. Ezek különféle példa-osztályokat alakítanak Java bean formából RDF gráfba, és vissza. A példa-osztályok
jó része rendelkezik attribútumokkal vagy kollekciókkal (többnyire a Set, List és Map interfészeket megvalósító osztályokat várnak), primitív és referencia 
típusokból egyaránt. Olyan példaosztályok is léteznek, amelyeket az RDFMapper nem tud gráffá alakítani - ezek a rossz bemenetre adott választ hivatottak tesztelni. Az ilyen
osztályok nem rendelkeznek alapértelmezett konstruktorral, vagy absztraktak.

Az eredetileg meglévő tesztek által lefedett kód mennyiségét az IntelliJ IDEA fejlesztői környezetben megtalálható lefedettség-vizsgálóval mértük. A vizsgálat azt mutatta,
hogy az RDFMapper osztály metódusainak sorait több, mint 70%-ban használják az eredeti tesztek. A tesztek által nem érintett kód legnagyobb része az RDF -> Java Bean
irányú átalakításnál van; olyan esetekre tér ki, mint a WildcardTypeImpl osztályhoz tartozó objektumok visszaalakítása, és a nem létrehozható / nem talált osztályok kezelése. 
Mivel a dokumentációból nem derül ki, hogy ezeknek a kódrészleteknek mi a rendeltetése (vagyis: mi célból kezel a könyvtár WildcardTypeImpl-ből származó objektumokat), 
és a futásukkor mi lenne az elvárt viselkedés, a kódfedettséget itt nem tudtuk érdemben növelni. Maradt tehát az a megközelítés, hogy a meglévő tesztekkel, és a tesztfájl
meglévő funkcionalitásának használatával fogjuk bővíteni a tesztkészletet.

//TODO
Találtunk olyan teszteseteket, amelyek az @Ignored annotációval vannak jelölve. 


A testWriteEnumSet() teszt az eredeti kódban @Ignored annotációval van jelölve, az implementáció üres. Megvalósítottuk a tesztet, mivel volt hozzá egy TestEnum típus, és
egy ClassWithEnumSet osztály, ami egy enum kollekciót tartalmaz.
A teszteset első megvalósítása a mostani testWriteEnumSetMixed() függvényben található. Az eredeti kódban megtalálható ClassWithEnumSet osztályból létrehozunk egy példányt,
amelynek enum-kollekciójához hozzáadunk egy TestEnum.Foo, egy TestEnum.Bar, és egy TestEnum.Baz példányt. A meglévő tesztesetek alapján arra számítottunk, hogy ez az eset 
nem igényel majd különösebb figyelmet. Ellenben azt láttuk, hogy a ClassWithEnumSet példány gráfba alakítása nem sikeres. A Stack trace-en az RDFMapper.Write() és az
RDFMapper.setValue() függvények egymás utáni hívását láttuk, ami egy idő után verem túlcsorduláshoz vezetett.

A TestEnum típusnak 3 különböző értéke van. A TestEnum.Foo-t érvénytelen IRI annotáció azonosítja, a TestEnum.Bar-on érvényes az IRI annotáció, a TestEnum.Baz-on pedig 
nincsen annotációval megadott IRI. Szétbontottuk a testWriteEnumSetMixed() esetet 3 alesetre annak reményében, hogy jobb képet kapunk majd az előzőleg leírt hibáról.

A testWriteEnumSetValidIRI() függvény egy TestEnum.Bar-t ad hozzá a ClassWithEnumSet példány kollekciójához, majd megpróbálja átalakítani a példányt RDF gráffá. Azt tapasztaltuk,
hogy a teszt sikertelen, továbbra is stack overflow hibával szembesülünk.

A testWriteEnumSetInvalidIRI() függvény annyiban tér el a testWriteEnumSetValidIRI()-től, hogy egy TestEnum.Foo példányt adunk hozzá a ClassWithEnumSet kollekciójához. 
Erre a hibalehetőségre vonatkozóan nem látunk eldobott kivételt, a stack trace továbbra is megegyezik a testWriteEnumSetMixed() tesztnél látottal.

//TODO

