package cucumber;

import com.complexible.common.openrdf.model.ModelIO;
import com.complexible.pinto.RDFMapper;
import com.complexible.pinto.RDFMapperTests;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openrdf.model.Model;
import org.openrdf.model.impl.SimpleValueFactory;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReadWriteEnumSteps {
    RDFMapperTests.ClassWithEnum aObj;
    Model aGraph;

    @Given("ClassWithEnum object with invalid IRI")
    public void createEnumWithInvalidIRI() {
    aObj= new RDFMapperTests.ClassWithEnum();
    aObj.id(SimpleValueFactory.getInstance().createIRI("urn:testWriteEnumInvalidIri"));
    aObj.setValue(RDFMapperTests.TestEnum.Foo);

    }


    @When("I try to create Model with given enum")
    public void createModel() {
        aGraph = RDFMapper.create().writeValue(aObj);
    }


    @Then("Model should be empty")
    public void checkIfModelIsEmpty() {
        assertTrue(aGraph.isEmpty());
    }


    //2nd test variables
    Model bGraph;
    RDFMapperTests.ClassWithEnum bExpected ;
    RDFMapperTests.ClassWithEnum bResult ;

    @Given("Graph read from a file")
    public void graphInFile() throws IOException {
        bGraph = ModelIO.read(RDFMapperTests.Files3.classPath("/data/enum.nt").toPath());
        bExpected= new RDFMapperTests.ClassWithEnum();
        bExpected.id(SimpleValueFactory.getInstance().createIRI("urn:testReadEnum"));
        bExpected.setValue(RDFMapperTests.TestEnum.Bar);
    }


    @When("Create with the read object")
    public void readFromFile() {
        bResult = RDFMapper.create().readValue(bGraph, RDFMapperTests.ClassWithEnum.class);
    }


    @Then("ClassWithEnum object is created")
    public void checkIfObjectCreated() {
        assertEquals(bExpected, bResult);
    }
}
