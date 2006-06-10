package com.sun.xml.ws.message.jaxb;

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.message.Attachment;

import javax.activation.DataHandler;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.WebServiceException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Vivek Pandey
 */
final class SwarefAttachment implements Attachment {
    private final DataHandler dh;
    private final String contentId;

    /**
     * This will be constructed by {@link AttachmentMarshallerImpl}
     */
    public SwarefAttachment(@NotNull String contentId, @NotNull DataHandler dh) {
        this.dh = dh;
        this.contentId = contentId;
    }

    public String getContentId() {
        return contentId;
    }

    public String getContentType() {
        return dh.getContentType();
    }

    public byte[] asByteArray() {
        ByteArrayOutputStreamEx baos = new ByteArrayOutputStreamEx(1024);
        try {
            InputStream is = dh.getDataSource().getInputStream();
            baos.readFrom(is);
            is.close();
        } catch (IOException e) {
            throw new WebServiceException(e);
        }
        return baos.getBuffer();
    }

    public DataHandler asDataHandler() {
        return dh;
    }

    public Source asSource() {
        try {
            return new StreamSource(dh.getInputStream());
        } catch (IOException e) {
            throw new WebServiceException(e);
        }
    }

    public InputStream asInputStream() {
        try {
            return dh.getInputStream();
        } catch (IOException e) {
            throw new WebServiceException(e);
        }
    }

    public void writeTo(OutputStream os) throws IOException {
        os.write(asByteArray());
    }

    public void writeTo(SOAPMessage saaj) throws SOAPException {
        AttachmentPart part = saaj.createAttachmentPart();
        part.setDataHandler(dh);
        part.setContentId(contentId);
        saaj.addAttachmentPart(part);
    }

    //TODO: remove if its made public or protected from stax-ex
    /**
     * {@link ByteArrayOutputStream} with access to its raw buffer.
     */
    private final class ByteArrayOutputStreamEx extends ByteArrayOutputStream {
        public ByteArrayOutputStreamEx() {
        }

        public ByteArrayOutputStreamEx(int size) {
            super(size);
        }

        public byte[] getBuffer() {
            return buf;
        }

        /**
         * Reads the given {@link InputStream} completely into the buffer.
         */
        public void readFrom(InputStream is) throws IOException {
            while(true) {
                if(count==buf.length) {
                    // realllocate
                    byte[] data = new byte[buf.length*2];
                    System.arraycopy(buf,0,data,0,buf.length);
                    buf = data;
                }

                int sz = is.read(buf,count,buf.length-count);
                if(sz<0)     return;
                count += sz;
            }
        }
    }
}
