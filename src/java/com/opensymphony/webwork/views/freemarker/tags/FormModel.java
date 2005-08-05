package com.opensymphony.webwork.views.freemarker.tags;

import com.opensymphony.webwork.components.Form;
import com.opensymphony.webwork.components.UIBean;
import com.opensymphony.xwork.util.OgnlValueStack;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * User: plightbo
 * Date: Jul 18, 2005
 * Time: 7:17:47 PM
 */
public class FormModel extends TagModel {
    public FormModel(OgnlValueStack stack, HttpServletRequest req, HttpServletResponse res) {
        super(stack, req, res);
    }

    protected UIBean getBean() {
        return new Form(stack, req, res);
    }
}