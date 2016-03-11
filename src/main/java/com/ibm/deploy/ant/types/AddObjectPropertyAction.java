/**
 * 
 */
package com.ibm.deploy.ant.types;

import org.apache.tools.ant.Location;

/**
 * @author pvdhorst
 *
 */
public class AddObjectPropertyAction extends AddPropertyDefinitionToClassDefinitionAction {

	private String requiredClass;

	public String getRequiredClass() {
		return requiredClass;
	}

	public void setRequiredClass(String requiredClass) {
		this.requiredClass = requiredClass;
	}
	
	@Override
	public void validate(Location location) {
		super.validate(location);
		checkForNull("requiredClass", requiredClass, location);
	}
}
