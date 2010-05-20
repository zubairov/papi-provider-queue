/*******************************************************************************
 * Copyright (c) 2010 SOPERA GmbH
 * All rights reserved. 
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.sopera.talend.provider;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.sopware.papi.exception.ParticipantException;
import org.sopware.papi.exception.SBBException;
import org.sopware.papi.untyped.CallContext;
import org.sopware.papi.untyped.IncomingMessage;
import org.sopware.papi.untyped.Operation;
import org.sopware.papi.untyped.OutgoingMessage;
import org.sopware.papi.untyped.provider.RequestResponseOperationSkeleton;

/**
 * Object that encapsulates the queued call. It is very important to call
 * {@link #release()} method when you are done with processing of given object
 * 
 * @author zubairov
 */
public class QueuedRequestImpl implements QueuedRequest {

	/**
	 * Exchange timeout in seconds
	 */
	private static final long EXCHANGE_TIMEOUT = 50;

	private OutgoingMessage response = null;
	private ParticipantException error = null;

	private final IncomingMessage msg;
	private final Operation operation;
	private final OperationType type;
	private final CallContext ctx;
	
	private final Exchanger<SBBException> exceptionExchange = new Exchanger<SBBException>();
	private final CountDownLatch latch = new CountDownLatch(1);

	public QueuedRequestImpl(IncomingMessage inMsg, Operation operation,
			OperationType type) {
		this.msg = inMsg;
		this.operation = operation;
		this.type = type;
		this.ctx = inMsg.getCallContext();
	}

	/**
	 * Returns the {@link IncomingMessage} that was passed with request
	 */
	public IncomingMessage getMessage() {
		return msg;
	}

	/**
	 * Convenience method that indeitify a type of the request. Same information could be obtained from {@link CallContext#getCommunicationStyle()}
	 */
	public OperationType getType() {
		return type;
	}

	/**
	 * Don't forget to call this method when you are done with processing of the {@link QueuedRequest}
	 */
	public void release() throws SBBException {
		latch.countDown();
		SBBException sbbException;
		try {
			sbbException = exceptionExchange.exchange(null,
					EXCHANGE_TIMEOUT, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new SBBException(e);
		} catch (TimeoutException e) {
			throw new SBBException(e);
		}
		if (sbbException != null) {
			throw sbbException;
		}
	}

	/**
	 * This operation have to be called on the SBB thread to send response if
	 * required
	 * 
	 * @throws InterruptedException
	 */
	void completeQueuedProcessing() throws InterruptedException {
		if (type == OperationType.REQUEST_RESPONSE) {
			RequestResponseOperationSkeleton op = ((RequestResponseOperationSkeleton) operation);
			try {
				if (response != null) {
					op.sendResponse(response, ctx);
				} else if (error != null) {
					op.sendError(error, ctx);
				}
				exceptionExchange.exchange(null);
			} catch (SBBException e) {
				exceptionExchange.exchange(e);
			}
		} else {
			exceptionExchange.exchange(null);
		}
	}

	/**
	 * @throws InterruptedException
	 */
	void waitForRelease(long timeout, TimeUnit unit)
			throws InterruptedException {
		latch.await(timeout, unit);
	}

	/**
	 * Similar to {@link RequestResponseOperationSkeleton#sendResponse(OutgoingMessage, CallContext)} however
	 * {@link CallContext} is automatically set from the {@link QueuedRequest} internal state.
	 * 
	 * And {@link SBBException} is thrown from {@link #release()} method instead.
	 */
	public void sendResponse(OutgoingMessage response) {
		this.response = response;
		this.error = null;
	}

	/**
	 * Similar to {@link RequestResponseOperationSkeleton#sendError(OutgoingMessage, CallContext)} however
	 * {@link CallContext} is automatically set from the {@link QueuedRequest} internal state.
	 * 
	 * And {@link SBBException} is thrown from {@link #release()} method instead.	 
	 **/
	public void sendError(ParticipantException error) {
		this.error = error;
		this.response = null;
	}
	
}
