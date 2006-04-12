/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.xml.ws.sandbox.fault;

import com.sun.xml.bind.api.Bridge;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.model.CheckedException;
import com.sun.xml.ws.api.model.ExceptionType;
import com.sun.xml.ws.encoding.soap.SOAP12Constants;
import com.sun.xml.ws.encoding.soap.SOAPConstants;
import com.sun.xml.ws.encoding.soap.SerializationException;
import com.sun.xml.ws.model.CheckedExceptionImpl;
import com.sun.xml.ws.model.JavaMethodImpl;
import com.sun.xml.ws.sandbox.message.impl.jaxb.JAXBMessage;
import com.sun.xml.ws.util.StringUtils;
import com.sun.istack.Nullable;
import com.sun.istack.NotNull;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPFault;
import javax.xml.transform.dom.DOMResult;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.ProtocolException;
import javax.xml.ws.soap.SOAPFaultException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;

/**
 * Base class that represents SOAP 1.1 or SOAP 1.2 fault. This class can be used by the invocation handlers to create
 * an Exception from a received messge.
 *
 * @author Vivek Pandey
 */
public abstract class SOAPFaultBuilder {

    /**
     * Gives the {@link DetailType} for a Soap 1.1 or Soap 1.2 message that can be used to create either a checked exception or
     * a protocol specific exception
     */
    abstract DetailType getDetail();

    /**
     * gives the fault string that can be used to create an {@link Exception}
     */
    abstract String getFaultString();

    /**
     * This should be called from the client side to throw an {@link Exception} for a given soap mesage
     */
    public Throwable createException(Map<QName, CheckedExceptionImpl> exceptions, Message msg) throws JAXBException {
        DetailType dt = getDetail();
        if ((dt == null) || (dt.getDetails() == null) || (dt.getDetails().size() != 1) || (exceptions == null)) {
            // No soap detail, doesnt look like its a checked exception
            // throw a protocol exception
            return getProtocolException(msg);
        }
        Node jaxbDetail = (Node)dt.getDetails().get(0);
        QName detailName = new QName(jaxbDetail.getNamespaceURI(), jaxbDetail.getLocalName());
        CheckedExceptionImpl ce = exceptions.get(detailName);
        if (ce == null) {
            //No Checked exception for the received detail QName, throw a SOAPFault exception
            return getProtocolException(msg);

        }
        if (ce.getExceptionType().equals(ExceptionType.UserDefined)) {
            return createUserDefinedException(ce);

        }
        Class exceptionClass = ce.getExcpetionClass();
        try {
            Constructor constructor = exceptionClass.getConstructor(String.class, (Class) ce.getDetailType().type);
            Object exception = constructor.newInstance(getFaultString(), getJAXBObject(jaxbDetail, ce));
            return (Exception) exception;
        } catch (Exception e) {
            throw new WebServiceException(e);
        }
    }

    /**
     * To be called to convert a  {@link ProtocolException} and faultcode for a given {@link SOAPVersion} in to a {@link Message}.
     *
     * @param soapVersion {@link SOAPVersion#SOAP_11} or {@link SOAPVersion#SOAP_12}
     * @param ex a ProtocolException
     * @param faultcode soap faultcode. Its ignored if the {@link ProtocolException} instance is {@link SOAPFaultException} and it has a 
     * faultcode present in the underlying {@link SOAPFault}.
     * @return {@link Message} representing SOAP fault
     */
    public static @NotNull Message createSOAPFaultMessage(@NotNull SOAPVersion soapVersion, @NotNull ProtocolException ex, @Nullable QName faultcode){
        Object detail = getFaultDetail(null, ex);
        if(soapVersion == SOAPVersion.SOAP_12)
            return createSOAP12Fault(soapVersion, ex, detail, null, faultcode);
        return createSOAP11Fault(soapVersion, ex, detail, null, faultcode);
    }

