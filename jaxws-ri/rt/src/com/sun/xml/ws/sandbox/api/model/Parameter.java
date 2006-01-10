package com.sun.xml.ws.sandbox.api.model;

import com.sun.xml.bind.api.TypeReference;
import com.sun.xml.ws.model.Mode;
import com.sun.xml.ws.model.ParameterBinding;

import javax.xml.namespace.QName;

/**
 * @author Vivek Pandey
 */
public interface Parameter {
    /**
     * @return Returns the name.
     */
    QName getName();

    /**
     * TODO: once the model gets JAXBContext, shouldn't {@link com.sun.xml.bind.api.Bridge}s
     * be made available from model objects?
     *
     * @return Returns the TypeReference associated with this Parameter
     */
    TypeReference getTypeReference();

    /**
     * @return Returns the mode.
     */
    Mode getMode();

    /**
     * @return Returns the index.
     */
    int getIndex();

    /**
     * @return true if <tt>this instanceof {@link com.sun.xml.ws.model.WrapperParameter}</tt>.
     */
    boolean isWrapperStyle();

    /**
     * @return the Binding for this Parameter
     */
    ParameterBinding getBinding();

    ParameterBinding getInBinding();

    ParameterBinding getOutBinding();

    boolean isIN();

    boolean isOUT();

    boolean isINOUT();

    /**
     * If true, this parameter maps to the return value of a method invocation.
     *
     * <p>
     * {@link JavaMethod#getResponseParameters()} is guaranteed to have
     * at most one such {@link Parameter}. Note that there coule be none,
     * in which case the method returns <tt>void</tt>.
     */
    boolean isResponse();

    /**
     * Gets the holder value if applicable. To be called for inbound client side
     * message.
     *
     * @param obj
     * @return the holder value if applicable.
     */
    Object getHolderValue(Object obj);

    String getPartName();
}
