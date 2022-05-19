/*
 * Copyright (c) 2015 Complexible Inc. <http://complexible.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.complexible.pinto;

import com.complexible.common.openrdf.model.ModelIO;
import com.complexible.common.openrdf.model.Models2;
import com.complexible.common.openrdf.model.Statements;
import com.complexible.common.openrdf.vocabulary.FOAF;
import com.complexible.pinto.annotations.Iri;
import com.complexible.pinto.annotations.RdfId;
import com.complexible.pinto.annotations.RdfProperty;
import com.complexible.pinto.annotations.RdfsClass;
import com.complexible.pinto.codecs.UUIDCodec;
import com.complexible.pinto.impl.IdentifiableImpl;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openrdf.model.*;
import org.openrdf.model.impl.SimpleValueFactory;
import org.openrdf.model.util.Models;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.rio.RDFFormat;
import sun.reflect.generics.reflectiveObjects.WildcardTypeImpl;
import sun.reflect.generics.tree.Wildcard;

import java.io.*;
import java.lang.reflect.WildcardType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static org.junit.Assert.*;

/**
 * <p></p>
 *
 * @author Michael Grove
 */
public class RDFMapperTests {

	private static TimeZone tz = TimeZone.getDefault();

	@BeforeClass
	public static void setup() {
		// so that serialized dates get same TZ as reference data, not local TZ
		TimeZone.setDefault(TimeZone.getTimeZone("US/Eastern"));
	}

	@AfterClass
	public static void teardown() {
		TimeZone.setDefault(tz);
	}

	@Test(expected = UnidentifiableObjectException.class)
	public void testUnidentifiable() throws Exception {
		RDFMapper aMapper = RDFMapper.builder()
		                             .set(MappingOptions.REQUIRE_IDS, true)
		                             .build();

		aMapper.writeValue(new ClassWithPrimitives());
	}

	@Test
	public void testWritePrimitives() throws Exception {
		RDFMapper aMapper = RDFMapper.create();

		ClassWithPrimitives aObj = new ClassWithPrimitives();
		aObj.setString("str value");
		aObj.setInt(8);
		aObj.setURI(java.net.URI.create("urn:any"));
		aObj.setFloat(4.5f);
		aObj.setDouble(20.22);
		aObj.setChar('o');
		aObj.id(SimpleValueFactory.getInstance().createIRI("tag:complexible:pinto:3d1c9ece37c3f9ee6068440cf9a383cc"));

		Model aGraph = aMapper.writeValue(aObj);

		Model aExpected = ModelIO.read(new File(getClass().getResource("/data/primitives.nt").toURI()).toPath());

		assertTrue(Models.isomorphic(aGraph, aExpected));
	}

	@Test
	public void testReadPrimitives() throws Exception {
		Model aGraph = ModelIO.read(new File(getClass().getResource("/data/primitives.nt").toURI()).toPath());

		RDFMapper aMapper = RDFMapper.create();

		final ClassWithPrimitives aResult = aMapper.readValue(aGraph, ClassWithPrimitives.class);

		ClassWithPrimitives aExpected = new ClassWithPrimitives();
		aExpected.setString("str value");
		aExpected.setInt(8);
		aExpected.setURI(java.net.URI.create("urn:any"));
		aExpected.setFloat(4.5f);
		aExpected.setDouble(20.22);
		aExpected.setChar('o');

		assertEquals(aExpected, aResult);
	}

	@Test
	public void testReadMixed() throws Exception {
		ClassWithPrimitives aChild = new ClassWithPrimitives();
		aChild.setString("str value");
		aChild.setInt(8);
		aChild.setURI(java.net.URI.create("urn:any"));
		aChild.setFloat(4.5f);
		aChild.setDouble(20.22);
		aChild.setChar('o');

		ClassWithMixed aExpected = new ClassWithMixed();
		aExpected.setChild(aChild);
		aExpected.setString("class with mixed");

		Model aGraph = ModelIO.read(new File(getClass().getResource("/data/mixed.nt").toURI()).toPath());

		final ClassWithMixed aResult = RDFMapper.create().readValue(aGraph, ClassWithMixed.class,
		                                                            SimpleValueFactory.getInstance().createIRI("tag:complexible:pinto:45ad04336c95c0be6bba90e4b663da4d"));

		assertEquals(aExpected, aResult);
	}

	@Test
	public void testWriteMixed() throws Exception {
		ClassWithPrimitives aChild = new ClassWithPrimitives();
		aChild.setString("str value");
		aChild.setInt(8);
		aChild.setURI(java.net.URI.create("urn:any"));
		aChild.setFloat(4.5f);
		aChild.setDouble(20.22);
		aChild.setChar('o');

		ClassWithMixed aObj = new ClassWithMixed();
		aObj.setChild(aChild);
		aObj.setString("class with mixed");
		aObj.id(SimpleValueFactory.getInstance().createIRI("tag:complexible:pinto:45ad04336c95c0be6bba90e4b663da4d"));

		Model aGraph = RDFMapper.create().writeValue(aObj);

		Model aExpected = ModelIO.read(new File(getClass().getResource("/data/mixed.nt").toURI()).toPath());

		assertTrue(Models.isomorphic(aExpected, aGraph));
	}

	@Test
	public void testWriteListOfPrimitives() throws Exception {
		ClassWithPrimitiveLists aObj = new ClassWithPrimitiveLists();

		aObj.setInts(Lists.newArrayList(4, 5));
		aObj.setFloats(Sets.newLinkedHashSet(Lists.newArrayList(8f, 20f)));
		aObj.setBools(Sets.newTreeSet(Lists.newArrayList(true, false)));
		aObj.setDoubles(Sets.newLinkedHashSet(Lists.newArrayList(22d, 33d)));
		aObj.id(SimpleValueFactory.getInstance().createIRI("tag:complexible:pinto:b7d283d3a73c7b8a870087942b9a43b1"));

		Model aResult = RDFMapper.create().writeValue(aObj);

		assertTrue(Models.isomorphic(ModelIO.read(Files3.classPath("/data/primitive_lists.nt").toPath()),
		                            aResult));
	}

	@Test
	public void testWriteListsOfPrimitivesAsRdfListsWithOption() throws Exception {
		ClassWithPrimitiveLists aObj = new ClassWithPrimitiveLists();

		aObj.setInts(Lists.newArrayList(4, 5));
		aObj.id(SimpleValueFactory.getInstance().createIRI("tag:complexible:pinto:9017b0ab9335e4d090290a0dffc81826"));

		final Model aResult = RDFMapper.builder()
		                               .set(MappingOptions.SERIALIZE_COLLECTIONS_AS_LISTS, true)
		                               .build()
		                               .writeValue(aObj);

		assertTrue(Models.isomorphic(ModelIO.read(Files3.classPath("/data/primitive_rdf_lists.nt").toPath()),
		                            aResult));
	}

	@Test
	public void testWriteListsOfPrimitivesAsRdfListWithAnnotation() throws Exception {
		ClassWithPrimitiveRdfList aObj = new ClassWithPrimitiveRdfList();

		aObj.setInts(Lists.newArrayList(4, 5));
		aObj.id(SimpleValueFactory.getInstance().createIRI("tag:complexible:pinto:2b04e666a0fec2830d882740dbec8262"));

		final Model aResult = RDFMapper.create().writeValue(aObj);

		assertTrue(Models.isomorphic(ModelIO.read(Files3.classPath("/data/primitive_rdf_lists2.nt").toPath()),
		                            aResult));
	}

