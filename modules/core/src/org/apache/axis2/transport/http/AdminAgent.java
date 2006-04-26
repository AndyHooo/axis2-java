/*
* Copyright 2004,2006 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
*
*/

package org.apache.axis2.transport.http;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.deployment.util.PhasesInfo;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Provides methods to process axis2 admin requests.
 */
public class AdminAgent extends AbstractAgent {

  /**
   * Field LIST_MULTIPLE_SERVICE_JSP_NAME
   */
  private static final String LIST_SERVICE_GROUP_JSP = "ListServiceGroup.jsp";
  private static final String LIST_SERVICES_JSP_NAME = "listService.jsp";
  private static final String SELECT_SERVICE_JSP_NAME = "SelectService.jsp";
  private static final String IN_ACTIVATE_SERVICE_JSP_NAME = "InActivateService.jsp";
  private static final String ACTIVATE_SERVICE_JSP_NAME = "ActivateService.jsp";

  /**
   * Field LIST_SINGLE_SERVICE_JSP_NAME
   */
  private static final String LIST_PHASES_JSP_NAME = "viewphases.jsp";
  private static final String LIST_GLOABLLY_ENGAGED_MODULES_JSP_NAME = "globalModules.jsp";
  private static final String LIST_AVAILABLE_MODULES_JSP_NAME = "listModules.jsp";
  private static final String ENGAGING_MODULE_TO_SERVICE_JSP_NAME = "engagingtoaservice.jsp";
  private static final String ENGAGING_MODULE_TO_SERVICE_GROUP_JSP_NAME = "EngageToServiceGroup.jsp";
  private static final String ENGAGING_MODULE_GLOBALLY_JSP_NAME = "engagingglobally.jsp";
  public static final String ADMIN_JSP_NAME = "admin.jsp";
  private static final String VIEW_GLOBAL_HANDLERS_JSP_NAME = "ViewGlobalHandlers.jsp";
  private static final String VIEW_SERVICE_HANDLERS_JSP_NAME = "ViewServiceHandlers.jsp";
  private static final String SERVICE_PARA_EDIT_JSP_NAME = "ServiceParaEdit.jsp";
  private static final String ENGAGE_TO_OPERATION_JSP_NAME = "engagingtoanoperation.jsp";
  private static final String LOGIN_JSP_NAME = "Login.jsp";

  private File serviceDir;

  public AdminAgent(ConfigurationContext aConfigContext) {
    super(aConfigContext);
    File repoDir = new File(configContext.getAxisConfiguration().getRepository().getFile());
    serviceDir = new File(repoDir, "services");

    if (!serviceDir.exists()) {
      serviceDir.mkdirs();
    }
  }

  public void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
    throws IOException, ServletException {

    // We forward to login page if axis2 security is enabled
    // and the user is not authorized
    // TODO Fix workaround for login test
    if (axisSecurityEnabled() && authorizationRequired(httpServletRequest)) {
      renderView(LOGIN_JSP_NAME, httpServletRequest, httpServletResponse);
    } else {
      super.handle(httpServletRequest, httpServletResponse);
    }
  }

  protected void processIndex(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
    renderView(ADMIN_JSP_NAME, req, res);
  }

  // supported web operations

  protected void processUpload(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

    boolean isMultipart = FileUpload.isMultipartContent(req);
    if (isMultipart) {

      try {
        // Create a new file upload handler
        DiskFileUpload upload = new DiskFileUpload();

        List items = upload.parseRequest(req);

        // Process the uploaded items
        Iterator iter = items.iterator();
        while (iter.hasNext()) {
          FileItem item = (FileItem) iter.next();

          if (!item.isFormField()) {

            String fileName = item.getName();
            String fileExtesion = fileName;
            fileExtesion = fileExtesion.toLowerCase();
            if (!(fileExtesion.endsWith(".jar") || fileExtesion.endsWith(".aar"))) {
              req.setAttribute("status", "failure");
              req.setAttribute("cause", "Unsupported file type " + fileExtesion);
            } else {

              String fileNameOnly = "";
              if (fileName.indexOf("\\") < 0) {
                fileNameOnly = fileName.substring(fileName.lastIndexOf("/") + 1, fileName.length());
              } else {
                fileNameOnly = fileName.substring(fileName.lastIndexOf("\\") + 1, fileName.length());
              }


              File uploadedFile = new File(serviceDir, fileNameOnly);
              item.write(uploadedFile);
              req.setAttribute("status", "success");
              req.setAttribute("filename", fileNameOnly);
            }
          }
        }
      } catch (Exception e) {
        req.setAttribute("status", "failure");
        req.setAttribute("cause", e.getMessage());

      }
    }

    renderView("upload.jsp", req, res);
  }


