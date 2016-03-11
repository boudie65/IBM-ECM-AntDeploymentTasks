/**
 * 
 */
package com.ibm.deploy.ant.task.ce;

import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;

import com.filenet.api.admin.ClassDefinition;
import com.filenet.api.constants.RefreshMode;
import com.ibm.deploy.ant.tools.ObjectStoreClassDefinitionManager;
import com.ibm.deploy.ant.types.AddObjectPropertyAction;
import com.ibm.deploy.ant.types.AddPropertyDefinitionToClassDefinitionAction;
import com.ibm.deploy.ant.types.PropertyAction;

import lombok.Getter;
import lombok.Setter;

/**
 * The Class ImportClassTask.
 *
 * @author pvdhorst
 */
public class ImportClassTask extends ObjectStoreNestedTask{

	@Getter @Setter private String className;
	@Getter @Setter private String parentClassName;
	@Getter @Setter private String displayName;
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.ProjectComponent#getDescription()
	 */
	@Getter @Setter private String description = "";
	
	private List<AddPropertyDefinitionToClassDefinitionAction> addPropertyActions = new ArrayList<AddPropertyDefinitionToClassDefinitionAction>();
	private List<AddObjectPropertyAction> addObjectPropertyActions = new ArrayList<AddObjectPropertyAction>();
	
    @Override
	public void execute() throws BuildException {
        validateInput();
        
        ObjectStoreClassDefinitionManager manager = new ObjectStoreClassDefinitionManager(getObjectStore(), this, verbosity);
        ClassDefinition classDefinition = manager.getClassDefinition(className, getDisplayName(), parentClassName, description);
        
        for (AddPropertyDefinitionToClassDefinitionAction addPropertyAction : addPropertyActions) {
        	manager.addPropertyToClassDefinition(classDefinition, addPropertyAction.getName(), addPropertyAction );
        }
        
        classDefinition.save(RefreshMode.REFRESH);

        //now refresh the objectstore as we might need the info for other tasks
        getObjectStore().refresh();
    }
	
    public void validateInput() {
        checkForNull("className", getClassName() );
        checkForNull("displayName", getDisplayName() );
        checkForNull("parentClassName", getParentClassName() );
        
        validatePropertyActions(addPropertyActions);
        validatePropertyActions(addObjectPropertyActions);
    }

	private void validatePropertyActions(List<? extends PropertyAction> propertyActions ) {
		for (PropertyAction propertyAction : propertyActions) {
        	propertyAction.validate(getLocation());
        }
	}
    
	public void addConfigured(AddPropertyDefinitionToClassDefinitionAction addPropertyAction) {
		addPropertyActions.add(addPropertyAction);
	} 
	
	public void addConfigured(AddObjectPropertyAction addObjectPropertyAction) {
		addObjectPropertyActions.add(addObjectPropertyAction);
	} 
}
