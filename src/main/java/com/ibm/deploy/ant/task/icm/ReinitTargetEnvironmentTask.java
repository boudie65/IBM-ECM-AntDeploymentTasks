package com.ibm.deploy.ant.task.icm;

import java.util.Locale;

import javax.security.auth.Subject;

import org.apache.tools.ant.BuildException;

import com.filenet.api.core.Connection;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.util.UserContext;
import com.ibm.casemgmt.api.admin.DevelopmentEnvironment;
import com.ibm.casemgmt.api.context.CaseMgmtContext;
import com.ibm.casemgmt.api.context.P8ConnectionCache;
import com.ibm.casemgmt.api.context.SimpleP8ConnectionCache;
import com.ibm.casemgmt.api.context.SimpleVWSessionCache;
import com.ibm.casemgmt.api.objectref.ObjectStoreReference;
import com.ibm.casemgmt.intgimpl.reinitos.ObjectStoreReinitStatus;
import com.ibm.deploy.ant.DeployTasks;

import lombok.Getter;
import lombok.Setter;

public class ReinitTargetEnvironmentTask extends DeployTasks {

	@Getter @Setter private String url;
	@Getter @Setter private String username;
	@Getter @Setter private String password;

	@Getter @Setter private String objectStoreName;
	@Getter @Setter private String connectionDefName;

	@Override
	public void execute() throws BuildException {

		validateInput();

		ObjectStore os = this.getObjectStore(this.objectStoreName);
		ObjectStoreReference designOS = new ObjectStoreReference(os);
		this.log("OS Reference grabbed");

		try {
			this.log("Invoking reset");
			DevelopmentEnvironment.reSet(url, designOS, connectionDefName,
					"true");
			this.log("Reset invoked");
		} catch (Exception e1) {
			this.log("Error: " + e1.getMessage());
		}

		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			this.log(e.getMessage());
		}
		ObjectStoreReinitStatus reinitStatus = new ObjectStoreReinitStatus(
				designOS, connectionDefName);
		this.log("Reinit status requested");

		while (reinitStatus.isReinitInProgress()) {
			this.log("Reinit busy");
		}
		reinitStatus = new ObjectStoreReinitStatus(
				designOS, connectionDefName);
		this.log("Reinit status is " + reinitStatus.getStatus());
	}

	/**
	 * Gets the object store.
	 * 
	 * @param osName
	 *            the os name
	 * @return the object store
	 */
	private ObjectStore getObjectStore(String osName) {

		// Create P8 Connection
		P8ConnectionCache connCache = new SimpleP8ConnectionCache();
		Connection connection = connCache.getP8Connection(this.url);

		Subject sub = UserContext.createSubject(connection, this.username,
				this.password, "icmP8WSI");
		UserContext uc = UserContext.get();
		uc.pushSubject(sub);
		Locale origLocale = uc.getLocale();
		uc.setLocale(origLocale);

		// Create ICM Connection
		@SuppressWarnings("unused")
		CaseMgmtContext origCmctx = CaseMgmtContext.set(new CaseMgmtContext(
				new SimpleVWSessionCache(), connCache));

		// Grab ObjectStore
		Domain domain = Factory.Domain.fetchInstance(connection, null, null);
		ObjectStore objectStore = Factory.ObjectStore.fetchInstance(domain,
				this.objectStoreName, null);

		return objectStore;
	}

	private void validateInput() {
		checkForNull("userName", username);
		checkForNull("password", password);
		checkForNull("url", url);
		checkForNull("objectStoreName", objectStoreName);
		checkForNull("connectionDefName", connectionDefName);
	}
}