  protected void processLogin(HttpServletRequest req, HttpServletResponse res)
    throws IOException, ServletException {
    String username = req.getParameter("userName");
    String password = req.getParameter("password");

    if ((username == null) || (password == null) || username.trim().equals("")
      || password.trim().equals("")) {
      req.setAttribute("errorMessage", "Invalid auth credentials!");
      renderView(LOGIN_JSP_NAME, req, res);
      return;
    }

    String adminUserName = (String) configContext.getAxisConfiguration().getParameter(
      Constants.USER_NAME).getValue();
    String adminPassword = (String) configContext.getAxisConfiguration().getParameter(
      Constants.PASSWORD).getValue();

    if (username.equals(adminUserName) && password.equals(adminPassword)) {
      req.getSession().setAttribute(Constants.LOGGED, "Yes");
      renderView(ADMIN_JSP_NAME, req, res);
    } else {
      req.setAttribute("errorMessage", "Invalid auth credentials!");
      renderView(LOGIN_JSP_NAME, req, res);
    }
  }

  protected void processEditServicePara(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
    if (req.getParameter("editServicepara") != null) {
      String serviceName = req.getParameter("axisService");
      AxisService service = configContext.getAxisConfiguration().getService(serviceName);

      if (service != null) {
        ArrayList service_para = service.getParameters();

        for (int i = 0; i < service_para.size(); i++) {
          Parameter parameter = (Parameter) service_para.get(i);
          String para = req.getParameter(serviceName + "_" + parameter.getName());

          service.addParameter(new Parameter(parameter.getName(), para));
        }

        for (Iterator iterator = service.getOperations(); iterator.hasNext();) {
          AxisOperation axisOperation = (AxisOperation) iterator.next();
          String op_name = axisOperation.getName().getLocalPart();
          ArrayList operation_para = axisOperation.getParameters();

          for (int i = 0; i < operation_para.size(); i++) {
            Parameter parameter = (Parameter) operation_para.get(i);
            String para = req.getParameter(op_name + "_" + parameter.getName());

            axisOperation.addParameter(new Parameter(parameter.getName(), para));
          }
        }
      }

      res.setContentType("text/css");

      PrintWriter out_writer = new PrintWriter(res.getOutputStream());

      out_writer.println("Parameters  changed Successfully");
      out_writer.flush();
      out_writer.close();
      req.getSession().removeAttribute(Constants.SERVICE);

      return;
    } else {
      String service = req.getParameter("axisService");

      if (service != null) {
        req.getSession().setAttribute(
          Constants.SERVICE, configContext.getAxisConfiguration().getService(service));
      }
    }

    renderView(SERVICE_PARA_EDIT_JSP_NAME, req, res);
  }


  protected void processEngagingGlobally(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
    HashMap modules = configContext.getAxisConfiguration().getModules();

    req.getSession().setAttribute(Constants.MODULE_MAP, modules);

    String moduleName = req.getParameter("modules");

    req.getSession().setAttribute(Constants.ENGAGE_STATUS, null);

    if (moduleName != null) {
      try {
        configContext.getAxisConfiguration().engageModule(new QName(moduleName));
        req.getSession().setAttribute(Constants.ENGAGE_STATUS,
          moduleName + " module engaged globally Successfully");
      } catch (AxisFault axisFault) {
        req.getSession().setAttribute(Constants.ENGAGE_STATUS, axisFault.getMessage());
      }
    }

    req.getSession().setAttribute("modules", null);
    renderView(ENGAGING_MODULE_GLOBALLY_JSP_NAME, req, res);

  }

  protected void processListOperations(HttpServletRequest req, HttpServletResponse res)
    throws IOException, ServletException {
    HashMap modules = configContext.getAxisConfiguration().getModules();

    req.getSession().setAttribute(Constants.MODULE_MAP, modules);

    String moduleName = req.getParameter("modules");

    req.getSession().setAttribute(Constants.ENGAGE_STATUS, null);
    req.getSession().setAttribute("modules", null);

    String serviceName = req.getParameter("axisService");

    if (serviceName != null) {
      req.getSession().setAttribute("service", serviceName);
    } else {
      serviceName = (String) req.getSession().getAttribute("service");
    }

    req.getSession().setAttribute(
      Constants.OPERATION_MAP,
      configContext.getAxisConfiguration().getService(serviceName).getOperations());
    req.getSession().setAttribute(Constants.ENGAGE_STATUS, null);

    String operationName = req.getParameter("axisOperation");

    if ((serviceName != null) && (moduleName != null) && (operationName != null)) {
      try {
        AxisOperation od = configContext.getAxisConfiguration().getService(
          serviceName).getOperation(new QName(operationName));

        od.engageModule(
          configContext.getAxisConfiguration().getModule(new QName(moduleName)),
          configContext.getAxisConfiguration());
        req.getSession().setAttribute(Constants.ENGAGE_STATUS,
          moduleName
            + " module engaged to the operation Successfully");
      } catch (AxisFault axisFault) {
        req.getSession().setAttribute(Constants.ENGAGE_STATUS, axisFault.getMessage());
      }
    }

    req.getSession().setAttribute("operation", null);
    renderView(ENGAGE_TO_OPERATION_JSP_NAME, req, res);
  }

