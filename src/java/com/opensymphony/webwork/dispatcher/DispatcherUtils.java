/*
 * Copyright (c) 2002-2006 by OpenSymphony
 * All rights reserved.
 */
package com.opensymphony.webwork.dispatcher;

import com.opensymphony.util.ClassLoaderUtil;
import com.opensymphony.util.FileManager;
import com.opensymphony.webwork.ServletActionContext;
import com.opensymphony.webwork.WebWorkStatics;
import com.opensymphony.webwork.WebWorkConstants;
import com.opensymphony.webwork.views.freemarker.FreemarkerManager;
import com.opensymphony.webwork.config.Configuration;
import com.opensymphony.webwork.dispatcher.mapper.ActionMapping;
import com.opensymphony.webwork.dispatcher.multipart.MultiPartRequest;
import com.opensymphony.webwork.dispatcher.multipart.MultiPartRequestWrapper;
import com.opensymphony.webwork.util.AttributeMap;
import com.opensymphony.webwork.util.ObjectFactoryDestroyable;
import com.opensymphony.webwork.util.ObjectFactoryInitializable;
import com.opensymphony.xwork.*;
import com.opensymphony.xwork.config.ConfigurationException;
import com.opensymphony.xwork.config.ConfigurationManager;
import com.opensymphony.xwork.interceptor.component.ComponentInterceptor;
import com.opensymphony.xwork.interceptor.component.ComponentManager;
import com.opensymphony.xwork.util.*;
import com.opensymphony.xwork.util.location.Location;
import com.opensymphony.xwork.util.location.LocationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.*;

import freemarker.template.Template;

/**
 * A utility class whereby FilterDispatcher delegate most of its tasks to. A static
 * singleton that gets initlialized upon the call to it's
 * <code>initalize(ServletContext)</code>
 * method
 *
 * @author patrick
 * @author Rainer Hermanns
 * @author tm_jee
 * @version $Date$ $Id$
 *
 * @see com.opensymphony.webwork.dispatcher.FilterDispatcher
 */
public class DispatcherUtils {
    private static final Log LOG = LogFactory.getLog(DispatcherUtils.class);

    private static DispatcherUtils instance;

    private static boolean portletSupportActive;

    public static void initialize(ServletContext servletContext) {
        synchronized (DispatcherUtils.class) {
            if (instance == null) {
                instance = new DispatcherUtils(servletContext);
            }
        }
    }

    public static DispatcherUtils getInstance() {
        return instance;
    }

    public static void setInstance(DispatcherUtils instance) {
        DispatcherUtils.instance = instance;
    }

    protected boolean devMode = false;

    // used to get WebLogic to play nice
    protected boolean paramsWorkaroundEnabled = false;

    protected DispatcherUtils(ServletContext servletContext) {
        init(servletContext);
    }

    public void cleanup() {
        ObjectFactory objectFactory = ObjectFactory.getObjectFactory();
        
    	// inform ShutDownListener, we are shuting down
        if (Configuration.isSet(WebWorkConstants.WEBWORK_DISPATCHER_SHUTDOWN_LISTENER)) {
        	String[] shutdownListenerClassNames = Configuration.getString(WebWorkConstants.WEBWORK_DISPATCHER_SHUTDOWN_LISTENER).split(",");
        	for (int a=0; a<shutdownListenerClassNames.length; a++) {
        		String shutdownListenerClassName = shutdownListenerClassNames[a].trim();
        		try {
                    ShutDownListener shutDownListener = (ShutDownListener) objectFactory.buildBean(shutdownListenerClassName, Collections.EMPTY_MAP);
        			if (LOG.isDebugEnabled()) {
        				LOG.debug("notifying shutdown listener ["+shutDownListener+"]");
        			}
        			shutDownListener.shutdown();
        		}
        		catch(Exception e) { // we might also get ClassCastException
        			LOG.warn("shutdown listener ["+shutdownListenerClassName+"] failed to be initialized, it will be ignored", e);
        		}
        	}
        }
    	
    	
    	
    	// clean up ObjectFactory
        if (objectFactory == null) {
            LOG.warn("Object Factory is null, something is seriously wrong, no clean up will be performed");
        }
        if (objectFactory instanceof ObjectFactoryDestroyable) {
            try {
                ((ObjectFactoryDestroyable)objectFactory).destroy();
            }
            catch(Exception e) {
                // catch any exception that may occured during destroy() and log it
                LOG.error("exception occurred while destroying ObjectFactory ["+objectFactory+"]", e);
            }
        }
        
        // clean up ConfigurationManager
        ConfigurationManager.destroyConfiguration();
    }

