package com.sun.xml.ws.sandbox.impl;


import com.sun.xml.messaging.saaj.packaging.mime.MessagingException;
import com.sun.xml.messaging.saaj.packaging.mime.internet.ContentType;
import com.sun.xml.messaging.saaj.packaging.mime.internet.InternetHeaders;
import com.sun.xml.messaging.saaj.packaging.mime.internet.ParseException;
import com.sun.xml.messaging.saaj.util.ByteOutputStream;
import com.sun.xml.ws.api.pipe.Decoder;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.util.ASCIIUtility;

import javax.xml.ws.WebServiceException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.util.BitSet;

/**
 * @author Vivek Pandey
 */

public class AttachmentStreamSOAPDecoder implements Decoder{


    private InputStream in;
    private ContentType ct;
    private String boundary;
    private String start;
    private byte[] boundaryBytes;

    private boolean parsed;
    private BitSet lastPartFound = new BitSet(1);
    // current stream position, set to -1 on EOF
    int b = 0;
    /*
     * When true it indicates parsing hasnt been done at all
     */
    private boolean begining = true;

    private final int[] bcs = new int[256];
    int[] gss = null;
    private static final int BUFFER_SIZE = 4096;
    private byte[] buffer = new byte[BUFFER_SIZE];
    private byte[] prevBuffer = new byte[BUFFER_SIZE];
//    private List<MimeBodyPart> parts;

    /**
     * This guy can decode the SOAP part.
     */
    private final StreamSOAPDecoder streamDecoder;

    public AttachmentStreamSOAPDecoder(StreamSOAPDecoder streamDecoder) {
        this.streamDecoder = streamDecoder;
    }

    /**
     *
     * @param in
     * @param contentType
     * @return
     * @throws IOException
     */
    public Packet decode(InputStream in, String contentType) throws IOException {
        //InputStream MUST support mark()
        if(!this.in.markSupported()){
            this.in = new BufferedInputStream(in);
        }else{
            this.in = in;
        }

        try {
            ct = new ContentType(contentType);
            boundary = ct.getParameter("boundary");
            String bnd = "--" + boundary;
            boundaryBytes = ASCIIUtility.getBytes(bnd);
            start = ct.getParameter("start");

        } catch (ParseException e) {
            //TODO: dont we need DecoderExcpetion or somthing of that sort
            throw new WebServiceException(e);
        }

        //TODO: use "start" parameter to get to the root MIME part, is
        // the only option is to cache all the parts then get the part with
        // content-Id value same as start?

        ByteOutputStream os = parseMessage();
        try {
            InternetHeaders ih = new InternetHeaders(in);
            String[] ctype = ih.getHeader("Content-Type");
            if(ctype != null){
                if(isSOAP11Message(ctype[0])){
                    StreamSOAPDecoder decoder =  new StreamSOAP11Decoder();
                    return decoder.decode(os.newInputStream(), ctype[0]);
                }else if(isSOAP12Message(ctype[0])){
                    StreamSOAPDecoder decoder =  new StreamSOAP12Decoder();
                    return decoder.decode(os.newInputStream(), ctype[0]);
                }
            }
        } catch (MessagingException e) {
            throw new WebServiceException(e);
        }
        return null;
    }

    private boolean isSOAP12Message(String contentType) throws ParseException {
        ContentType ct = new ContentType(contentType);
        String base = ct.getBaseType();
        String sub =  ct.getSubType();
        if(base.equals("application") && sub.equals("soap+xml")){
            return true;
        }

        //it might be a XOP package
        if(base.equals("application") && sub.equals("xop+xml")){
            String type = ct.getParameter("type");
            if(type.equals("application/soap+xml"))
                return true;
        }

        return false;
    }

    private boolean isSOAP11Message(String contentType) throws ParseException {
        ContentType ct = new ContentType(contentType);
        String base = ct.getBaseType();
        String sub =  ct.getSubType();
        if(base.equals("text") && sub.equals("xml")){
            return true;
        }

        //it might be a XOP package
        if(base.equals("application") && sub.equals("xop+xml")){
            String type = ct.getParameter("type");
            if(type.equals("text/xml"))
                return true;
        }

        return false;
    }

    private ByteOutputStream parseMessage() throws IOException {
            return getNextPart(in);
    }