  protected void processEngageToService(HttpServletRequest req, HttpServletResponse res)
    throws IOException, ServletException {
    HashMap modules = configContext.getAxisConfiguration().getModules();

    req.getSession().setAttribute(Constants.MODULE_MAP, modules);

    HashMap services = configContext.getAxisConfiguration().getServices();

    req.getSession().setAttribute(Constants.SERVICE_MAP, services);

    String moduleName = req.getParameter("modules");

    req.getSession().setAttribute(Constants.ENGAGE_STATUS, null);
    req.getSession().setAttribute("modules", null);

    String serviceName = req.getParameter("service");

    req.getSession().setAttribute(Constants.ENGAGE_STATUS, null);

    if ((serviceName != null) && (moduleName != null)) {
      try {
        configContext.getAxisConfiguration().getService(serviceName).engageModule(
          configContext.getAxisConfiguration().getModule(new QName(moduleName)),
          configContext.getAxisConfiguration());
        req.getSession().setAttribute(Constants.ENGAGE_STATUS,
          moduleName
            + " module engaged to the service Successfully");
      } catch (AxisFault axisFault) {
        req.getSession().setAttribute(Constants.ENGAGE_STATUS, axisFault.getMessage());
      }
    }

    req.getSession().setAttribute("service", null);
    renderView(ENGAGING_MODULE_TO_SERVICE_JSP_NAME, req, res);
  }

  protected void processEngageToServiceGroup(HttpServletRequest req, HttpServletResponse res)
    throws IOException, ServletException {
    HashMap modules = configContext.getAxisConfiguration().getModules();

    req.getSession().setAttribute(Constants.MODULE_MAP, modules);

    Iterator services = configContext.getAxisConfiguration().getServiceGroups();

    req.getSession().setAttribute(Constants.SERVICE_GROUP_MAP, services);

    String moduleName = req.getParameter("modules");

    req.getSession().setAttribute(Constants.ENGAGE_STATUS, null);
    req.getSession().setAttribute("modules", null);

    String serviceName = req.getParameter("service");

    req.getSession().setAttribute(Constants.ENGAGE_STATUS, null);

    if ((serviceName != null) && (moduleName != null)) {
      configContext.getAxisConfiguration().getServiceGroup(serviceName).engageModule(
        configContext.getAxisConfiguration().getModule(new QName(moduleName)));
      req.getSession().setAttribute(Constants.ENGAGE_STATUS,
        moduleName
          + " module engaged to the serviceGroup Successfully");
    }

    req.getSession().setAttribute("service", null);
    renderView(ENGAGING_MODULE_TO_SERVICE_GROUP_JSP_NAME, req, res);
  }


  protected void processLogout(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
    req.getSession().invalidate();
    renderView("index.jsp", req, res);
  }

  protected void processSelectServiceParaEdit(HttpServletRequest req, HttpServletResponse res)
    throws IOException, ServletException {
    HashMap services = configContext.getAxisConfiguration().getServices();

    req.getSession().setAttribute(Constants.SERVICE_MAP, services);
    req.getSession().setAttribute(Constants.SELECT_SERVICE_TYPE, "SERVICE_PARAMETER");
    renderView(SELECT_SERVICE_JSP_NAME, req, res);
  }

  protected void processListOperation(HttpServletRequest req, HttpServletResponse res)
    throws IOException, ServletException {
    HashMap services = configContext.getAxisConfiguration().getServices();

    req.getSession().setAttribute(Constants.SERVICE_MAP, services);
    req.getSession().setAttribute(Constants.SELECT_SERVICE_TYPE, "MODULE");

    renderView(SELECT_SERVICE_JSP_NAME, req, res);
  }

  protected void processActivateService(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
    if (req.getParameter("submit") != null) {
      String serviceName = req.getParameter("axisService");
      String turnon = req.getParameter("turnon");
      if (serviceName != null) {
        if (turnon != null) {
          AxisService service = configContext.getAxisConfiguration().getServiceForActivation(serviceName);
          service.setActive(true);
        }
      }
    } else {
      HashMap services = configContext.getAxisConfiguration().getServices();
      req.getSession().setAttribute(Constants.SERVICE_MAP, services);
    }

    renderView(ACTIVATE_SERVICE_JSP_NAME, req, res);
  }

