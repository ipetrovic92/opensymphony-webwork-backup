/*
 * Copyright (c) 2002-2003 by OpenSymphony
 * All rights reserved.
 */
package com.opensymphony.webwork.views.jsp;

import javax.servlet.jsp.JspException;


/**
 * @author $Author$
 * @version $Revision$
 */
public class IfTag extends WebWorkTagSupport {
    //~ Static fields/initializers /////////////////////////////////////////////

    public static final String ANSWER = "webwork.if.answer";

    //~ Instance fields ////////////////////////////////////////////////////////

    Boolean answer;
    String test;

    //~ Methods ////////////////////////////////////////////////////////////////

    public void setTest(String test) {
        this.test = test;
    }

    public int doEndTag() throws JspException {
        if (answer != null) {
            pageContext.setAttribute(ANSWER, answer);
        }

        return SKIP_BODY;
    }

    public int doStartTag() throws JspException {
        answer = (Boolean) findValue(test, Boolean.class);

        if (answer != null && answer.booleanValue()) {
            return EVAL_BODY_INCLUDE;
        }

        return SKIP_BODY;
    }
}