    /**
     * To be called by the server runtime in the situations when there is an Exception that needs to be transformed in
     * to a soapenv:Fault payload.
     *
     * @param ceModel     {@link CheckedExceptionImpl} model that provides useful informations such as the detail tagname
     *                    and the Exception associated with it. Caller of this constructor should get the CheckedException
     *                    model by calling {@link JavaMethodImpl#getCheckedException(Class)}, where
     *                    Class is t.getClass().
     *                    <p>
     *                    If its null then this is not a checked exception  and in that case the soap fault will be
     *                    serialized only from the exception as described below.
     * @param ex          Exception that needs to be translated into soapenv:Fault, always non-null.
     *                    <ul>
     *                    <li>If t is instance of {@link SOAPFaultException} then its serilaized as protocol exception.
     *                    <li>If t.getCause() is instance of {@link SOAPFaultException} and t is a checked exception then
     *                    the soap fault detail is serilaized from t and the fault actor/string/role is taken from t.getCause().
     *                    </ul>
     * @param soapVersion non-null
     */
    public static Message createSOAPFaultMessage(SOAPVersion soapVersion, CheckedExceptionImpl ceModel, Throwable ex) {
        Object detail = getFaultDetail(ceModel, ex);
        if(soapVersion == SOAPVersion.SOAP_12)
            return createSOAP12Fault(soapVersion, ex, detail, ceModel, null);
        return createSOAP11Fault(soapVersion, ex, detail, ceModel, null);
    }

    /**
     * Server runtime will call this when there is some internal error not resulting from an exception.
     *
     * @param soapVersion {@link SOAPVersion#SOAP_11} or {@link SOAPVersion#SOAP_12}
     * @param faultString must be non-null
     * @param faultCode   For SOAP 1.1, it must be one of
     *                    <ul>
     *                    <li>{@link SOAPConstants#FAULT_CODE_CLIENT}
     *                    <li>{@link SOAPConstants#FAULT_CODE_SERVER}
     *                    <li>{@link SOAPConstants#FAULT_CODE_MUST_UNDERSTAND}
     *                    <li>{@link SOAPConstants#FAULT_CODE_VERSION_MISMATCH}
     *                    </ul>
     *
     *                    For SOAP 1.2
     *                    <ul>
     *                    <li>{@link SOAP12Constants#FAULT_CODE_CLIENT}
     *                    <li>{@link SOAP12Constants#FAULT_CODE_SERVER}
     *                    <li>{@link SOAP12Constants#FAULT_CODE_MUST_UNDERSTAND}
     *                    <li>{@link SOAP12Constants#FAULT_CODE_VERSION_MISMATCH}
     *                    <li>{@link SOAP12Constants#FAULT_CODE_DATA_ENCODING_UNKNOWN}
     *                    </ul>
     * @return non-null {@link Message}
     */
    public static Message createSOAPFaultMessage(SOAPVersion soapVersion, String faultString, QName faultCode) {
        if (faultCode == null)
            faultCode = getDefaultFaultCode(soapVersion);
        return createSOAPFaultMessage(soapVersion, faultString, faultCode, null);

    }

    private static Message createSOAPFaultMessage(SOAPVersion soapVersion, String faultString, QName faultCode, Node detail) {
        switch (soapVersion) {
            case SOAP_11:
                return new JAXBMessage(JAXB_MARSHALLER, new SOAP11Fault(faultCode, faultString, null, detail), soapVersion);
            case SOAP_12:
                return new JAXBMessage(JAXB_MARSHALLER, new SOAP12Fault(faultCode, faultString, null, detail), soapVersion);
            default:
                throw new AssertionError();
        }
    }

    abstract protected Throwable getProtocolException(Message msg);

    private Object getJAXBObject(Node jaxbBean, CheckedException ce) throws JAXBException {
        Bridge bridge = ce.getBridge();
        return bridge.unmarshal(ce.getOwner().getBridgeContext(), jaxbBean);
    }

