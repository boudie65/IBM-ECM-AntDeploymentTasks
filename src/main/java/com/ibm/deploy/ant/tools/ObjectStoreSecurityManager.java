package com.ibm.deploy.ant.tools;

import java.text.MessageFormat;
import java.util.Iterator;

import org.apache.tools.ant.Task;

import com.filenet.api.admin.ClassDefinition;
import com.filenet.api.collection.AccessPermissionList;
import com.filenet.api.collection.RepositoryRowSet;
import com.filenet.api.constants.PropertyNames;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.property.FilterElement;
import com.filenet.api.property.PropertyFilter;
import com.filenet.api.query.RepositoryRow;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.security.AccessPermission;
import com.filenet.api.security.User;

/**
 * The Class SecurityManager.
 *
 * @author pvdhorst
 */
public class ObjectStoreSecurityManager {

	/** The object store. */
	private ObjectStore objectStore;
	
	/** The task. */
	private final Task task;
	
	/**
	 * Instantiates a new security manager.
	 *
	 * @param objectStore the object store
	 * @param task the task
	 * @param verbosity the verbosity
	 */
	public ObjectStoreSecurityManager(ObjectStore objectStore, Task task) {
		this.objectStore = objectStore;
		this.task = task;
	}

	/**
	 * Adds the default security.
	 *
	 * @param displayName the display name
	 * @param description the description
	 * @param items the items
	 * @param dataType the data type
	 * @param accessType the access type
	 * @param accessMask the access mask
	 * @param depth the depth
	 * @param granteeName 
	 * @param clearSecurity 
	 */
	@SuppressWarnings("unchecked")
	public void addDefaultSecurity(String displayName, AccessPermission ap, boolean clearSecurity) {

	
		ClassDefinition classDef = getClassDefinitionByName(displayName);
		AccessPermissionList apl = getDefaultSecurity(classDef);
		
		if(clearSecurity)
		{
			clearDefaultSecurity(classDef);
		}
		
		if (apl != null) {
			task.log("Using AccessPermissions '" + apl + "'");

			apl.add(ap);
			
			classDef.set_DefaultInstancePermissions(apl);
			classDef.save(RefreshMode.NO_REFRESH);
		}

	}

	/**
	 * Clear default security.
	 *
	 * @param classDef the class def
	 * @return the class definition
	 */
	private ClassDefinition clearDefaultSecurity(ClassDefinition classDef) {
		classDef.set_DefaultInstancePermissions(Factory.AccessPermission
				.createList());
		classDef.save(RefreshMode.NO_REFRESH);
		return classDef;
	}
	
	/**
	 * Gets the default security.
	 *
	 * @param displayName the display name
	 * @return the default security
	 */
	private AccessPermissionList getDefaultSecurity(ClassDefinition classDef) {
		return classDef.get_DefaultInstancePermissions();
	}
	
	/**
	 * Gets the class definition by name.
	 *
	 * @param name the name
	 * @return the class definition by name
	 */
	private ClassDefinition getClassDefinitionByName(String name) {
		String classDefinitionQueryFormat = "SELECT [This] FROM [ClassDefinition] WHERE ([SymbolicName] = ''{0}'')";
		SearchScope scope = new SearchScope(objectStore);
		PropertyFilter pf = new PropertyFilter();
		pf.addIncludeProperty(new FilterElement(0, null, Boolean.TRUE,
				PropertyNames.ID, null));

		String query = MessageFormat.format(classDefinitionQueryFormat, name);
		RepositoryRowSet fetchRows = scope.fetchRows(new SearchSQL(query),
				new Integer(1999), pf, Boolean.TRUE);
		Iterator<?> iterator = fetchRows.iterator();
		if (!iterator.hasNext()) {
			task.log("ClassDefinition '" + name + "' not found");
			return null;
		}

		RepositoryRow row = (RepositoryRow) iterator.next();

		return (ClassDefinition) row.getProperties().getObjectValue("This");
	}

	/**
	 * Gets the user object.
	 *
	 * @param userName the user name
	 * @return the user object
	 */
	public User getUserObject(String userName) {
		User user = Factory.User.fetchInstance(this.objectStore.getConnection(), userName, null);
		return user;
	}

}
