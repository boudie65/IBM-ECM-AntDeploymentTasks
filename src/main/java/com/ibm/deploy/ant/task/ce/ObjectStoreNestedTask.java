package com.ibm.deploy.ant.task.ce;

import com.filenet.api.core.ObjectStore;
import com.ibm.deploy.ant.DeployTasks;

import lombok.Getter;
import lombok.Setter;

/**
 * The Class ObjectStoreNestedTask.
 */
public class ObjectStoreNestedTask extends DeployTasks {

	/** The object store. */
	@Getter @Setter private ObjectStore objectStore;
	
}