  protected void processDeactivateService(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
    if (req.getParameter("submit") != null) {
      String serviceName = req.getParameter("axisService");
      String turnoff = req.getParameter("turnoff");
      if (serviceName != null) {
        if (turnoff != null) {
          AxisService service = configContext.getAxisConfiguration().getService(serviceName);
          service.setActive(false);
        }
      }
    } else {
      HashMap services = configContext.getAxisConfiguration().getServices();
      req.getSession().setAttribute(Constants.SERVICE_MAP, services);
    }

    renderView(IN_ACTIVATE_SERVICE_JSP_NAME, req, res);
  }


  protected void processViewGlobalHandlers(HttpServletRequest req, HttpServletResponse res)
    throws IOException, ServletException {
    req.getSession().setAttribute(Constants.GLOBAL_HANDLERS,
      configContext.getAxisConfiguration());

    renderView(VIEW_GLOBAL_HANDLERS_JSP_NAME, req, res);
  }

  protected void processViewServiceHandlers(HttpServletRequest req, HttpServletResponse res)
    throws IOException, ServletException {
    String service = req.getParameter("axisService");

    if (service != null) {
      req.getSession().setAttribute(Constants.SERVICE_HANDLERS,
        configContext.getAxisConfiguration().getService(service));
    }

    renderView(VIEW_SERVICE_HANDLERS_JSP_NAME, req, res);
  }


  protected void processListPhases(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
    ArrayList phaselist = new ArrayList();

    PhasesInfo info = configContext.getAxisConfiguration().getPhasesInfo();

    phaselist.add(info.getINPhases());
    phaselist.add(info.getIN_FaultPhases());
    phaselist.add(info.getOUTPhases());
    phaselist.add(info.getOUT_FaultPhases());
    phaselist.add(info.getOperationInPhases());
    phaselist.add(info.getOperationInFaultPhases());
    phaselist.add(info.getOperationOutPhases());
    phaselist.add(info.getOperationOutFaultPhases());
    req.getSession().setAttribute(Constants.PHASE_LIST, phaselist);

    renderView(LIST_PHASES_JSP_NAME, req, res);
  }

  protected void processListServiceGroups(HttpServletRequest req, HttpServletResponse res)
    throws IOException, ServletException {
    Iterator serviceGroups = configContext.getAxisConfiguration().getServiceGroups();
    HashMap services = configContext.getAxisConfiguration().getServices();

    req.getSession().setAttribute(Constants.SERVICE_MAP, services);
    req.getSession().setAttribute(Constants.SERVICE_GROUP_MAP, serviceGroups);

    renderView(LIST_SERVICE_GROUP_JSP, req, res);
  }

  protected void processListService(HttpServletRequest req, HttpServletResponse res)
    throws IOException, ServletException {
    HashMap services = configContext.getAxisConfiguration().getServices();

    req.getSession().setAttribute(Constants.SERVICE_MAP, services);
    req.getSession().setAttribute(Constants.ERROR_SERVICE_MAP,
      configContext.getAxisConfiguration().getFaultyServices());

    renderView(LIST_SERVICES_JSP_NAME, req, res);
  }

  protected void processListContexts(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
    req.getSession().setAttribute(Constants.CONFIG_CONTEXT, configContext);
    renderView("ViewContexts.jsp", req, res);
  }

  protected void processglobalModules(HttpServletRequest req, HttpServletResponse res)
    throws IOException, ServletException {
    Collection modules = configContext.getAxisConfiguration().getEngagedModules();

    req.getSession().setAttribute(Constants.MODULE_MAP, modules);

    renderView(LIST_GLOABLLY_ENGAGED_MODULES_JSP_NAME, req, res);
  }

  protected void processListModules(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
    HashMap modules = configContext.getAxisConfiguration().getModules();

    req.getSession().setAttribute(Constants.MODULE_MAP, modules);
    req.getSession().setAttribute(Constants.ERROR_MODULE_MAP,
      configContext.getAxisConfiguration().getFaultyModules());

    renderView(LIST_AVAILABLE_MODULES_JSP_NAME, req, res);
  }

  protected void processSelectService(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
    HashMap services = configContext.getAxisConfiguration().getServices();

    req.getSession().setAttribute(Constants.SERVICE_MAP, services);
    req.getSession().setAttribute(Constants.SELECT_SERVICE_TYPE, "VIEW");

    renderView(SELECT_SERVICE_JSP_NAME, req, res);
  }


  private boolean authorizationRequired(HttpServletRequest httpServletRequest) {
    return httpServletRequest.getSession().getAttribute(Constants.LOGGED) == null &&
      !httpServletRequest.getRequestURI().endsWith("login");
  }

  private boolean axisSecurityEnabled() {
    Parameter parameter = configContext.getAxisConfiguration().getParameter(Constants.ADMIN_SECURITY_DISABLED);
    return parameter == null || !"true".equals(parameter.getValue());
  }

}
