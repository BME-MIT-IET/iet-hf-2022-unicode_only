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




