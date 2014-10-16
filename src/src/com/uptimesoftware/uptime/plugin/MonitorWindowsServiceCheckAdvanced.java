package com.uptimesoftware.uptime.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.fortsoft.pf4j.PluginWrapper;
import com.uptimesoftware.uptime.plugin.api.Extension;
import com.uptimesoftware.uptime.plugin.api.Plugin;
import com.uptimesoftware.uptime.plugin.api.PluginMonitor;
import com.uptimesoftware.uptime.plugin.monitor.MonitorState;
import com.uptimesoftware.uptime.plugin.monitor.Parameters;

/**
 * Advanced Windows Service Check Monitor
 * 
 * @author uptime software
 */
public class MonitorWindowsServiceCheckAdvanced extends Plugin {

	/**
	 * Constructor - a plugin wrapper.
	 * 
	 * @param wrapper
	 */
	public MonitorWindowsServiceCheckAdvanced(PluginWrapper wrapper) {
		super(wrapper);
	}

	/**
	 * A nested static class which has to extend PluginMonitor.
	 * Private helper functions have too many parameters but it was needed to test client-side plugin Java code in unit
	 * test. Manual testings just take too much time.
	 * 
	 * Functions that require implementation :
	 * 1) The monitor function will implement the main functionality and should set the monitor's state and result
	 * message prior to completion.
	 * 2) The setParameters function will accept a Parameters object containing the values filled into the monitor's
	 * configuration page in Up.time.
	 */
	@Extension
	public static class UptimeMonitorWindowsServiceCheckAdvanced extends PluginMonitor {

		// Simple Logging Facade for Java (SLF4J)
		private static final Logger LOGGER = LoggerFactory.getLogger(UptimeMonitorWindowsServiceCheckAdvanced.class);

		private static final String HOSTNAME = "hostname";
		private static final String USERNAME = "userName";
		private static final String DOMAIN_NAME = "domainName";
		private static final String PASSWORD = "password";
		private static final String SERVICE_DISPLAY_NAME = "serviceDisplayName";
		private static final String STARTUP_TYPE_INCLUDE = "startupTypeInclude";
		private static final String STARTUP_TYPE_EXCLUDE = "startupTypeExclude";
		private static final String SERVICE_STATUS_INCLUDE = "serviceStatusInclude";
		private static final String SERVICE_STATUS_EXCLUDE = "serviceStatusExclude";
		private static final String MATCHED_SERVICES = "matchedServices";
		private static final String NUMBER_OF_MATCHES = "numberOfMatches";

		private static final String AUTOMATIC = "Automatic";
		private static final String AUTO = "Auto";

		private static final String COMMA_DELIMITER = ",";
		// On WMIC, caption=Display Name(Description), name=Service Name, startmode=Startup Type, state=Service Status.
		private static final String DISPLAY_NAME = "Caption";
		// SERVICE_NAME is selected but it's not used in output because WMIC client on Linux returns Name always.
		private static final String SERVICE_NAME = "Name";
		private static final String STARTUP_MODE = "StartMode";
		private static final String STATE = "State";
		private static final String WMIC_TOKENS = DISPLAY_NAME + COMMA_DELIMITER + SERVICE_NAME + COMMA_DELIMITER
				+ STARTUP_MODE + COMMA_DELIMITER + STATE;

		private static final int SERVICE_STARTUPTYPE_INDEX = 0;
		private static final int SERVICE_STATUS_INDEX = 1;

		// WSCPluginParams object will store all input params from Up.time
		private WSCPluginParams wscParams;

		// See definition in .xml file for plugin. Each plugin has different number of input/output parameters.
		// [Input]
		String hostName;
		String domainName;
		String userName;
		String password;
		String serviceDisplayName; // This String can be regex.
		String startupTypeInclude;
		String startupTypeExclude;
		String serviceStatusInclude;
		String serviceStatusExclude;