    protected void init(ServletContext servletContext) {
        boolean reloadi18n = Boolean.valueOf((String) Configuration.get(WebWorkConstants.WEBWORK_I18N_RELOAD)).booleanValue();
        LocalizedTextUtil.setReloadBundles(reloadi18n);

        // initialize ObjectFactory
        if (Configuration.isSet(WebWorkConstants.WEBWORK_OBJECTFACTORY)) {
            String className = (String) Configuration.get(WebWorkConstants.WEBWORK_OBJECTFACTORY);
            if (className.equals("spring")) {
                // note: this class name needs to be in string form so we don't put hard
                //       dependencies on spring, since it isn't technically required.
                className = "com.opensymphony.webwork.spring.WebWorkSpringObjectFactory";
            } else if (className.equals("plexus")) {
                // note: this class name needs to be in string form so we don't put hard
                //       dependencies on spring, since it isn't technically required.
                className = "com.opensymphony.webwork.plexus.PlexusObjectFactory";
            }

            try {
                Class clazz = ClassLoaderUtil.loadClass(className, DispatcherUtils.class);
                ObjectFactory objectFactory = (ObjectFactory) clazz.newInstance();
                if (objectFactory instanceof ObjectFactoryInitializable) {
                    ((ObjectFactoryInitializable) objectFactory).init(servletContext);
                }
                ObjectFactory.setObjectFactory(objectFactory);
            } catch (Exception e) {
                LOG.error("Could not load ObjectFactory named " + className + ". Using default ObjectFactory.", e);
            }
        }
       

        
        // Intialize ObjecTypeDeterminer
        if (Configuration.isSet(WebWorkConstants.WEBWORK_OBJECTTYPEDETERMINER)) {
            String className = (String) Configuration.get(WebWorkConstants.WEBWORK_OBJECTTYPEDETERMINER);
            if (className.equals("tiger")) {
                // note: this class name needs to be in string form so we don't put hard
                //       dependencies on xwork-tiger, since it isn't technically required.
                className = "com.opensymphony.xwork.util.GenericsObjectTypeDeterminer";
            }
            else if (className.equals("notiger")) {
                className = "com.opensymphony.xwork.util.DefaultObjectTypeDeterminer";
            }

            try {
                Class clazz = ClassLoaderUtil.loadClass(className, DispatcherUtils.class);
                ObjectTypeDeterminer objectTypeDeterminer = (ObjectTypeDeterminer) clazz.newInstance();
                ObjectTypeDeterminerFactory.setInstance(objectTypeDeterminer);
            } catch (Exception e) {
                LOG.error("Could not load ObjectTypeDeterminer named " + className + ". Using default DefaultObjectTypeDeterminer.", e);
            }
        }

        if ("true".equals(Configuration.get(WebWorkConstants.WEBWORK_DEVMODE))) {
            devMode = true;
            Configuration.set(WebWorkConstants.WEBWORK_I18N_RELOAD, "true");
            Configuration.set(WebWorkConstants.WEBWORK_CONFIGURATION_XML_RELOAD, "true");
        }

        //check for configuration reloading
        if (Configuration.isSet(WebWorkConstants.WEBWORK_CONFIGURATION_XML_RELOAD) &&
                "true".equalsIgnoreCase(Configuration.getString(WebWorkConstants.WEBWORK_CONFIGURATION_XML_RELOAD))) {
            FileManager.setReloadingConfigs(true);
        }

        if (Configuration.isSet(WebWorkConstants.WEBWORK_CONTINUATIONS_PACKAGE)) {
            String pkg = Configuration.getString(WebWorkConstants.WEBWORK_CONTINUATIONS_PACKAGE);
            ObjectFactory.setContinuationPackage(pkg);
        }

        // test wether param-access workaround needs to be enabled
        if (servletContext.getServerInfo().indexOf("WebLogic") >= 0) {
            LOG.info("WebLogic server detected. Enabling WebWork parameter access work-around.");
            paramsWorkaroundEnabled = true;
        } else if (Configuration.isSet(WebWorkConstants.WEBWORK_DISPATCHER_PARAMETERSWORKAROUND)) {
            paramsWorkaroundEnabled = "true".equals(Configuration.get(WebWorkConstants.WEBWORK_DISPATCHER_PARAMETERSWORKAROUND));
        } else {
            LOG.debug("Parameter access work-around disabled.");
        }
        
        // inform startup listeners
        if (Configuration.isSet(WebWorkConstants.WEBWORK_DISPATCHER_START_UP_LISTENER)) {
        	String[] startupListenerClassNames = Configuration.getString(WebWorkConstants.WEBWORK_DISPATCHER_START_UP_LISTENER).split(",");
        	for (int a=0; a<startupListenerClassNames.length; a++) {
        		String startupListenerClassName = startupListenerClassNames[a].trim();
        		try {
                    ObjectFactory objectFactory = ObjectFactory.getObjectFactory();
                    StartUpListener startUpListener = (StartUpListener) objectFactory.buildBean(startupListenerClassName, Collections.EMPTY_MAP);
        			if (LOG.isDebugEnabled()) {
        				LOG.debug("notifying start up listener ["+startUpListener+"]");
        			}
        			startUpListener.startup();
        		}
        		catch(Exception e) { // we might also get ClassCastException
        			LOG.warn("startup listener ["+startupListenerClassName+"] failed to be initialized, it will be ignored", e);
        		}
        	}
        }

    }

