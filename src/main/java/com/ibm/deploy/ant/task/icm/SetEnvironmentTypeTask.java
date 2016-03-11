package com.ibm.deploy.ant.task.icm;

import java.util.Iterator;
import java.util.Locale;

import javax.security.auth.Subject;

import org.apache.tools.ant.BuildException;

import com.filenet.api.admin.ClassDefinition;
import com.filenet.api.admin.PropertyDefinition;
import com.filenet.api.collection.PropertyDefinitionList;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Connection;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.util.UserContext;
import com.ibm.casemgmt.api.constants.TargetEnvironmentType;
import com.ibm.casemgmt.api.context.P8ConnectionCache;
import com.ibm.casemgmt.api.context.SimpleP8ConnectionCache;
import com.ibm.deploy.ant.DeployTasks;

import lombok.Getter;
import lombok.Setter;

/**
 * The Class DeploySolutionTask.
 * @author pvdhorst
 */
public class SetEnvironmentTypeTask extends DeployTasks {

	/** The url. */
	@Getter @Setter private String url;

	/** The username. */
	@Getter @Setter private String username;

	/** The password. */
	@Getter @Setter private String password;

	/** The object store name. */
	@Getter @Setter private String objectStoreName;

	/** The connection def name. */
	@Getter private TargetEnvironmentType targetEnvironmentType;

	/**
	 * Sets the target environment type.
	 * 
	 * @param targetEnvironmentType
	 *            the new target environment type
	 */
	public void setTargetEnvironmentType(Integer targetEnvironmentType) {
		this.log("Setting type value " +targetEnvironmentType);
		this.targetEnvironmentType = TargetEnvironmentType
				.fromIntValue(Integer.valueOf(targetEnvironmentType));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.tools.ant.Task#execute()
	 */
	@Override
	public void execute() throws BuildException {
		this.log("Setting Target environment type");

		ObjectStore os = this.getObjectStore();
		this.log("Target OS retrieved "
				+ os.get_Domain().getConnection().getURI());

		ClassDefinition classDef = Factory.ClassDefinition.fetchInstance(os,
				"CmAcmDeployedSolution", null);

		PropertyDefinitionList pdl = classDef.get_PropertyDefinitions();

		@SuppressWarnings("unchecked")
		Iterator<PropertyDefinition> iter = pdl.iterator();
		boolean found = false;
		while ((iter.hasNext()) && (!found)) {
			PropertyDefinition pd = iter.next();
			String symName = pd.get_SymbolicName();
			if (symName.compareTo("CmAcmTargetEnvironmentType") == 0) {
				try {
					this.log("Current value "
							+ pd.getProperties()
									.get("PropertyDefaultInteger32")
									.getInteger32Value());
					pd.getProperties()
							.get("PropertyDefaultInteger32")
							.setObjectValue(
									Integer.valueOf(targetEnvironmentType
											.intValue()));
					this.log("New value "
							+ pd.getProperties()
									.get("PropertyDefaultInteger32")
									.getInteger32Value());
					found = true;
				} catch (Exception localException) {
				}
			}
		}
		classDef.save(RefreshMode.REFRESH);
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
		checkForNull("targetEnvironmentType",
				this.targetEnvironmentType.stringValue());
	}
}
