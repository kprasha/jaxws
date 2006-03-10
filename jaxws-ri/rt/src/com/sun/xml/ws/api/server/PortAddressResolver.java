package com.sun.xml.ws.api.server;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;

/**
 * Resolves port address for an endpoint. A WSDL may contain multiple
 * endpoints, and some of the endpoints may be packaged in a single WAR file.
 * If an endpoint is serving the WSDL, it would be nice to fill the port addresses
 * of other endpoints in the WAR.
 *
 * <p>
 * This interface is implemented by the caller of
 * {@link SDDocument#writeTo} method so
 * that the {@link SDDocument} can correctly fills the addresses of known
 * endpoints.
 *
 *
 * @author Jitendra Kotamraju
 */
public interface PortAddressResolver {
    /**
     * Gets the endpoint address for a WSDL port
     *
     * @param portName
     *       WSDL port name for which address is needed. Always non-null.
     * @return
     *      The address needs to be put in WSDL for port element's location
     *      attribute. Can be null.
     */
    @Nullable String getAddressFor(@NotNull String portName);
}
