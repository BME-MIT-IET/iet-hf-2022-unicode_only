package cucumber;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import com.complexible.pinto.RDFMapper;
import com.complexible.pinto.RDFMapperTests;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.openrdf.model.Model;
import org.openrdf.model.impl.SimpleValueFactory;
import org.openrdf.model.util.Models;


import java.util.Random;

import static org.junit.Assert.*;

public class IsomorphicGraphsSteps {
    RDFMapperTests.ClassWithObjectList[] ObjList;
    RDFMapperTests.ClassWithObjectList aObj;
    RDFMapperTests.ClassWithObjectList aOtherObj;
    Random rand=new Random();
    Model FirstGraph;
    Model SecondGraph;
    @Given("there is a lot of data")
    public void createGraphWithSameData() {
        ObjList = new RDFMapperTests.ClassWithObjectList[100];
        for(int i=0;i<100;i++){
            ObjList[i]=new RDFMapperTests.ClassWithObjectList();
            ObjList[i].setCollection(Sets.newLinkedHashSet(
                    Lists.newArrayList(
                            new RDFMapperTests.Person("Person0"+i),
                            new RDFMapperTests.Person("Person1"+i))));
            ObjList[i].id(SimpleValueFactory.getInstance().
                    createIRI("tag:complexible:pinto:4f372f7bfb03f7b80be8777603d3b1ed"));
        }
    }


    @When("I ask whether two graphs from random different data are isomorphic")
    public void writeObjTwice() {
        aObj= ObjList[rand.nextInt(100)];
        aOtherObj=ObjList[rand.nextInt(100)];
        while(aObj==aOtherObj){
            aOtherObj=ObjList[rand.nextInt(100)];
        }
        FirstGraph = RDFMapper.create().writeValue(aObj);
        SecondGraph = RDFMapper.create().writeValue(aOtherObj);
    }


    @Then("Should be told they are not isomorphic")
    public void checkIfTheyAreIsomorphic() {
        assertFalse(Models.isomorphic(FirstGraph, SecondGraph));
    }

    @When("I ask whether two graphs from the same date are isomorphic")
    public void iAskWhetherTwoGraphsFromTheSameDateAreIsomorphic() {
        aObj= ObjList[0];
        FirstGraph = RDFMapper.create().writeValue(aObj);
        SecondGraph = RDFMapper.create().writeValue(aObj);
    }

    @Then("Should be told they are isomorphic")
    public void shouldBeToldTheyAreIsomorphic() {
        assertTrue(Models.isomorphic(FirstGraph, SecondGraph));
    }
}