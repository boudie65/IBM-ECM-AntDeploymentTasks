/**
 * 
 */
package com.ibm.deploy.ant.task.ce;

import java.util.ArrayList;
import java.util.List;

import javax.security.auth.Subject;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.filenet.api.core.Connection;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.util.UserContext;
import com.ibm.deploy.ant.DeployTasks;

import lombok.Getter;
import lombok.Setter;

/**
 * The Class ObjectStoreTask.
 *
 * @author pvdhorst
 */
public class ObjectStoreTask extends DeployTasks {

	/** The url. */
	@Getter @Setter private String url;
	
	/** The object store name. */
	@Getter @Setter private String objectStoreName;
	
	/** The user name. */
	@Getter @Setter private String userName;
	
	/** The password. */
	@Getter @Setter private String password;

    /** The tasks. */
    private List<Task> tasks = new ArrayList<Task>();
	
	/** The object store. */
	private ObjectStore objectStore;
    
    /**
     * Validate input.
     */
    protected void validateInput() {
        checkForNull("userName", userName);
        checkForNull("password", password);
        checkForNull("url", url);
        checkForNull("objectStoreName", objectStoreName);
    }
    
    /**
     * Gets the object store.
     *
     * @return the object store
     */
    protected ObjectStore getObjectStore() {
    	
    	if ( this.objectStore == null ) {
	        Connection connection = Factory.Connection.getConnection(getUrl());
	 
	        Subject sub = UserContext.createSubject(connection,getUserName(),getPassword(),null);
	        UserContext uc = UserContext.get();
	        uc.pushSubject(sub);
	        Domain domain = Factory.Domain.fetchInstance(connection, null, null);
	        this.objectStore = Factory.ObjectStore.fetchInstance(domain, getObjectStoreName(), null);
	         
	        log("Using object store '" + getObjectStoreName() + "'", verbosity);
    	}
         
        return this.objectStore;
    }       

    /* (non-Javadoc)
     * @see org.apache.tools.ant.Task#execute()
     */
    @Override
	public void execute() throws BuildException {
    	
    	validateInput();
    	
    	for ( Task task : getTasks() ) {
    		initializeTask(task);
    		task.perform();
    	}
    }

	protected List<Task> getTasks() {
		return tasks;
	}

	/**
	 * Initialize task.
	 *
	 * @param task the task
	 */
	protected void initializeTask(Task task) {
		if ( task instanceof ObjectStoreNestedTask ) {
			((ObjectStoreNestedTask)task).setObjectStore(getObjectStore());
		}
	}	

	/**
	 * Adds the importpropertytemplate task.
	 *
	 * @param nestedTask the nested task
	 */
	public void addConfiguredCreateproperty(ImportPropertyTemplateTask nestedTask) {
		addTask(nestedTask);
	}

	/**
	 * Adds the importclass task.
	 *
	 * @param nestedTask the nested task
	 */
	public void addConfiguredCreateClass(ImportClassTask nestedTask) {
		addTask(nestedTask);
	}
	
	/**
	 * Adds the set default instance security task.
	 *
	 * @param nestedTask the nested task
	 */
	public void addSetDefaultInstanceSecurity(SetDefaultInstanceSecurityTask nestedTask) {
		addTask(nestedTask);
	}

	/**
	 * Adds the task.
	 *
	 * @param nestedTask the nested task
	 * @return true, if successful
	 */
	protected boolean addTask(Task nestedTask) {
		return tasks.add(nestedTask);
	}
}
