package com.sun.xml.ws.sandbox.fault;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;

/**
 * SOAP 1.2 Fault class that can be marshalled/unmarshalled by JAXB
 * <p/>
 * <pre>
 * Example:
 * &lt;env:Envelope xmlns:env="http://www.w3.org/2003/05/soap-envelope"
 *            xmlns:m="http://www.example.org/timeouts"
 *            xmlns:xml="http://www.w3.org/XML/1998/namespace">
 * &lt;env:Body>
 *     &lt;env:Fault>
 *         &lt;env:Code>
 *             &lt;env:Value>env:Sender* &lt;/env:Value>
 *             &lt;env:Subcode>
 *                 &lt;env:Value>m:MessageTimeout* &lt;/env:Value>
 *             &lt;/env:Subcode>
 *         &lt;/env:Code>
 *         &lt;env:Reason>
 *             &lt;env:Text xml:lang="en">Sender Timeout* &lt;/env:Text>
 *         &lt;/env:Reason>
 *         &lt;env:Detail>
 *             &lt;m:MaxTime>P5M* &lt;/m:MaxTime>
 *         &lt;/env:Detail>
 *     &lt;/env:Fault>
 * &lt;/env:Body>
 * &lt;/env:Envelope>
 * </pre>
 *
 * @author Vivek Pandey
 */
@XmlRootElement(name = "Fault", namespace = "http://www.w3.org/2003/05/soap-envelope")
public class SOAP12Fault {
    public static final String ns = "http://www.w3.org/2003/05/soap-envelope";

    @XmlElement(name = "Code", namespace = ns, type = CodeType.class)
    public CodeType code;

    @XmlElement(name = "Reason", namespace = ns, type = ReasonType.class)
    public ReasonType reason;

    @XmlElement(name = "Node", namespace = ns, type = URI.class, nillable = true)
    public URI node;

    @XmlElement(name = "Role", namespace = ns, type = URI.class, nillable = true)
    public URI role;

    @XmlElement(name = "Detail", namespace = ns, nillable = true, type = DetailType.class)
    public DetailType detail;

}

