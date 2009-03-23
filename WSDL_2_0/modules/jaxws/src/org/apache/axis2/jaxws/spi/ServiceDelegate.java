/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.axis2.jaxws.spi;

import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.handler.HandlerResolver;
import javax.xml.ws.http.HTTPBinding;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.jaxws.ExceptionFactory;
import org.apache.axis2.jaxws.JAXWSClientContext;
import org.apache.axis2.jaxws.client.JAXBDispatch;
import org.apache.axis2.jaxws.client.XMLDispatch;
import org.apache.axis2.jaxws.client.proxy.JAXWSProxyHandler;
import org.apache.axis2.jaxws.description.DescriptionFactory;
import org.apache.axis2.jaxws.description.EndpointDescription;
import org.apache.axis2.jaxws.description.ServiceDescription;
import org.apache.axis2.jaxws.description.ServiceDescriptionWSDL;
import org.apache.axis2.jaxws.description.impl.PortInfoImpl;
import org.apache.axis2.jaxws.i18n.Messages;
import org.apache.axis2.jaxws.util.WSDLWrapper;

/**
 * The ServiceDelegate serves as the backing implementation for all of the 
 * methods in the {@link javax.xml.ws.Service} API.  This is the plug 
 * point for the client implementation. 
 */
public class ServiceDelegate extends javax.xml.ws.spi.ServiceDelegate {
    private Executor executor;

    private ServiceDescription serviceDescription;
    private QName serviceQname;
    private ServiceClient serviceClient = null;
    
    public ServiceDelegate(URL url, QName qname, Class clazz) throws WebServiceException{
    	super();
    	this.serviceQname = qname;

        if(!isValidServiceName()){
    		throw ExceptionFactory.makeWebServiceException(Messages.getMessage("serviceDelegateConstruct0", ""));
    	}
        serviceDescription = DescriptionFactory.createServiceDescription(url, serviceQname, clazz);
        if (isValidWSDLLocation()) {
            if(!isServiceDefined(serviceQname)){
            	throw ExceptionFactory.makeWebServiceException(Messages.getMessage("serviceDelegateConstruct0", serviceQname.toString(), url.toString()));
            }
        }
    }
    
    //================================================
    // JAX-WS API methods
    //================================================
    
    /*
     * (non-Javadoc)
     * @see javax.xml.ws.spi.ServiceDelegate#addPort(javax.xml.namespace.QName, java.lang.String, java.lang.String)
     */
    // Creates a DISPATCH ONLY port.  Per JAXWS Sec 4.1 javax.xm..ws.Service, p. 49, ports added via addPort method
    // are only suitibale for creating Distpach instances.
    public void addPort(QName portName, String bindingId, String endpointAddress)
        throws WebServiceException {
        
        EndpointDescription endpointDesc = 
            DescriptionFactory.updateEndpoint(serviceDescription, null, portName, ServiceDescription.UpdateType.ADD_PORT);
        // TODO: Need to set endpointAddress and set or check bindingId on the EndpointDesc
        endpointDesc.setEndpointAddress(endpointAddress);
        endpointDesc.setClientBindingID(bindingId);
    }
    /*
     * (non-Javadoc)
     * @see javax.xml.ws.spi.ServiceDelegate#createDispatch(javax.xml.namespace.QName, java.lang.Class, javax.xml.ws.Service.Mode)
     */
    public <T> Dispatch<T> createDispatch(QName qname, Class<T> clazz, Mode mode) throws WebServiceException {
    	if(qname == null){
    		throw ExceptionFactory.makeWebServiceException(Messages.getMessage("createDispatchFail0"));
    	}
    	if(!isValidDispatchType(clazz)){
    		throw ExceptionFactory.makeWebServiceException(Messages.getMessage("dispatchInvalidType"));
    	}
    	
        EndpointDescription endpointDesc = 
            DescriptionFactory.updateEndpoint(serviceDescription, null, qname, ServiceDescription.UpdateType.CREATE_DISPATCH);
        if (endpointDesc == null) {
            throw ExceptionFactory.makeWebServiceException(Messages.getMessage("createDispatchFail2", qname.toString()));
        }

        // FIXME: This call needs to be revisited.  Not really sure what we're trying to do here. 
        addBinding(endpointDesc.getClientBindingID());
    	
        XMLDispatch<T> dispatch = new XMLDispatch<T>(this, endpointDesc);
        if (mode != null) {
            dispatch.setMode(mode);
        }
        else {
            dispatch.setMode(Service.Mode.PAYLOAD);
        }
        
        if (serviceClient == null)
            serviceClient = getServiceClient(qname);
        
        dispatch.setServiceClient(serviceClient);
    	dispatch.setType(clazz);
        return dispatch;        
    }
    
