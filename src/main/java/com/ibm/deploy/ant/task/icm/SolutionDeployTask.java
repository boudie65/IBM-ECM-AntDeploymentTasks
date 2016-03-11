package com.ibm.deploy.ant.task.icm;

import java.util.Locale;

import javax.security.auth.Subject;

import org.apache.commons.json.JSONException;
import org.apache.tools.ant.BuildException;

import com.filenet.api.core.Connection;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.util.UserContext;
import com.ibm.casemgmt.api.admin.DevelopmentSolution;
import com.ibm.casemgmt.api.admin.SolutionStatus;
import com.ibm.casemgmt.api.context.CaseMgmtContext;
import com.ibm.casemgmt.api.context.P8ConnectionCache;
import com.ibm.casemgmt.api.context.SimpleP8ConnectionCache;
import com.ibm.casemgmt.api.context.SimpleVWSessionCache;
import com.ibm.casemgmt.api.objectref.DomainReference;
import com.ibm.casemgmt.api.objectref.ObjectStoreReference;
import com.ibm.deploy.ant.DeployTasks;

import lombok.Getter;
import lombok.Setter;

/**
 * The Class DeploySolutionTask.
 */
public class SolutionDeployTask extends DeployTasks {

	/** The url. */
	@Getter @Setter private String url;

	/** The username. */
	@Getter @Setter private String username;

	/** The password. */
	@Getter @Setter private String password;

	/** The object store name. */
	@Getter @Setter private String objectStoreName;

	/** The connection def name. */
	@Getter @Setter private String connectionDefName;

	/** The solution name. */
	@Getter @Setter private String solutionName;


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.tools.ant.Task#execute()
	 */
	@Override
	public void execute() throws BuildException {
		this.log("Solution deployment started with + " + this.url
				+ " " + this.username + " " + this.password + " "
				+ this.objectStoreName + " " + this.connectionDefName + " "
				+ this.solutionName);
		ObjectStore os = this.getObjectStore();
		this.log("OS retrieved "
				+ os.get_Domain().getConnection().getURI());

		ObjectStoreReference designOS = new ObjectStoreReference(
				new DomainReference(this.url), this.objectStoreName);
		this.log("Reference grabbed");

		DevelopmentSolution solutionDeployment = DevelopmentSolution
				.initiateDeployment(designOS, this.connectionDefName,
						this.solutionName);

		String statusString = "";
		while (!statusString.equals("Completed") && !statusString.equals("Failed")) {
			try {
				this.log("Grabbing status");
				statusString = getStatusInfo(solutionDeployment);
				this.log("Deployment status is " + statusString);
				Thread.sleep(500);
			} catch (Exception e) {
				this.log("Error: " + e.getMessage());
			}
		}
		try {
			printPostResponse(solutionDeployment);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Prints the post response.
	 * 
	 * @param solutionDeployment
	 *            the solution deployment
	 * @throws JSONException
	 *             the JSON exception
	 */
	private void printPostResponse(DevelopmentSolution solutionDeployment)
			throws JSONException {
		this.log("SolutionName: " + solutionDeployment.getName()
				+ " is " + getStatusInfo(solutionDeployment));
	}

	/**
	 * Gets the status info.
	 * 
	 * @param deployedSolution
	 *            the deployed solution
	 * @return the status info
	 */
	private String getStatusInfo(DevelopmentSolution deployedSolution) {
		SolutionStatus solStatus = deployedSolution.fetchStatus();
		return solStatus.getStatus().stringValue();
	}

	protected ObjectStore getObjectStore() {
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

	/**
	 * Validate input.
	 */
	protected void validateInput() {
		checkForNull("username", this.username);
		checkForNull("password", this.password);
		checkForNull("url", this.url);
		checkForNull("objectStoreName", this.objectStoreName);
		checkForNull("connectionDefName", this.connectionDefName);
		checkForNull("solutionName", this.solutionName);
	}
}