		/**
		 * The setParameters function will accept a Parameters object containing the values filled into the monitor's
		 * configuration page in Up.time.
		 * 
		 * @param params
		 *            Parameters object which contains inputs.
		 */
		@Override
		public void setParameters(Parameters params) {
			LOGGER.debug("Step 1 : Setting parameters.");
			// [Input]
			hostName = params.getString(HOSTNAME);
			domainName = params.getString(DOMAIN_NAME);
			userName = params.getString(USERNAME);
			password = params.getString(PASSWORD);
			serviceDisplayName = params.getString(SERVICE_DISPLAY_NAME);
			startupTypeInclude = params.getString(STARTUP_TYPE_INCLUDE);
			startupTypeExclude = params.getString(STARTUP_TYPE_EXCLUDE);
			serviceStatusInclude = params.getString(SERVICE_STATUS_INCLUDE);
			serviceStatusExclude = params.getString(SERVICE_STATUS_EXCLUDE);

			wscParams = new WSCPluginParams(hostName, domainName, userName, password, serviceDisplayName,
					startupTypeInclude, startupTypeExclude, serviceStatusInclude, serviceStatusExclude);

			// If startup type is "Automatic", convert it to "Auto" because WMI only outputs "Auto".
			if (wscParams.isStartupTypeIncluded() && wscParams.getStartupTypeInclude() != null
					&& wscParams.getStartupTypeInclude().equals(AUTOMATIC)) {
				wscParams.setStartupTypeInclude(AUTO);
			} else if (wscParams.isStartupTypeExcluded() && wscParams.getStartupTypeExclude() != null
					&& wscParams.getStartupTypeExclude().equals(AUTOMATIC)) {
				wscParams.setStartupTypeExclude(AUTO);
			}
		}

		/**
		 * The monitor function will implement the main functionality and should set the monitor's state and result
		 * message prior to completion.
		 */
		@Override
		public void monitor() {
			LOGGER.debug("Error handling : Check either Admin name or password is missing");
			if (!checkAdminOrPasswordMissing(wscParams)) {
				return;
			}

			LOGGER.debug("Error handling : A user cannot select both Include and Exclude in a same category.");
			if (!checkIncludeExclude(wscParams)) {
				return;
			}

			LOGGER.debug("Error handling : Check validity of regex syntax.");
			HashSet<String> regexes = new HashSet<String>();
			if (!checkRegexAndAdd(regexes, wscParams)) {
				return;
			}

			LOGGER.debug("Step 2 : Check OS type and build args of ProcessBuilder.");
			ArrayList<String> args = new ArrayList<String>();
			if (!buildArgsOfProcessBuilder(args, wscParams)) {
				return;
			}

			LOGGER.debug("Step 3 : Execute WMIC command");
			HashMap<String, String[]> result = new HashMap<String, String[]>();
			if (!execWmicCommand(result, args, regexes, wscParams)) {
				return;
			}

			LOGGER.debug("Output the filtered list of services.");
			StringBuilder theList = new StringBuilder();
			for (String key : result.keySet()) {
				theList.append(System.lineSeparator());
				theList.append(key + " / Startup Type : " + result.get(key)[SERVICE_STARTUPTYPE_INDEX] + " / Status : "
						+ result.get(key)[SERVICE_STATUS_INDEX]);
			}
			addVariable(MATCHED_SERVICES, theList.toString().trim());
			addVariable(NUMBER_OF_MATCHES, result.size());

			LOGGER.debug("Monitor ran successfully. Set monitor state to OK.");
			setStateAndMessage(MonitorState.OK, "Monitor ran successfully.");
		}

		/**
		 * Check if both Include and Exclude are selected. If so, error.
		 * 
		 * @param wscParams
		 *            An object that holds all input params from Up.time.
		 * @return True both Include & Exclude are not selected, false otherwise.
		 */
		private boolean checkIncludeExclude(WSCPluginParams wscParams) {
			if (wscParams.isStartupTypeIncluded() && wscParams.isStartupTypeExcluded()) {
				setStateAndMessage(MonitorState.UNKNOWN,
						"Please select only one of Startup Type (Include) & (Exclude) or select neither.");
				return false;
			}

			if (wscParams.isServiceStatusIncluded() && wscParams.isServiceStatusExcluded()) {
				setStateAndMessage(MonitorState.UNKNOWN,
						"Please select only one of Service Status (Include) & (Exclude) or select neither.");
				return false;
			}
			return true;
		}

		/**
		 * Check if Username/Password/Domain is/are missing.
		 * 
		 * @param wscParams
		 *            An object that holds all input params from Up.time.
		 * @return True if username/password/domain is not missing when non-localhost, false otherwise. True if
		 *         admin&password&domain are missing when localhost, false otherwise.
		 */
		private boolean checkAdminOrPasswordMissing(WSCPluginParams wscParams) {

			String password = wscParams.getPassword();
			String userName = wscParams.getUserName();
			String domainName = wscParams.getDomainName();

			if (!wscParams.isItLocalhost()) {
				if (password == null || password.equals("")) {
					setStateAndMessage(MonitorState.UNKNOWN, "Please enter Password.");
					return false;
				} else if (userName == null || userName.equals("")) {
					setStateAndMessage(MonitorState.UNKNOWN, "Please enter Username.");
					return false;
				} else if (domainName == null || domainName.equals("")) {
					setStateAndMessage(MonitorState.UNKNOWN, "Please enter Domain.");
					return false;
				}
			} else if (wscParams.isItLocalhost() && (userName != null || password != null || domainName != null)) {
				// localhost but admin name or/and password are entered. localhost doesn't need them for wmic.
				setStateAndMessage(MonitorState.UNKNOWN, "localhost does not need Domain, Administrator, and Password.");
				return false;
			}
			return true;
		}