    /*
     * (non-Javadoc)
     * @see javax.xml.ws.spi.ServiceDelegate#createDispatch(javax.xml.namespace.QName, javax.xml.bind.JAXBContext, javax.xml.ws.Service.Mode)
     */
    public Dispatch<java.lang.Object> createDispatch(QName qname, JAXBContext context, Mode mode) {
        if (qname == null) {
            throw ExceptionFactory.makeWebServiceException(Messages.getMessage("createDispatchFail0"));
        }
        
        EndpointDescription endpointDesc = 
            DescriptionFactory.updateEndpoint(serviceDescription, null, qname, ServiceDescription.UpdateType.CREATE_DISPATCH);
        if (endpointDesc == null) {
            throw ExceptionFactory.makeWebServiceException(Messages.getMessage("createDispatchFail2", qname.toString()));
        }
        
        addBinding(endpointDesc.getClientBindingID());
        
        JAXWSClientContext clientCtx = createClientContext(endpointDesc, Object.class, mode);
        clientCtx.setJAXBContext(context);
        
        JAXBDispatch<Object> dispatch = new JAXBDispatch(this, endpointDesc);
        
        if (mode != null) {
            dispatch.setMode(mode);
        }
        else {
            dispatch.setMode(Service.Mode.PAYLOAD);
        }
        
        if (serviceClient == null)
            serviceClient = getServiceClient(qname);
        
        dispatch.setJAXBContext(context);
        dispatch.setServiceClient(serviceClient);
        
        return dispatch;
    }

    /*
     * (non-Javadoc)
     * @see javax.xml.ws.spi.ServiceDelegate#getPort(java.lang.Class)
     */
    public <T> T getPort(Class<T> sei) throws WebServiceException {
       return getPort(null, sei);
    }
    
    /*
     * (non-Javadoc)
     * @see javax.xml.ws.spi.ServiceDelegate#getPort(javax.xml.namespace.QName, java.lang.Class)
     */
    public <T> T getPort(QName portName, Class<T> sei) throws WebServiceException {
        /* TODO Check to see if WSDL Location is provided.
         * if not check WebService annotation's WSDLLocation
         * if both are not provided then throw exception.
         * (JLB): I'm not sure lack of WSDL should cause an exception
         */
        

    	if(!isValidWSDLLocation()){
    		//TODO: Should I throw Exception if no WSDL
    		//throw ExceptionFactory.makeWebServiceException("WSLD Not found");
    	}
    	if(sei == null){
    		throw ExceptionFactory.makeWebServiceException(Messages.getMessage("getPortInvalidSEI", portName.toString(), "null"));
    	}

        EndpointDescription endpointDesc = 
            DescriptionFactory.updateEndpoint(serviceDescription, sei, portName, ServiceDescription.UpdateType.GET_PORT);
        if (endpointDesc == null) {
            // TODO: NLS
            throw ExceptionFactory.makeWebServiceException("Unable to getPort for port QName " + portName.toString());
        }
        
    	JAXWSProxyHandler proxyHandler = new JAXWSProxyHandler(this, sei, endpointDesc);
    	
    	Class[] seiClazz = new Class[]{sei, BindingProvider.class};
    	Object proxyClass = Proxy.newProxyInstance(sei.getClassLoader(), seiClazz, proxyHandler);
    	return sei.cast(proxyClass);
    }
    
    /*
     * (non-Javadoc)
     * @see javax.xml.ws.spi.ServiceDelegate#getExecutor()
     */
    public Executor getExecutor() {
    	//FIXME: Use client provider executor too.	
        executor = getDefaultExecutor();
        return executor;
     }
    
    /*
     * (non-Javadoc)
     * @see javax.xml.ws.spi.ServiceDelegate#getHandlerResolver()
     */
    public HandlerResolver getHandlerResolver() {
        return null;
    }
    
