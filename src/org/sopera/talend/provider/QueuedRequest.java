/*******************************************************************************
 * Copyright (c) 2010 SOPERA GmbH
 * All rights reserved. 
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.sopera.talend.provider;

import java.util.concurrent.TimeoutException;

import org.sopware.papi.exception.ParticipantException;
import org.sopware.papi.exception.SBBException;
import org.sopware.papi.untyped.CallContext;
import org.sopware.papi.untyped.IncomingMessage;
import org.sopware.papi.untyped.OutgoingMessage;

/**
 * This interface encapsulates a queued request that can be processed in non-SBB initiated
 * thread.
 * 
 * Important contract on using this interface is to call {@link #release()} method as soon
 * as you are done with processing of the request.
 * 
 * The Queued request has a type which is convenience method to access {@link OperationType}
 * enumerator, the same information you can obtain using {@link #getMessage()}.getCallContext()
 * 
 * In case of request-response operation you can send response using {@link #sendResponse(OutgoingMessage)}
 * or send error using {@link #sendError(ParticipantException)} 
 * 
 * @see CallContext
 * @see OperationType
 * @see QueuedMessageHandler
 * 
 * @author zubairov
 *
 */
public interface QueuedRequest {

	public abstract IncomingMessage getMessage();

	public abstract OperationType getType();

	/**
	 * WARNING: You have to call this method explicitly after you are done with
	 * the object.
	 * 
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws SBBException
	 */
	public abstract void release() throws SBBException;

	/**
	 * Sending response. Delayed. 
	 * 
	 * @param response2
	 * @param object
	 */
	public abstract void sendResponse(OutgoingMessage response);

	/**
	 * Sending error. Delayed.
	 * 
	 * @param error
	 * @param ctx
	 */
	public abstract void sendError(ParticipantException error);

}