	@Test
	public void testWriteListOfObjects() throws Exception {
		ClassWithObjectList aObj = new ClassWithObjectList();
		aObj.setCollection(Sets.newLinkedHashSet(Lists.newArrayList(new Person("Earl Weaver"), new Person("Brooks Robinson"))));
		aObj.setSet(Sets.newLinkedHashSet(Lists.newArrayList(new Person("JJ Hardy"), new Person("Manny Machado"))));
		aObj.setList(Lists.newArrayList(new Person("Alejandro De Aza"), new Person("Adam Jones")));
		aObj.setSortedSet(Sets.newTreeSet(Lists.newArrayList(new Person("Steve Pearce"), new Person("Zach Britton"))));
		aObj.id(SimpleValueFactory.getInstance().createIRI("tag:complexible:pinto:881b2f11232944aeda9ba543e030dcfc"));

		final Model aResult = RDFMapper.create().writeValue(aObj);

		assertTrue(Models.isomorphic(ModelIO.read(Files3.classPath("/data/object_lists.nt").toPath()), aResult));
	}

	@Test
	public void testWriteListsOfObjectsAsRdfListWithOption() throws Exception {
		ClassWithObjectList aObj = new ClassWithObjectList();

		aObj.setCollection(Sets.newLinkedHashSet(Lists.newArrayList(new Person("Earl Weaver"), new Person("Brooks Robinson"))));
		aObj.id(SimpleValueFactory.getInstance().createIRI("tag:complexible:pinto:4f372f7bfb03f7b80be8777603d3b1ed"));

		final Model aResult = RDFMapper.builder()
		                               .set(MappingOptions.SERIALIZE_COLLECTIONS_AS_LISTS, true)
		                               .build()
		                               .writeValue(aObj);

		assertTrue(Models.isomorphic(ModelIO.read(Files3.classPath("/data/object_rdf_lists.nt").toPath()), aResult));
	}

	@Test
	public void testWriteListsOfObjectsAsRdfListWithAnnotation() throws Exception {
		ClassWithRdfObjectList aObj = new ClassWithRdfObjectList();

		aObj.setList(Lists.newArrayList(new Person("Earl Weaver"), new Person("Brooks Robinson")));
		aObj.id(SimpleValueFactory.getInstance().createIRI("tag:complexible:pinto:716825cc44cff258b685466022250434"));

		final Model aResult = RDFMapper.create().writeValue(aObj);

		assertTrue(Models.isomorphic(ModelIO.read(Files3.classPath("/data/object_rdf_lists2.nt").toPath()), aResult));
	}

	@Test
	public void testReadListOfPrimitives() throws Exception {
		Model aGraph = ModelIO.read(Files3.classPath("/data/primitive_lists.nt").toPath());

		final ClassWithPrimitiveLists aResult = RDFMapper.create().readValue(aGraph,
		                                                                     ClassWithPrimitiveLists.class,
		                                                                     SimpleValueFactory.getInstance().createIRI("tag:complexible:pinto:b7d283d3a73c7b8a870087942b9a43b1"));

		ClassWithPrimitiveLists aExpected = new ClassWithPrimitiveLists();

		aExpected.setInts(Lists.newArrayList(4, 5));
		aExpected.setFloats(Sets.newLinkedHashSet(Lists.newArrayList(8f, 20f)));
		aExpected.setBools(Sets.newTreeSet(Lists.newArrayList(true, false)));
		aExpected.setDoubles(Sets.newLinkedHashSet(Lists.newArrayList(22d, 33d)));

		assertEquals(aExpected, aResult);
	}

	@Test
	public void testReadRdfListOfPrimitives() throws Exception {
		Model aGraph = ModelIO.read(Files3.classPath("/data/primitive_rdf_lists.nt").toPath());

		final ClassWithPrimitiveLists aResult = RDFMapper.create().readValue(aGraph,
		                                                                     ClassWithPrimitiveLists.class,
		                                                                     SimpleValueFactory.getInstance().createIRI("tag:complexible:pinto:9017b0ab9335e4d090290a0dffc81826"));

		ClassWithPrimitiveLists aExpected = new ClassWithPrimitiveLists();

		aExpected.setInts(Lists.newArrayList(4, 5));

		assertEquals(aExpected, aResult);
	}

	@Test
	public void testReadListOfObjects() throws Exception {
		Model aGraph = ModelIO.read(Files3.classPath("/data/object_lists.nt").toPath());

		ClassWithObjectList aExpected = new ClassWithObjectList();
		aExpected.setCollection(Sets.newLinkedHashSet(Lists.newArrayList(new Person("Earl Weaver"), new Person("Brooks Robinson"))));
		aExpected.setSet(Sets.newLinkedHashSet(Lists.newArrayList(new Person("JJ Hardy"), new Person("Manny Machado"))));
		aExpected.setList(Lists.newArrayList(new Person("Alejandro De Aza"), new Person("Adam Jones")));
		aExpected.setSortedSet(Sets.newTreeSet(Lists.newArrayList(new Person("Steve Pearce"), new Person("Zach Britton"))));

		final ClassWithObjectList aResult = RDFMapper.create().readValue(aGraph,
		                                                                 ClassWithObjectList.class,
		                                                                 SimpleValueFactory.getInstance().createIRI("tag:complexible:pinto:881b2f11232944aeda9ba543e030dcfc"));

		assertEquals(aExpected, aResult);
	}

	@Test
	public void testReadRdfListOfObjects() throws Exception {
		Model aGraph = ModelIO.read(Files3.classPath("/data/object_rdf_lists.nt").toPath());

		ClassWithObjectList aExpected = new ClassWithObjectList();

		aExpected.setCollection(Sets.newLinkedHashSet(Lists.newArrayList(new Person("Earl Weaver"), new Person("Brooks Robinson"))));

		final ClassWithObjectList aResult = RDFMapper.create().readValue(aGraph,
		                                                                 ClassWithObjectList.class,
		                                                                 SimpleValueFactory.getInstance().createIRI("tag:complexible:pinto:4f372f7bfb03f7b80be8777603d3b1ed"));
		assertEquals(aExpected, aResult);
	}

	@Test
	public void testUseRdfIdForIdentification() throws Exception {
		// require ids so the default id generation cannot be used
		RDFMapper aMapper = RDFMapper.builder()
		                             .set(MappingOptions.REQUIRE_IDS, true)
		                             .build();

		Company aCompany = new Company();
		aCompany.setName("Clark & Parsia");
		aCompany.setWebsite("http://clarkparsia.com");

		Model aGraph = aMapper.writeValue(aCompany);

		assertTrue(!aGraph.isEmpty());

		final String aExpected = Hashing.md5().newHasher()
		                                .putString(aCompany.getName(), Charsets.UTF_8)
		                                .putString(aCompany.getWebsite(), Charsets.UTF_8)
		                                .hash().toString();

		assertEquals(aExpected,
		             ((org.openrdf.model.URI) aGraph.iterator().next().getSubject()).getLocalName());
	}

	@Test(expected = UnidentifiableObjectException.class)
	public void testUseRdfIdForIdentificationWithInvalidID() throws Exception {
		// require ids so the default id generation cannot be used
		RDFMapper aMapper = RDFMapper.builder()
				.set(MappingOptions.REQUIRE_IDS, true)
				.build();

		BadCompany aCompany = new BadCompany();
		aCompany.setName("Very bad company");
		aCompany.setNumberOfEmployees(2);
		aCompany.setWebsite("https://verybadcompany.co");

		Model aGraph = aMapper.writeValue(aCompany);

		assertTrue(!aGraph.isEmpty());

		final String aExpected = Hashing.md5().newHasher()
				.putString(aCompany.getName(), Charsets.UTF_8)
				.putString(aCompany.getWebsite(), Charsets.UTF_8)
				.hash().toString();

		assertEquals(aExpected,
				((org.openrdf.model.URI) aGraph.iterator().next().getSubject()).getLocalName());
	}


