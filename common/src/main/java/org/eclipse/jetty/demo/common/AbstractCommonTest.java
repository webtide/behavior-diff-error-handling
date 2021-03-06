package org.eclipse.jetty.demo.common;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;

public abstract class AbstractCommonTest
{
    public static final int OUTPUT_BUFFER_SIZE = 8 * 1024;
    public static final int OUTPUT_AGGREGATION_SIZE = OUTPUT_BUFFER_SIZE / 4;

    public enum Behavior
    {
        PLAIN,
        GZIP_INTERNAL,
        GZIP_EXTERNAL
    }

    public GzipHandler newGzipHandler()
    {
        GzipHandler gzipHandler = new GzipHandler();
        gzipHandler.setMinGzipSize(10);
        return gzipHandler;
    }

    public Server newServer()
    {
        return getJettySpecific().newServer();
    }

    public HttpClient newClient()
    {
        return getJettySpecific().newClient();
    }

    private static JettySpecific getJettySpecific()
    {
        Iterator<JettySpecific> iterJettySpecific = ServiceLoader.load(JettySpecific.class).iterator();
        while (iterJettySpecific.hasNext())
        {
            JettySpecific specific = iterJettySpecific.next();
            if (specific != null)
                return specific;
        }
        throw new RuntimeException("Unable to find JettySpecific");
    }
}
