/*
 * Copyright (c) 2002-2003 by OpenSymphony
 * All rights reserved.
 */
package com.opensymphony.webwork.views.jsp;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.servlet.MockServletConfig;
import com.opensymphony.webwork.ServletActionContext;
import com.opensymphony.webwork.views.velocity.VelocityManager;
import com.opensymphony.webwork.views.freemarker.FreemarkerManager;
import com.opensymphony.xwork.ActionContext;
import org.apache.velocity.app.Velocity;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.StringTokenizer;


/**
 * @author Matt Ho <a href="mailto:matt@indigoegg.com">&lt;matt@indigoegg.com&gt;</a>
 * @version $Id$
 */
public abstract class AbstractUITagTest extends AbstractTagTest {
    //~ Methods ////////////////////////////////////////////////////////////////

    /**
     * Attempt to verify the contents of this.writer against the contents of the URL specified.  verify() performs a
     * trim on both ends
     *
     * @param url the HTML snippet that we want to validate against
     * @throws Exception if the validation failed
     */
    public void verify(URL url) throws Exception {
        if (url == null) {
            fail("unable to verify a null URL");
        } else if (this.writer == null) {
            fail("AbstractJspWriter.writer not initialized.  Unable to verify");
        }

        StringBuffer buffer = new StringBuffer(128);
        InputStream in = url.openStream();
        byte[] buf = new byte[4096];
        int nbytes;

        while ((nbytes = in.read(buf)) > 0) {
            buffer.append(new String(buf, 0, nbytes));
        }

        in.close();

        /**
         * compare the trimmed values of each buffer and make sure they're equivalent.  however, let's make sure to
         * normalize the strings first to account for line termination differences between platforms.
         */
        String writerString = writer.toString();
        String bufferString = buffer.toString();

        assertEquals(bufferString, writerString);
    }

    protected void setUp() throws Exception {
        super.setUp();

        MockServletConfig config = new MockServletConfig();

        ServletContext servletContext = pageContext.getServletContext();
        config.setServletContext(servletContext);
        config.setInitParameter("resource.loader", "file1,file2");

        ServletActionContext.setServletConfig(config);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        ActionContext.setContext(null);
    }

    /**
     * normalizes a string so that strings generated on different platforms can be compared.  any group of one or more
     * space, tab, \r, and \n characters are converted to a single space character
     *
     * @param obj the object to be normalized.  normalize will perform its operation on obj.toString().trim() ;
     * @return the normalized string
     */
    private String normalize(Object obj) {
        StringTokenizer st = new StringTokenizer(obj.toString().trim(), " \t\r\n");
        StringBuffer buffer = new StringBuffer(128);

        while (st.hasMoreTokens()) {
            buffer.append(st.nextToken());

            if (st.hasMoreTokens()) {
                buffer.append(" ");
            }
        }

        return buffer.toString();
    }
}