	@Test
	public void testWriteTwice() throws Exception {
		ClassWithObjectList aObj = new ClassWithObjectList();

		aObj.setCollection(Sets.newLinkedHashSet(Lists.newArrayList(new Person("Earl Weaver"), new Person("Brooks Robinson"))));
		aObj.id(SimpleValueFactory.getInstance().createIRI("tag:complexible:pinto:4f372f7bfb03f7b80be8777603d3b1ed"));

		Model aGraph = RDFMapper.create().writeValue(aObj);

		Model aOtherGraph = RDFMapper.create().writeValue(aObj);

		assertTrue(Models.isomorphic(aGraph, aOtherGraph));
	}

	@Test
	public void testReadTwice() throws Exception {
		Model aGraph = ModelIO.read(Files3.classPath("/data/object_lists.nt").toPath());

		final ClassWithObjectList aObj = RDFMapper.create().readValue(aGraph, ClassWithObjectList.class,
		                                                              SimpleValueFactory.getInstance().createIRI("tag:complexible:pinto:881b2f11232944aeda9ba543e030dcfc"));

		final ClassWithObjectList aObj2 = RDFMapper.create().readValue(aGraph, ClassWithObjectList.class,
		                                                               SimpleValueFactory.getInstance().createIRI("tag:complexible:pinto:881b2f11232944aeda9ba543e030dcfc"));

		assertEquals(aObj, aObj2);
	}

	@Test
	public void testIdentifiableNoIdFallbackToDefault() throws Exception {
		Person aPerson = new Person("Michael Grove");

		Model aGraph = RDFMapper.create().writeValue(aPerson);

		assertEquals(1, aGraph.size());

		Statement aStmt = aGraph.iterator().next();

		assertEquals("name", aStmt.getPredicate().getLocalName());
		assertEquals(aPerson.getName(), aStmt.getObject().stringValue());
	}

	@Test
	public void testReadSetsIdentifiableId() throws Exception {
		Person aPerson = new Person("Michael Grove");

		aPerson.id(SimpleValueFactory.getInstance().createIRI("urn:mg"));
		Model aGraph = RDFMapper.create().writeValue(aPerson);

		assertEquals(1, aGraph.size());

		Person aCopy = RDFMapper.create().readValue(aGraph, Person.class);

		assertEquals(aPerson.id(), aCopy.id());
	}

	@Test
	public void testWriteSetsIdentifiableId() throws Exception {
		Person aPerson = new Person("Michael Grove");

		Model aGraph = RDFMapper.create().writeValue(aPerson);

		assertTrue(aPerson.id() != null);

		assertEquals(1, aGraph.size());

		Person aCopy = RDFMapper.create().readValue(aGraph, Person.class);

		assertEquals(aPerson.id(), aCopy.id());
	}

	@Test(expected = UnidentifiableObjectException.class)
	public void testIdentifiableNoIdNoFallback() throws Exception {
		RDFMapper.builder()
		         .set(MappingOptions.REQUIRE_IDS, true)
		         .build().writeValue(new ClassWithPrimitives());
	}

	@Test
	public void testCustomDatatypingOfLiteral() throws Exception {
		Company aCompany = new Company();
		aCompany.setNumberOfEmployees(10);

		Model aGraph = RDFMapper.create().writeValue(aCompany);

		Optional<Statement> aStatement = aGraph.stream().filter(Statements.predicateIs(SimpleValueFactory.getInstance().createIRI(RDFMapper.DEFAULT_NAMESPACE + "numberOfEmployees"))).findFirst();

		assertTrue("should have found the triple", aStatement.isPresent());

		Literal aResult = (Literal) aStatement.get().getObject();

		assertEquals(XMLSchema.INTEGER, aResult.getDatatype());
		assertEquals("10", aResult.getLabel());
	}

	@Test
	public void testCustomDatatypingOfLiteralWithInvalidDatatype() throws Exception {
		BadCompany aCompany = new BadCompany();
		aCompany.setNumberOfEmployees(10);

		Model aGraph = RDFMapper.create().writeValue(aCompany);

		// this will be empty since there are no valid assertions
		assertTrue(aGraph.isEmpty());
	}

	@Test(expected = RDFMappingException.class)
	public void testCustomDatatypingOfLiteralWithInvalidDatatypeFatal() throws Exception {
		BadCompany aCompany = new BadCompany();
		aCompany.setNumberOfEmployees(10);

		RDFMapper.builder().set(MappingOptions.IGNORE_INVALID_ANNOTATIONS, false).build().writeValue(aCompany);
	}

	@Test
	public void testRdfIdWithNoValuesSet() throws Exception {
		Company aCompany = new Company();
		aCompany.setNumberOfEmployees(10);

		Model aGraph = RDFMapper.create().writeValue(aCompany);

		// should be two stmts, the rdf:type assertion and the one for :numberOfEmployees
		// id might be different every time since we have no inputs to the id hash, so we'll just rely on the count
		// here when checking for correctness.
		assertEquals(2, aGraph.size());
	}



	@Test
	public void testUseSerializationAnnotations() throws Exception {
		Company aCompany = new Company();
		aCompany.setName("Clark & Parsia");
		aCompany.setWebsite("http://clarkparsia.com");

		Model aGraph = RDFMapper.create().writeValue(aCompany);

		assertTrue(Models.isomorphic(ModelIO.read(Files3.classPath("/data/company.nt").toPath()),
		                            aGraph));
	}

	@Test(expected=RDFMappingException.class)
	public void testCardinalityViolationFatal() throws Exception {
		RDFMapper.create().readValue(ModelIO.read(Files3.classPath("/data/company-card.nt").toPath()), Company.class);
	}

	@Test
	public void testCanIgnoreCardinalityViolation() throws Exception {
		Company aCompany = RDFMapper.builder()
		                            .set(MappingOptions.IGNORE_CARDINALITY_VIOLATIONS, true)
		                            .build()
		                            .readValue(ModelIO.read(Files3.classPath("/data/company-card.nt").toPath()), Company.class);

		assertEquals("http://clarkparsia.com", aCompany.getWebsite());

		assertTrue(aCompany.getName().equals("Complexible") || aCompany.getName().equals("Clark & Parsia"));
	}

