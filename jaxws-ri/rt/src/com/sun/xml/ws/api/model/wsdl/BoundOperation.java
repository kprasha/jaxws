package com.sun.xml.ws.api.model.wsdl;

import com.sun.xml.ws.api.model.Mode;
import com.sun.xml.ws.api.model.ParameterBinding;

import javax.xml.namespace.QName;
import java.util.Map;

/**
 * Abstracts wsdl:binding/wsdl:operation. It can be used to determine the parts and their binding.
 *
 * @author Vivek Pandey
 */
public interface BoundOperation extends Extensible {
    /**
     * Short-cut for {@code getOperation().getName()}
     */
    QName getName();

    /**
     * Short-cut for {@code getOperation().getLocalName()}
     */
    String getLocalName();

    /**
     * Gets {@link com.sun.xml.ws.api.model.wsdl.Part} for the given wsdl:input or wsdl:output part
     *
     * @param partName must be non-null
     * @param mode     must be non-null
     * @return null if no part is found
     */
    Part getPart(String partName, Mode mode);

    /**
     * Map of wsdl:input part name and the binding as {@link ParameterBinding}
     *
     * @return empty Map if there is no parts
     */
    Map<String, ParameterBinding> getInputParts();

    /**
     * Map of wsdl:output part name and the binding as {@link ParameterBinding}
     *
     * @return empty Map if there is no parts
     */
    Map<String, ParameterBinding> getOutputParts();

    /**
     * Map of mime:content@part and the mime type from mime:content@type for wsdl:output
     *
     * @return empty Map if there is no parts
     */
    Map<String, String> getInputMimeTypes();

    /**
     * Map of mime:content@part and the mime type from mime:content@type for wsdl:output
     *
     * @return empty Map if there is no parts
     */
    Map<String, String> getOutputMimeTypes();

    /**
     * Gets {@link ParameterBinding} for a given wsdl part in wsdl:input
     *
     * @param part Name of wsdl:part, must be non-null
     * @return null if the part is not found.
     */
    ParameterBinding getInputBinding(String part);

    /**
     * Gets {@link ParameterBinding} for a given wsdl part in wsdl:output
     *
     * @param part Name of wsdl:part, must be non-null
     * @return null if the part is not found.
     */
    ParameterBinding getOutputBinding(String part);

    /**
     * Gets the MIME type for a given wsdl part in wsdl:input
     *
     * @param part Name of wsdl:part, must be non-null
     * @return null if the part is not found.
     */
    String getMimeTypeForInputPart(String part);

    /**
     * Gets the MIME type for a given wsdl part in wsdl:output
     *
     * @param part Name of wsdl:part, must be non-null
     * @return null if the part is not found.
     */
    String getMimeTypeForOutputPart(String part);

    /**
     * Gets the wsdl:portType/wsdl:operation model - {@link Operation},
     * associated with this binding operation.
     * @return non-null {@link Operation}
     */
    public Operation getOperation();
}