    private ByteOutputStream getNextPart(InputStream stream) throws IOException {

        if (!stream.markSupported()) {
            throw new WebServiceException("InputStream does not support Marking");
        }

        if (begining) {
            compile(boundaryBytes);
            if (!skipPreamble(stream, boundaryBytes)) {
                throw new WebServiceException(
                    "Missing Start Boundary, or boundary does not start on a new line");
            }
            begining = false;
        }

        if (lastBodyPartFound()) {
            throw new WebServiceException("No parts found in Multipart InputStream");
        }
        ByteOutputStream baos = new ByteOutputStream();
        b = readBody(stream, boundaryBytes, baos);
        return baos;
    }


//    /**
//     * parses the inputStream
//     */
//    private void parse() {
//        if (parsed)
//	        return;
//
//        String bnd = "--" + boundary;
//        byte[] bndbytes = ASCIIUtility.getBytes(bnd);
//        try {
//                parse(in, bndbytes);
//        } catch (IOException ioex) {
//            throw new WebServiceException("IO Error", ioex);
//        } catch (Exception ex) {
//            throw new WebServiceException("Error", ex);
//        }
//        parsed = true;
//    }
//
//    public boolean parse(InputStream stream, byte[] pattern)
//        throws Exception {
//
//        while (!lastPartFound.get(0) && (b != -1)) {
//           getNextPart(stream, pattern);
//        }
//        return true;
//    }
//
//    public MimeBodyPart getNextPart(InputStream stream, byte[] pattern)
//        throws Exception {
//
//        if (!stream.markSupported()) {
//            throw new Exception("InputStream does not support Marking");
//        }
//
//        if (begining) {
//            compile(pattern);
//            if (!skipPreamble(stream, pattern)) {
//                throw new Exception(
//                    "Missing Start Boundary, or boundary does not start on a new line");
//            }
//            begining = false;
//        }
//
//        if (lastBodyPartFound()) {
//            throw new Exception("No parts found in Multipart InputStream");
//        }
//
//
//        InternetHeaders headers = new InternetHeaders(stream);
//        ByteOutputStream baos = new ByteOutputStream();
//        b = readBody(stream, pattern, baos);
//        MimeBodyPart mbp = new MimeBodyPart(headers, baos.getBytes(),baos.getCount());
//        addBodyPart(mbp);
//        return mbp;
//    }
//
//    private void addBodyPart(MimeBodyPart mbp) {
//        if (parts == null)
//	    parts = new FinalArrayList<MimeBodyPart>();
//
//	parts.add(mbp);
//	//part.setParent(this);
//    }


    private int readBody(InputStream is, byte[] pattern, ByteOutputStream baos) throws IOException {
        if (!find(is, pattern, baos)) {
            throw new WebServiceException(
            "Missing boundary delimitier while reading Body WSDLPartImpl");
        }
        return b;
    }

    private boolean find(InputStream is, byte[] pattern, ByteOutputStream out) throws IOException {
        int i;
        int l = pattern.length;
        int lx = l -1;
        int bufferLength = 0;
        int s = 0;
        long endPos = -1;
        byte[] tmp = null;

        boolean first = true;
        BitSet eof = new BitSet(1);

        while (true) {
            is.mark(l);
            if (!first) {
                tmp = prevBuffer;
                prevBuffer = buffer;
                buffer = tmp;
            }
            bufferLength = readNext(is, buffer, l, eof);

            if (bufferLength == -1) {
                    b = -1;
                    if ((s == l)) {
                        out.write(prevBuffer, 0, s);
                    }
                    return true;
            }

            if (bufferLength < l) {
                out.write(buffer, 0, bufferLength);
                b = -1;
                return true;
            }

            for(i = lx; i >= 0; i--) {
                if (buffer[i] != pattern[i]) {
                    break;
                }
            }

            if (i < 0) {
                if (s > 0) {
                    // so if s == 1 : it must be an LF
                    // if s == 2 : it must be a CR LF
                    if (s <= 2) {
                        String crlf = new String(prevBuffer, 0, s);
                        if (!"\n".equals(crlf) && !"\r\n".equals(crlf)) {
                            throw new WebServiceException(
                                "Boundary characters encountered in part Body " +
                                "without a preceeding CRLF");
                        }
                    } else if (s > 2) {
                        if ((prevBuffer[s-2] == '\r') && (prevBuffer[s-1] == '\n')) {
                            out.write(prevBuffer, 0, s - 2);
                        } else if (prevBuffer[s-1] == '\n') {
                            out.write(prevBuffer, 0, s - 1);
                        } else {
                            throw new WebServiceException(
                                "Boundary characters encountered in part Body " +
                                "without a preceeding CRLF");
                        }
                    }
                }
                // found the boundary, skip *LWSP-char and CRLF
                if (!skipLWSPAndCRLF(is)) {
                    //throw new Exception(
                     //   "Boundary does not terminate with CRLF");
                }
                return true;
            }

            if ((s > 0)) {
                if (prevBuffer[s-1] == (byte)13) {
                    // if buffer[0] == (byte)10
                    if (buffer[0] == (byte)10) {
                        int j=lx-1;
                        for(j = lx-1; j > 0; j--) {
                            if (buffer[j+1] != pattern[j]) {
                                break;
                            }
                        }
                        if (j == 0) {
                            // matched the pattern excluding the last char of the pattern
                            // so dont write the CR into stream
                            out.write(prevBuffer,0,s-1);
                        } else {
                            out.write(prevBuffer,0,s);
                        }
                    } else {
                        out.write(prevBuffer, 0, s);
                    }
                } else {
                    out.write(prevBuffer, 0, s);
                }
            }

            s = Math.max(i + 1 - bcs[buffer[i] & 0x7f], gss[i]);
            is.reset();
            is.skip(s);
            if (first) {
                first = false;
            }
        }
    }


