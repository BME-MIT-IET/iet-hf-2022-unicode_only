# Teljesítmény teszt

Szerettük volna tesztelni, hogy a könyvtár implementációja mennyire gyors, mennyire hatékony.

Ehhez 6 teszt-szcenáriót hoztunk létre, melyekben különböző nagy mennyiségekben vizsgáljuk azt, hogy az RDF -> Beans és a Beans -> RDF leképezés sebessége hogyan változik a mennyiség függvényében.

## Beans -> RDF tesztek

### 1 RDF létrehozása 1 Person objektumból

![](img/performance/perf1.png)

A képen a kapott eredmények láthatóak. A pontosság kedvéért háromszor is elvégezzük a konverziót, majd ezeknek az átlagértékét is megvizsgáljuk.
Erre azért van szükség, mert valószínűleg az egyes konverziók ideje függhet egyéb lokális környezeti változóktól is.

A képen például az első eredmény nagyságrendekkel nagyobb, mint a többi, ebből következtetünk az előző kijelentésre, és ez sajnos az átlagot is torzítja.

Az viszont jól látható, hogy 1 konverzió ideje kb. 6500 nanoszekundumig tart. A kérdés már csak az, hogy lehet-e ezen javítani a kódbázis optimalizálásával?

Ehhez előbb tekintsük meg a többi eredményt is!

### 1 000 RDF létrehozása 1 Person objektumból

![](img/performance/perf2.png)

Itt a következőt vehetjük észre: nincs nagyságrendi eltérés a három mért érték között, ez is azt sugallja, hogy az előző kijelentésünk 
mely szerint lokális paraméterek kilendítettük a konverziót, igaznak bizonyul.

Most vizsgáljuk meg, hogy ha elosztjuk a konverziók számával az átlagos időt, akkor mit kapunk? - 5110,374 ns-ot. 
Ez felveti a kérdést, hogy vajon nagy mennyiségben gyorsabb egy konverzió? Lehet, de erről biztosabbat csak a harmadik mérés értékeinek 
ismeretében tudunk mondani majd. (A valóságban lehet, hogy egyéb lokális beállítások is segítik a gyorsítást, ha van ilyen egyáltalán)

### 1 000 000 RDF létrehozása 1 Person objektumból

![](img/performance/perf3.png)

A nagyságrendek itt is stimmelnek! Nézzük meg az egy konverzióra jutó időt: ~769ns. Ez igen érdekes. 
Ezek szerint, mintha valami optimalizálás lenne a dolog mögött. Vagy talán rossz a teszt? Egyáltalán erre számítottunk?

A valóságban valószínűleg az történik, hogy a fordító rátanul arra, hogy mit kell csinálnia, ezért nagyon sokat tud optimalizálni, így 
nagy mennyiségű művelet esetén ez az optimalizálás hatékony, ezáltal egységre bontva kevesebb ideig tart, mint egy esetén.

Viszont a mi célunk az, hogy megnézzük, lehet-e még ennél gyorsabban csinálni a konverziót!

## A tesztek javítása után

A tesztek eredeti kódja a következő volt: 

```java
@Test
public void performanceTestThree(){
    ArrayList<Long> times = new ArrayList<>();

    Person p = new Person("Peter");

    for(int j = 0; j < 3; ++j){
        long start = System.nanoTime();

        for(int i = 0; i < 1000000; ++i) {
            Model m = RDFMapper.create().writeValue(p);
        }
        long finish = System.nanoTime();
        times.add(finish - start);
    }

    System.out.print("Elapsed time creating 1 000 000 RDFs from Beans: ");
    long sum = 0;
    for (Long time : times) {
        sum += time;
        System.out.print(time + "ns ");
    }

    System.out.print("average elapsed time: " + (sum / times.size()) + "ns\n");
}
```

Ebben a középső ciklusban, ahol az iterációkat csináljuk egy hívás található, feltétel nélkül, ezért a fordító ezt könnyen optimalizálja.

Ha ide valamiféle elágazást is teszünk, akkor valamivel jobb képet kaphatunk a műveletről, viszont így olyan lépéseket is végrehajtunk, amik feleslegesek a teszt szempontjából.

Az elágazás betétele után az egységre jutó nagyságrendek visszaálltak az eredi 5-6000 ns-os méretre.

Vizsgáljuk most azt meg, hogy lehet-e még ennél is gyorsabban!

### Gyorsítás

Az előbbi kódrészletben használt RDFMappe.create().writeValue() hívási láncban a `create` hívásáig leginkább csak objektumokat hozunk létre, 
a tényleges munkát a `writeValue` végzi, így ebben, vagyis ebben hívott `write` függvényben keressük, hogy tudunk-e valamit rövidíteni, netán gyorsabba tenni.

