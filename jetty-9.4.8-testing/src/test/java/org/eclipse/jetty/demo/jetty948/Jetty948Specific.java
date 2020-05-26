package org.eclipse.jetty.demo.jetty948;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.demo.common.AbstractCommonTest;
import org.eclipse.jetty.demo.common.JettySpecific;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class Jetty948Specific implements JettySpecific
{
    @Override
    public Server newServer()
    {
        Server server = new Server();

        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setOutputBufferSize(AbstractCommonTest.OUTPUT_BUFFER_SIZE);
        httpConfiguration.setOutputAggregationSize(AbstractCommonTest.OUTPUT_AGGREGATION_SIZE);

        HttpConnectionFactory connectionFactory = new HttpConnectionFactory(httpConfiguration);
        ServerConnector connector = new ServerConnector(server, connectionFactory);
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
