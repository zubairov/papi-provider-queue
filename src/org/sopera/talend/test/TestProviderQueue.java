/*******************************************************************************
 * Copyright (c) 2010 SOPERA GmbH
 * All rights reserved. 
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.sopera.talend.test;

import javax.xml.namespace.QName;

import org.sopera.talend.provider.OperationType;
import org.sopera.talend.provider.QueuedRequest;
import org.sopera.talend.provider.QueuedMessageHandler;
import org.sopware.papi.ParticipantIdentity;
import org.sopware.papi.SBB;
import org.sopware.papi.SBBFactory;
import org.sopware.papi.untyped.IncomingMessage;
import org.sopware.papi.untyped.MessageFactory;
import org.sopware.papi.untyped.OutgoingMessage;
import org.sopware.papi.untyped.provider.RequestResponseOperationSkeleton;
import org.sopware.papi.untyped.provider.ServiceSkeleton;

import junit.framework.TestCase;

public class TestProviderQueue extends TestCase {

	public static final QName SERVICE_NAME = new QName(
			"http://services.sopware.org/demo/Whiteboard", "Whiteboard");

	public static final QName PROVIDER_NAME = new QName(
			"http://services.sopware.org/demo/Whiteboard", "WhiteboardProvider");

	public static final ParticipantIdentity PARTICIPANT_IDENTITY = new ParticipantIdentity() {

		@Override
		public String getInstanceID() {
			return "instance";
		}

		@Override
		public String getApplicationID() {
			return "application";
		}
	};

	public void testProviderQueue() throws Exception {
		SBB sbb = SBBFactory.getSBB(PARTICIPANT_IDENTITY);
		try {
			ServiceSkeleton skeleton = sbb.lookupServiceSkeleton(SERVICE_NAME,
					PROVIDER_NAME);
			assertNotNull(skeleton);
			RequestResponseOperationSkeleton operation = skeleton
					.getRequestResponseOperation("echo");
			MessageFactory factory = sbb.getEnvironment().getMessageFactory();
			QueuedMessageHandler handler = new QueuedMessageHandler(operation);
			while (true) {
				QueuedRequest request = handler.next();
				if (request == null) {
					break;
				}
				try {
					IncomingMessage msg = request.getMessage();
					System.err.println(msg.getXMLString());
					if (request.getType() == OperationType.REQUEST_RESPONSE) {
						OutgoingMessage response = factory.createMessage(msg
								.getXMLString());
						request.sendResponse(response);
					}
				} finally {
					// Very important to do it!
					request.release();
				}
			}
		} finally {
			sbb.release();
		}
	}

}