A `write` jelenlegi kódja a következő:

```java
private <T> ResourceBuilder write(final T theValue) {
    // before we do anything, do we have a custom codec for this?
    RDFCodec aCodec = mCodecs.get(theValue.getClass());
    if (aCodec != null) {
        final Value aResult = aCodec.writeValue(theValue);

        if (aResult instanceof ResourceBuilder) {
            return (ResourceBuilder) aResult;
        }
        else {
            return new ResourceBuilder(id(theValue)).addType(getType(theValue)).addProperty(VALUE, aResult);
        }
    }

    final Resource aId = id(theValue);

    final IRI aType = getType(theValue);

    try {
        final ModelBuilder aGraph = new ModelBuilder(mValueFactory);

        ResourceBuilder aBuilder = aGraph.instance(aType, aId);

        for (Map.Entry<String, Object> aEntry : PropertyUtils.describe(theValue).entrySet()) {
            final PropertyDescriptor aDescriptor = PropertyUtils.getPropertyDescriptor(theValue, aEntry.getKey());

            if (isIgnored(aDescriptor)) {
                continue;
            }

            final IRI aProperty = getProperty(aDescriptor);

            if (aProperty == null) {
                continue;
            }

            final Object aObj = aEntry.getValue();

            if (aObj != null) {
                setValue(aGraph, aBuilder, aDescriptor, aProperty, aObj);
            }
        }

        return aBuilder;
    }
    catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        Throwables.propagateIfInstanceOf(e, RDFMappingException.class);
        throw new RDFMappingException(e);
    }
}   
```

Ebben úgy néz ki elég sok elágazás van, jó eséllyel találunk valamit, amifelesleges, netán túlzó.

Nézzük is meg a kódot! Az első dolog, amit észreveszünk, hogy van benne egy `instanceof` hívás. Ez gyanúgy, 
ugynis ha megnézzük, hogy az `aResult` változó hogyan jön létre, akkor láthatjuk, hogy a `writeValue` tulajdonképpen 
csak visszaad egy új `ResourceBuilder` objektumot, és mindig csak ennyit csinál.

```java
public ResourceBuilder writeValue(final UUID theValue) {
    return new ResourceBuilder(SimpleValueFactory.getInstance().createBNode())
            .addType(TYPE)
            .addProperty(PROPERTY, SimpleValueFactory.getInstance().createLiteral(theValue.toString()));
}
```

Ebből kifolyólag, a típusellenőrzéstől, ami egyébként sem felel meg az OO paradigmáknak, meg is szabadulhatunk, mivel mindig
 egy `ResourceBuilder` objektumot fogunk visszakapni, vagy még a hívás során el fog szállni a kód. A cast-tól nem tudunk sajnsos 
megszabadulni, mivel a `Codec` sablont használ.

Ezután az iteráción és egyes változók beállításán kvíül nincs túl sok érdekes, viszont találunk a `try-catch` blokkon belül néhány 
`if`-et. Az`isIgnored` egy kiértékelést végez, majd visszatérít egy `bool` értéket.

Ezen máris gyorsíthatunk, ha a kiértékelést elhagyjuk, és rögtön az értékét térítjük vissza:
```java
private static boolean isIgnored(final PropertyDescriptor thePropertyDescriptor) {
    // we'll ignore getClass() on the bean
    /**if (thePropertyDescriptor.getName().equals("class")
        && thePropertyDescriptor.getReadMethod().getDeclaringClass() == Object.class
        && thePropertyDescriptor.getReadMethod().getReturnType().equals(Class.class)) {
        return  true;
    }

    return false;*/

    return thePropertyDescriptor.getName().equals("class")
            && thePropertyDescriptor.getReadMethod().getDeclaringClass() == Object.class
            && thePropertyDescriptor.getReadMethod().getReturnType().equals(Class.class);
}
```

Globálisan követendő konvenió, hogy ahol csak lehet ne használjuk a `continue`
 vagy a `go - goto` módszereket. Ezek eltűntetésére kiértékelést vezethetünk be.

Némi átalakítás után a ciklusunk a következő képpen néz ki:
```java
for (Map.Entry<String, Object> aEntry : PropertyUtils.describe(theValue).entrySet()) {
    final PropertyDescriptor aDescriptor = PropertyUtils.getPropertyDescriptor(theValue, aEntry.getKey());
 
    if(!isIgnored(aDescriptor)){
        final IRI aProperty = getProperty(aDescriptor);
 
        if(aProperty != null){
            final Object aObj = aEntry.getValue();
 
            if (aObj != null) {
                setValue(aGraph, aBuilder, aDescriptor, aProperty, aObj);
            }
        }
 
    }
}
```

