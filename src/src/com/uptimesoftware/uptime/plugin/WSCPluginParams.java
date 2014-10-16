package com.uptimesoftware.uptime.plugin;

/**
 * WSCPluginParams class that store input parameters from Up.time.
 * 
 * @author syoon
 */
public class WSCPluginParams {

	private static final String LOCAL_HOST = "localhost";

	// See definition in .xml file for plugin. Each plugin has different number of input/output parameters.
	// [Input]
	private String hostName;
	private String domainName;
	private String userName;
	private String password;
	private String serviceDisplayName; // This String can be regex.
	private String startupTypeInclude;
	private String startupTypeExclude;
	private String serviceStatusInclude;
	private String serviceStatusExclude;

	/**
	 * Create WSCPluginParams with all the input paramters from Up.time.
	 * 
	 * @param hostName
	 *            Name of host.
	 * @param domainName
	 *            Name of domain.
	 * @param userName
	 *            Name of user.
	 * @param password
	 *            Administrator password.
	 * @param serviceDisplayName
	 *            A service display name String that is regex.
	 * @param startupTypeInclude
	 *            Startup Type to include. null if nothing is selected.
	 * @param startupTypeExclude
	 *            Startup Type to exclude. null if nothing is selected.
	 * @param serviceStatusInclude
	 *            Service Status to include. null if nothing is selected.
	 * @param serviceStatusExclude
	 *            Service Status to exclude. null if nothing is selected.
	 */
	public WSCPluginParams(String hostName, String domainName, String userName, String password,
			String serviceDisplayName, String startupTypeInclude, String startupTypeExclude,
			String serviceStatusInclude, String serviceStatusExclude) {
		this.hostName = hostName;
		this.domainName = domainName;
		this.userName = userName;
		this.password = password;
		this.serviceDisplayName = serviceDisplayName;
		this.startupTypeInclude = startupTypeInclude;
		this.startupTypeExclude = startupTypeExclude;
		this.serviceStatusInclude = serviceStatusInclude;
		this.serviceStatusExclude = serviceStatusExclude;
	}

	/**
	 * Get hostName.
	 * 
	 * @return hostName.
	 */
	public String getHostName() {
		return this.hostName;
	}

	/**
	 * Get domainName.
	 * 
	 * @return domainName.
	 */
	public String getDomainName() {
		return this.domainName;
	}

	/**
	 * Get userName.
	 * 
	 * @return userName.
	 */
	public String getUserName() {
		return this.userName;
	}

	/**
	 * Get password.
	 * 
	 * @return password.
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * Get serviceDisplayName.
	 * 
	 * @return serviceDisplayName.
	 */
	public String getServiceDisplayName() {
		return this.serviceDisplayName;
	}

	/**
	 * Get startupTypeInclude.
	 * 
	 * @return startupTypeInclude.
	 */
	public String getStartupTypeInclude() {
		return this.startupTypeInclude;
	}

	/**
	 * Get startupTypeExclude.
	 * 
	 * @return startupTypeExclude.
	 */
	public String getStartupTypeExclude() {
		return this.startupTypeExclude;
	}

	/**
	 * Get serviceStatusInclude.
	 * 
	 * @return serviceStatusInclude.
	 */
	public String getServiceStatusInclude() {
		return this.serviceStatusInclude;
	}

	/**
	 * Get serviceStatusExclude.
	 * 
	 * @return serviceStatusExclude.
	 */
	public String getServiceStatusExclude() {
		return this.serviceStatusExclude;
	}

	/**
	 * Check if host is localhost or not.
	 * 
	 * @return True if localhost, false otherwise.
	 */
	public boolean isItLocalhost() {
		return getHostName().equals(LOCAL_HOST);
	}

	/**
	 * Check if Startup Type Include is selected or not.
	 * 
	 * @return True if Startup Type Include is selected, false otherwise.
	 */
	public boolean isStartupTypeIncluded() {
		return getStartupTypeInclude() != null && !getStartupTypeInclude().equals("");
	}

	/**
	 * Check if Startup Type Exclude is selected or not.
	 * 
	 * @return True if Startup Type Exclude is selected, false otherwise.
	 */
	public boolean isStartupTypeExcluded() {
		return getStartupTypeExclude() != null && !getStartupTypeExclude().equals("");
	}

	/**
	 * Check if Service Status Include is selected or not.
	 * 
	 * @return True if Service Status Include is selected, false otherwise.
	 */
	public boolean isServiceStatusIncluded() {
		return getServiceStatusInclude() != null && !getServiceStatusInclude().equals("");
	}

	/**
	 * Check if Service Status Exclude is selected or not.
	 * 
	 * @return True if Service Status Exclude is selected, false otherwise.
	 */
	public boolean isServiceStatusExcluded() {
		return getServiceStatusExclude() != null && !getServiceStatusExclude().equals("");
	}

	/**
	 * Set serviceDisplayName.
	 * 
	 * @param serviceDisplayName
	 *            serviceDisplayName string.
	 */
	public void setServiceDisplayName(String serviceDisplayName) {
		this.serviceDisplayName = serviceDisplayName;
	}

	/**
	 * Set startupTypeInclude.
	 * 
	 * @param startupType
	 *            Startup Type string.
	 */
	public void setStartupTypeInclude(String startupType) {
		this.startupTypeInclude = startupType;
	}

	/**
	 * Set startupTypeExclude.
	 * 
	 * @param startupType
	 *            Startup Type string.
	 */
	public void setStartupTypeExclude(String startupType) {
		this.startupTypeExclude = startupType;
	}

	/**
	 * Set serviceStatusInclude.
	 * 
	 * @param serviceStatusInclude
	 *            Service Status string.
	 */
	public void setServiceStatusInclude(String serviceStatusInclude) {
		this.serviceStatusInclude = serviceStatusInclude;
	}

	/**
	 * Set serviceStatusExclude.
	 * 
	 * @param serviceStatusExclude
	 *            Service Status string.
	 */
	public void setServiceStatusExclude(String serviceStatusExclude) {
		this.serviceStatusExclude = serviceStatusExclude;
	}

	/**
	 * Set hostName
	 * 
	 * @param hostName
	 *            hostName string.
	 */
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	/**
	 * Set domainName
	 * 
	 * @param domainName
	 *            domainName string.
	 */
	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}

	/**
	 * Set userName
	 * 
	 * @param userName
	 *            userName string.
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * Set password
	 * 
	 * @param password
	 *            password string.
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Reset credential properties of WSCPluginParams object for re-use.
	 */
	public void resetCredentialProperties() {
		this.setHostName(null);
		this.setDomainName(null);
		this.setUserName(null);
		this.setPassword(null);
	}

	/**
	 * Reset Startup Type and Service Status properties of WSCPluginParams object for re-use.
	 */
	public void resetStartupTypeServiceStatusProperties() {
		this.setStartupTypeInclude(null);
		this.setStartupTypeExclude(null);
		this.setServiceStatusInclude(null);
		this.setServiceStatusExclude(null);
	}
}
