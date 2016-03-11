package com.ibm.deploy.ant.task.ce;

import java.util.ArrayList;
import java.util.List;

import javax.security.auth.Subject;

import org.apache.tools.ant.Task;

import com.filenet.api.action.Create;
import com.filenet.api.admin.DirectoryConfigurationIBM;
import com.filenet.api.collection.AccessPermissionList;
import com.filenet.api.collection.DirectoryConfigurationList;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Connection;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.security.AccessPermission;
import com.filenet.api.util.UserContext;
import com.ibm.deploy.ant.task.types.Permission;
import com.ibm.deploy.ant.task.types.directoryconfigurations.DirectoryConfiguration;

import lombok.Getter;
import lombok.Setter;

public class CreateP8Domain extends Task {

	private List<Permission> permissions = new ArrayList<Permission>();
	private DirectoryConfiguration dc;
	@Getter @Setter private String userName;
	@Getter @Setter private String password;
	@Getter @Setter private String url;
	@Getter @Setter private String stanza;
	@Getter @Setter private String domainName;

	public void add(Permission c) {
		permissions.add(c);
	}

	public void add(DirectoryConfiguration d) {
		this.dc = d;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void execute() {
		
		// iterator over the conditions
		Connection localConnection = Factory.Connection.getConnection(this.getUrl());
		Subject localSubject = UserContext.createSubject(localConnection, this.getUserName(), this.getPassword(), this.getStanza());
		UserContext localUserContext = UserContext.get();
		localUserContext.pushSubject(localSubject);

		Domain localDomain = Factory.Domain.getInstance(localConnection, this.getDomainName());
		localDomain.addPendingAction(new Create("Domain", null, null, null, null, null));
		localDomain.set_Name( this.getDomainName());
		localDomain.save(RefreshMode.REFRESH);
		localDomain.set_DirectoryConfigurations(getDirectoryConfigurationList());
		localDomain.save(RefreshMode.REFRESH);

		AccessPermissionList localAccessPermissionList = Factory.AccessPermission.createList();
		for(Permission p : permissions)
		{
			AccessPermission localAccessPermission = Factory.AccessPermission.createInstance();
			localAccessPermission.set_GranteeName( p.getGranteeName() );
			localAccessPermission.set_AccessType( p.getAccessType() );
			localAccessPermission.set_AccessMask( p.getAccessMask() );
			localAccessPermissionList.add(localAccessPermission);
		}
		localDomain.set_Permissions(localAccessPermissionList);
		localDomain.save(RefreshMode.REFRESH);
	}

	@SuppressWarnings("unchecked")
	private DirectoryConfigurationList getDirectoryConfigurationList() {
		if(this.dc.getDirectoryServerType().equalsIgnoreCase("IBM"))
		{
			DirectoryConfigurationIBM localDirectoryConfigurationIBM = Factory.DirectoryConfigurationIBM.createInstance();
			localDirectoryConfigurationIBM.set_DisplayName( this.dc.getDisplayName() );
			localDirectoryConfigurationIBM.set_DirectoryServerHost(this.dc.getServerHost() );
			localDirectoryConfigurationIBM.set_DirectoryServerPort(this.dc.getServerPort());
			localDirectoryConfigurationIBM.set_DirectoryServerUserName(this.dc.getServerUserName());
			localDirectoryConfigurationIBM.set_DirectoryServerPassword(this.dc.getServerPassword().getBytes());
			localDirectoryConfigurationIBM.set_IsSSLEnabled(this.dc.getSslEnabled());
			localDirectoryConfigurationIBM.set_UserBaseDN(this.dc.getUserBaseDN() );
			localDirectoryConfigurationIBM.set_UserSearchFilter(this.dc.getUserSearchFilter());
			localDirectoryConfigurationIBM.set_UserDisplayNameAttribute(this.dc.getUserDisplayNameAttribute());
			localDirectoryConfigurationIBM.set_UserNameAttribute(this.dc.getUserBaseDN());
			localDirectoryConfigurationIBM.set_GroupBaseDN(this.dc.getGroupBaseDN());
			localDirectoryConfigurationIBM.set_GroupSearchFilter(this.dc.getGroupSearchFilter());
			localDirectoryConfigurationIBM.set_GroupDisplayNameAttribute(this.dc.getGroupDisplayNameAttribute());
			localDirectoryConfigurationIBM.set_GroupNameAttribute(this.dc.getGroupNameAttribute());
			localDirectoryConfigurationIBM.set_GroupMembershipSearchFilter(this.dc.getGroupMembershipSearchFilter());
			localDirectoryConfigurationIBM.set_RestrictMembershipToConfiguredRealms(this.dc.getRestrictMembershipToConfiguredRealms());
			DirectoryConfigurationList localDirectoryConfigurationList = Factory.DirectoryConfiguration.createList();
			localDirectoryConfigurationList.add(localDirectoryConfigurationIBM);
			return localDirectoryConfigurationList;
		}
		return null;
	}

	public List<Permission> getPermissions() {
		return permissions;
	}

	public DirectoryConfiguration getDc() {
		return dc;
	}
}
