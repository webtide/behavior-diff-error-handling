package org.eclipse.jetty.demo.common;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.server.Server;

public abstract class AbstractCommonTest
{
    public abstract Server newServer();

    public abstract HttpClient newClient();
}
