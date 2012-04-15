package org.cloudsherpa.ui.server;

import org.openstack.client.ComputeClient;
import org.openstack.client.OpenStackClient;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public abstract class OpenStackRemoteServiceServlet extends RemoteServiceServlet {

	protected OpenStackClient getClient() {
		OpenStackSession session = (OpenStackSession) perThreadRequest.get().getSession().getAttribute(Constants.OPENSTACK_SESSION);
		return OpenStackClient.authenticate(session.getProperties(), session.getAccess());
		
	}
	
	protected ComputeClient getComputeClient() {
		OpenStackClient osc = getClient();
		return osc.getComputeClient();
	}
	
}
