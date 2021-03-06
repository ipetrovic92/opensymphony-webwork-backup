/*
 * Copyright (c) 2002-2007 by OpenSymphony
 * All rights reserved.
 */
package com.opensymphony.webwork.dwr;

import org.directwebremoting.extend.Creator;
import org.directwebremoting.impl.DefaultAccessControl;

import java.lang.reflect.Method;

/**
 * Control who should be accessing which methods on which classes, allowing
 * WebWork to access classes under org.directwebremoting.webwork package.
 * <p/>
 * To configure this, add the following in web.xml
 * <pre>
 * &lt;servlet&gt;
 *       &lt;servlet-name&gt;dwr&lt;/servlet-name&gt;
 *       &lt;servlet-class&gt;org.directwebremoting.servlet.DwrServlet&lt;/servlet-class&gt;
 *       &lt;init-param&gt;
 *           &lt;param-name&gt;debug&lt;/param-name&gt;
 *           &lt;param-value&gt;true&lt;/param-value&gt;
 *       &lt;/init-param&gt;
 *       &lt;init-param&gt;
 *           &lt;param-name&gt;org.directwebremoting.extend.AccessControl&lt;/param-name&gt;
 *           &lt;param-value&gt;com.opensymphony.webwork.dwr.WebWorkDwrAccessControl&lt;/param-value&gt;
 *       &lt;/init-param&gt;
 *   &lt;/servlet&gt;
 *
 *   &lt;servlet-mapping&gt;
 *       &lt;servlet-name&gt;dwr&lt;/servlet-name&gt;
 *       &lt;url-pattern&gt;/dwr/*&lt;/url-pattern&gt;
 *   &lt;/servlet-mapping&gt;
 * </pre>
 *
 * @author tmjee
 * @version $Date$ $Id$
 */
public class WebWorkDwrAccessControl extends DefaultAccessControl {

    public static final String WEBWORK_INTEGRATION_PACKAGE = "org.directwebremoting.webwork";

    /**
     * Check the parameters are not DWR internal but allows it if its webwork integration
     * {@link #WEBWORK_INTEGRATION_PACKAGE}.
     * 
     * @param method The method that we want to execute
     */
    protected void assertAreParametersDwrInternal(Method method) {
        for (int j = 0; j < method.getParameterTypes().length; j++) {
            Class paramType = method.getParameterTypes()[j];
            if (paramType.getName().startsWith(WEBWORK_INTEGRATION_PACKAGE)) {
                // we allow access to WebWork integration stuff.
                return;
            }
            else {
                super.assertAreParametersDwrInternal(method);
            }
        }
    }

    /**
     * Is the class that we are executing a method on part of DWR? if so deny but if its
     * WebWork integration stuff, allows it {@link #WEBWORK_INTEGRATION_PACKAGE}.
     * 
     * @param creator The {@link Creator} that exposes the class
     */
    protected void assertIsClassDwrInternal(Creator creator) {
        if (creator.getType().getName().startsWith(WEBWORK_INTEGRATION_PACKAGE)) {
            // we allow access to WebWork integration stuff.
            return;
        }
        else {
            super.assertIsClassDwrInternal(creator);
        }
    }
}
