package org.eclipse.jetty.demo.common;

import java.io.EOFException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpMethod;
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
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

public abstract class CommonWriteLargeSendErrorRedirectBodyTest extends AbstractCommonTest
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
        ExecutionException e = assertThrows(ExecutionException.class, () ->
            client.newRequest(server.getURI().resolve("/toss/"))
                .method(HttpMethod.GET)
                .send());

        assertThat("failure.cause", e.getCause(), instanceOf(EOFException.class));
    }

    public static class TossErrorServlet extends HttpServlet
    {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
        {
            // Write some content (above aggregate buffer threshold)
            resp.setContentType("text/html");
            resp.setCharacterEncoding("ISO-8859-1");
            PrintWriter writer = resp.getWriter();
            for (int i = 0; i <= OUTPUT_BUFFER_COUNT; i++)
            {
                char[] buf = new char[OUTPUT_BUFFER_WRITE_SIZE];
                Arrays.fill(buf, (char)('a' + i));
                writer.println(buf);
            }

            // Then attempt to use sendError
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

            // produce content below the aggregate buffer threshold
            PrintWriter writer = resp.getWriter();
            writer.println("Not here, go there: " + resp.getHeader("Location"));
        }
    }
}