		/**
		 * Build ProcessBuilder arguments for Windows / Linux.
		 * 
		 * @param wscParams
		 *            An object that holds all input params from Up.time.
		 * @return True if arguments building is successful, false otherwise.
		 */
		private boolean buildArgsOfProcessBuilder(ArrayList<String> args, WSCPluginParams wscParams) {

			String hostName = wscParams.getHostName();
			String domainName = wscParams.getDomainName();
			String userName = wscParams.getUserName();
			String password = wscParams.getPassword();
			boolean isItLocalhost = wscParams.isItLocalhost();

			if (SystemUtils.IS_OS_WINDOWS) {
				LOGGER.debug("[Windows] Set a new admin name if domain is entered");
				userName = domainName != null ? domainName + "\\" + userName : userName;
				// Windows WMIC : wmic /node:<hostname> /user:<username> /password:<password> Service GET
				// Caption,Name,StartMode,State.
				if (isItLocalhost) {
					args.add("wmic");
					args.add("/node:\"" + hostName + "\"");
					args.add("Service");
					args.add("GET");
					args.add(WMIC_TOKENS);
					args.add("/format:csv");
				} else {
					args.add("wmic");
					args.add("/node:\"" + hostName + "\"");
					args.add("/user:" + userName);
					args.add("/password:" + password);
					args.add("Service");
					args.add("GET");
					args.add(WMIC_TOKENS);
					args.add("/format:csv");
				}
			} else if (SystemUtils.IS_OS_LINUX) {
				LOGGER.debug("[Linux] Check if a plugin is trying to run against localhost and WMI Client is installed.");
				if (isItLocalhost) {
					setStateAndMessage(MonitorState.UNKNOWN, "The localhost is Linux OS, Choose remote Windows host.");
					return false;
				} else if (!isWmicClientInstalled()) {
					setStateAndMessage(MonitorState.UNKNOWN,
							"WMIC Client is not installed on the Linux monitoring station.");
					return false;
				}
				LOGGER.debug("[Linux] Set a new admin name if domain is entered");
				userName = domainName != null ? domainName + "/" + userName : userName;
				// Linux WMIC : wmic -U [domain/]<username>%<password> //<hostname>
				// "select * from Win32_Service --delimiter=,"
				args.add("wmic");
				args.add("-U");
				args.add(userName + "%" + password);
				args.add("//" + hostName);
				// No need to escape quotes even though the usage description WMIC client uses it around WQL.
				args.add("select " + WMIC_TOKENS + " from Win32_Service");
				args.add("--delimiter=" + COMMA_DELIMITER);
			} else {
				setStateAndMessage(MonitorState.UNKNOWN,
						"Advanced Windows Service Check plug-in can only run on Windows / Linux monitoring station.");
				return false;
			}
			return true;
		}

		/**
		 * Private helper method to check if WMIC Client is installed on Linux.
		 * 
		 * @return True if WMIC Client is installed on Linux, false otherwise.
		 */
		private boolean isWmicClientInstalled() {
			boolean itIsThere = false;
			try {
				LOGGER.debug("Check if WMIC Client is installed on the Linux monitoring station or not.");
				// Run this command on Linux, "0" if wmic is installed, "1" otherwise.
				ArrayList<String> args = new ArrayList<String>();
				args.add("/bin/sh");
				args.add("-c");
				args.add("which wmic 2>/dev/null 1>&2");

				ProcessBuilder pb = new ProcessBuilder(args);
				Process process = pb.start();
				process.waitFor();
				itIsThere = process.exitValue() == 0;
				process.destroy();
			} catch (IOException | InterruptedException e) {
				LOGGER.error("Error occurred while checking availability of WMIC Client on Linux.", e);
			}
			return itIsThere;
		}

