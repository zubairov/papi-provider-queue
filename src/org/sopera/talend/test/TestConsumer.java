/*******************************************************************************
 * Copyright (c) 2010 SOPERA GmbH
 * All rights reserved. 
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.sopera.talend.test;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.sopware.papi.ParticipantIdentity;
import org.sopware.papi.SBB;
import org.sopware.papi.SBBFactory;
import org.sopware.papi.untyped.IncomingMessage;
import org.sopware.papi.untyped.MessageFactory;
import org.sopware.papi.untyped.consumer.RequestResponseOperationProxy;
import org.sopware.papi.untyped.consumer.ServiceProxy;

public class TestConsumer extends TestCase {

	private static final Random rnd = new Random(System.currentTimeMillis());
	
	public static void main(String[] params) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(20);
		SBB sbb = SBBFactory.getSBB(new ParticipantIdentity() {
			
			@Override
			public String getInstanceID() {
				return "instance";
			}
			
			@Override
			public String getApplicationID() {
				return "application";
			}
		});
		try {
			ServiceProxy proxy = sbb.lookupServiceProxy(TestProviderQueue.SERVICE_NAME, "DefaultConsumerPolicy");
			final RequestResponseOperationProxy operation = proxy.getRequestResponseOperation("echo");
			final MessageFactory factory = sbb.getEnvironment().getMessageFactory();
			ArrayList<Callable<Void>> tasks = new ArrayList<Callable<Void>>();
			for(int i=0;i<1000;i++) {
				tasks.add(new Callable<Void>() {
					
					@Override
					public Void call() throws Exception {
						String msgStr = createMessage();
						System.out.println("Calling operation echo : " + Thread.currentThread().getName());
						IncomingMessage response = operation.callBlocking(factory.createMessage(msgStr));
						System.out.println("Called operation, response is " + response.getXMLString() + ": " + Thread.currentThread().getName());
						if (!msgStr.equals(response.getXMLString())) {
							throw new RuntimeException("Not equals!");
						}
						return null;
					}
				});
				
			}
			executor.invokeAll(tasks);
			executor.shutdown();
			executor.awaitTermination(1, TimeUnit.HOURS);
		} finally {
			sbb.release();
		}
		for(int i=0;i<10;i++) {
			System.out.println("Done!");
		}

	}
	
	private static String createMessage() {
		return "<test>" + rnd.nextInt(1000000) + "</test>";
	}
	
}
