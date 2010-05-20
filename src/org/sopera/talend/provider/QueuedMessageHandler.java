/*******************************************************************************
 * Copyright (c) 2010 SOPERA GmbH
 * All rights reserved. 
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.sopera.talend.provider;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.sopware.papi.exception.ParticipantException;
import org.sopware.papi.exception.SBBException;
import org.sopware.papi.untyped.CallContext;
import org.sopware.papi.untyped.IncomingMessage;
import org.sopware.papi.untyped.IncomingMessageHandler;
import org.sopware.papi.untyped.Operation;
import org.sopware.papi.untyped.consumer.NotificationOperationProxy;
import org.sopware.papi.untyped.provider.OnewayOperationSkeleton;
import org.sopware.papi.untyped.provider.RequestResponseOperationSkeleton;

/**
 * Implementation of {@link IncomingMessageHandler}
 * 
 * @author zubairov
 */
public class QueuedMessageHandler implements IncomingMessageHandler {

	private static final int MAX_QUEUE_SIZE = 1000;

	private static final int WAIT_TIMEOUT_SECONDS = 120;
	
	private final Operation operation;
	
	private final OperationType type;
	
	private static final BlockingQueue<QueuedRequestImpl> queue = new LinkedBlockingQueue<QueuedRequestImpl>(MAX_QUEUE_SIZE);
	
	public QueuedMessageHandler(Operation operation) throws SBBException {
		if (operation == null) {
			throw new NullPointerException("Operation can't be null");
		}
		if (operation instanceof RequestResponseOperationSkeleton) {
			type = OperationType.REQUEST_RESPONSE;
		} else if (operation instanceof OnewayOperationSkeleton) {
			type = OperationType.ONE_WAY;
		} else if (operation instanceof NotificationOperationProxy) {
			type = OperationType.NOTIFICATION;
		} else {
			type = OperationType.INVALID;
		}
		if (type == OperationType.INVALID) {
			throw new IllegalArgumentException("Operations of class " + operation.getClass() + " are not supported" +
					"Supperted operations are RequestResponseOperationSkeleton, OneWayOperationSkeleton and NotificationOperationProxy.");
		}
		this.operation = operation;
		if (type == OperationType.REQUEST_RESPONSE) {
			((RequestResponseOperationSkeleton)operation).registerMessageHandler(this);
		}
		if (type == OperationType.ONE_WAY) {
			((OnewayOperationSkeleton)operation).registerMessageHandler(this);
		}
		if (type == OperationType.NOTIFICATION) {
			((NotificationOperationProxy)operation).registerMessageHandler(this);
		}
	}

	@Override
	public void handleError(SBBException arg0, CallContext arg1)
			throws ParticipantException {
		throw new ParticipantException("Participant can't handle exception " + arg0 + " happened with " + arg1);
	}

	/**
	 * This method add a newly created {@link QueuedRequestImpl} into the internal blocking queue
	 * where consumer thread is waiting for it.
	 * Then it waits until the {@link QueuedRequestImpl} will be completed for request-response operations
	 */
	@Override
	public void handleMessage(IncomingMessage inMsg) throws ParticipantException {
		QueuedRequestImpl context = new QueuedRequestImpl(inMsg, operation, type);
		boolean inserted = queue.offer(context);
		if (!inserted) {
			try {
				context.release();
			} catch (Exception e) {
				e.printStackTrace();
			}
			throw new ParticipantException("Can't queue request, queue size of " + MAX_QUEUE_SIZE + " is exceeded");
		} else {
			try {
				context.waitForRelease(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
				context.completeQueuedProcessing();
			} catch (InterruptedException e) {
				throw new ParticipantException(e);
			}
		}
	}

	@Override
	public void onRelease(boolean arg0) {
		// TODO Auto-generated method stub
	}

	/**
	 * Blocking method to obtain the next message from the queue
	 * 
	 * @return
	 * @throws InterruptedException
	 */
	public QueuedRequest next() throws InterruptedException {
		return queue.take();
	}
}
