package test;


import org.junit.Assert;
import org.junit.Test;


public class MainTest {

    @Test
    public void testHelloToZXJ() {
        Assert.assertEquals("Hello ZXJ", new Main().helloToZXJ());
    }

}
