package org.eclipse.jetty.demo.jetty9425;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.demo.common.CommonSendErrorRedirectBodyTest;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class SendErrorRedirectBodyTest extends CommonSendErrorRedirectBodyTest
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
        SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
        HttpClient client = new HttpClient(sslContextFactory);
        client.setFollowRedirects(false);
        return client;
    }
}
