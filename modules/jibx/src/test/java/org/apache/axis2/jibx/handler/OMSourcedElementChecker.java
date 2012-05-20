/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.axis2.jibx.handler;

import java.util.Iterator;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMSourcedElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class OMSourcedElementChecker extends AbstractHandler {
    private static final Log log = LogFactory.getLog(OMSourcedElementChecker.class);
    
    public InvocationResponse invoke(MessageContext msgContext) throws AxisFault {
        OMElement bodyElement = msgContext.getEnvelope().getBody().getFirstElement();
        if (bodyElement instanceof OMSourcedElement) {
            checkOMSourcedElement((OMSourcedElement)bodyElement);
        } else {
            // The body element may be a wrapper; check the children
            for (Iterator it = bodyElement.getChildElements(); it.hasNext(); ) {
                OMElement child = (OMElement)it.next();
                if (child instanceof OMSourcedElement) {
                    checkOMSourcedElement((OMSourcedElement)child);
                }
            }
        }
        return InvocationResponse.CONTINUE;
    }
    
    private void checkOMSourcedElement(OMSourcedElement element) {
        // Force OMSourcedElement to get the element name via QNameAwareOMDataSource
        log.info("Found OMSourcedElement: name=" + element.getLocalName()
                + "; namespace=" + element.getNamespaceURI()
                + "; prefix=" + element.getPrefix());
        // Now force expansion to let OMSourcedElement validate that the name is correct
        element.getFirstOMChild();
    }
}
