package com.sun.xml.ws.sandbox.api.model;

import com.sun.xml.bind.api.TypeReference;
import com.sun.xml.ws.model.Mode;
import com.sun.xml.ws.model.ParameterBinding;

import javax.xml.namespace.QName;

/**
 * Runtime Parameter that abstracts the annotated java parameter
 * <p/>
 * <p/>
 * A parameter may be bound to a header, a body, or an attachment.
 * Note that when it's bound to a body, it's bound to a body,
 * it binds to the whole payload.
 * <p/>
 * <p/>
 * Sometimes multiple Java parameters are packed into the payload,
 * in which case the subclass {@link com.sun.xml.ws.model.WrapperParameter} is used.
 *
 * @author Vivek Pandey
 */
public interface Parameter {
    /**
     * @return Returns the {@link QName} of the payload/infoset of a SOAP body or header.
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
     * @return Returns the mode, such as IN, OUT or INOUT.
     */
    Mode getMode();

    /**
     * Position of a parameter in the method signature. It would be -1 if the parameter is a return.
     *
     * @return Returns the index.
     */
    int getIndex();

    /**
     * @return true if <tt>this instanceof {@link com.sun.xml.ws.model.WrapperParameter}</tt>.
     */
    boolean isWrapperStyle();

    /**
     * Returns the binding associated with the parameter. For IN parameter the binding will be
     * same as {@link #getInBinding()}, for OUT parameter the binding will be same as
     * {@link #getOutBinding()} and for INOUT parameter the binding will be same as calling
     * {@link #getInBinding()}
     *
     * @return the Binding for this Parameter. Returns {@link ParameterBinding#BODY} by default.
     */
    ParameterBinding getBinding();

    /**
     * Returns the {@link ParameterBinding} associated with the IN mode
     *
     * @return the binding
     */
    ParameterBinding getInBinding();

    /**
     * Returns the {@link ParameterBinding} associated with the IN mode
     *
     * @return the binding
     */
    ParameterBinding getOutBinding();

    /**
     * @return true if the {@link Mode} associated with the parameter is {@link Mode#IN} and false otherwise.
     */
    boolean isIN();

    /**
     * @return true if the {@link Mode} associated with the parameter is {@link Mode#OUT} and false otherwise.
     */
    boolean isOUT();

    /**
     * @return true if the {@link Mode} associated with the parameter is {@link Mode#INOUT} and false otherwise.
     */
    boolean isINOUT();

    /**
     * If true, this parameter maps to the return value of a method invocation.
     * <p/>
     * <p/>
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

    /**
     * Gives the wsdl:part@name value
     *
     * @return Value of {@link javax.jws.WebParam#partName()} annotation if present,
     *         otherwise its the localname of the infoset associated with the parameter
     */
    String getPartName();
}
