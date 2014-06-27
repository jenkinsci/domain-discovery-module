package org.jenkinsci.modules.domain_discovery;

import com.google.common.net.InternetDomainName;
import hudson.Extension;
import hudson.model.PeriodicWork;
import hudson.util.IOUtils;
import hudson.util.TimeUnit2;
import jenkins.model.JenkinsLocationConfiguration;

import javax.inject.Inject;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static java.util.logging.Level.*;

/**
 * Based on the host name of Jenkins, find a suitable discovery node and if found, report our location.
 * This allows organizations to discover Jenkins instances running in their domains.
 *
 * <p>
 * For example, if the domain name is "myjenkins.foo.bar.com", then this class will try to report
 * our location to "discover-jenkins.foo.bar.com" or "discover-jenkins.bar.com".
 *
 * <p>
 * This code uses public suffix list to avoid reporting to "discover-jenkins.com", as that would
 * allow the owner of this regular domain name to collect Jenkins usages in all the .com domains, which
 * is clearly not what we want.
 *
 * <p>
 *
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class Pinger extends PeriodicWork {
    @Inject
    JenkinsLocationConfiguration loc;

    /**
     * Runs once a day.
     *
     * A long initial delay also helps avoid reporting a short-lived test instance.
     */
    @Override
    public long getRecurrencePeriod() {
        return TimeUnit2.DAYS.toMillis(1);
    }

    @Override
    protected void doRun() throws Exception {
        URL url = new URL(loc.getUrl());
        String hostName = url.getHost();
        handle(hostName);
    }

    protected void handle(String hostName) throws IOException {
        if (hostName.startsWith("[")) {
            LOGGER.fine("Literal IPv6 address: "+hostName);
            return;
        }
        if (IPV4_ADDRESS.matcher(hostName).matches()) {
            LOGGER.fine("Literal IPv4 address: "+hostName);
            return;
        }

        InternetDomainName n = InternetDomainName.from(hostName);
        while (true) {
            LOGGER.fine("Considering "+n);

            if (n.isPublicSuffix()) {
                LOGGER.fine(n+" is public suffix. done");
                break;
            }

            reportTo(n.child("discover-jenkins").name());

            if (!n.hasParent()) {
                LOGGER.fine("No more parents. done.");
                break;
            }
            n = n.parent();
        }
    }

    protected void reportTo(String name) throws IOException {
        try {
            URL url = new URL("http://" + name + "/");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("Referer",loc.getUrl());
            con.setRequestMethod("POST");
            con.connect();
            String o = IOUtils.toString(con.getInputStream());
            LOGGER.fine("POSTed to "+url+"\n"+o);
        } catch (UnknownHostException e) {
            LOGGER.fine("No such host name: "+name);
        } catch (IOException e) {
            LOGGER.log(FINE,"Failed to report our location", e);
        }
    }

    private static final Logger LOGGER = Logger.getLogger(Pinger.class.getName());
    private static final Pattern IPV4_ADDRESS = Pattern.compile("[\\d.]+");
}
