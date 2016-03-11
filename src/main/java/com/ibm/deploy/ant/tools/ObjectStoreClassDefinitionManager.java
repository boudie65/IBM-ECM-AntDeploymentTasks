/**
 * 
 */
package com.ibm.deploy.ant.tools;

import java.util.Iterator;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import com.filenet.api.admin.ClassDefinition;
import com.filenet.api.admin.PropertyDefinition;
import com.filenet.api.admin.PropertyDefinitionBoolean;
import com.filenet.api.admin.PropertyDefinitionDateTime;
import com.filenet.api.admin.PropertyDefinitionInteger32;
import com.filenet.api.admin.PropertyDefinitionString;
import com.filenet.api.admin.PropertyTemplate;
import com.filenet.api.collection.PropertyDefinitionList;
import com.filenet.api.collection.PropertyTemplateSet;
import com.filenet.api.constants.PropertyNames;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.exception.EngineRuntimeException;
import com.filenet.api.exception.ExceptionCode;
import com.filenet.api.util.Id;
import com.ibm.deploy.ant.types.AddPropertyDefinitionToClassDefinitionAction;

/**
 * The Class ObjectStoreClassDefinitionManager.
 *
 * @author pvdhorst
 */
public class ObjectStoreClassDefinitionManager {

	/** The object store. */
	private final ObjectStore objectStore;
	
	/** The task. */
	private final Task task;
	
	/** The verbosity. */
	@SuppressWarnings("unused")
	private final int verbosity;
	
	/**
	 * Instantiates a new object store class definition manager.
	 *
	 * @param objectStore the object store
	 * @param task the task
	 * @param verbosity the verbosity
	 */
	public ObjectStoreClassDefinitionManager(ObjectStore objectStore, Task task, int verbosity) {
		this.objectStore = objectStore;
		this.task = task;
		this.verbosity = verbosity;
	}


	/**
	 * Gets the class definition.
	 *
	 * @param classDefinitionName the class definition name
	 * @param displayName the display name
	 * @param parentClassName the parent class name
	 * @param description the description
	 * @return the class definition
	 */
	public ClassDefinition getClassDefinition(String classDefinitionName, String displayName, String parentClassName, String description) 
	{
		ClassDefinition classDef = getExistingClassDefinition(classDefinitionName);
		
		if ( classDef == null ) 
		{
			task.log("Creating class definition '" + classDefinitionName + "'", Project.MSG_INFO);
			
			ClassDefinition parentClassDefinition = getExistingClassDefinition(parentClassName);
			ClassDefinition classDefinition = parentClassDefinition.createSubclass();
	
			classDefinition.set_SymbolicName(classDefinitionName);
			setClassDefinitionProperties(classDefinition, displayName, description);
			
			return classDefinition;
			
		} else {
			
            task.log("Updating class definition", Project.MSG_INFO);
			setClassDefinitionProperties(classDef, displayName, description);
			return classDef;
		}
	}

	/**
	 * Sets the class definition properties.
	 *
	 * @param classDefinition the class definition
	 * @param displayName the display name
	 * @param description the description
	 */
	@SuppressWarnings("unchecked")
	private void setClassDefinitionProperties(ClassDefinition classDefinition, String displayName,
			String description) {
        task.log("Setting localized DisplayNames", Project.MSG_VERBOSE);
		classDefinition.set_DisplayNames( Factory.LocalizedString.createList() );
		classDefinition.get_DisplayNames().add( CE_Utils.getLocalizedString(this.objectStore,displayName) );
		
        task.log("Setting localized Description", Project.MSG_VERBOSE);
		classDefinition.set_DescriptiveTexts(Factory.LocalizedString.createList() );
		classDefinition.get_DescriptiveTexts().add( CE_Utils.getLocalizedString(this.objectStore, description) );
	}

	/**
	 * Adds the property to class definition.
	 *
	 * @param classDefinition the class definition
	 * @param propertyName the property name
	 * @param action the action
	 */
	@SuppressWarnings("unchecked")
	public void addPropertyToClassDefinition(ClassDefinition classDefinition, String propertyName, AddPropertyDefinitionToClassDefinitionAction action )
	{
		PropertyDefinition propertyDefinition = getPropertyDefinition(classDefinition, propertyName );
		
		if ( propertyDefinition == null ) 
		{
			task.log("Adding property '" + propertyName + "'");
			
			PropertyTemplate propertyTemplate = getPropertyTemplate(propertyName); 
			propertyDefinition = propertyTemplate.createClassProperty();
			propertyDefinition.set_IsNameProperty( action.isNameProperty() );

			setPropertyDefinitionProperties(propertyDefinition, action);
			
			PropertyDefinitionList propertyDefinitions = classDefinition.get_PropertyDefinitions();
			propertyDefinitions.add( propertyDefinition);
		} else {

			task.log("Updating property '" + propertyName + "'");

			setPropertyDefinitionProperties(propertyDefinition, action);
		}
	}