	@Test
	public void testAnnotationsWithInvalidURI() throws Exception {
		BadCompany aCompany = new BadCompany();
		aCompany.setName("Clark & Parsia");
		aCompany.setWebsite("http://clarkparsia.com");

		Model aGraph = RDFMapper.create().writeValue(aCompany);
		assertTrue(Models.isomorphic(ModelIO.read(Files3.classPath("/data/bad_company.nt").toPath()),
		                            aGraph));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoInvalidNamespaceToBuilder() throws Exception {
		RDFMapper.builder().namespace("prefix", "not a valid namespace").build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoInvalidPrefixToBuilder() throws Exception {
		RDFMapper.builder().namespace("not a valid prefix", "urnnamespace").build();
	}

	@Test(expected = RDFMappingException.class)
	public void testAnnotationsWithInvalidURIFatal() throws Exception {
		BadCompany aCompany = new BadCompany();
		aCompany.setName("Clark & Parsia");
		aCompany.setWebsite("http://clarkparsia.com");

		RDFMapper.builder()
		         .set(MappingOptions.IGNORE_INVALID_ANNOTATIONS, false)
		         .build()
		         .writeValue(aCompany);
	}

	@Test
	public void testWriteAnnotationsWithNamespace() throws Exception {
		ShortCompany aCompany = new ShortCompany();
		aCompany.setName("Clark & Parsia");
		aCompany.setWebsite("http://clarkparsia.com");
		aCompany.setNumberOfEmployees(10);

		final Model aGraph = RDFMapper.builder()
		                              .namespace("fo", FOAF.ontology().uri().toString())
		                              .namespace("xs", XMLSchema.NAMESPACE)
		                              .build().writeValue(aCompany);

		assertTrue(Models.isomorphic(ModelIO.read(Files3.classPath("/data/company2.nt").toPath()), aGraph));
	}

	@Test
	public void testReadWithUnmatchedRDF() throws Exception {
		Model aGraph = ModelIO.read(Files3.classPath("/data/object_rdf_lists.nt").toPath());
		aGraph.addAll(ModelIO.read(Files3.classPath("/data/mixed.nt").toPath()));

		ClassWithObjectList aExpected = new ClassWithObjectList();

		aExpected.setCollection(Sets.newLinkedHashSet(Lists.newArrayList(new Person("Earl Weaver"),
		                                                                 new Person("Brooks Robinson"))));


		final ClassWithObjectList aObj = RDFMapper.create().readValue(aGraph, ClassWithObjectList.class,
		                                                              SimpleValueFactory.getInstance().createIRI("tag:complexible:pinto:4f372f7bfb03f7b80be8777603d3b1ed"));
		assertEquals(aExpected, aObj);
	}

	@Test
	public void testWriteEnum() throws Exception {
		ClassWithEnum aObj = new ClassWithEnum();

		aObj.id(SimpleValueFactory.getInstance().createIRI("urn:testWriteEnum"));
		aObj.setValue(TestEnum.Bar);

		Model aGraph = RDFMapper.create().writeValue(aObj);

		assertEquals(1, aGraph.size());

		Optional<Statement> aStatement = aGraph.stream().filter(Statements.predicateIs(SimpleValueFactory.getInstance().createIRI(RDFMapper.DEFAULT_NAMESPACE + "value"))).findFirst();

		assertTrue("should have found the triple", aStatement.isPresent());

		Resource aResult = (Resource) aStatement.get().getObject();

		assertEquals(SimpleValueFactory.getInstance().createIRI("urn:TestEnum:Bar"), aResult);

		aObj.setValue(TestEnum.Baz);

		aGraph = RDFMapper.create().writeValue(aObj);

		assertEquals(1, aGraph.size());

		aStatement = aGraph.stream().filter(Statements.predicateIs(SimpleValueFactory.getInstance().createIRI(RDFMapper.DEFAULT_NAMESPACE + "value"))).findFirst();

		assertTrue("should have found the triple", aStatement.isPresent());

		aResult = (Resource) aStatement.get().getObject();

		assertEquals(SimpleValueFactory.getInstance().createIRI(RDFMapper.DEFAULT_NAMESPACE, "Baz"), aResult);
	}

	@Test
	public void testWriteEnumInvalidIri() throws Exception {
		ClassWithEnum aObj = new ClassWithEnum();

		aObj.id(SimpleValueFactory.getInstance().createIRI("urn:testWriteEnumInvalidIri"));
		aObj.setValue(TestEnum.Foo);

		final Model aGraph = RDFMapper.create().writeValue(aObj);
		assertTrue(aGraph.isEmpty());
	}

	@Test(expected = RDFMappingException.class)
	public void testWriteEnumInvalidIriFatal() throws Exception {
		ClassWithEnum aObj = new ClassWithEnum();

		aObj.id(SimpleValueFactory.getInstance().createIRI("urn:testWriteEnumInvalidIri"));
		aObj.setValue(TestEnum.Foo);

		RDFMapper.builder()
		         .set(MappingOptions.IGNORE_INVALID_ANNOTATIONS, false)
		         .build()
		         .writeValue(aObj);
	}

	@Test
	public void testReadEnum() throws Exception {
		final Model aGraph = ModelIO.read(Files3.classPath("/data/enum.nt").toPath());

		final ClassWithEnum aExpected = new ClassWithEnum();

		aExpected.id(SimpleValueFactory.getInstance().createIRI("urn:testReadEnum"));
		aExpected.setValue(TestEnum.Bar);

		final ClassWithEnum aResult = RDFMapper.create().readValue(aGraph, ClassWithEnum.class);

		assertEquals(aExpected, aResult);
	}


	@Test(expected = RDFMappingException.class)
	public void testNoDefaultConstructor() throws Exception {
		RDFMapper.create().readValue(ModelIO.read(Files3.classPath("/data/mixed.nt").toPath()), CannotConstructMe.class);
	}

	@Test(expected = RDFMappingException.class)
	public void testCannotConstructAbstract() throws Exception {
		RDFMapper.create().readValue(ModelIO.read(Files3.classPath("/data/mixed.nt").toPath()), CannotConstructMe2.class);
	}

	@Test
	public void testConstructFromEmpty() throws Exception {
		assertEquals(RDFMapper.create().readValue(Models2.newModel(), Person.class), new Person());
	}

	@Test(expected = RDFMappingException.class)
	public void testMultipleSubjectsNoIdProvided() throws Exception {
		RDFMapper.create().readValue(ModelIO.read(Files3.classPath("/data/object_lists.nt").toPath()), ClassWithObjectList.class);
	}

	@Test
	public void testWriteWithCodec() throws Exception {

		final Model aGraph = RDFMapper.builder()
		                              .codec(UUID.class, UUIDCodec.Instance)
		                              .build()
		                              .writeValue(UUID.fromString("0110f311-964b-440d-b772-92c621c5d1e4"));

		assertTrue(Models.isomorphic(aGraph,
		                            ModelIO.read(Files3.classPath("/data/uuid.nt").toPath())));
	}

	@Test
	public void testReadWithCodec() throws Exception {
		final Model aGraph = ModelIO.read(Files3.classPath("/data/uuid.nt").toPath());

		final UUID aResult = RDFMapper.builder()
				                              .codec(UUID.class, UUIDCodec.Instance)
				                              .build()
				                              .readValue(aGraph, UUID.class);

		assertEquals(UUID.fromString("0110f311-964b-440d-b772-92c621c5d1e4"), aResult);
	}

	@Test
	public void testWriteMap() throws Exception {
		final ClassWithMap aObj = new ClassWithMap();

		aObj.mMap = Maps.newLinkedHashMap();

		aObj.mMap.put("bob", new Person("Bob the tester"));
		aObj.mMap.put(1L, "the size of something");
		aObj.mMap.put(new Date(1426361082470L), 57.4);
		aObj.mMap.put(new Person("another person"), new Company("The company"));

		final Model aGraph = RDFMapper.builder()
		                              .map(FOAF.ontology().Person, Person.class)
		                              .build()
		                              .writeValue(aObj);

		assertTrue(Models.isomorphic(aGraph,
		                            ModelIO.read(Files3.classPath("/data/map.nt").toPath())));
	}

	@Test
	public void testReadMap() throws Exception {
		final ClassWithMap aExpected = new ClassWithMap();

		aExpected.mMap = Maps.newLinkedHashMap();

		aExpected.mMap.put("bob", new Person("Bob the tester"));
		aExpected.mMap.put(1L, "the size of something");
		aExpected.mMap.put(new Date(1426361082470L), 57.4);
		aExpected.mMap.put(new Person("another person"), new Company("The company"));

		final Model aGraph = ModelIO.read(Files3.classPath("/data/map.nt").toPath());

		assertEquals(aExpected, RDFMapper.builder()
		                                 .map(FOAF.ontology().Person, Person.class)
		                                 .map(SimpleValueFactory.getInstance().createIRI("urn:Company"), Company.class)
		                                 .build()
		                                 .readValue(aGraph, ClassWithMap.class,
		                                                    SimpleValueFactory.getInstance().createIRI("tag:complexible:pinto:06f95e70fea33fcd99e6804b02f96cc9")));
	}

	@Test
	public void performanceTestBeansToRdfOne(){
		ArrayList<Long> times = new ArrayList<>();

		Person p = new Person("Peter");

		for(int j = 0; j < 3; ++j){
			long start = System.nanoTime();

			Model m = RDFMapper.create().writeValue(p);

			long finish = System.nanoTime();
			times.add(finish - start);
		}

		System.out.print("Elapsed time creating 1 RDF from Beans: ");
		long sum = 0;
		for (Long time : times) {
			sum += time;
			System.out.print(time + "ns ");
		}

		System.out.print("average elapsed time: " + (sum / times.size()) + "ns\n");

	}

	@Test
	public void performanceTestBeansToRdfThousand(){
		ArrayList<Long> times = new ArrayList<>();

		Person p = new Person("Peter");

		int randomDestrutor = 0;
		Random rand = new Random();

		for(int j = 0; j < 3; ++j){
			long start = System.nanoTime();

			for(int i = 0; i < 1000; ++i) {
				if(rand.nextInt(2) == 1)
					randomDestrutor += 1;

				Model m = RDFMapper.create().writeValue(p);
			}
			long finish = System.nanoTime();
			times.add(finish - start);
		}

		System.out.print("Elapsed time creating 1 000 RDFs from Beans: ");
		long sum = 0;
		for (Long time : times) {
			sum += time;
			System.out.print(time + "ns ");
		}

		System.out.print("average elapsed time: " + (sum / times.size()) + "ns\n");
	}

	@Test
	public void performanceTestBeansToRdfMillion(){
		ArrayList<Long> times = new ArrayList<>();

		Person p = new Person("Peter");

		int randomDestrutor = 0;
		Random rand = new Random();

		for(int j = 0; j < 3; ++j){
			long start = System.nanoTime();

			for(int i = 0; i < 1000000; ++i) {
				if(rand.nextInt(2) == 1)
					randomDestrutor += 1;

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

	@Test
	public void performanceTestRdfToBeansOne(){
		ArrayList<Long> times = new ArrayList<>();

		Person p = new Person("Peter");

		Model model = RDFMapper.create().writeValue(p);

		for(int j = 0; j < 3; ++j) {
			long start = System.nanoTime();

			Person temp = RDFMapper.create().readValue(model, Person.class);

			long finish = System.nanoTime();
			times.add(finish - start);
		}

		System.out.print("Elapsed time creating 1 Bean from RDF: ");
		long sum = 0;
		for (Long time : times) {
			sum += time;
			System.out.print(time + "ns ");
		}

		System.out.print("average elapsed time: " + (sum / times.size()) + "ns\n");
	}

	@Test
	public void performanceTestRdfToBeansThousand(){
		ArrayList<Long> times = new ArrayList<>();

		Person p = new Person("Peter");

		Model model = RDFMapper.create().writeValue(p);

		int randomDestrutor = 0;
		Random rand = new Random();

		for(int j = 0; j < 3; ++j) {
			long start = System.nanoTime();

			for(int i = 0; i < 1000; ++i) {
				if(rand.nextInt(2) == 1)
					randomDestrutor += 1;

				Person temp = RDFMapper.create().readValue(model, Person.class);
			}

			long finish = System.nanoTime();
			times.add(finish - start);
		}

		System.out.print("Elapsed time creating 1 000 Beans from RDF: ");
		long sum = 0;
		for (Long time : times) {
			sum += time;
			System.out.print(time + "ns ");
		}

		System.out.print("average elapsed time: " + (sum / times.size()) + "ns\n");
	}

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
  
  //Újak

	//TODO
	@Test
	@Ignore("This test is failing it needs to be looked into.")
	public void testWriteEnumSetValidIRI() {
		ClassWithEnumSet cwes = new ClassWithEnumSet();

		EnumSet<TestEnum> set = EnumSet.noneOf(TestEnum.class);
		set.add(TestEnum.Bar);
		cwes.setEnums(set);
		Model m = RDFMapper.create().writeValue(cwes);
		assertFalse(m.isEmpty());
	}

	//TODO
	@Test
	@Ignore("This test is failing it needs to be looked into.")
	public void testWriteEnumSetInvalidIRI() {

		ClassWithEnumSet cwes = new ClassWithEnumSet();

		EnumSet<TestEnum> set = EnumSet.noneOf(TestEnum.class);
		set.add(TestEnum.Foo);

		cwes.setEnums(set);
		Model m = RDFMapper.create().writeValue(cwes);
	}

	//TODO
	@Test
	@Ignore("This test is failing it needs to be looked into.")
	public void testWriteEnumSetNoIRI() {
		ClassWithEnumSet cwes = new ClassWithEnumSet();

		EnumSet<TestEnum> set = EnumSet.noneOf(TestEnum.class);
		set.add(TestEnum.Baz);
		cwes.setEnums(set);
		Model m = RDFMapper.create().writeValue(cwes);
		assertFalse(m.isEmpty());
	}

	//TODO
	@Test
	@Ignore("This test is failing it needs to be looked into.")
	public void testWriteEnumSetMixed() {
		ClassWithEnumSet cwes = new ClassWithEnumSet();

		EnumSet<TestEnum> set = EnumSet.noneOf(TestEnum.class);
		set.add(TestEnum.Bar);
		set.add(TestEnum.Baz);
		set.add(TestEnum.Foo);

		cwes.setEnums(set);
		Model m = RDFMapper.create().writeValue(cwes);
		assertFalse(m.isEmpty());
	}

	@Test
	public void testMultipleSubjectsWithIdProvided() throws Exception {
		ClassWithObjectList cwls = RDFMapper.create().readValue(ModelIO.read(Files3.classPath("/data/object_lists.nt").toPath()), ClassWithObjectList.class,
				SimpleValueFactory.getInstance().createIRI("urn:someIdentifier"));
		assertFalse(cwls == null);
	}

	public String buildLongString() {
		StringBuilder longStringBuilder = new StringBuilder();
		for(int i=0; i<1000000; ++i){
			longStringBuilder.append("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
		}
		return longStringBuilder.toString();
	}

	@Test()
	public void testCharBeanTypeWithLongString() throws Exception {

		ClassWithPrimitives cwp = new ClassWithPrimitives();
		cwp.id(SimpleValueFactory.getInstance().createIRI("urn:testCharBeanTypeWithLongString"));
		cwp.setString(buildLongString());
		final Model model = RDFMapper.create().writeValue(cwp);

		ClassWithPrimitives des = RDFMapper.create().readValue(model, ClassWithPrimitives.class,
				SimpleValueFactory.getInstance().createIRI("urn:testCharBeanTypeWithLongString"));

		assertTrue(cwp.equals(des));

	}

	@Test
	public void testLongUri() throws Exception {
		ClassWithPrimitives cwp = new ClassWithPrimitives();
		cwp.id(SimpleValueFactory.getInstance().createIRI("urn:"+buildLongString()));
		final Model md = RDFMapper.create().writeValue(cwp);
	}

	@Test
	public void testReadIdentical() {
		ClassWithObjectList aObj = new ClassWithObjectList();

		aObj.setCollection(Sets.newLinkedHashSet(Lists.newArrayList(new Person("Earl Weaver"), new Person("Brooks Robinson"))));
		aObj.id(SimpleValueFactory.getInstance().createIRI("tag:complexible:pinto:4f372f7bfb03f7b80be8777603d3b1ed"));

		Model aGraph = RDFMapper.create().writeValue(aObj);

		ClassWithObjectList deserialized = RDFMapper.create().readValue(aGraph, ClassWithObjectList.class,
				SimpleValueFactory.getInstance().createIRI("tag:complexible:pinto:4f372f7bfb03f7b80be8777603d3b1ed"));

		assertTrue(aObj.equals(deserialized));
		assertTrue(deserialized.equals(aObj));

	}

	@Test(expected = RDFMappingException.class)
	public void testWriteMapWithInvalidURI() throws Exception {

		BadCompany bc1 = new BadCompany();
		bc1.setName("Bad company");
		bc1.setWebsite("https://foo.bar");
		bc1.setNumberOfEmployees(3);

		BadCompany bc2 = new BadCompany();
		bc1.setName("Worse company");
		bc1.setWebsite("https://foo.bar.co.uk");
		bc1.setNumberOfEmployees(4);

		Map<Object, Object> x = new HashMap<Object, Object>();
		x.put(1, bc1);
		x.put(2, bc2);

		ClassWithMap cwm = new ClassWithMap();
		cwm.setMap(x);

		Model md = RDFMapper.create().writeValue(cwm);
		ClassWithMap des = RDFMapper.create().readValue(md, ClassWithMap.class);
	}

	@Test
	public void testURIMapping() throws Exception {
		// alternative to RdfsClass, verify that if you specify a type URI -> class mapping it is used
		// test 1, that the rdf:type is included in a top level class

		ClassWithRdfsClassAnnotation cls = new ClassWithRdfsClassAnnotation();
		cls.someValue = "aasdasd";
		PropertyClass pc = new PropertyClass();
		pc.someValue = 2.3f;
		cls.instanceOfPropClass = pc;

		final Model modelOfCompositeObject = RDFMapper.create().writeValue(cls);
		ClassWithRdfsClassAnnotation deserialized = RDFMapper.create().readValue(modelOfCompositeObject, ClassWithRdfsClassAnnotation.class);

		assert(deserialized instanceof ClassWithRdfsClassAnnotation);
	}

	//TODO
	@Test
	@Ignore("This test is failing it needs to be looked into.")
	public void testURIMapping2() throws Exception {

		// test 2, that the type can be used to find the correct implementation for a property
		//         eg:  Object getFoo() -> this will be populated by a individual w/ a type :Bar to the class Baz
		//         so the mapping would specify :Bar <-> Baz
		ClassWithRdfsClassAnnotation cls = new ClassWithRdfsClassAnnotation();
		cls.someValue = "some string";
		PropertyClass pc = new PropertyClass();
		pc.someValue = 2.3f;
		cls.instanceOfPropClass = pc;

		final Model modelOfCompositeObject = RDFMapper.create().writeValue(cls);
		ClassWithRdfsClassAnnotation deserialized = RDFMapper.create().readValue(modelOfCompositeObject, ClassWithRdfsClassAnnotation.class);

		assert(deserialized instanceof ClassWithRdfsClassAnnotation &&
				deserialized.instanceOfPropClass instanceof PropertyClass);
	}

	@RdfsClass("foaf:PropertyClass")
	public static class PropertyClass {
		public float someValue;
		public PropertyClass() { }
		@Override
		public int hashCode() {return Objects.hashCode(someValue); }
		@Override
		public boolean equals(final Object theObj) {
			if (theObj == this) {
				return true;
			}
			else if (theObj instanceof PropertyClass) {
				return Objects.equals(someValue, ((PropertyClass) theObj).someValue);
			}
			else {
				return false;
			}
		}
		@Override
		public String toString() {
			return String.valueOf(someValue);
		}
	}

	@RdfsClass("foaf:ClassWithRdfsClass")
	public static class ClassWithRdfsClassAnnotation {
		public String someValue;
		public PropertyClass instanceOfPropClass;

		public ClassWithRdfsClassAnnotation() {}
		@Override
		public int hashCode() {
			return 0;
		}
		@Override
		public boolean equals(final Object theObj) {
			if (theObj == this) {
				return true;
			}
			else if (theObj instanceof ClassWithRdfsClassAnnotation) {
				return Objects.equals(someValue, ((ClassWithRdfsClassAnnotation) theObj).someValue)
						&& Objects.equals(instanceOfPropClass, ((ClassWithRdfsClassAnnotation) theObj).instanceOfPropClass);
			}
			else {
				return false;
			}
		}

	public static final class Files3 {
		public static File classPath(final String thePath) {
			try {
				return new File(Files3.class.getResource(thePath).toURI());
			}
			catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public enum TestEnum {
		@Iri("invalid URI")
		Foo,

		@Iri("urn:TestEnum:Bar")
		Bar,

		Baz
	}

	public static class CannotConstructMe {
		public CannotConstructMe(final String theValue) {
		}
	}

	public static abstract class CannotConstructMe2 {
		public CannotConstructMe2() {
		}
	}

	@RdfsClass("fo:Company")
	public static final class ShortCompany {

		private String mName;
		private String mWebsite;
		private Integer mNumberOfEmployees;

		@RdfId
		@RdfProperty("foaf:name")
		public String getName() {
			return mName;
		}

		public void setName(final String theName) {
			mName = theName;
		}

		@RdfId
		public String getWebsite() {
			return mWebsite;
		}

		public void setWebsite(final String theWebsite) {
			mWebsite = theWebsite;
		}

		@RdfProperty(datatype="xs:integer")
		public Integer getNumberOfEmployees() {
			return mNumberOfEmployees;
		}

		public void setNumberOfEmployees(final Integer theNumberOfEmployees) {
			mNumberOfEmployees = theNumberOfEmployees;
		}

		@Override
		public int hashCode() {
			return Objects.hash(mName, mWebsite);
		}

		@Override
		public boolean equals(final Object theObj) {
			if (theObj == this) {
				return true;
			}
			else if (theObj instanceof ShortCompany) {
				ShortCompany aObj = (ShortCompany) theObj;
				return mNumberOfEmployees == aObj.mNumberOfEmployees
				       && Objects.equals(mName, aObj.mName)
				       && Objects.equals(mWebsite, aObj.mWebsite);
			}
			else {
				return false;
			}
		}
	}

	@RdfsClass("not a valid uri")
	public static final class BadCompany {
		private String mName;
		private String mWebsite;
		private int mNumberOfEmployees;

		@RdfProperty("not a valid uri")
		public String getName() {
			return mName;
		}

		public void setName(final String theName) {
			mName = theName;
		}

		public String getWebsite() {
			return mWebsite;
		}

		public void setWebsite(final String theWebsite) {
			mWebsite = theWebsite;
		}

		@RdfProperty(datatype="not a valid uri")
		public int getNumberOfEmployees() {
			return mNumberOfEmployees;
		}

		public void setNumberOfEmployees(final int theNumberOfEmployees) {
			mNumberOfEmployees = theNumberOfEmployees;
		}

		@Override
		public int hashCode() {
			return Objects.hash(mName, mWebsite);
		}

		@Override
		public boolean equals(final Object theObj) {
			if (theObj == this) {
				return true;
			}
			else if (theObj instanceof BadCompany) {
				BadCompany aObj = (BadCompany) theObj;
				return Objects.equals(mName, aObj.mName) && Objects.equals(mWebsite, aObj.mWebsite);
			}
			else {
				return false;
			}
		}
	}

	@RdfsClass("urn:Company")
	public static final class Company {

		private String mName;
		private String mWebsite;
		private Integer mNumberOfEmployees;

		public Company() {
		}

		public Company(final String theName) {
			mName = theName;
		}

		@RdfId
		@RdfProperty("urn:name")
		public String getName() {
			return mName;
		}

		public void setName(final String theName) {
			mName = theName;
		}

		@RdfId
		public String getWebsite() {
			return mWebsite;
		}

		public void setWebsite(final String theWebsite) {
			mWebsite = theWebsite;
		}

		@RdfProperty(datatype="http://www.w3.org/2001/XMLSchema#integer")
		public Integer getNumberOfEmployees() {
			return mNumberOfEmployees;
		}

		public void setNumberOfEmployees(final Integer theNumberOfEmployees) {
			mNumberOfEmployees = theNumberOfEmployees;
		}

		@Override
		public int hashCode() {
			return Objects.hash(mName, mWebsite);
		}

		@Override
		public boolean equals(final Object theObj) {
			if (theObj == this) {
				return true;
			}
			else if (theObj instanceof Company) {
				Company aObj = (Company) theObj;
				return Objects.equals(mName, aObj.mName) && Objects.equals(mWebsite, aObj.mWebsite);
			}
			else {
				return false;
			}
		}
	}

	public static class ClassWithEnumSet /*implements Identifiable*/ {

		/*private Identifiable mIdentifiable = new IdentifiableImpl();

		@Override
		public Resource id() {
			return mIdentifiable.id();
		}

		@Override
		public void id(final Resource theResource) {
			mIdentifiable.id(theResource);
		}*/

		private EnumSet<TestEnum> mEnums = EnumSet.noneOf(TestEnum.class);

		public EnumSet<TestEnum> getEnums() {
			return mEnums;
		}

		public void setEnums(final EnumSet<TestEnum> theEnums) {
			mEnums = theEnums;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(mEnums);
		}

		@Override
		public boolean equals(final Object theObj) {
			if (theObj == this) {
				return true;
			}
			else if (theObj instanceof ClassWithEnumSet) {
				return Objects.equals(mEnums, ((ClassWithEnumSet) theObj).mEnums);
			}
			else {
				return false;
			}
		}
	}

	public static class ClassWithEnum implements Identifiable {
		private TestEnum mValue;

		private Identifiable mIdentifiable = new IdentifiableImpl();

		@Override
		public Resource id() {
			return mIdentifiable.id();
		}

		@Override
		public void id(final Resource theResource) {
			mIdentifiable.id(theResource);
		}

		public TestEnum getValue() {
			return mValue;
		}

		public void setValue(final TestEnum theValue) {
			mValue = theValue;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(mValue);
		}

		@Override
		public boolean equals(final Object theObj) {
			if (theObj == this) {
				return true;
			}
			else if (theObj instanceof ClassWithEnum) {
				ClassWithEnum aObj = (ClassWithEnum) theObj;

				return Objects.equals(mValue, aObj.mValue);
			}
			else {
				return false;
			}
		}
	}

	public static class ClassWithPrimitiveRdfList implements Identifiable {
		private List<Integer> mInts = Lists.newArrayList();

		private Identifiable mIdentifiable = new IdentifiableImpl();

		@Override
		public Resource id() {
			return mIdentifiable.id();
		}

		@Override
		public void id(final Resource theResource) {
			mIdentifiable.id(theResource);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(mInts);
		}

		@Override
		public boolean equals(final Object theObj) {
			if (theObj == this) {
				return true;
			}
			else if (theObj instanceof ClassWithPrimitiveLists) {
				ClassWithPrimitiveLists aObj = (ClassWithPrimitiveLists) theObj;

				return Objects.equals(mInts, aObj.mInts);
			}
			else {
				return false;
			}
		}

		@RdfProperty(isList = true)
		public List<Integer> getInts() {
			return mInts;
		}

		public void setInts(final List<Integer> theInts) {
			mInts = theInts;
		}
	}

	public static final class Person implements Comparable<Person>, Identifiable {
		private String mName;

		private Identifiable mIdentifiable = new IdentifiableImpl();

		public Person() {
		}

		public Person(final String theName) {
			mName = theName;
		}

		@Override
		public String toString() {
			return mName;
		}

		@Override
		public Resource id() {
			return mIdentifiable.id();
		}

		@Override
		public void id(final Resource theResource) {
			mIdentifiable.id(theResource);
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
				return Objects.equals(mName, ((Person) theObj).mName);
			}
			else {
				return false;
			}
		}

		@Override
		public int compareTo(final Person thePerson) {
			return mName.compareTo(thePerson.getName());
		}
	}


	/**
	 * We can get collections which have a different order than the original when we don't use the rdf:list
	 * serialized collection.  But order will matter in an .equals call.  in real usage, if order matters,
	 * users will use the rdf:list version.  for now, we just want to make sure the collections have the same
	 * elements and are of the same type.  that's sufficient.
	 */
	private static <T> boolean equalsTypeAndContents(Collection<T> theCollection, final Collection<T> theOther) {
		return (theCollection == null && theOther == null)
		       || (theCollection != null && theOther != null
		           && theCollection.getClass() == theOther.getClass()
		           && Sets.newHashSet(theCollection).equals(Sets.newHashSet(theOther)));
	}

	@Test
	public void checkCWOL() throws Exception {

		ClassWithObjectList cw = new ClassWithObjectList();
		cw.id(SimpleValueFactory.getInstance().createIRI("urn:ClassWithObjectList"));

		final Model md = RDFMapper.create().writeValue(cw);
		ClassWithObjectList des = RDFMapper.create().readValue(md, ClassWithObjectList.class);

		Person p = new Person();
		p.id(SimpleValueFactory.getInstance().createIRI("urn:Person"));
		List<Person> people = new ArrayList<Person>();
		people.add(p);
		des.setList(people);
		final Model modelWithPersonAdded = RDFMapper.create().writeValue(des);
		ClassWithObjectList des2 = RDFMapper.create().readValue(modelWithPersonAdded, ClassWithObjectList.class);


	}

	public static class ClassWithObjectList implements Identifiable {
		private List<Person> mList = Lists.newArrayList();

		private Set<Person> mSet = Sets.newLinkedHashSet();

		private Collection<Person> mCollection = Sets.newLinkedHashSet();

		private SortedSet<Person> mSortedSet = Sets.newTreeSet();

		private Identifiable mIdentifiable = new IdentifiableImpl();

		@Override
		public Resource id() {
			return mIdentifiable.id();
		}

		@Override
		public void id(final Resource theResource) {
			mIdentifiable.id(theResource);
		}

		@Override
		public int hashCode() {
			return Objects.hash(mList, mSet, mCollection, mSortedSet);
		}

		@Override
		public boolean equals(final Object theObj) {
			if (theObj == this) {
				return true;
			}
			else if (theObj instanceof ClassWithObjectList) {
				ClassWithObjectList aObj = (ClassWithObjectList) theObj;

				return equalsTypeAndContents(mList, aObj.mList)
				       && equalsTypeAndContents(mSet, aObj.mSet)
				       && equalsTypeAndContents(mCollection, aObj.mCollection)
				       && equalsTypeAndContents(mSortedSet, aObj.mSortedSet);
			}
			else {
				return false;
			}
		}

		public Collection<Person> getCollection() {
			return mCollection;
		}

		public void setCollection(final Collection<Person> theCollection) {
			mCollection = theCollection;
		}

		public List<Person> getList() {
			return mList;
		}

		public void setList(final List<Person> theList) {
			mList = theList;
		}

		public Set<Person> getSet() {
			return mSet;
		}

		public void setSet(final Set<Person> theSet) {
			mSet = theSet;
		}

		public SortedSet<Person> getSortedSet() {
			return mSortedSet;
		}

		public void setSortedSet(final SortedSet<Person> theSortedSet) {
			mSortedSet = theSortedSet;
		}
	}

	public static class ClassWithRdfObjectList implements Identifiable {
		private List<Person> mList = Lists.newArrayList();

		private Identifiable mIdentifiable = new IdentifiableImpl();

		@Override
		public Resource id() {
			return mIdentifiable.id();
		}

		@Override
		public void id(final Resource theResource) {
			mIdentifiable.id(theResource);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(mList);
		}

		@Override
		public boolean equals(final Object theObj) {
			if (theObj == this) {
				return true;
			}
			else if (theObj instanceof ClassWithRdfObjectList) {
				ClassWithRdfObjectList aObj = (ClassWithRdfObjectList) theObj;

				return Objects.equals(mList, aObj.mList);
			}
			else {
				return false;
			}
		}

		@RdfProperty(isList = true)
		public List<Person> getList() {
			return mList;
		}

		public void setList(final List<Person> theList) {
			mList = theList;
		}
	}

	public static class ClassWithPrimitiveLists implements Identifiable {
		private List<Integer> mInts = Lists.newArrayList();

		private Set<Float> mFloats = Sets.newLinkedHashSet();

		private Collection<Double> mDoubles = Sets.newLinkedHashSet();

		private SortedSet<Boolean> mBools = Sets.newTreeSet();

		private Identifiable mIdentifiable = new IdentifiableImpl();

		@Override
		public Resource id() {
			return mIdentifiable.id();
		}

		@Override
		public void id(final Resource theResource) {
			mIdentifiable.id(theResource);
		}

		@Override
		public int hashCode() {
			return Objects.hash(mInts, mFloats, mDoubles, mBools);
		}

		@Override
		public boolean equals(final Object theObj) {
			if (theObj == this) {
				return true;
			}
			else if (theObj instanceof ClassWithPrimitiveLists) {
				ClassWithPrimitiveLists aObj = (ClassWithPrimitiveLists) theObj;

				return equalsTypeAndContents(mInts, aObj.mInts)
				    && equalsTypeAndContents(mFloats, aObj.mFloats)
				    && equalsTypeAndContents(mDoubles, aObj.mDoubles)
				    && equalsTypeAndContents(mBools, aObj.mBools);
			}
			else {
				return false;
			}
		}

		public SortedSet<Boolean> getBools() {
			return mBools;
		}

		public void setBools(final SortedSet<Boolean> theBools) {
			mBools = theBools;
		}

		public Collection<Double> getDoubles() {
			return mDoubles;
		}

		public void setDoubles(final Collection<Double> theDoubles) {
			mDoubles = theDoubles;
		}

		public Set<Float> getFloats() {
			return mFloats;
		}

		public void setFloats(final Set<Float> theFloats) {
			mFloats = theFloats;
		}

		public List<Integer> getInts() {
			return mInts;
		}

		public void setInts(final List<Integer> theInts) {
			mInts = theInts;
		}
	}

	public static class ClassWithMixed implements Identifiable {
		private ClassWithPrimitives mChild;

		private String mString;

		private Identifiable mIdentifiable = new IdentifiableImpl();

		@Override
		public Resource id() {
			return mIdentifiable.id();
		}

		@Override
		public void id(final Resource theResource) {
			mIdentifiable.id(theResource);
		}

		public String getString() {
			return mString;
		}

		public void setString(final String theString) {
			mString = theString;
		}

		public ClassWithPrimitives getChild() {
			return mChild;
		}

		public void setChild(final ClassWithPrimitives theChild) {
			mChild = theChild;
		}

		@Override
		public int hashCode() {
			return Objects.hash(mString, mChild);
		}

		@Override
		public boolean equals(final Object theObj) {
			if (theObj == this) {
				return true;
			}
			else if (theObj instanceof ClassWithMixed) {
				ClassWithMixed aObj = (ClassWithMixed) theObj;
				return Objects.equals(mString, aObj.mString)
						&& Objects.equals(mChild, aObj.mChild);
			}
			else {
				return false;
			}
		}
	}

	public static class ClassWithPrimitives implements Identifiable {
		private String mString;
		private int mInt;
		private java.net.URI mURI;
		private float mFloat;
		private double mDouble;
		private char mChar;

		private Identifiable mIdentifiable = new IdentifiableImpl();

		@Override
		public Resource id() {
			return mIdentifiable.id();
		}

		@Override
		public void id(final Resource theResource) {
			mIdentifiable.id(theResource);
		}

		@Override
		public boolean equals(final Object theObj) {
			if (this == theObj) {
				return true;
			}
			if (theObj == null || getClass() != theObj.getClass()) {
				return false;
			}

			ClassWithPrimitives that = (ClassWithPrimitives) theObj;

			return mChar == that.mChar
			       && Double.compare(mDouble, that.mDouble) == 0
			       && Float.compare(mFloat, that.mFloat) == 0
			       && mInt == that.mInt
			       && Objects.equals(mString, that.mString)
			       && Objects.equals(mURI, that.mURI);
		}

		@Override
		public int hashCode() {
			return Objects.hash(mChar, mDouble, mFloat, mInt, mString, mURI);
		}

		public char getChar() {
			return mChar;
		}

		public void setChar(final char theChar) {
			mChar = theChar;
		}

		public double getDouble() {
			return mDouble;
		}

		public void setDouble(final double theDouble) {
			mDouble = theDouble;
		}

		public float getFloat() {
			return mFloat;
		}

		public void setFloat(final float theFloat) {
			mFloat = theFloat;
		}

		public int getInt() {
			return mInt;
		}

		public void setInt(final int theInt) {
			mInt = theInt;
		}

		public String getString() {
			return mString;
		}

		public void setString(final String theString) {
			mString = theString;
		}

		public URI getURI() {
			return mURI;
		}

		public void setURI(final URI theURI) {
			mURI = theURI;
		}
	}

	public static final class ClassWithMap {
		private Map<Object, Object> mMap;

		public Map<Object, Object> getMap() {
			return mMap;
		}

		public void setMap(final Map<Object, Object> theMap) {
			mMap = theMap;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(mMap);
		}

		@Override
		public boolean equals(final Object theObj) {
			if (theObj == this) {
				return true;
			}
			else if (theObj instanceof ClassWithMap) {
				return Objects.equals(mMap, ((ClassWithMap) theObj).mMap);
			}
			else {
				return false;
			}
		}
	}



}

