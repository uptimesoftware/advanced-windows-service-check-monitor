package com.uptimesoftware.uptime.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
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

		// On WMIC, caption=Display Name(Description), name=Service Name, startmode=Startup Type, state=Service Status.
		private static final String DISPLAY_NAME = "Caption";
		private static final String SERVICE_NAME = "Name";
		private static final String STARTUP_MODE = "StartMode";
		private static final String STATE = "State";
		private static final String WINDOWS_WMIC_SERVICE_GET = DISPLAY_NAME + "," + SERVICE_NAME + "," + STARTUP_MODE
				+ "," + STATE;
		// SERVICE_NAME is in output even though it is not needed and not selected in WQL. Just to make parsing process
		// consistent, get SERVICE_NAME on both Windows and Linux and then parse it out. And no need to escape
		// quotations on Linux even if the usage example uses quotations around WQL.
		private static final String LINUX_WMIC_SERVICE_GET = "select " + DISPLAY_NAME + "," + SERVICE_NAME + ","
				+ STARTUP_MODE + "," + STATE + " from Win32_Service";
		private static final String SERVICE_NAME_SEPARATOR = ",";
		private static final int SERVICE_STARTUPTYPE_INDEX = 0;
		private static final int SERVICE_STATUS_INDEX = 1;
		// booleans to be used in various if-statement conditions.
		private boolean isItLocalhost;
		private boolean includeSelected;
		private boolean excludeSelected;

		// See definition in .xml file for plugin. Each plugin has different number of input/output parameters.
		// [Input]
		String hostname;
		String domainName;
		String adminName;
		String password;
		String serviceDisplayName; // This String can be regex.
		String startupTypeInclude;
		String startupTypeExclude;
		String serviceStatus;

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
			hostname = params.getString("hostname");
			domainName = params.getString("domainName");
			adminName = params.getString("adminName");
			password = params.getString("password");
			serviceDisplayName = params.getString("serviceDisplayName");
			startupTypeInclude = params.getString("startupTypeInclude");
			startupTypeExclude = params.getString("startupTypeExclude");
			serviceStatus = params.getString("serviceStatus");
		}

		/**
		 * The monitor function will implement the main functionality and should set the monitor's state and result
		 * message prior to completion.
		 */
		@Override
		public void monitor() {
			LOGGER.debug("Error handling : Check either Admin name or password is missing");
			isItLocalhost = hostname.equals("localhost");
			if (!isItLocalhost && ((adminName != null && password == null) || (adminName == null && password != null))) {
				// Not localhost but missing admin name or password.
				setStateAndMessage(MonitorState.UNKNOWN, "Please enter both Administrator and Password.");
				return;
			} else if (isItLocalhost && (adminName != null || password != null || domainName != null)) {
				// localhost but admin name or/and password are entered. localhost doesn't need them for wmic.
				setStateAndMessage(MonitorState.UNKNOWN, "localhost does not need Domain, Administrator, and Password.");
				return;
			}

			LOGGER.debug("Error handling : A user cannot select both Startup Type (Include) and Startup Type (Exclude)");
			includeSelected = startupTypeInclude != null;
			excludeSelected = startupTypeExclude != null;
			if (includeSelected && excludeSelected) {
				setStateAndMessage(MonitorState.UNKNOWN,
						"Please select one of Startup Type (Include) & Startup Type (Exclude)");
				return;
			}

			LOGGER.debug("Replacing 'Automatic' with 'Auto' because WMIC returns 'Auto'");
			startupTypeInclude = includeSelected && startupTypeInclude.equals("Automatic") ? "Auto"
					: startupTypeInclude;
			startupTypeExclude = excludeSelected && startupTypeExclude.equals("Automatic") ? "Auto"
					: startupTypeExclude;

			LOGGER.debug("Error handling : Check validity of regex syntax.");
			HashSet<String> regexes = new HashSet<String>();
			if (serviceDisplayName.contains(SERVICE_NAME_SEPARATOR)) {
				// If Service Display Name input contains comma(s), separate them.
				String[] temp = serviceDisplayName.split(SERVICE_NAME_SEPARATOR);
				for (String regex : temp) {
					if (!checkRegex(regex)) {
						setStateAndMessage(MonitorState.UNKNOWN,
								"One or more service display name(s) contains invalid regex syntax.");
						return;
					}
					regexes.add(regex);
				}
			} else {
				// No comma in Service Display Name. It should only contain one service display name/regex.
				if (!checkRegex(serviceDisplayName)) {
					setStateAndMessage(MonitorState.UNKNOWN, "The service display name has invalid regex syntax.");
					return;
				}
				regexes.add(serviceDisplayName);
			}

			LOGGER.debug("Step 2 : Check OS type, execute WMIC.");
			ArrayList<String> args = new ArrayList<String>();
			boolean gotResult = false;
			HashMap<String, String[]> result = new HashMap<String, String[]>();
			if (SystemUtils.IS_OS_WINDOWS) {
				LOGGER.debug("[Windows] Set a new admin name if domain is entered");
				adminName = domainName != null ? domainName + "\\" + adminName : adminName;
				// Windows WMIC : wmic /node:<hostname> /user:<username> /password:<password> service get name.
				if (isItLocalhost) {
					args.add("wmic");
					args.add("/node:\"" + hostname + "\"");
					args.add("service");
					args.add("GET");
					args.add(WINDOWS_WMIC_SERVICE_GET);
				} else {
					args.add("wmic");
					args.add("/node:\"" + hostname + "\"");
					args.add("/user:" + adminName);
					args.add("/password:" + password);
					args.add("Service");
					args.add("GET");
					args.add(WINDOWS_WMIC_SERVICE_GET);
				}
				gotResult = execWmicCommand(result, args, regexes);

			} else if (SystemUtils.IS_OS_LINUX) {
				LOGGER.debug("[Linux] Check if a plugin is trying to run against localhost and WMI Client is installed.");
				if (isItLocalhost) {
					setStateAndMessage(MonitorState.UNKNOWN, "The localhost is Linux OS, Choose remote Windows host.");
					return;
				} else if (!isWmicClientInstalled()) {
					setStateAndMessage(MonitorState.UNKNOWN,
							"WMIC Client is not installed on the Linux monitoring station.");
					return;
				}
				LOGGER.debug("[Linux] Set a new admin name if domain is entered");
				adminName = domainName != null ? domainName + "/" + adminName : adminName;
				// Linux WMIC : wmic -U [domain/]<username>%<password> //<hostname> "select * from Win32_Service"
				args.add("wmic");
				args.add("-U");
				args.add(adminName + "%" + password);
				args.add("//" + hostname);
				args.add(LINUX_WMIC_SERVICE_GET);
				gotResult = execWmicCommand(result, args, regexes);

			} else {
				setStateAndMessage(MonitorState.UNKNOWN,
						"Advanced Windows Service Check plug-in can only run on Windows / Linux monitoring station.");
				return;
			}

			LOGGER.debug("Error handling : Stop monitor if getting result was unsuccessful");
			if (!gotResult) {
				setStateAndMessage(MonitorState.UNKNOWN, "Unable to get result from executing wmic command.");
				return;
			}

			LOGGER.debug("Output the filtered list of services.");
			StringBuilder theList = new StringBuilder();
			for (String key : result.keySet()) {
				theList.append(System.lineSeparator());
				theList.append(key + " / Startup Type : " + result.get(key)[SERVICE_STARTUPTYPE_INDEX] + " / Status : "
						+ result.get(key)[SERVICE_STATUS_INDEX]);
			}
			addVariable("matchedServices", theList.toString().trim());
			addVariable("numberOfMatches", result.size());

			LOGGER.debug("Monitor ran successfully. Set monitor state to OK.");
			setStateAndMessage(MonitorState.OK, "Monitor ran successfully.");
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
		 * Private helper method to execute WMIC command.
		 * 
		 * @param wmicCommand
		 *            ArrayList<String> which contains args of WMIC command to be executed on a remote Windows host.
		 * @return A result String that contains a (filtered) list of Windows services.
		 */
		private boolean execWmicCommand(HashMap<String, String[]> result, ArrayList<String> wmicCommand,
				HashSet<String> regexes) {
			boolean gotResult = false;
			try {
				LOGGER.debug("Make a Process to execute wmic command.");
				ProcessBuilder pb = new ProcessBuilder(wmicCommand);
				Process process = pb.start();

				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

				// TODO : (Find a way to get rid of the errors) On Linux, first few lines of output contain weird error
				// messages, find a line that contains the column names and then start parsing.
				LOGGER.debug("Read line(s) and put them in the HashMap.");
				boolean columnNamesFound = false;
				String line = "";
				while ((line = bufferedReader.readLine()) != null) {
					// Check if the line contains column names. If yes, then start parsing.
					if (!columnNamesFound) {
						columnNamesFound = line.contains(DISPLAY_NAME) && line.contains(SERVICE_NAME)
								&& line.contains(STARTUP_MODE) && line.contains(STATE);
					}
					if (!line.trim().equals("") && columnNamesFound) {
						gotResult = splitLineAndPutInHashMap(result, line, regexes);
					}
				}
				process.waitFor();
				process.destroy();

			} catch (IOException | InterruptedException e) {
				LOGGER.error("Error occurred while executing wmic command.", e);
				return gotResult;
			}
			return gotResult;
		}

		/**
		 * Private helper method to split a line by two- or more-spaces delimiter on Windows or split a line by "|"
		 * delimiter on Linux. And put the split data into a HashMap.
		 * 
		 * @param result
		 *            HashMap that will store Service name as a key and String array, which has Startup type and Status
		 *            elements, as value.
		 * @param line
		 *            The String line that contains Service name, Startup type, Status.
		 * @param regexes
		 *            Service names or regexes from Service Display Name input field.
		 * @return True if splitting the input worked, false otherwise.
		 */
		private boolean splitLineAndPutInHashMap(HashMap<String, String[]> result, String line, HashSet<String> regexes) {
			boolean splitOkay = false;
			String serviceDisplayName, serviceShortName, startupType, status;

			// Split the input line by a two- or more-space delimiter.
			try (@SuppressWarnings("resource")
			// If Windows use two- or more-space characters as a delimiter, or "|" as a delimiter.
			Scanner scanner = SystemUtils.IS_OS_WINDOWS ? new Scanner(line).useDelimiter("\\s{2,}") : new Scanner(line)
					.useDelimiter("\\|");) {
				serviceDisplayName = scanner.hasNext() ? scanner.next() : "";
				serviceShortName = scanner.hasNext() ? scanner.next() : "";
				startupType = scanner.hasNext() ? scanner.next() : "";
				status = scanner.hasNext() ? scanner.next() : "";
				scanner.close();
			}

			if (serviceDisplayName.equals("") || serviceShortName.equals("") || startupType.equals("")
					|| status.equals("")) {
				// serviceShortName is not used in this plugin, but just making sure splitting the input went well.
				LOGGER.error("Check which one of serviceName, startupType, and/or status is empty.");
				return splitOkay;
			}

			// TODO check this part of code. serviceShortName is added so make sure.
			boolean hasMatch = false;
			for (String regex : regexes) {
				// Filter the list of services with service name / regex. and filter again with startup type.
				hasMatch = serviceDisplayName.matches(regex);
				if (hasMatch && includeSelected && !excludeSelected && startupType.contains(startupTypeInclude)) {
					// (Include) is selected, and the line contains selected startup type.
					result.put(serviceDisplayName, new String[] { startupType, status });
				} else if (hasMatch && !includeSelected && excludeSelected && !startupType.contains(startupTypeExclude)) {
					// (Exclude) is selected, and the line does not contain the selected Startup type.
					result.put(serviceDisplayName, new String[] { startupType, status });
				} else if (hasMatch && !includeSelected && !excludeSelected) {
					// If neither Startup Type (Include) / (Exclude) is selected, no need to filter.
					result.put(serviceDisplayName, new String[] { startupType, status });
				}
			}

			if (serviceStatus == null) {
				// If service status is not selected, no more filtering is needed.
				return splitOkay = true;
			}

			// Last filtering with service status.
			if (result.containsKey(serviceDisplayName)
					&& !result.get(serviceDisplayName)[SERVICE_STATUS_INDEX].contains(serviceStatus)) {
				result.remove(serviceDisplayName);
			}

			return splitOkay = true;
		}

		/**
		 * Private helper method to check validity of the given regex.
		 * 
		 * @param regex
		 *            Regex string to be used.
		 * @return True if the given regex is valid, false otherwise.
		 */
		private boolean checkRegex(String regex) {
			boolean regexValid = false;
			try {
				Pattern.compile(regex);
				regexValid = true;
			} catch (PatternSyntaxException e) {
				LOGGER.error("Invalid regex syntax.");
			}
			return regexValid;
		}

	}
}