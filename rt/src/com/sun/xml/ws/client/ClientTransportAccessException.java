package com.sun.xml.ws.client;

import com.sun.xml.ws.util.localization.Localizable;

/**
 * Indicates that an authorization or authentication error occurred at the
 * client transport level.
 */
public class ClientTransportAccessException
  extends ClientTransportException {

  public ClientTransportAccessException(Localizable msg) {
    super(msg);
  }

  public ClientTransportAccessException(Localizable msg, Throwable cause) {
    super(msg, cause);
  }

  public ClientTransportAccessException(Throwable throwable) {
    super(throwable);
  }
}
