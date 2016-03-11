package com.ibm.deploy.ant.types;

import java.util.Date;

import org.apache.tools.ant.Location;

import com.ibm.deploy.ant.DeployTasks;

import lombok.Getter;
import lombok.Setter;

public abstract class PropertyAction extends DeployTasks{

	@Getter @Setter private String name;
	@Getter @Setter private Boolean hidden;
	@Getter @Setter private Boolean required;
	@Getter @Setter private Integer maximumLength;
	@Getter @Setter private Date maximumDateTime;
	@Getter @Setter private Date minimumDateTime;
	@Getter @Setter private Date defaultDateTime;
	
	public void validate(Location location) {
		checkForNull("name", name, location);
	}

}
