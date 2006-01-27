/**
 * Hosts {@link Pipe} that invokes hhandlers.
 *
 * <p>
 * For now put Handler related classes in sandbox, need to move these lateronce Server
 * Runtime is also changed. All the classes in this package are taken from the original
 * Handler classes and are massaged a bit to suit the new runtime.
 * Need to refactor a lot.
 *
 * <p>
 * Who sets the {@link MessageContext#MESSAGE_OUTBOUND_PROPERTY}?
 * Is it Handler Pipe or the someone else who calls the HandlerPipe
 *
 * <p>
 * Who populates Attachments in to attachment properties
 *
 * <p>
 * Client-side handler processing is different from Server-side, So need separate Pipe Implmentation
 * Client-side 
 * call handleMessage() on SOAPMessages with fault
 * call MUHeaderPipe on inbound msg
 * close handlers after completion of MEP, In oneway case, close handlers before message is sent.
 * Exceptions during process, wrap in WebServiceException and throw it up to the client
 *
 * <p>
 * Server-side should call handleFault() when endpoint throws any Exception
 * call MUHeaderPipe outbound message
 * close handlers after MEP, in oneway case close before handing it over to endpoint
 * Exceptions during process, covert in SOAPFault
 *
 * 
 * MUHeaderPipe, should it be in soap package?
 */
package com.sun.xml.ws.sandbox.handler;

import com.sun.xml.ws.api.pipe.Pipe;

import javax.xml.ws.handler.MessageContext;