    /**
     * Loads the action and executes it. This method first creates the action context from the given
     * parameters then loads an <tt>ActionProxy</tt> from the given action name and namespace. After that,
     * the action is executed and output channels throught the response object. Actions not found are
     * sent back to the user via the {@link DispatcherUtils#sendError} method, using the 404 return code.
     * All other errors are reported by throwing a ServletException.
     *
     * @param request  the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @param mapping  the action mapping object
     * @throws ServletException when an unknown error occurs (not a 404, but typically something that
     *                          would end up as a 5xx by the servlet container)
     */
    public void serviceAction(HttpServletRequest request, HttpServletResponse response, ServletContext context, ActionMapping mapping) throws ServletException {
        Map extraContext = createContextMap(request, response, mapping, context);

        // If there was a previous value stack, then create a new copy and pass it in to be used by the new Action
        OgnlValueStack stack = (OgnlValueStack) request.getAttribute(ServletActionContext.WEBWORK_VALUESTACK_KEY);
        if (stack != null) {
            extraContext.put(ActionContext.VALUE_STACK, new OgnlValueStack(stack));
        }

        try {
            String namespace = mapping.getNamespace();
            String name = mapping.getName();
            String method = mapping.getMethod();

            String id = request.getParameter(XWorkContinuationConfig.CONTINUE_PARAM);
            if (id != null) {
                // remove the continue key from the params - we don't want to bother setting
                // on the value stack since we know it won't work. Besides, this breaks devMode!
                Map params = (Map) extraContext.get(ActionContext.PARAMETERS);
                params.remove(XWorkContinuationConfig.CONTINUE_PARAM);

                // and now put the key in the context to be picked up later by XWork
                extraContext.put(XWorkContinuationConfig.CONTINUE_KEY, id);
            }

            ActionProxy proxy = ActionProxyFactory.getFactory().createActionProxy(namespace, name, extraContext, true, false);
            proxy.setMethod(method);
            request.setAttribute(ServletActionContext.WEBWORK_VALUESTACK_KEY, proxy.getInvocation().getStack());

            // if the ActionMapping says to go straight to a result, do it!
            if (mapping.getResult() != null) {
                Result result = mapping.getResult();
                result.execute(proxy.getInvocation());
            } else {
                proxy.execute();
            }

            // If there was a previous value stack then set it back onto the request
            if (stack != null) {
                request.setAttribute(ServletActionContext.WEBWORK_VALUESTACK_KEY, stack);
            }
        } catch (ConfigurationException e) {
            LOG.error("Could not find action", e);
            sendError(request, response, context, HttpServletResponse.SC_NOT_FOUND, e);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    public Map createContextMap(HttpServletRequest request, HttpServletResponse response, ActionMapping mapping, ServletContext context) {
        // request map wrapping the http request objects
        Map requestMap = new RequestMap(request);

        // parameters map wrapping the http paraneters.
        Map params = null;
        if (mapping != null) {
            params = mapping.getParams();
        }
        Map requestParams = new HashMap(request.getParameterMap());
        if (params != null) {
            params.putAll(requestParams);
        } else {
            params = requestParams;
        }

        // session map wrapping the http session
        Map session = new SessionMap(request);

        // application map wrapping the ServletContext
        Map application = new ApplicationMap(context);

        return createContextMap(requestMap, params, session, application, request, response, context);
    }

    /**
     * Merges all application and servlet attributes into a single <tt>HashMap</tt> to represent the entire
     * <tt>Action</tt> context.
     *
     * @param requestMap     a Map of all request attributes.
     * @param parameterMap   a Map of all request parameters.
     * @param sessionMap     a Map of all session attributes.
     * @param applicationMap a Map of all servlet context attributes.
     * @param request        the HttpServletRequest object.
     * @param response       the HttpServletResponse object.
     * @param servletContext the ServletContext object.
     * @return a HashMap representing the <tt>Action</tt> context.
     */
    public HashMap createContextMap(Map requestMap,
                                    Map parameterMap,
                                    Map sessionMap,
                                    Map applicationMap,
                                    HttpServletRequest request,
                                    HttpServletResponse response,
                                    ServletContext servletContext) {
        HashMap extraContext = new HashMap();
        extraContext.put(ActionContext.PARAMETERS, new HashMap(parameterMap));
        extraContext.put(ActionContext.SESSION, sessionMap);
        extraContext.put(ActionContext.APPLICATION, applicationMap);

        Locale locale = null;
        if (Configuration.isSet(WebWorkConstants.WEBWORK_LOCALE)) {
            locale = LocalizedTextUtil.localeFromString(Configuration.getString(WebWorkConstants.WEBWORK_LOCALE), request.getLocale());
        } else {
            locale = request.getLocale();
        }

        extraContext.put(ActionContext.LOCALE, locale);
        extraContext.put(ActionContext.DEV_MODE, Boolean.valueOf(devMode));

        extraContext.put(WebWorkStatics.HTTP_REQUEST, request);
        extraContext.put(WebWorkStatics.HTTP_RESPONSE, response);
        extraContext.put(WebWorkStatics.SERVLET_CONTEXT, servletContext);
        extraContext.put(ComponentInterceptor.COMPONENT_MANAGER, request.getAttribute(ComponentManager.COMPONENT_MANAGER_KEY));

        // helpers to get access to request/session/application scope
        extraContext.put("request", requestMap);
        extraContext.put("session", sessionMap);
        extraContext.put("application", applicationMap);
        extraContext.put("parameters", parameterMap);

        AttributeMap attrMap = new AttributeMap(extraContext);
        extraContext.put("attr", attrMap);

        return extraContext;
    }

    /**
     * Returns the maximum upload size allowed for multipart requests (this is configurable).
     *
     * @return the maximum upload size allowed for multipart requests
     */
    public static int getMaxSize() {
        Integer maxSize = new Integer(Integer.MAX_VALUE);
        try {
            String maxSizeStr = Configuration.getString(WebWorkConstants.WEBWORK_MULTIPART_MAXSIZE);

            if (maxSizeStr != null) {
                try {
                    maxSize = new Integer(maxSizeStr);
                } catch (NumberFormatException e) {
                    LOG.warn("Unable to format 'webwork.multipart.maxSize' property setting. Defaulting to Integer.MAX_VALUE");
                }
            } else {
                LOG.warn("Unable to format 'webwork.multipart.maxSize' property setting. Defaulting to Integer.MAX_VALUE");
            }
        } catch (IllegalArgumentException e1) {
            LOG.warn("Unable to format 'webwork.multipart.maxSize' property setting. Defaulting to Integer.MAX_VALUE");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("maxSize=" + maxSize);
        }

        return maxSize.intValue();
    }

    /**
     * Returns the path to save uploaded files to (this is configurable).
     *
     * @return the path to save uploaded files to
     */
    public String getSaveDir(ServletContext servletContext) {
        String saveDir = Configuration.getString(WebWorkConstants.WEBWORK_MULTIPART_SAVEDIR).trim();

        if (saveDir.equals("")) {
            File tempdir = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
            LOG.info("Unable to find 'webwork.multipart.saveDir' property setting. Defaulting to javax.servlet.context.tempdir");

            if (tempdir != null) {
                saveDir = tempdir.toString();
            }
        } else {
            File multipartSaveDir = new File(saveDir);

            if (!multipartSaveDir.exists()) {
                multipartSaveDir.mkdir();
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("saveDir=" + saveDir);
        }

        return saveDir;
    }

    public void prepare(HttpServletRequest request, HttpServletResponse response) {
        String encoding = null;
        if (Configuration.isSet(WebWorkConstants.WEBWORK_I18N_ENCODING)) {
            encoding = Configuration.getString(WebWorkConstants.WEBWORK_I18N_ENCODING);
        }

        Locale locale = null;
        if (Configuration.isSet(WebWorkConstants.WEBWORK_LOCALE)) {
            locale = LocalizedTextUtil.localeFromString(Configuration.getString(WebWorkConstants.WEBWORK_LOCALE), request.getLocale());
        }

        if (encoding != null) {
            try {
                request.setCharacterEncoding(encoding);
            } catch (Exception e) {
                LOG.error("Error setting character encoding to '" + encoding + "' - ignoring.", e);
            }
        }

        if (locale != null) {
            response.setLocale(locale);
        }

        if (paramsWorkaroundEnabled) {
            request.getParameter("foo"); // simply read any parameter (existing or not) to "prime" the request
        }
    }

    /**
     * Wraps and returns the given response or returns the original response object. This is used to transparently
     * handle multipart data as a wrapped class around the given request. Override this method to handle multipart
     * requests in a special way or to handle other types of requests. Note, {@link com.opensymphony.webwork.dispatcher.multipart.MultiPartRequestWrapper} is
     * flexible - you should look to that first before overriding this method to handle multipart data.
     *
     * @param request the HttpServletRequest object.
     * @return a wrapped request or original request.
     * @see com.opensymphony.webwork.dispatcher.multipart.MultiPartRequestWrapper
     */
    public HttpServletRequest wrapRequest(HttpServletRequest request, ServletContext servletContext) throws IOException {
        // don't wrap more than once
        if (request instanceof WebWorkRequestWrapper) {
            return request;
        }

        if (MultiPartRequest.isMultiPart(request)) {
            request = new MultiPartRequestWrapper(request, getSaveDir(servletContext), getMaxSize());
        } else {
            request = new WebWorkRequestWrapper(request);
        }

        return request;
    }

    /**
     * Sends an HTTP error response code.
     *
     * @param request  the HttpServletRequest object.
     * @param response the HttpServletResponse object.
     * @param code     the HttpServletResponse error code (see {@link javax.servlet.http.HttpServletResponse} for possible error codes).
     * @param e        the Exception that is reported.
     */
    public void sendError(HttpServletRequest request, HttpServletResponse response, ServletContext ctx, int code, Exception e) {
        if (devMode) {
            response.setContentType("text/html");

            try {
                freemarker.template.Configuration config = FreemarkerManager.getInstance().getConfiguration(ctx);
                Template template = config.getTemplate("/com/opensymphony/webwork/dispatcher/error.ftl");

                List chain = new ArrayList();
                Throwable cur = e;
                chain.add(cur);
                while ((cur = cur.getCause()) != null) {
                    chain.add(cur);
                }

                HashMap data = new HashMap();
                data.put("exception", e);
                data.put("unknown", Location.UNKNOWN);
                data.put("chain", chain);
                data.put("locator", new Locator());
                template.process(data, response.getWriter());
                response.getWriter().close();
            } catch (Exception exp) {
                try {
                    response.sendError(code, "Unable to show problem report: " + exp);
                } catch (IOException ex) {
                    // we're already sending an error, not much else we can do if more stuff breaks
                }
            }
        } else {
            try {
                // send a http error response to use the servlet defined error handler
                // make the exception availible to the web.xml defined error page
                request.setAttribute("javax.servlet.error.exception", e);

                // for compatibility
                request.setAttribute("javax.servlet.jsp.jspException", e);

                // send the error response
                response.sendError(code, e.getMessage());
            } catch (IOException e1) {
                // we're already sending an error, not much else we can do if more stuff breaks
            }
        }
    }

    /**
     * Returns <tt>true</tt>, if portlet support is active, <tt>false</tt> otherwise.
     *
     * @return <tt>true</tt>, if portlet support is active, <tt>false</tt> otherwise.
     */
    public static boolean isPortletSupportActive() {
        return portletSupportActive;
    }

    /**
     * Set the flag that portlet support is active or not.
     * @param portletSupportActive <tt>true</tt> or <tt>false</tt>
     */
    public static void setPortletSupportActive(boolean portletSupportActive) {
        DispatcherUtils.portletSupportActive = portletSupportActive;
    }

    /** Simple accessor for a static method */
    public class Locator {
        public Location getLocation(Object obj) {
            Location loc = LocationUtils.getLocation(obj);
            if (loc == null) {
                return Location.UNKNOWN;
            }
            return loc;
        }
    }
}
