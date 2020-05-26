package org.eclipse.jetty.demo.common;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
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

public abstract class CommonGetOutputStreamSendErrorRedirectBodyLargeTest extends AbstractCommonTest
{
    private static final int OUTPUT_BUFFER_COUNT = 5;
    private static final int OUTPUT_BUFFER_WRITE_SIZE = OUTPUT_BUFFER_SIZE / (OUTPUT_BUFFER_COUNT - 1);

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
            // First get the output stream (but do nothing with it)
            resp.getOutputStream();
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
            ServletOutputStream outputStream = resp.getOutputStream();
            outputStream.println("Not here, go there: " + resp.getHeader("Location"));

            for (int i = 0; i <= OUTPUT_BUFFER_COUNT; i++)
            {
                byte[] buf = new byte[OUTPUT_BUFFER_WRITE_SIZE];
                Arrays.fill(buf, (byte)('a' + i));
                outputStream.write(buf);
                outputStream.println();
            }
        }
    }
}
