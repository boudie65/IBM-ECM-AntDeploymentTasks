package com.ibm.deploy.ant.task.types;

import com.filenet.api.constants.AccessType;

import lombok.Getter;
import lombok.Setter;

/**
 * The Class Permission, used by the CPE security task.
 */
public class Permission {
	
	/** The grantee name. */
	@Getter @Setter private String granteeName;
	
	/** The access type. */
	@Getter private AccessType accessType; 
	
	/** The access mask. */
	@Getter @Setter private int accessMask;
	
	/** The inheritable depth. */
	@Getter @Setter private int depth;
	
	/**
	 * Sets the access type.
	 *
	 * @param accessType the new access type
	 */
	public void setAccessType(String accessType) {
		this.accessType = AccessType.getInstanceFromInt(new Integer(accessType).intValue() );
	}
}