#### Módosítások összesen

- `instanceof` eliminálása
- kiértékelések használata `continue` helyett
- kiértékelés és értékvisszaadás helyett csak értékvisszadunk

## RDF -> Beans tesztek

Az alábbi tesztekkel a korábbi ellentettjét fogjuk vizsgálni, azaz, hogy mennyire hatékony az objektumok visszalakítása RDF-ből.

### 1 Person objektum létrehozása RDF-ből

Nézzük is meg, hogy mennyi időbe telik egy objektum -> RDF konverzió:

![](img/performance/perf5.png)

Azt kaptuk, hogy átlagosan 19986 nanoszekundum. Itt most úgy néz ki, hogy a nagyságrendek 
tekintetében nincs eltérés a három mért érték között. Az viszont már sokkal érdekesebb, hogy minden esetben jelentősen hosszabb ideig, 
egy nagyságrenddel tovább tart ez a konverziót, mint a korábban megismert inverze. Ez több okból is lehet, 
de ehhez meg kell vizsgálnunk a konverziót végző kódot. (erre később visszatérünk)

### 1 000 Person objektum létrehozása RDF-ből

![](img/performance/perf6.png)

Itt is szembesülünk azzal, hogy az első mérés nagyságrendekkel tovább tarttott, 
mint a másik kettő, ezért ettől tekintsünk el. Vessük össze az itt kapott adatokat a konverzió inverzével. 
Ha csak az utolsó két mérés eredményét nézzük, akkor azt látjuk, hogy kb. ugyanannyi ideig tartott, mint amikor RDF-et csináltunk 
objektumból. Ez itt is tovább tart konverziónként, csak úgy, mint 1 példány esetében. 

Valószínűleg az állhat a dolog mögött, hogy a beolvasás, majd megfelelően visszaalakítás kevésbé univerzűális művelet, mivel egy RDF 
hármas az mindig egy RDF hármas, maximum egy objektumhoz több ilyen tartozik, de egy objektum összetevődhet tetszőlegesen 
sok RDF hármasból, és ezeket nem mindegy, hogy hogyan kell kombinálni az objektum visszaállításához.

### 1 000 000 Person objektum létrehozása RDF-ből

![](img/performance/perf7.png)

Itt a három teszt egymáshoz viszonyított nagyságrendjei stimmelnek, ez még inkább megerősít minket abban, hogy az 
előző teszt kiugró első esete nem mérvadó. Érdekes azonban, hogy az inverz műveletekhez képest itt is ugyanaz a nagyságrend érvényesül. 
Ez azt bizonyíthatja, hogy a fordító itt is tud optimalizálni. 

## Tesztek javítás után

A tesztek eredeti kódja a következő volt: 

```java
@Test
public void performanceTestRdfToBeansMillions(){
    ArrayList<Long> times = new ArrayList<>();

    Person p = new Person("Peter");

    Model model = RDFMapper.create().writeValue(p);

    int randomDestrutor = 0;
    Random rand = new Random();

    for(int j = 0; j < 3; ++j) {
        long start = System.nanoTime();

        for(int i = 0; i < 1000000; ++i) {
            if(rand.nextInt(2) == 1)
                randomDestrutor += 1;

            Person temp = RDFMapper.create().readValue(model, Person.class);
        }

        long finish = System.nanoTime();
        times.add(finish - start);
    }

    System.out.print("Elapsed time creating 1 000 000 Beans from RDF: ");
    long sum = 0;
    for (Long time : times) {
        sum += time;
        System.out.print(time + "ns ");
    }

    System.out.print("average elapsed time: " + (sum / times.size()) + "ns\n");
}
```

Az elégazásos módszer itt is segíthet, hogy a teszt pontosabb legyen kevesebb optimalizációval. 

#### Módosítások összesen
- asd
- asd
- asd

# Összegzés

Nagyvonalakban azt láthatjuk, hogy RDF-ből visszaalakítani kényesebb probléma, mint a fordítottjét megcsinálni. Emiatt 
 ezek a műveletek tovább is tartanak.

A kódon egetrengető sebesség-optimalizációt nem tudtunk elérni, mivel nagyon sok befolyásoló tényező van, kezdve a futtatókörnyezet 
specifikumától egészen a használt Java verzióig, de a felesleges, vagy esetleg nehezebben optimalizálható részeken 
könnyíteni tudtunk, ezzel javítva az általános teljesítményen, és a kód átláthatóságán.