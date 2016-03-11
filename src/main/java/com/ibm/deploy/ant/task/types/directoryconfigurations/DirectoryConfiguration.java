package com.ibm.deploy.ant.task.types.directoryconfigurations;

import lombok.Getter;
import lombok.Setter;

/**
 * The Class DirectoryConfiguration.
 */
public class DirectoryConfiguration {

	/** The display name. */
	@Getter @Setter private String displayName;
	
	/** The server host. */
	@Getter @Setter private String serverHost;
	
	/** The server port. */
	@Getter @Setter private int serverPort;
	
	/** The server user name. */
	@Getter @Setter private String serverUserName;
	
	/** The server password. */
	@Getter @Setter private String serverPassword;
	
	/** The ssl enabled. */
	@Getter @Setter private Boolean sslEnabled;
	
	/** The user base dn. */
	@Getter @Setter private String userBaseDN;
	
	/** The User search filter. */
	@Getter @Setter private String userSearchFilter;
	
	/** The user display name attribute. */
	@Getter @Setter private String userDisplayNameAttribute;
	
	/** The group base dn. */
	@Getter @Setter private String groupBaseDN;
	
	/** The group search filter. */
	@Getter @Setter private String groupSearchFilter;
	
	/** The group display name attribute. */
	@Getter @Setter private String groupDisplayNameAttribute;
	
	/** The group name attribute. */
	@Getter @Setter private String groupNameAttribute;
	
	/** The group membership search filter. */
	@Getter @Setter private String groupMembershipSearchFilter;
	
	/** The restrict membership to configured realms. */
	@Getter @Setter private Boolean restrictMembershipToConfiguredRealms;

	/** The directory server type. */
	@Getter @Setter private String directoryServerType;
}