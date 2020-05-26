package org.eclipse.jetty.demo.jetty948;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.demo.common.AbstractSendErrorRedirectBodyTest;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class SendErrorRedirectBodyTest extends AbstractSendErrorRedirectBodyTest
{
    @Override
    public Server newServer()
    {
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(0);
        server.addConnector(connector);
        return server;
    }

    @Override
    public HttpClient newClient()
    {
        SslContextFactory sslContextFactory = new SslContextFactory();
        HttpClient client = new HttpClient(sslContextFactory);
        client.setFollowRedirects(false);
        return client;
    }
}
