package com.sun.xml.ws.sandbox.api.model;

import com.sun.xml.bind.api.TypeReference;
import com.sun.xml.ws.model.ExceptionType;

/**
 * This class provides abstractio to the  the exception class corresponding to the wsdl:fault, such as class MUST have
 * {@link javax.xml.ws.WebFault} annotation defined on it.
 *
 * Also the exception class must have
 *
 * <code>public WrapperException()String message, FaultBean){}</code>
 *
 * and method
 *
 * <code>public FaultBean getFaultInfo();</code>
 *
 * @author Vivek Pandey
 */
public interface CheckedException {
    /**
     * The returned exception class would be userdefined or WSDL exception class that
     * extends java.lang.Exception.
     */
    Class getExcpetionClass();

    /**
     * The detail bean is serialized inside the detail entry in the SOAP message.
     * This must be known to the {@link javax.xml.bind.JAXBContext} inorder to get
     * marshalled/unmarshalled.
     *
     * @return the detail bean
     */
    Class getDetailBean();

    /**
     * {@link TypeReference} associated with the dettail bean.
     */
    TypeReference getDetailType();

    /**
     * Tells whether the exception class is a userdefined or a WSDL exception.
     * A WSDL exception class follows the pattern defined in JSR 224. According to that
     * a WSDL exception class must have:
     *
     * <code>public WrapperException()String message, FaultBean){}</code>
     *
     * and accessor method
     *
     * <code>public FaultBean getFaultInfo();</code>     
     */
    ExceptionType getExceptionType();
}
