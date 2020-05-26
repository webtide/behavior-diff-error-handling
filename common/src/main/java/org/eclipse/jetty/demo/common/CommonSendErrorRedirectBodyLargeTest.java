package org.eclipse.jetty.demo.common;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.component.LifeCycle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public abstract class CommonSendErrorRedirectBodyLargeTest extends AbstractCommonTest
{
    private static final int OUTPUT_BUFFER_COUNT = 5;
    private static final int OUTPUT_BUFFER_WRITE_SIZE = OUTPUT_BUFFER_SIZE / (OUTPUT_BUFFER_COUNT - 1);

    private Server server;
    private HttpClient client;

    @BeforeEach
    public void setup() throws Exception
    {
        client = newClient();
        client.start();
    }

    @AfterEach
    public void teardown()
    {
        LifeCycle.stop(client);
        LifeCycle.stop(server);
    }

    public void startServer(Behavior behavior) throws Exception
    {
        server = newServer();

        ServletContextHandler contextHandler = new ServletContextHandler();
        contextHandler.setContextPath("/");
        contextHandler.addServlet(TossErrorServlet.class, "/toss/");
        contextHandler.addServlet(MyErrorServlet.class, "/error/");

        ErrorPageErrorHandler errorPageErrorHandler = new ErrorPageErrorHandler();
        errorPageErrorHandler.addErrorPage(301, "/error/");
        contextHandler.setErrorHandler(errorPageErrorHandler);

        switch (behavior)
        {
            case PLAIN:
                server.setHandler(contextHandler);
                break;
            case GZIP_EXTERNAL:
            {
                GzipHandler gzipHandler = newGzipHandler();
                gzipHandler.setHandler(contextHandler);
                server.setHandler(gzipHandler);
                break;
            }
            case GZIP_INTERNAL:
            {
                GzipHandler gzipHandler = newGzipHandler();
                contextHandler.setGzipHandler(gzipHandler);
                server.setHandler(contextHandler);
                break;
            }
        }

        server.start();
    }

    @ParameterizedTest
    @EnumSource(Behavior.class)
    public void testRedirect(Behavior behavior) throws Exception
    {
        startServer(behavior);
        ContentResponse response = client.GET(server.getURI().resolve("/toss/"));
        assertThat("response.status", response.getStatus(), is(301));
        assertThat("response[transfer-encoding]", response.getHeaders().get(HttpHeader.TRANSFER_ENCODING), is("chunked"));
        String responseBody = response.getContentAsString();
        assertThat("response.body", responseBody, containsString("Not here, go there: https://webtide.com/"));

        for (int i = 0; i <= OUTPUT_BUFFER_COUNT; i++)
        {
            char[] buf = new char[OUTPUT_BUFFER_WRITE_SIZE];
            Arrays.fill(buf, (char)('a' + i));
            assertThat("response.body", responseBody, containsString(new String(buf)));
        }
    }

    public static class TossErrorServlet extends HttpServlet
    {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
        {
            // Simply set the location header and then sendError
            resp.setHeader("Location", "https://webtide.com/");
            resp.sendError(301);
        }
    }

    public static class MyErrorServlet extends HttpServlet
    {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
        {
            resp.setContentType("text/plain");
            resp.setCharacterEncoding("UTF-8");

            // produce content above the aggregate buffer threshold
            PrintWriter writer = resp.getWriter();
            writer.println("Not here, go there: " + resp.getHeader("Location"));

            for (int i = 0; i <= OUTPUT_BUFFER_COUNT; i++)
            {
                char[] buf = new char[OUTPUT_BUFFER_WRITE_SIZE];
                Arrays.fill(buf, (char)('a' + i));
                writer.println(buf);
            }
        }
    }
}
