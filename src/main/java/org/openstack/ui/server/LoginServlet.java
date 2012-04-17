package org.openstack.ui.server;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.openstack.client.OpenStackClient;
import org.openstack.model.identity.TenantList;

import com.google.gwt.core.client.GWT;

public class LoginServlet extends HttpServlet {
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		Properties properties = SetupServlet.loadProperties();
		
		if(properties.getProperty("identity.endpoint.publicURL") == null) {
			resp.sendRedirect(String.format("%s/setup",req.getContextPath()));
		} else {
			req.getRequestDispatcher("/index.jsp").forward(req, resp);
		}	
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		HttpSession session = req.getSession(false);
		if(session != null) {
			session.invalidate();
		}
		
		Properties properties = SetupServlet.loadProperties();
		
		if(properties.getProperty("identity.endpoint.publicURL") == null) {
			resp.sendRedirect(String.format("%s/setup",req.getContextPath()));
		}
		
		properties.setProperty("auth.username", req.getParameter("username"));
		properties.setProperty("auth.password", req.getParameter("password"));
		
		OpenStackClient openstack = OpenStackClient.authenticate(properties);
		
		TenantList tenants = openstack.getIdentityEndpoint().tenants().get();
		
		openstack.exchangeTokenForTenant(tenants.getList().get(0).getId());
		
		//openstack.reauthenticateOnTenant(tenants.getList().get(0).getName());
		
		OpenStackSession oss = new OpenStackSession();
		oss.setProperties(properties);
		oss.setAccess(openstack.getAccess());
		
		req.getSession().setAttribute(Constants.OPENSTACK_SESSION, oss);
		
		if(GWT.isProdMode()) {
			resp.sendRedirect(String.format("%s/portal.html",req.getContextPath()));
		} else {
			resp.sendRedirect(String.format("%s/portal.html?gwt.codesvr=127.0.0.1:9997",req.getContextPath()));
		}
		
	}

	

}