    private Exception createUserDefinedException(CheckedExceptionImpl ce) {
        Class exceptionClass = ce.getExcpetionClass();
        try {
            Constructor constructor = exceptionClass.getConstructor(String.class);
            Object exception = constructor.newInstance(getFaultString());
            Object jaxbDetail = getDetail().getDetails().get(0);
            Field[] fields = jaxbDetail.getClass().getFields();
            for (Field f : fields) {
                Method m = exceptionClass.getMethod(getWriteMethod(f));
                m.invoke(exception, f.get(jaxbDetail));
            }
            throw (Exception) exception;
        } catch (Exception e) {
            throw new WebServiceException(e);
        }
    }

    private static String getWriteMethod(Field f) {
        return "set" + StringUtils.capitalize(f.getName());
    }

    private static Object getFaultDetail(CheckedExceptionImpl ce, Throwable exception) {
        if (ce == null)
            return null;
        if (ce.getExceptionType().equals(ExceptionType.UserDefined)) {
            return createDetailFromUserDefinedException(ce, exception);
        }
        try {
            Method m = exception.getClass().getMethod("getFaultInfo");
            return m.invoke(exception);
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    private static Object createDetailFromUserDefinedException(CheckedExceptionImpl ce, Object exception) {
        Class detailBean = ce.getDetailBean();
        Field[] fields = detailBean.getDeclaredFields();
        try {
            Object detail = detailBean.newInstance();
            for (Field f : fields) {
                Method em = exception.getClass().getMethod(getReadMethod(f));
                Method sm = detailBean.getMethod(getWriteMethod(f), em.getReturnType());
                sm.invoke(detail, em.invoke(exception));
            }
            return detail;
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    private static String getReadMethod(Field f) {
        if (f.getType().isAssignableFrom(boolean.class))
            return "is" + StringUtils.capitalize(f.getName());
        return "get" + StringUtils.capitalize(f.getName());
    }

    private static Message createSOAP11Fault(SOAPVersion soapVersion, Throwable e, Object detail, CheckedExceptionImpl ce, QName faultCode) {
        SOAPFaultException soapFaultException = null;
        String faultString = null;
        String faultActor = null;
        Throwable cause = e.getCause();
        if (e instanceof SOAPFaultException) {
            soapFaultException = (SOAPFaultException) e;
        } else if (cause != null && cause instanceof SOAPFaultException) {
            soapFaultException = (SOAPFaultException) e.getCause();
        }
        if (soapFaultException != null) {
            QName soapFaultCode = soapFaultException.getFault().getFaultCodeAsQName();
            if(soapFaultCode != null)
                faultCode = soapFaultCode;

            faultString = soapFaultException.getFault().getFaultString();
            faultActor = soapFaultException.getFault().getFaultActor();
        }

        if (faultCode == null) {
            faultCode = getDefaultFaultCode(soapVersion);
        }

        if (faultString == null) {
            faultString = e.getMessage();
            if (faultString == null) {
                faultString = e.toString();
            }
        }
        Node detailNode = null;
        if (detail == null && soapFaultException != null) {
            detailNode = soapFaultException.getFault().getDetail();
        } else if(detail != null){
            try {
                DOMResult dr = new DOMResult();
                ce.getBridge().marshal(ce.getOwner().getBridgeContext(), detail, dr);
                detailNode = dr.getNode().getFirstChild();
            } catch (JAXBException e1) {
                //Should we throw Internal Server Error???
                faultString = e.getMessage();
                faultCode = getDefaultFaultCode(soapVersion);
            }
        }
        return new JAXBMessage(JAXB_MARSHALLER, new SOAP11Fault(faultCode, faultString, null, detailNode), soapVersion);
    }

    private static Message createSOAP12Fault(SOAPVersion soapVersion, Throwable e, Object detail, CheckedExceptionImpl ce, QName faultCode) {
        SOAPFaultException soapFaultException = null;
        CodeType code = null;
        String faultString = null;
        String faultActor = null;
        Throwable cause = e.getCause();
        if (e instanceof SOAPFaultException) {
            soapFaultException = (SOAPFaultException) e;
        } else if (cause != null && cause instanceof SOAPFaultException) {
            soapFaultException = (SOAPFaultException) e.getCause();
        }
        if (soapFaultException != null) {
            SOAPFault fault = soapFaultException.getFault();
            QName soapFaultCode = fault.getFaultCodeAsQName();
            if(soapFaultCode != null){
                if(soapFaultCode != null)
                    faultCode = soapFaultCode;
                code = new CodeType(faultCode);
                Iterator iter = fault.getFaultSubcodes();
                boolean first = true;
                SubcodeType subcode = null;
                while(iter.hasNext()){
                    QName value = (QName)iter.next();
                    if(first){
                        SubcodeType sct = new SubcodeType(value);
                        code.setSubcode(sct);
                        subcode = sct;
                        first = false;
                        continue;
                    }
                    subcode = fillSubcodes(subcode, value);
                }
            }
            faultString = soapFaultException.getFault().getFaultString();
            faultActor = soapFaultException.getFault().getFaultActor();
        }

        if (faultCode == null && code == null) {
            faultCode = getDefaultFaultCode(soapVersion);
            code = new CodeType(faultCode);
        }

        if (faultString == null) {
            faultString = e.getMessage();
            if (faultString == null) {
                faultString = e.toString();
            }
        }

        ReasonType reason = new ReasonType(faultString);
        Node detailNode = null;
        if (detail == null && soapFaultException != null) {
            detailNode = soapFaultException.getFault().getDetail();
        } else if(detail != null){
            try {
                DOMResult dr = new DOMResult();
                ce.getBridge().marshal(ce.getOwner().getBridgeContext(), detail, dr);
                detailNode = dr.getNode().getFirstChild();
            } catch (JAXBException e1) {
                //Should we throw Internal Server Error???
                faultString = e.getMessage();
                faultCode = getDefaultFaultCode(soapVersion);
            }
        }
        DetailType detailType = null;
        if(detailNode != null)
            detailType = new DetailType(detailNode);
        return new JAXBMessage(JAXB_MARSHALLER, new SOAP12Fault(code, reason, null, null, detailType), soapVersion);
    }

    private static SubcodeType fillSubcodes(SubcodeType parent, QName value){
        SubcodeType newCode = new SubcodeType(value);
        parent.setSubcode(newCode);
        return newCode;
    }

    private static QName getDefaultFaultCode(SOAPVersion soapVersion) {
        switch (soapVersion) {
            case SOAP_12:
                return SOAP12Constants.FAULT_CODE_SERVER;
            default:
                return SOAPConstants.FAULT_CODE_SERVER;
        }

    }

    /**
     * Parses a fault {@link Message} and returns it as a {@link SOAPFaultBuilder}.
     *
     * @return always non-null valid object.
     * @throws JAXBException if the parsing fails.
     */
    public static SOAPFaultBuilder create(Message msg) throws JAXBException {
        return msg.readPayloadAsJAXB(JAXB_CONTEXT.createUnmarshaller());
    }

    /**
     * This {@link JAXBContext} can handle SOAP 1.1/1.2 faults.
     */
    private static final JAXBContext JAXB_CONTEXT;

    private static final Marshaller JAXB_MARSHALLER;

    static {
        try {
            JAXB_CONTEXT = JAXBContext.newInstance(SOAP11Fault.class, SOAP12Fault.class);
            JAXB_MARSHALLER = JAXB_CONTEXT.createMarshaller();
            JAXB_MARSHALLER.setProperty(Marshaller.JAXB_FRAGMENT, true);
        } catch (JAXBException e) {
            throw new Error(e); // this must be a bug in our code
        }
    }
}
