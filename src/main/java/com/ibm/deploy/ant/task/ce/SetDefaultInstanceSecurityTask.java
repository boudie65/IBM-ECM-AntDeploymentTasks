package com.ibm.deploy.ant.task.ce;

import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;

import com.filenet.api.core.Factory;
import com.filenet.api.security.User;
import com.filenet.api.security.AccessPermission;
import com.ibm.deploy.ant.task.types.Permission;
import com.ibm.deploy.ant.tools.ObjectStoreSecurityManager;

import lombok.Getter;
import lombok.Setter;

/**
 * The Class SetDefaultInstanceSecurityTask.
 *
 * @author pvdhorst
 *
 */
public class SetDefaultInstanceSecurityTask extends ObjectStoreNestedTask {

	/** The display name. */
	@Getter @Setter private String displayName;
		
	/** The reset default. */
	@Getter @Setter private boolean resetDefault;

    /** The Permissions. */
    private List<Permission> permissions = new ArrayList<Permission>();

    /* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	@Override
	public void execute() throws BuildException {
		validateInput();

		ObjectStoreSecurityManager manager = new ObjectStoreSecurityManager(
				getObjectStore(), this);

    	for ( Permission perm : getPermissions() ) {

    		User usr = manager.getUserObject(perm.getGranteeName());
    		
			AccessPermission ap = Factory.AccessPermission.createInstance();
			ap.set_GranteeName( usr.get_DistinguishedName() );
			ap.set_AccessType( perm.getAccessType() );
			ap.set_AccessMask( perm.getAccessMask() );
			ap.set_InheritableDepth( perm.getDepth() );
    		
			manager.addDefaultSecurity( getDisplayName(), ap, isResetDefault());
    	}
	}

	/**
	 * Validate input.
	 */
	public void validateInput() {
		checkForNull("displayName", getDisplayName());
		checkForNull("resetDefault", isResetDefault());
	}
	
	/**
	 * Adds the permission.
	 *
	 * @param permission the permission
	 */
	public void addPermission(Permission permission) {
		permissions.add(permission);
	} 
	
	/**
	 * Gets the permissions.
	 *
	 * @return the permissions
	 */
	protected List<Permission> getPermissions() {
		return permissions;
	}
}
