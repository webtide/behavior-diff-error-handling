package org.eclipse.jetty.demo.common;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.server.Server;

public interface JettySpecific
{
    Server newServer();

    HttpClient newClient();
}