		/**
		 * Helper to execute wmic command.
		 * 
		 * @param result
		 *            HashMap that will store result of executing wmic command.
		 * @param wmicCommand
		 *            Command to execute.
		 * @param regexes
		 *            A list of regexes.
		 * @param wscParams
		 *            An object that holds all input params from Up.time.
		 * @return True if executing wmic command is successful, false otherwise.
		 */
		private boolean execWmicCommand(HashMap<String, String[]> result, ArrayList<String> wmicCommand,
				HashSet<String> regexes, WSCPluginParams wscParams) {
			boolean gotResult = false;
			try {
				LOGGER.debug("Make a Process to execute wmic command.");
				ProcessBuilder pb = new ProcessBuilder(wmicCommand);
				Process process = pb.start();

				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

				// TODO : (Find a way to get rid of the errors) On Linux, first few lines of output contain weird error
				// messages, find a line that contains the column names (aka WMIC_TOKENS) and then start parsing.
				LOGGER.debug("Read line(s) and put them in the HashMap.");
				boolean columnNamesFound = false;
				String line = "";
				while ((line = bufferedReader.readLine()) != null) {
					if (!columnNamesFound) {
						columnNamesFound = line.contains(WMIC_TOKENS);
						continue;
					}
					if (!line.trim().equals("") && columnNamesFound) {
						// On Linux, WMIC_TOKENS re-appears multiple times, splitLineAndPutInHashMap() will handle the
						// duplication.
						gotResult = splitLineAndPutInHashMap(result, line, regexes, wscParams);
						if (!gotResult) {
							// Splitting the given line was unsuccessful. Break out of while loop and destroy process.
							break;
						}
					}
				}

				// If output does not contain wmic tokens, authentication failed.
				if (!columnNamesFound) {
					setStateAndMessage(MonitorState.UNKNOWN, "Authentication failed");
				}

				process.waitFor();
				process.destroy();
			} catch (IOException | InterruptedException e) {
				LOGGER.error("Error occurred while executing wmic command.", e);
				gotResult = false;
			}
			return gotResult;
		}

