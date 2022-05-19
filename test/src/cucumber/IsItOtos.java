package cucumber;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import static org.junit.Assert.*;

class IsItOtoscl {
    static boolean isItOtos() {
        return true;
    }
}

public class IsItOtos {
    int grade = 5;
    @Given("today is Vedes")
    public void today_is_Vedes() {}

    @When("We bemutatjuk a hazit")
    public void bemutatjuk_a_hazit() {}

    @Then("We get otos")
    public void get_otos() {
        assertEquals(IsItOtoscl.isItOtos(), true);
    }
}