	/**
	 * Sets the property definition properties.
	 *
	 * @param propertyDefinition the property definition
	 * @param action the action
	 */
	private void setPropertyDefinitionProperties(PropertyDefinition propertyDefinition, AddPropertyDefinitionToClassDefinitionAction action) {
		
		if ( action.getHidden() != null) {
			propertyDefinition.set_IsHidden( action.getHidden() );
		}
		
		if ( action.getRequired() != null ) {
			propertyDefinition.set_IsValueRequired( action.getRequired() );
		}
		
		if ( propertyDefinition instanceof PropertyDefinitionString ) {
			if ( action.getMaximumLength() != null) {
				((PropertyDefinitionString) propertyDefinition).set_MaximumLengthString(action.getMaximumLength() );
			}
		} else if( propertyDefinition instanceof PropertyDefinitionInteger32 )
		{
			if ( action.getMaximumLength() != null) {
				((PropertyDefinitionString) propertyDefinition).set_MaximumLengthString(action.getMaximumLength() );
			}
		} else if( propertyDefinition instanceof PropertyDefinitionBoolean)
		{
			
		} else if( propertyDefinition instanceof PropertyDefinitionDateTime )
		{
			PropertyDefinitionDateTime propDefDateTime = (PropertyDefinitionDateTime) propertyDefinition;
			if ( action.getMaximumDateTime() != null) {
				propDefDateTime.set_PropertyMaximumDateTime(action.getMaximumDateTime());
			}

			if ( action.getMinimumDateTime() != null) {
				propDefDateTime.set_PropertyMinimumDateTime(action.getMinimumDateTime());
			}

			if ( action.getDefaultDateTime() != null) {
				propDefDateTime.set_PropertyDefaultDateTime(action.getDefaultDateTime());
			}

		}
	}
	
	/**
	 * Removes the property from class.
	 *
	 * @param classDefinition the class definition
	 * @param propertyName the property name
	 */
	public void removePropertyFromClass(ClassDefinition classDefinition, String propertyName )
	{
		PropertyDefinition def = getPropertyDefinition(classDefinition, propertyName ); 
		if (  def != null ) 
		{
			task.log("Removing property '" + propertyName + "'");

			PropertyDefinitionList propertyDefinitions = classDefinition.get_PropertyDefinitions();
			if (propertyDefinitions.remove(def))
			{
				classDefinition.set_PropertyDefinitions(propertyDefinitions);
			}
		}
	}

	/**
	 * Gets the class id.
	 *
	 * @param name the name
	 * @return the class id
	 */
	public Id getClassId(String name )
	{
		ClassDefinition classDefinition = Factory.ClassDefinition.fetchInstance(objectStore, name, null);
		return classDefinition.get_Id();
	}	

	/**
	 * Gets the property template.
	 *
	 * @param propertyName the property name
	 * @return the property template
	 */
	private PropertyTemplate getPropertyTemplate(String propertyName) 
	{
		objectStore.refresh( new String[] { PropertyNames.PROPERTY_TEMPLATES, PropertyNames.SYMBOLIC_NAME, PropertyNames.MAXIMUM_LENGTH_STRING } );

		PropertyTemplateSet propertyTemplates = objectStore.get_PropertyTemplates();
		@SuppressWarnings("rawtypes")
		Iterator iterator = propertyTemplates.iterator();
		while ( iterator.hasNext() )
		{
			PropertyTemplate propertyTemplate = (PropertyTemplate) iterator.next();
			if ( propertyTemplate.get_SymbolicName().equals(propertyName) ) {
				return propertyTemplate;
			}
		}
		return null;
	}
	
	/**
	 * Gets the property definition.
	 *
	 * @param classDefinition the class definition
	 * @param propertyName the property name
	 * @return the property definition
	 */
	private PropertyDefinition getPropertyDefinition(ClassDefinition classDefinition, String propertyName) 
	{
		PropertyDefinitionList propertyDefinitions = classDefinition.get_PropertyDefinitions();

		@SuppressWarnings("rawtypes")
		Iterator iterator = propertyDefinitions.iterator();
		while ( iterator.hasNext() )
		{
			PropertyDefinition propertyDefinition = (PropertyDefinition) iterator.next();
			if ( propertyDefinition.getProperties().isPropertyPresent(PropertyNames.SYMBOLIC_NAME) && 
					propertyDefinition.get_SymbolicName().equals(propertyName) ) {
				return propertyDefinition;
			}
		}

		return null;
	}
	
	/**
	 * Gets the existing class definition. If not found, it returns null
	 *
	 * @param classDefinitionName the class definition name
	 * @return the existing class definition
	 */
	private ClassDefinition getExistingClassDefinition(String classDefinitionName) 
	{
		ClassDefinition classDef = null;
		try
		{
			classDef = Factory.ClassDefinition.fetchInstance(objectStore, classDefinitionName, null);
			task.log("Class '" + classDefinitionName +  "' exists");
		} 
		catch ( EngineRuntimeException e )
		{
			if ( !e.getExceptionCode().equals( ExceptionCode.E_BAD_CLASSID ) ) 
			{
				task.log("Class '" + classDefinitionName +  "' not found, error: " + e.getMessage());
			}
		}
		return classDef;
	}	
}
