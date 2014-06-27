package org.jenkinsci.modules.domain_discovery;

import jenkins.model.JenkinsLocationConfiguration;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PingerTest {
    class Tester extends Pinger {
        List<String> names = new ArrayList<String>();
        @Override
        protected void reportTo(String name) throws IOException {
            names.add(name);
        }
    }

    /**
     * Verifies where the discovery information will be sent.
     */
    @Test
    public void handle() throws Exception {

        test("test.sfbay.sun.com",
            "discover-jenkins.test.sfbay.sun.com",
            "discover-jenkins.sfbay.sun.com",
            "discover-jenkins.sun.com"
            // NOT "discover-jenkins.com",
        );


        test("kohsuke.co.jp",
            "discover-jenkins.kohsuke.co.jp"
            // NOT "discover-jenkins.co.jp"
        );
    }

    private void test(String hostName, String... expected) throws IOException {
        Tester t = new Tester();
        t.handle(hostName);
        assertEquals(t.names, Arrays.asList(expected));
    }

    /**
     * Makes sure it can send data to somewhere.
     */
    @Test
    public void report() throws Exception {
        JenkinsLocationConfiguration loc = mock(JenkinsLocationConfiguration.class);
        Mockito.when(loc.getUrl()).thenReturn("http://bogus.com/");

        Pinger p = new Pinger();
        p.loc = loc;
        p.reportTo("google.com");
    }
}