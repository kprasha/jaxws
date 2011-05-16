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

import java.util.logging.Logger;
import java.util.logging.Level;

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Tube;
import com.sun.xml.ws.api.pipe.helper.AbstractTubeImpl;
import com.sun.xml.ws.api.pipe.helper.PipeAdapter;

@SuppressWarnings("deprecation")
public class ContinuationCloner extends PipeClonerImpl {

  private static final Logger LOGGER = Logger.getLogger(ContinuationCloner.class.getName());


    Tube[] conts = new Tube[16];
    int contsSize;
    
    private TubeCloner delegate;
    private boolean isLead;
    private boolean isSplicing;
    private Tube stopTube;
    private ContinuationObserver observer = null;

	ContinuationCloner(TubeCloner delegate, boolean isLead, boolean isSplicing) {
		this(delegate, isLead, isSplicing, null);
	}
	
	ContinuationCloner(TubeCloner delegate, boolean isLead, boolean isSplicing, Tube stopTube) {
		super(delegate.master2copy);
		this.delegate = delegate;
		this.isLead = isLead;
		this.isSplicing = isSplicing;
		this.stopTube = stopTube;
	}

    private void pushCont(Tube tube) {
        conts[contsSize++] = observer != null ? observer.notify(tube) : tube;

        // expand if needed
        int len = conts.length;
        if(contsSize==len) {
            Tube[] newBuf = new Tube[len*2];
            System.arraycopy(conts,0,newBuf,0,len);
            conts = newBuf;
        }
    }
    
    @Override
	public <T extends Pipe> T copy(T p) {
		if (p == stopTube)
			return (T) new DummyTube();
		return super.copy(p);
	}

	@Override
	public <T extends Tube> T copy(T p) {
		if (p == stopTube)
			return (T) new DummyTube();
		return super.copy(p);
	}
	
	private static class DummyTube extends AbstractTubeImpl {

		public DummyTube() {}
		
		public DummyTube(DummyTube that, TubeCloner cloner) {
			super(that, cloner);
		}
		
		@Override
		public AbstractTubeImpl copy(TubeCloner cloner) {
			return new DummyTube(this, cloner);
		}

		@Override
		public void preDestroy() {
		}

		@Override
		public NextAction processException(Throwable t) {
			return doThrow(t);
		}

		@Override
		public NextAction processRequest(Packet request) {
			return doReturnWith(request);
		}

		@Override
		public NextAction processResponse(Packet response) {
			return doReturnWith(response);
		}
	}

	public void dropLast() {
    	if(contsSize > 0)
    		contsSize--;
    }

	public void add(AbstractTubeImpl original, AbstractTubeImpl copy) {
		pushCont(copy);
		super.add((Tube) original, (Tube) copy);
	}

	@Override
	public void add(Pipe original, Pipe copy) {
		pushCont(PipeAdapter.adapt(copy));
		super.add(original, copy);
	}

	@Override
	public void add(Tube original, Tube copy) {
    pushCont(copy);
		if (LOGGER.isLoggable(Level.FINER)) {
      String originalStr = getTubeToString(original);
      String copyStr = getTubeToString(copy);
      LOGGER.fine("ContinationCloner " + this + " adding original " + originalStr + " and copy " + copyStr + ". Have stored " + contsSize + " tubes");
    }
    super.add(original, copy);
	}

  private String getTubeToString(Tube original) {
    String originalStr;
    try {
      originalStr = original.toString();
    } catch (Exception e) {
      originalStr = original.getClass().getSimpleName();
    }
    return originalStr;
  }

  TubeCloner getDelegate() {
		return delegate;
	}
	
	public boolean isLead() {
		return isLead;
	}
	
	public boolean isSplicing() {
		return isSplicing;
	}
	
	public void setContinuationObserver(ContinuationObserver observer) {
		this.observer = observer;
	}
	
	public static interface ContinuationObserver {
		public Tube notify(Tube t);
	}
}
