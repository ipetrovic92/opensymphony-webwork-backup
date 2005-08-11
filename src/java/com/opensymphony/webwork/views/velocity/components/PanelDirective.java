package com.opensymphony.webwork.views.velocity.components;

import com.opensymphony.webwork.components.UIBean;
import com.opensymphony.webwork.components.Panel;
import com.opensymphony.xwork.util.OgnlValueStack;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Ian Roughley
 * @version $Id$
 */
public class PanelDirective extends AbstractDirective {
    public String getBeanName() {
        return "panel";
    }

    protected UIBean getBean(OgnlValueStack stack, HttpServletRequest req, HttpServletResponse res) {
        return new Panel(stack, req, res);
    }
}