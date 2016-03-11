package com.ibm.deploy.ant;

import java.text.MessageFormat;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * The Class DeployTasks.
 *
 * @author PHORST
 */
public abstract class DeployTasks extends Task {

	protected int verbosity = Project.MSG_INFO;

	/**
	 * Check for null.
	 *
	 * @param attributeName the attribute name
	 * @param attributeValue the attribute value
	 */
	protected void checkForNull(String attributeName, Object attributeValue) {
	    if ( attributeValue == null ) {
	        throw new BuildException(MessageFormat.format("The attribute ''{0}'' was not set", attributeName), getLocation() );
	    }
	}
	
    /**
     * Check for null.
     *
     * @param attributeName the attribute name
     * @param attributeValue the attribute value
     */
    protected void checkForNull(String attributeName, String attributeValue) {
        if ( attributeValue == null || attributeValue.isEmpty() ) {
            throw new BuildException(MessageFormat.format("The attribute ''{0}'' was not set", attributeName), getLocation());
        }
    }

    /**
     * Check for null.
     *
     * @param attributeName the attribute name
     * @param attributeValue the attribute value
     * @param location the location
     */
    protected void checkForNull(String attributeName, String attributeValue, Location location) {
        if ( attributeValue == null || attributeValue.isEmpty() ) {
            throw new BuildException(MessageFormat.format("The attribute ''{0}'' was not set", attributeName), location);
        }
    }
    
    /**
     * Validate.
     */
    protected void validate() {}
    
	public void setVerbose(boolean verbose) {
		if (verbose) {
			this.verbosity = Project.MSG_VERBOSE;
		} else {
			this.verbosity = Project.MSG_INFO;
		}
	}
}
