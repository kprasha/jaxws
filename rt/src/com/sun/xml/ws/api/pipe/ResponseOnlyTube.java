/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.xml.ws.api.pipe;

import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.helper.AbstractTubeImpl;
import com.sun.xml.ws.developer.JAXWSProperties;
import com.sun.xml.ws.addressing.WsaPropertyBag;

public class ResponseOnlyTube extends AbstractTubeImpl {

	private static final Logger LOGGER = Logger
			.getLogger(ResponseOnlyTube.class.getName());

	private Tube[] conts;
	private int contsSize;
	private Tube leadTube;
	private boolean isLead;
	private boolean isSplicing;

	public ResponseOnlyTube(Tube leadTube) {
		this(leadTube, null);
	}

	public ResponseOnlyTube(Tube leadTube, Tube stopTube) {
		this(leadTube, stopTube, true, true);
	}

	public ResponseOnlyTube(Tube leadTube, boolean isLead, boolean isSplicing) {
		this(leadTube, null, isLead, isSplicing);
	}

	public ResponseOnlyTube(Tube leadTube, Tube stopTube, boolean isLead, boolean isSplicing) {
		this(new ContinuationCloner(new PipeClonerImpl(), isLead, isSplicing, stopTube),
				leadTube, isLead, isSplicing);
	}

	ResponseOnlyTube(ContinuationCloner cc, Tube leadTube,
			boolean isLead, boolean isSplicing) {
		this.isLead = isLead;
		this.isSplicing = isSplicing;
		this.leadTube = leadTube;
		cc.copy(leadTube);
		int off = isLead ? 1 : 0;
		this.conts = new Tube[cc.contsSize + 1 + off];
		System.arraycopy(cc.conts, 0, this.conts, 0, cc.contsSize);
		if (isLead) {
			this.conts[cc.contsSize] = this;
		}
		this.contsSize = cc.contsSize + off;
		verifyNewConts();
	}

	protected ResponseOnlyTube(ResponseOnlyTube that, ContinuationCloner cc) {
		this(cc, that.leadTube, that.isLead, that.isSplicing);
		verifyNewConts();
	}

	static boolean oneGoodTrace = false;

	private void verifyNewConts() {
		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER.fine("In ResponseOnlyTube " + this + ", set conts = "
					+ dumpConts());
			if (!oneGoodTrace) {
				oneGoodTrace = true;
				Throwable t = new Throwable("Good Stack trace");
				t.fillInStackTrace();
				LOGGER.log(Level.FINE, "Good ResponseOnlyTube List", t);
			}
			if (conts != null && contsSize == 1) {
				Throwable t = new Throwable("Bad Stack trace");
				t.fillInStackTrace();
				LOGGER.log(Level.FINE, "Bad ResponseOnlyTube List", t);
			}
		}
	}

	@Override
	public AbstractTubeImpl copy(TubeCloner cloner) {
		ContinuationCloner contCloner = getCloner(cloner);
		if (LOGGER.isLoggable(Level.FINE)) {
			String contsStr = dumpConts();
			String clonerContsStr = dumpConts(contCloner.conts,
					contCloner.contsSize);
			LOGGER.fine("In ResponseOnlyTube.copy " + this + ". My conts="
					+ contsStr + " and the cloner " + cloner
					+ " has set conts = " + clonerContsStr);
		}
		return new ResponseOnlyTube(this, contCloner);
	}

	protected ContinuationCloner getCloner(TubeCloner cloner) {
		ContinuationCloner contCloner;
		if (cloner instanceof ContinuationCloner) {
			contCloner = (ContinuationCloner) cloner;
			if (LOGGER.isLoggable(Level.FINE)) {
				LOGGER
						.fine("In ResponseOnlyTube.getCloner, returning provided cloner");
			}
		} else {
			contCloner = new ContinuationCloner(cloner, isLead, isSplicing);
			if (LOGGER.isLoggable(Level.FINE)) {
				LOGGER
						.fine("In ResponseOnlyTube.getCloner, returning NEW cloner");
			}
		}
		String clonerContsStr = dumpConts(contCloner.conts,
				contCloner.contsSize);
		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER
					.fine("In ResponseOnlyTube.getCloner, returning cloner with conts="
							+ clonerContsStr);
		}
		return contCloner;
	}

	public void preDestroy() {
	}

	public NextAction processException(Throwable t) {
		return doThrow(t);
	}

	protected void resetCont() {
		Fiber current = Fiber.current();
		current.resetCont(conts, contsSize);
	}

	public NextAction processRequest(Packet request) {

		if (LOGGER.isLoggable(Level.FINE)) {
			String msgId = null;
			if (request.supports(JAXWSProperties.ADDRESSING_MESSAGEID)) {
				msgId = (String) request
						.get(JAXWSProperties.ADDRESSING_MESSAGEID);
			} else if (request.supports(WsaPropertyBag.WSA_MSGID_FROM_REQUEST)) {
				msgId = (String) request
						.get(WsaPropertyBag.WSA_MSGID_FROM_REQUEST);
			} else {
				LOGGER
						.fine("In ResponseOnlyTube. Warning, dispatching a response in which no WS-Addressing property set has been initialized. Cannot determine msg id");
			}
			String contents = dumpPersistentContextContextProps(request.persistentContext);
			String tubes = dumpConts();
			LOGGER.fine("In ResponseOnlyTube " + this
					+ ", dispatching response path for msg id " + msgId
					+ " persistentContext: " + contents
					+ ". Will trigger response through tubes: " + tubes);
		}

		resetCont();
		return doReturnWith(request);
	}

	private String dumpConts() {
		return dumpConts(conts, contsSize);
	}

	private static String dumpConts(Tube[] conts, int contsSize) {
		StringBuffer buf = new StringBuffer("[");
		if (contsSize > 0) {
			for (int i = contsSize - 1; i >= 0; i--) {
				buf.append(conts[i].getClass().getSimpleName());
				if (i > 0) {
					buf.append(",");
				}
			}
		}
		buf.append("]");
		return buf.toString();
	}

	public static String dumpPersistentContextContextProps(Map<String, ?> map) {
		StringBuffer buf = new StringBuffer();
		buf.append(map.size());
		buf.append(" (");
		for (String key : map.keySet()) {
			buf.append(key).append("=");
			buf.append(map.get(key));
			buf.append(",");
		}
		buf.append(")");
		return buf.toString();
	}

	public NextAction processResponse(Packet response) {
		return doReturnWith(response);
	}
}
