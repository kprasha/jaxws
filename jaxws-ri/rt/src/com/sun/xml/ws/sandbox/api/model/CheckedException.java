package com.sun.xml.ws.sandbox.api.model;

import com.sun.xml.bind.api.TypeReference;
import com.sun.xml.ws.model.ExceptionType;

/**
 * @author Vivek Pandey
 */
public interface CheckedException {
    /**
     * @return the <code>Class</clode> for this object
     *
     */
    Class getExcpetionClass();

    Class getDetailBean();

    TypeReference getDetailType();

    ExceptionType getExceptionType();
}
