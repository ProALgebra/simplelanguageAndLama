package com.oracle.truffle.lama.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class LamaRegressionTest {
    private final String testName;

    public LamaRegressionTest(String testName) {
        this.testName = testName;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() throws IOException {
        List<Object[]> data = new ArrayList<>();
        for (String name : LamaRegressionSupport.listCaseNames()) {
            data.add(new Object[]{name});
        }
        return data;
    }

    @Test
    public void runRegressionCase() throws IOException {
        LamaRegressionSupport.runCase("lama", testName);
    }
}
