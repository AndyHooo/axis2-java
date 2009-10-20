
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

package org.apache.axis2.jaxws.sample.faultsservice;

import org.test.polymorphicfaults.DerivedFault1;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.Holder;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

/**
 * This class was generated by the JAXWS SI.
 * JAX-WS RI 2.0_01-b15-fcs
 * Generated source version: 2.0
 * 
 */
@WebService(name = "FaultsServicePortType", targetNamespace = "http://org/test/polymorphicfaults")
public interface FaultsServicePortType {


    /**
     * 
     * @param symbol
     * @return
     *     returns float
     * @throws InvalidTickerFault_Exception
     * @throws DerivedFault2_Exception
     * @throws BaseFault_Exception
     * @throws DerivedFault1_Exception
     * @throws SimpleFault
     */
    @WebMethod
    @WebResult(name = "result", targetNamespace = "")
    @RequestWrapper(localName = "getQuote", targetNamespace = "http://org/test/polymorphicfaults", className = "org.test.polymorphicfaults.GetQuote")
    @ResponseWrapper(localName = "getQuoteResult", targetNamespace = "http://org/test/polymorphicfaults", className = "org.test.polymorphicfaults.GetQuoteResult")
    public float getQuote(
        @WebParam(name = "symbol", targetNamespace = "")
        String symbol)
        throws BaseFault_Exception, DerivedFault1_Exception, DerivedFault2_Exception, InvalidTickerFault_Exception, SimpleFault
    ;

    /**
     * 
     * @param paramC
     * @param paramB
     * @param paramA
     * @return
     *     returns int
     * @throws BaseFault_Exception
     * @throws ComplexFault_Exception
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "throwFault", targetNamespace = "http://org/test/polymorphicfaults", className = "org.test.polymorphicfaults.ThrowFault")
    @ResponseWrapper(localName = "throwFaultReturn", targetNamespace = "http://org/test/polymorphicfaults", className = "org.test.polymorphicfaults.ThrowFaultReturn")
    public int throwFault(
        @WebParam(name = "paramA", targetNamespace = "")
        int paramA,
        @WebParam(name = "paramB", targetNamespace = "")
        String paramB,
        @WebParam(name = "paramC", targetNamespace = "")
        float paramC)
        throws BaseFault_Exception, ComplexFault_Exception
    ;

    /**
     * 
     * @param paramZ
     * @param paramY
     * @param paramX
     * @param fault
     * @throws DerivedFault1_Exception
     * @throws EqualFault
     */
    @WebMethod
    @RequestWrapper(localName = "returnFault", targetNamespace = "http://org/test/polymorphicfaults", className = "org.test.polymorphicfaults.ReturnFault")
    @ResponseWrapper(localName = "returnFaultResponse", targetNamespace = "http://org/test/polymorphicfaults", className = "org.test.polymorphicfaults.ReturnFaultResponse")
    public void returnFault(
        @WebParam(name = "paramX", targetNamespace = "")
        int paramX,
        @WebParam(name = "paramY", targetNamespace = "")
        String paramY,
        @WebParam(name = "paramZ", targetNamespace = "")
        float paramZ,
        @WebParam(name = "fault", targetNamespace = "", mode = Mode.INOUT)
        Holder<DerivedFault1> fault)
        throws DerivedFault1_Exception, EqualFault
    ;

}