		/**
		 * Private helper method to split a line by comma delimiter on Windows and Linux. And put the split data into
		 * the given HashMap.
		 * 
		 * @param result
		 *            HashMap that will store result of executing wmic command.
		 * @param wmicCommand
		 *            Command to execute.
		 * @param regexes
		 *            A list of regexes.
		 * @param wscParams
		 *            An object that holds all input params from Up.time.
		 * @return True if successful, false otherwise.
		 */
		private boolean splitLineAndPutInHashMap(HashMap<String, String[]> result, String line,
				HashSet<String> regexes, WSCPluginParams wscParams) {

			// Special case : On Linux, output returned from WMIC client often contains unwanted lines such as
			// "CLASS: Win32_Service" and "Caption,Name,StartMode,State" and "CLASS: Win32_TerminalService" in the list
			// of Windows services, and the unwanted lines appear multiple times in the list. Ignore them.
			if (line.contains("CLASS: Win32_Service") || line.contains(WMIC_TOKENS)
					|| line.contains("CLASS: Win32_TerminalService")) {
				// Ignore these lines and continue parsing.
				return true;
			}

			String serviceDisplayName = "", serviceShortName = "", startupType = "", status = "";

			String[] tokens = line.split(COMMA_DELIMITER);
			int arrayLength = tokens.length;
			// On Windows, we expect to have Node,Caption,Name,StartMode,State. (5 columns / tokens)
			// On Linux, we expect to have Caption,Name,StartMode,State. (4 columns / tokens)
			int expectedNumOfTokens = SystemUtils.IS_OS_WINDOWS ? 5 : 4;
			// If more than expectedNumOfTokens (5 on Windows, 4 on Linux) tokens in the array, the Caption( aka Service
			// Display Name) contains ','
			if (arrayLength > expectedNumOfTokens) {
				// On Windows, iteration starts at index 1 because tokens[0] aka Node is not needed.
				for (int i = (expectedNumOfTokens - 4); i < arrayLength - 3; i++) {
					// If i is not arrayLength - 4, concatenate strings with comma. Otherwise just add a string.
					// For example, "Service" + " Display" = "Service, Display" (original string contains comma).
					serviceDisplayName += i != arrayLength - 4 ? tokens[i] + COMMA_DELIMITER : tokens[i];
				}
				// last three items will always be Service Name, Startup Type, and Service Status.
				serviceShortName = tokens[arrayLength - 3];
				startupType = tokens[arrayLength - 2];
				status = tokens[arrayLength - 1];
			} else if (arrayLength < expectedNumOfTokens) {
				setStateAndMessage(MonitorState.UNKNOWN, "WMIC output contains a line with incorrect format.");
				return false;
			} else {
				// On Windows, we ignore tokens[0] aka Node because not needed
				serviceDisplayName = tokens[arrayLength - 4];
				serviceShortName = tokens[arrayLength - 3];
				startupType = tokens[arrayLength - 2];
				status = tokens[arrayLength - 1];
			}

			if (serviceDisplayName.equals("") || serviceShortName.equals("") || startupType.equals("")
					|| status.equals("")) {
				// serviceShortName is not used in this plugin, but just making sure splitting the input went well.
				// serviceShortName may be useful later.
				LOGGER.error("Check which one of serviceDisplayName, serviceShortName, startupType, and/or status is empty.");
				return false;
			}

			boolean hasMatch = false;
			boolean startupTypeIncluded = wscParams.isStartupTypeIncluded();
			boolean startupTypeExcluded = wscParams.isStartupTypeExcluded();
			String startupTypeInclude = wscParams.getStartupTypeInclude();
			String startupTypeExclude = wscParams.getStartupTypeExclude();

			for (String regex : regexes) {
				// Filter the list of services with service name / regex. and filter again with startup type.
				hasMatch = serviceDisplayName.matches(regex);
				if (hasMatch && startupTypeIncluded && startupType.contains(startupTypeInclude)) {
					// (Include) is selected, and the line contains selected startup type.
					result.put(serviceDisplayName, new String[] { startupType, status });
				} else if (hasMatch && startupTypeExcluded && !startupType.contains(startupTypeExclude)) {
					// (Exclude) is selected, and the line does not contain the selected Startup type.
					result.put(serviceDisplayName, new String[] { startupType, status });
				} else if (hasMatch && !startupTypeIncluded && !startupTypeExcluded) {
					// If neither Startup Type (Include) / (Exclude) is selected, no need to filter.
					result.put(serviceDisplayName, new String[] { startupType, status });
				}
			}

			boolean serviceStatusIncluded = wscParams.isServiceStatusIncluded();
			boolean serviceStatusExcluded = wscParams.isServiceStatusExcluded();
			String serviceStatusInclude = wscParams.getServiceStatusInclude();
			String serviceStatusExclude = wscParams.getServiceStatusExclude();

			if (!serviceStatusIncluded && !serviceStatusExcluded) {
				// If service status (Include) or (Exclude) is not selected, no more filtering is needed.
				return true;
			}

			// Last filtering with service status(Include) or (Exclude).
			boolean serviceAddedFromAbove = result.containsKey(serviceDisplayName);
			String servStatusOfKey = serviceAddedFromAbove ? result.get(serviceDisplayName)[SERVICE_STATUS_INDEX] : "";
			if (serviceStatusIncluded && serviceAddedFromAbove && !servStatusOfKey.contains(serviceStatusInclude)) {
				result.remove(serviceDisplayName);
			} else if (serviceStatusExcluded && serviceAddedFromAbove && servStatusOfKey.contains(serviceStatusExclude)) {
				result.remove(serviceDisplayName);
			}

			return true;
		}

		/**
		 * Check regex syntax and add it to the given HashSet<String>
		 * 
		 * @param regexes
		 *            A list of regexes.
		 * @param wscParams
		 *            An object that holds all input params from Up.time.
		 * @return True if the given regexes are valid and added to the HashSet, false otherwise.
		 */
		private boolean checkRegexAndAdd(HashSet<String> regexes, WSCPluginParams wscParams) {

			String serviceDisplayName = wscParams.getServiceDisplayName();

			if (serviceDisplayName.contains(COMMA_DELIMITER)) {
				// If Service Display Name input contains comma(s), separate them.
				String[] temp = serviceDisplayName.split(COMMA_DELIMITER);
				for (String regex : temp) {
					if (!checkRegex(regex)) {
						setStateAndMessage(MonitorState.UNKNOWN,
								"One or more service display name(s) contains invalid regex syntax.");
						return false;
					} else {
						regexes.add(regex);
					}
				}
			} else {
				// No comma in Service Display Name. It should only contain one service display name/regex.
				if (!checkRegex(serviceDisplayName)) {
					setStateAndMessage(MonitorState.UNKNOWN, "The service display name has invalid regex syntax.");
					return false;
				} else {
					regexes.add(serviceDisplayName);
				}
			}
			return true;
		}

		/**
		 * Private helper method to check syntax of the given regex.
		 * 
		 * @param regex
		 *            Regex string to be used.
		 * @return True if the given regex is valid, false otherwise.
		 */
		private boolean checkRegex(String regex) {
			try {
				Pattern.compile(regex);
			} catch (PatternSyntaxException e) {
				LOGGER.error("Invalid regex syntax.");
				return false;
			}
			return true;
		}

	}
}