package org.eclipse.jetty.demo.common;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.component.LifeCycle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public abstract class CommonSendErrorWriteRedirectBodyTest extends AbstractCommonTest
{
    private Server server;
    private HttpClient client;

    @BeforeEach
    public void setup() throws Exception
    {
        server = newServer();

        ServletContextHandler contextHandler = new ServletContextHandler();
        contextHandler.setContextPath("/");
        contextHandler.addServlet(TossErrorServlet.class, "/toss/");
        contextHandler.addServlet(MyErrorServlet.class, "/error/");

        ErrorPageErrorHandler errorPageErrorHandler = new ErrorPageErrorHandler();
        errorPageErrorHandler.addErrorPage(301, "/error/");
        contextHandler.setErrorHandler(errorPageErrorHandler);

        server.setHandler(contextHandler);
        server.start();

        client = newClient();
        client.start();
    }

    @AfterEach
    public void teardown()
    {
        LifeCycle.stop(client);
        LifeCycle.stop(server);
    }

    @Test
    public void testRedirect() throws InterruptedException, ExecutionException, TimeoutException
    {
        ContentResponse response = client.GET(server.getURI().resolve("/toss/"));
        assertThat("response.status", response.getStatus(), is(301));
        assertThat("response[transfer-encoding]", response.getHeaders().get(HttpHeader.TRANSFER_ENCODING), is(nullValue()));
        assertThat("response[Content-Length]", response.getHeaders().get(HttpHeader.CONTENT_LENGTH), is(notNullValue()));
        String responseBody = response.getContentAsString();
        assertThat("response.body", responseBody, containsString("Not here, go there: https://webtide.com/"));
        assertThat("response.body", responseBody, not(containsString("<html><body>What the world needs now...</body></html>")));
    }

    public static class TossErrorServlet extends HttpServlet
    {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
        {
            // Then send use error
            resp.setHeader("Location", "https://webtide.com/");
            resp.sendError(301);

            // Write some content (below aggregate buffer threshold)
            resp.setContentType("text/html");
            resp.setCharacterEncoding("ISO-8859-1");
            resp.getWriter().println("<html><body>What the world needs now...</body></html>");
        }
    }

    public static class MyErrorServlet extends HttpServlet
    {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
        {
            resp.setContentType("text/plain");
            resp.setCharacterEncoding("UTF-8");

            // produce content below the aggregate buffer threshold
            PrintWriter writer = resp.getWriter();
            writer.println("Not here, go there: " + resp.getHeader("Location"));
        }
    }
}
