package com.sun.xml.ws.sandbox.fault;

import com.sun.xml.bind.api.Bridge;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.model.CheckedException;
import com.sun.xml.ws.api.model.ExceptionType;
import com.sun.xml.ws.model.CheckedExceptionImpl;
import com.sun.xml.ws.util.StringUtils;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPFaultException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Base class that represents SOAP 1.1 or SOAP 1.2 fault. This class can be used by the invocation handlers to create
 * an Exception from a received messge.
 *
 * <p>
 * TODO: Add methods to create {@link SOAP11Fault} or (@link SOAP12Fault} from an {@link Exception)
 *
 * @author Vivek Pandey
 */
public abstract class SOAPFaultBuilder {

    /**
     * Gives the {@link DetailType} for a Soap 1.1 or Soap 1.2 message that can be used to create either a checked exception or
     * a protocol specific exception
     */
    abstract public DetailType getDetail();

    /**
     * gives the fault string that can be used to create an {@link Exception}
     */
    abstract public String getFaultString();


    /**
     * This should be called from the client side to throw an {@link Exception} for a given soap mesage
     */
    public Throwable createException(Map<QName, CheckedExceptionImpl> exceptions, Message msg) throws JAXBException {
        DetailType dt = getDetail();
        if ((dt == null) || (dt.detailEntry.size() != 1)) {
            // No soap detail, doesnt look like its a checked exception
            // throw a protocol exception
            return getProtocolException(msg);
        }
        Node jaxbDetail = dt.detailEntry.get(0);
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
            Constructor constructor = exceptionClass.getConstructor(new Class[]{String.class, (Class) ce.getDetailType().type});
            Object exception = constructor.newInstance(new Object[]{getFaultString(), getJAXBObject(jaxbDetail, ce)});
            return (Exception) exception;
        } catch (Exception e) {
            throw new WebServiceException(e);
        }
    }

    private Throwable getProtocolException(Message msg) {
        try {
            return new SOAPFaultException(msg.readAsSOAPMessage().getSOAPBody().getFault());
        } catch (SOAPException e) {
            throw new WebServiceException(e);
        }
    }

    private Object getJAXBObject(Node jaxbBean, CheckedException ce) throws JAXBException {
        Bridge bridge = ce.getBridge();
        return bridge.unmarshal(ce.getOwner().getBridgeContext(), jaxbBean);
    }

    private Exception createUserDefinedException(CheckedExceptionImpl ce) {
        Class exceptionClass = ce.getExcpetionClass();
        try {
            Constructor constructor = exceptionClass.getConstructor(new Class[]{String.class});
            Object exception = constructor.newInstance(new Object[]{getFaultString()});
            Object jaxbDetail = getDetail().detailEntry.get(0);
            Field[] fields = jaxbDetail.getClass().getFields();
            for (Field f : fields) {
                Method m = exceptionClass.getMethod(getWriteMethod(f));
                m.invoke(exception, new Object[]{f.get(jaxbDetail)});
            }
            throw (Exception) exception;
        } catch (Exception e) {
            throw new WebServiceException(e);
        }
    }

    private String getWriteMethod(Field f) {
        return "set" + StringUtils.capitalize(f.getName());
    }

    /**
     * Parses a fault {@link Message} and returns it as a {@link SOAPFaultBuilder}.
     *
     * @throws JAXBException
     *      if the parsing fails.
     * @return
     *      always non-null valid object.
     */
    public static SOAPFaultBuilder create(Message msg) throws JAXBException {
        return msg.readPayloadAsJAXB(JAXB_CONTEXT.createUnmarshaller());
    }

    /**
     * This {@link JAXBContext} can handle SOAP 1.1/1.2 faults.
     */
    public static final JAXBContext JAXB_CONTEXT;

    static {
        try {
            JAXB_CONTEXT = JAXBContext.newInstance(SOAP11Fault.class, SOAP12Fault.class);
        } catch (JAXBException e) {
            throw new Error(e); // this must be a bug in our code
        }
    }
}