    /*
     * (non-Javadoc)
     * @see javax.xml.ws.spi.ServiceDelegate#getPorts()
     */
    public Iterator<QName> getPorts() {
        return null;
    }
    
    /*
     * (non-Javadoc)
     * @see javax.xml.ws.spi.ServiceDelegate#getServiceName()
     */
    public QName getServiceName() {
        return serviceQname;
    }
    
    /*
     * (non-Javadoc)
     * @see javax.xml.ws.spi.ServiceDelegate#getWSDLDocumentLocation()
     */
    public URL getWSDLDocumentLocation() {
        return ((ServiceDescriptionWSDL) serviceDescription).getWSDLLocation();
    }
    
    /*
     * (non-Javadoc)
     * @see javax.xml.ws.spi.ServiceDelegate#setExecutor(java.util.concurrent.Executor)
     */
    public void setExecutor(Executor e) {
        if (e == null) {
            throw ExceptionFactory.makeWebServiceException(Messages.getMessage("cannotSetExcutorToNull"));
        }
        
        executor = e;
    }
    
    /*
     * (non-Javadoc)
     * @see javax.xml.ws.spi.ServiceDelegate#setHandlerResolver(javax.xml.ws.handler.HandlerResolver)
     */
    public void setHandlerResolver(HandlerResolver handlerresolver) {
        
    }
    
    //================================================
    // Internal public APIs
    //================================================
    
    /**
     * Get the ServiceDescription tree that this ServiceDelegate 
     */
    public ServiceDescription getServiceDescription() {
        return serviceDescription;
    }
    
    //TODO Change when ServiceDescription has to return ServiceClient or OperationClient
    /**
     * 
     */
    public ServiceClient getServiceClient(QName portQName) throws WebServiceException {
    	return serviceDescription.getServiceClient(portQName);      
    }

    //================================================
    // Impl methods
    //================================================
    
    //TODO: Need to make the default number of threads configurable
    private Executor getDefaultExecutor(){
        return Executors.newFixedThreadPool(3);
    }

    private <T> JAXWSClientContext<T> createClientContext(EndpointDescription epDesc, Class<T> clazz, Mode mode){
        JAXWSClientContext<T> clientContext = new JAXWSClientContext<T>();
        clientContext.setServiceDescription(serviceDescription);
        clientContext.setEndpointDescription(epDesc);
        clientContext.setClazz(clazz);
        clientContext.setServiceMode(mode);
        clientContext.setExecutor(this.getExecutor());  
        return clientContext;
    }
    
    private boolean isValidServiceName(){
    	return serviceQname != null && !"".equals(serviceQname.toString().trim());	
    }

    private boolean isValidWSDLLocation(){
        URL wsdlLocation = getWSDLDocumentLocation();
    	return wsdlLocation != null && !"".equals(wsdlLocation.toString().trim());
    }
    
    // TODO: Remove this method and put the WSDLWrapper methods on the ServiceDescriptor directly
    private WSDLWrapper getWSDLWrapper() {
    	return ((ServiceDescriptionWSDL) serviceDescription).getWSDLWrapper();
    }
    
    private boolean isServiceDefined(QName serviceName){
    	return getWSDLWrapper().getService(serviceName)!= null;
    }
    
    private void addBinding(String bindingId){
        // TODO: before creating binding do I have to do something with Handlers ... how is Binding related to Handler, this mistry sucks!!!
        if(bindingId != null){
	        //TODO: create all the bindings here
	        if(bindingId.equals(SOAPBinding.SOAP11HTTP_BINDING)){
	        	//instantiate soap11 binding implementation here and call setBinding in BindingProvider
	        }
	        
	        if(bindingId.equals(SOAPBinding.SOAP12HTTP_BINDING)){
	        	//instantiate soap11 binding implementation here and call setBinding in BindingProvider
	        }
	        
	        if(bindingId.equals(HTTPBinding.HTTP_BINDING)){
	        	//instantiate http binding implementation here and call setBinding in BindingProvider
	        }
        }
    }
    
    private boolean isValidDispatchType(Class clazz) {
    	return clazz != null && (clazz == String.class || 
    			clazz == Source.class || 
    			clazz == SOAPMessage.class);
    }
}