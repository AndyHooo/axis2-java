package org.apache.axis2.context;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.deployment.DeploymentException;
import org.apache.axis2.deployment.FileSystemConfigurator;
import org.apache.axis2.deployment.URLBasedAxisConfigurator;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisConfigurator;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.modules.Module;
import org.apache.axis2.transport.TransportSender;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public class ConfigurationContextFactory {

    protected static final Log log = LogFactory.getLog(ConfigurationContextFactory.class);

    /**
     * Creates a AxisConfiguration depending on the user requirment.
     * First creates an AxisConfigurator object with appropriate parameters.
     * Depending on the implementation getAxisConfiguration(), gets
     * the AxisConfiguration and uses it to create the ConfigurationContext.
     *
     * @param axisConfigurator
     * @return Returns ConfigurationContext.
     * @throws AxisFault
     */
    public static ConfigurationContext createConfigurationContext(
            AxisConfigurator axisConfigurator) throws AxisFault {
        AxisConfiguration axisConfig = axisConfigurator.getAxisConfiguration();
        ConfigurationContext configContext = new ConfigurationContext(axisConfig);
        //To override context path
        setContextPath(axisConfig, configContext);
        //To check whether transport level session managment is require or not
        configureTransportSessionManagement(axisConfig);
        init(configContext);
        axisConfigurator.engageGlobalModules();
        axisConfigurator.loadServices();
        return configContext;
    }

    private static void configureTransportSessionManagement(AxisConfiguration axisConfig) {
        Parameter manageSession = axisConfig.getParameter(Constants.MANAGE_TRANSPORT_SESSION);
        if (manageSession != null) {
            String value = ((String) manageSession.getValue()).trim();
            axisConfig.setManageTransportSession(Boolean.getBoolean(value));
        }
    }

    private static void setContextPath(AxisConfiguration axisConfig, ConfigurationContext configContext) {
        // Checking for context path
        Parameter servicePath = axisConfig.getParameter(Constants.SERVICE_PATH);
        if (servicePath != null) {
            String spath = ((String) servicePath.getValue()).trim();
            if (spath.length() > 0) {
                configContext.setServicePath(spath);
            }
        }
        Parameter contextPath = axisConfig.getParameter(Constants.CONTEXT_PATH);
        if (contextPath != null) {
            String cpath = ((String) contextPath.getValue()).trim();
            if (cpath.length() > 0) {
                configContext.setContextPath(cpath);
            }
        }
    }

    /**
     * To get a ConfigurationContext for  given data , and underline implementation
     * is Axis2 default impl which is file system based deployment model to create
     * an AxisConfiguration.
     * <p/>
     * Here either or both parameter can be null. So that boil down to following
     * scanarios and it should note that parameter value should be pull path ,
     * you are not allowed to give one relative to other. And these two can be located
     * in completely different locations.
     * <ul>
     * <li>If none of them are null , then AxisConfiguration will be based on the
     * value of axis2xml , and the repositoy will be the value specified by the
     * path paramter and there will not be any assumptions.</li>
     * <li>If axis2xml is null , then the repository will be the value specfied by
     * path parameter and , system will try to find axis2.xml from sub directory
     * called "conf" inside the repository, so if system find
     * repository/conf/axis2/xml then AxisConfiguration will be created using that
     * else AxisConfiguration will be created using default_axis2.xml</li>
     * <li>If path parameter is null , then AxisConfiguration will be created using
     * that axis2.xml. And after creatig AxisConfiguration system will try to
     * find user has specified repository parameter in axis2.xml
     * (&lt;parameter name="repository"&gt;locationo of the repo&lt;/parameter&gt;) , if it
     * find that then repository will be the value specified by that parameter.</li>
     * <li>If both are null , then it is simple , AixsConfiguration will be created
     * using default_axis2.xml and thats it.</li>
     * </ul>
     * <p/>
     * Note : rather than passing any paremeters you can give them as System
     * properties. Simple you can add following system properties before
     * you call this.
     * <ul>
     * <li>axis2.repo : same as path paramter</li>
     * <li>axis2.xml  : same as axis2xml</li>
     * </ul>
     *
     * @param path     : location of the repository
     * @param axis2xml : location of the axis2.xml (configuration) , you can not give
     *                 axis2xml relative to repository.
     * @return Returns the built ConfigurationContext.
     * @throws DeploymentException
     */
    public static ConfigurationContext createConfigurationContextFromFileSystem(
            String path,
            String axis2xml) throws AxisFault {
        return createConfigurationContext(new FileSystemConfigurator(path, axis2xml));
    }

    public static ConfigurationContext createConfigurationContextFromURIs(
            URL axis2xml, URL repositoy) throws AxisFault {
        return createConfigurationContext(new URLBasedAxisConfigurator(axis2xml, repositoy));
    }

    /**
     * Initializes modules and creates Transports.
     */

    private static void init(ConfigurationContext configContext) throws AxisFault {
        try {
            initModules(configContext);
            initTransportSenders(configContext);
        } catch (DeploymentException e) {
            throw new AxisFault(e);
        }
    }

    /**
     * Initializes the modules. If the module needs to perform some recovery process
     * it can do so in init and this is different from module.engage().
     *
     * @param context
     * @throws DeploymentException
     */
    private static void initModules(ConfigurationContext context) throws DeploymentException {
        try {
            HashMap modules = context.getAxisConfiguration().getModules();
            Collection col = modules.values();

            for (Iterator iterator = col.iterator(); iterator.hasNext();) {
                AxisModule axismodule = (AxisModule) iterator.next();
                Module module = axismodule.getModule();

                if (module != null) {
                    module.init(context, axismodule);
                }
            }
        } catch (AxisFault e) {
            throw new DeploymentException(e);
        }
    }

    /**
     * Initializes TransportSenders and TransportListeners with appropriate configuration information
     *
     * @param configContext
     */
    public static void initTransportSenders(ConfigurationContext configContext) {
        AxisConfiguration axisConf = configContext.getAxisConfiguration();

        // Initialize Transport Outs
        HashMap transportOuts = axisConf.getTransportsOut();

        Iterator values = transportOuts.values().iterator();

        while (values.hasNext()) {
            TransportOutDescription transportOut = (TransportOutDescription) values.next();
            TransportSender sender = transportOut.getSender();

            if (sender != null) {
                try {
                    sender.init(configContext, transportOut);
                } catch (AxisFault axisFault) {
                    log.info(Messages.getMessage("transportiniterror", transportOut.getName().getLocalPart()));
                }
            }
        }
    }

    /**
     * Gets the default configuration context by using the file system based AxisConfiguration.
     *
     * @return Returns ConfigurationContext.
     */
    public static ConfigurationContext createEmptyConfigurationContext() {
        return new ConfigurationContext(new AxisConfiguration());
    }
}
