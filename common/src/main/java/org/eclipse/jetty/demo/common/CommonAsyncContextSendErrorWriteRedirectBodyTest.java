package org.eclipse.jetty.demo.common;

import java.io.IOException;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.LifeCycle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class CommonAsyncContextSendErrorWriteRedirectBodyTest extends AbstractCommonTest
{
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
        ServletHolder tossHolder = contextHandler.addServlet(TossErrorServlet.class, "/toss/");
        tossHolder.setAsyncSupported(true);
        ServletHolder errorHolder = contextHandler.addServlet(MyErrorServlet.class, "/error/");
        errorHolder.setAsyncSupported(true);

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
        HttpFields responseFields = response.getHeaders();
        assertThat("response[transfer-encoding]", responseFields.get(HttpHeader.TRANSFER_ENCODING), is(nullValue()));
        assertThat("response[Content-Length]", responseFields.get(HttpHeader.CONTENT_LENGTH), is(notNullValue()));
        String responseBody = response.getContentAsString();
        assertThat("response.body", responseBody, not(containsString("Not here, go there: https://webtide.com/")));
        assertThat("response.body", responseBody, not(containsString("<html><body>What the world needs now...</body></html>")));
    }

    public static void sendMovedPermanently(HttpServletResponse response, String redirectUrl) throws IOException
    {
        // Reset the response.
        // Clears out recently set headers
        // Resets the Output mode from STREAM or WRITER to UNSET.
        response.reset();

        // Set the location header (requirement for 302 redirect)
        response.setHeader("Location", redirectUrl);

        // TODO: make sure to change caching headers here if you need to

        // Set status for 302 redirect
        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);

        // Ensure that no other content is written by other processes
        // And force the response to commit
        response.getOutputStream().close();
        response.flushBuffer();
    }

    public static class TossErrorServlet extends HttpServlet
    {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
        {
            AsyncContext async = (AsyncContext)req.getAttribute("async.context");
            if (async == null)
            {
                async = req.startAsync(req, resp);
                req.setAttribute("async.context", async);
                async.setTimeout(10000);
                async.start(new AsyncRun(async, resp));
            }

            // Old way
            // resp.setHeader("Location", "https://webtide.com/");
            // resp.sendError(301);

            // Then send redirect
            sendMovedPermanently(resp, "https://webtide.com/");
        }

        public static class AsyncRun
            implements Runnable
        {
            private final AsyncContext async;
            private final HttpServletResponse resp;

            public AsyncRun(AsyncContext async, HttpServletResponse resp)
            {
                this.async = async;
                this.resp = resp;
            }

            @Override
            public void run()
            {
                resp.setContentType("text/html");
                resp.setCharacterEncoding("ISO-8859-1");
                // Write some content (below aggregate buffer threshold)
                ServletOutputStream out = null;
                try
                {
                    out = resp.getOutputStream();
                    out.println("<html><body>What the world needs now...</body></html>");
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
                finally
                {
                    async.complete();
                }
            }
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
            ServletOutputStream out = resp.getOutputStream();
            out.println("Not here, go there: " + resp.getHeader("Location"));
        }
    }
}
