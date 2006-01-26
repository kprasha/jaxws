/**
 * For now put Handler related classes in sandbox, need to move these later once Server Runtime is also changed. 
 * All the classes in this package are taken from the original Handler classes and are massaged a bit to suit the new runtime.
 * Need to refactor a lot.
 * 
 * Who sets the MessageContext.MESSAGE_OUTBOUND_PROPERTY? Is it Handler Pipe or the someone else who calls the HandlerPipe
 * 
 * Who populates Attachments in to attachment properties
 *
 * Client-side handler processing is different from Server-side, So need separate Pipe Implmentation
 * Client-side 
 * call handleMessage() on SOAPMessages with fault
 * call MUHeaderPipe on inbound msg
 * close handlers after completion of MEP, In oneway case, close handlers before message is sent.
 * Exceptions during process, wrap in WebServiceException and throw it up to the client
 *
 * Server-side should call handleFault() when endpoint throws any Exception
 * call MUHeaderPipe outbound message
 * close handlers after MEP, in oneway case close before handing it over to endpoint
 * Exceptions during process, covert in SOAPFault
 *
 * 
 * MUHeaderPipe, should it be in soap package?
 */
package com.sun.xml.ws.sandbox.handler;
