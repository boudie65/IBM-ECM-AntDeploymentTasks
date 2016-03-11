package com.ibm.deploy.ant.task.types.directoryconfigurations;

import lombok.Getter;
import lombok.Setter;

public class DirectoryConfigurationAD extends DirectoryConfiguration {

	@Getter @Setter private Boolean AllowEmailOrUPNShortNames;
	
	@Getter @Setter private Integer ConnectionTimeout;
	
	@Getter @Setter private String GCHost;
	
	@Getter @Setter private String GDPort;
	
	@Getter @Setter private Boolean ReturnNameAsDN;
	
	@Getter @Setter private Boolean SearchCrossForestGroupMembership;
	
}