    public boolean lastBodyPartFound() {
        return lastPartFound.get(0);
    }

    private void compile(byte[] pattern) {
        int l = pattern.length;

        int i;
        int j;

        // Copied from J2SE 1.4 regex code
        // java.util.regex.Pattern.java

        // Initialise Bad Character Shift table
        for (i = 0; i < l; i++) {
            bcs[pattern[i]] = i + 1;
        }

        // Initialise Good Suffix Shift table
        gss = new int[l];

      NEXT: for (i = l; i > 0; i--) {
                // j is the beginning index of suffix being considered
                for (j = l - 1; j >= i; j--) {
                    // Testing for good suffix
                    if (pattern[j] == pattern[j - i]) {
                        // pattern[j..len] is a good suffix
                            gss[j - 1] = i;
                        } else {
                           // No match. The array has already been
                           // filled up with correct values before.
                           continue NEXT;
                        }
                    }
                while (j > 0) {
                    gss[--j] = i;
                }
            }
        gss[l - 1] = 1;
    }

    private boolean skipPreamble(InputStream is, byte[] pattern) throws IOException {
        if (!find(is, pattern)) {
            return false;
        }
        if (lastPartFound.get(0)) {
            throw new WebServiceException("Found closing boundary delimiter while trying to skip preamble");
        }
        return true;
    }

    public boolean find(InputStream is, byte[] pattern) throws IOException {
        int i;
        int l = pattern.length;
        int lx = l -1;
        int bufferLength = 0;
        BitSet eof = new BitSet(1);
        long[] posVector = new long[1];

        while (true) {
            is.mark(l);
            bufferLength = readNext(is, buffer, l, eof);
            if (eof.get(0)) {
                // End of stream
                return false;
            }

            for(i = lx; i >= 0; i--) {
                if (buffer[i] != pattern[i]) {
                    break;
                }
            }

            if (i < 0) {
                    // found the boundary, skip *LWSP-char and CRLF
                    if (!skipLWSPAndCRLF(is)) {
                        throw new WebServiceException("Boundary does not terminate with CRLF");
                    }
                    return true;
            }

            int s = Math.max(i + 1 - bcs[buffer[i] & 0x7f], gss[i]);
            is.reset();
            is.skip(s);
        }
    }

    private boolean skipLWSPAndCRLF(InputStream is) throws IOException {

        b = is.read();
        //looks like old impl allowed just a \n as well
        if (b == '\n') {
            return true;
        }

        if (b == '\r') {
            b = is.read();
            if (b == '\n') {
                return true;
            } else {
                throw new WebServiceException(
                    "transport padding after a Mime Boundary  should end in a CRLF, found CR only");
            }
        }

        if (b == '-') {
            b = is.read();
            if (b != '-') {
               throw new WebServiceException(
                   "Unexpected singular '-' character after Mime Boundary");
            } else {
                //System.out.println("Last WSDLPartImpl Found");
                lastPartFound.flip(0);
                // read the next char
                b  = is.read();
            }
        }

        while ((b != -1) && ((b == ' ') || (b == '\t'))) {
            b = is.read();
            if (b == '\r') {
                b = is.read();
                if (b == '\n') {
                   return true;
                }
            }
        }

        if (b == -1) {
            // the last boundary need not have CRLF
            if (!lastPartFound.get(0)) {
                throw new WebServiceException(
                    "End of Multipart Stream before encountering  closing boundary delimiter");
            }
            return true;
        }
        return false;
    }


    public int  readNext(InputStream is, byte[] buff, int patternLength, BitSet eof) throws IOException {
        int bufferLength = is.read(buffer, 0, patternLength);
        if (bufferLength == -1) {
           eof.flip(0);
        } else if (bufferLength < patternLength) {
            //repeatedly read patternLength - bufferLength
            int temp = 0;
            long pos = 0;
            int i = bufferLength;
            for (; i < patternLength; i++) {
                temp = is.read();
                if (temp == -1) {
                    eof.flip(0);
                    break;
                }
                buffer[i] = (byte)temp;
            }
            bufferLength=i;
        }
        return bufferLength;
    }

    public Packet decode(ReadableByteChannel in, String contentType) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a copy of this {@link com.sun.xml.ws.api.pipe.Decoder}.
     * <p/>
     * <p/>
     * See {@link com.sun.xml.ws.api.pipe.Encoder#copy()} for the detailed contract.
     */
    public Decoder copy() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
