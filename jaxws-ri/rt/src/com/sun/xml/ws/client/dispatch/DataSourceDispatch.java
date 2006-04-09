/*
 * Copyright (c) 2006 Sun Microsystems Corporation. All Rights Reserved.
 */

package com.sun.xml.ws.client.dispatch;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.client.WSServiceDelegate;
import com.sun.xml.ws.encoding.xml.XMLMessage;
import com.sun.xml.ws.encoding.xml.XMLMessage.HasDataSource;

import javax.activation.DataSource;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;

/**
 *
 * @author WS Development Team
 * @version 1.0
 */
public class DataSourceDispatch extends DispatchImpl<DataSource> {

    public DataSourceDispatch(QName port, Class<DataSource> clazz, Service.Mode mode, WSServiceDelegate service, Pipe pipe, BindingImpl binding) {
       super(port, clazz, mode, service, pipe, binding);
    }

    Packet createPacket(DataSource arg) {
        // TODO
         //Message message = null;
         /*switch (mode) {
            case PAYLOAD:
                //message = ??
                break;
            case MESSAGE:
                //Todo: temporary

                //todo:temp
                //message = ?;
                //todo: uncomment above when correct Message for DS is implemented
                break;
            default:
                throw new WebServiceException("Unrecognized message mode");
        }

        return new Packet(message);
        */
        return new Packet(XMLMessage.create(arg));
    }

    DataSource toReturnValue(Packet response) {
        // TODO
        // TODO
        //Message msg = response.getMessage();
        /*switch (mode){
            case PAYLOAD:
                //return ?;
            case MESSAGE:
                //return
                //return ?;
            default:
                throw new WebServiceException("Unrecognized dispatch mode");
        }
        */
        Message message = response.getMessage();
        if (message instanceof HasDataSource) {
            HasDataSource hasDS = (HasDataSource)message;
            // TODO Need to call hasUnconsumedDataSource()
            return hasDS.getDataSource();
        }
        // TODO need to convert message to DataSource
        throw new UnsupportedOperationException();
    }
}