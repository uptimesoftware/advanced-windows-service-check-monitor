package com.uptimesoftware.uptime.plugin.test;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.apache.commons.lang.SystemUtils;
import org.junit.Before;
import org.junit.Test;

import com.uptimesoftware.uptime.plugin.MonitorWindowsServiceCheckAdvanced.UptimeMonitorWindowsServiceCheckAdvanced;
import com.uptimesoftware.uptime.plugin.WSCPluginParams;

public class MonitorWindowsServiceCheckAdvancedTest {

	// Configure these private variables with your inputs. Default is 'null' not empty string "".
	private String hostname = "localhost";
	private String userName;
	private String password;
	private String domainName;
	private String serviceDisplayName = ".*,Win.*,wowDoge";
	private String startupTypeInclude;
	private String startupTypeExclude;
	private String serviceStatusInclude;
	private String serviceStatusExclude;

	// Store all input params in WSCPluginParams object for later use.
	private WSCPluginParams wscParams = new WSCPluginParams(hostname, domainName, userName, password,
			serviceDisplayName, startupTypeInclude, startupTypeExclude, serviceStatusInclude, serviceStatusExclude);

	private static final String STARTUP_TYPE_AUTO = "Auto";
	private static final String STARTUP_TYPE_MANUAL = "Manual";
	private static final String STARTUP_TYPE_DISABLED = "Disabled";

	private static final String SERVICE_STATUS_STOPPED = "Stopped";
	private static final String SERVICE_STATUS_START_PENDING = "Start Pending";
	private static final String SERVICE_STATUS_PENDING = "Pending";
	private static final String SERVICE_STATUS_RUNNING = "Running";
	private static final String SERVICE_STATUS_CONTINUE_PENDING = "Continue Pending";
	private static final String SERVICE_STATUS_PAUSE_PENDING = "Pause Pending";
	private static final String SERVICE_STATUS_PAUSED = "Paused";

	private HashSet<String> regexes = new HashSet<String>();
	private ArrayList<String> args = new ArrayList<String>();
	private HashMap<String, String[]> result = new HashMap<String, String[]>();

	@Before
	public void setup() {
		// clear for re-use.
		result.clear();
		args.clear();
		regexes.clear();
		wscParams.resetStartupTypeServiceStatusProperties();

		assertTrue(invokeBuildArgsOfProcessBuilder(args, wscParams));
		// Get a list of ALL Windows service.
		assertTrue(invokeCheckRegexAndAdd(regexes, wscParams));
	}

	@Test
	public void checkRegexAndAddTest() {
		LinkedList<String> temp = new LinkedList<String>();
		HashSet<String> regexTemp = new HashSet<String>();

		temp.add("Win.*");
		temp.add("gefe.*");
		temp.add("   roflcopter*");
		// Valid regexes. Comma is a separator.
		wscParams.setServiceDisplayName("Win.*,gefe.*,   roflcopter*");
		assertTrue(invokeCheckRegexAndAdd(regexTemp, wscParams));
		assertTrue(regexTemp.size() == temp.size());
		for (String regex : regexTemp) {
			assertTrue(temp.contains(regex));
		}

		// Invalid regexes. Comma is a separator.
		wscParams.setServiceDisplayName("*,*");
		assertFalse(invokeCheckRegexAndAdd(regexTemp, wscParams));
		wscParams.setServiceDisplayName("*");
		assertFalse(invokeCheckRegexAndAdd(regexTemp, wscParams));
	}

	@Test
	public void execWmicCommandStartupTypeIncludeTest() {
		// STARTUP_TYPE_MANUAL (Include) is selected.
		wscParams.resetStartupTypeServiceStatusProperties();
		wscParams.setStartupTypeInclude(STARTUP_TYPE_MANUAL);
		assertTrue(invokeExecWmicCommand(result, args, regexes, wscParams));
		// every entry should contain STARTUP_TYPE_MANUAL (Include)
		String startup = "";
		for (Entry<String, String[]> entry : result.entrySet()) {
			startup = entry.getValue()[0];
			assertTrue(startup.equals(STARTUP_TYPE_MANUAL));
			assertFalse(startup.equals(STARTUP_TYPE_DISABLED) || startup.equals(STARTUP_TYPE_AUTO));
		}
	}

	@Test
	public void execWmicCommandStartupTypeIncludeAndServiceStatusIncludeTest() {
		// STARTUP_TYPE_MANUAL (Include) and SERVICE_STATUS_RUNNING (Exclude) are selected.
		wscParams.setStartupTypeInclude(STARTUP_TYPE_MANUAL);
		wscParams.setServiceStatusInclude(SERVICE_STATUS_RUNNING);
		assertTrue(invokeExecWmicCommand(result, args, regexes, wscParams));
		// every entry should contain STARTUP_TYPE_MANUAL (Include) and not contain SERVICE_STATUS_RUNNING (Exclude).
		String startup = "", status = "";
		for (Entry<String, String[]> entry : result.entrySet()) {
			startup = entry.getValue()[0];
			status = entry.getValue()[1];
			assertTrue(startup.equals(STARTUP_TYPE_MANUAL));
			assertTrue(status.equals(SERVICE_STATUS_RUNNING));
			assertFalse(startup.equals(STARTUP_TYPE_AUTO) || startup.equals(STARTUP_TYPE_DISABLED)
					|| status.equals(SERVICE_STATUS_STOPPED) || status.equals(SERVICE_STATUS_START_PENDING)
					|| status.equals(SERVICE_STATUS_CONTINUE_PENDING) || status.equals(SERVICE_STATUS_PAUSE_PENDING)
					|| status.equals(SERVICE_STATUS_PAUSED) || status.equals(SERVICE_STATUS_PENDING));
		}
	}

	@Test
	public void execWmicCommandStartupTypeIncludeAndServiceStatusExcludeTest() {
		// STARTUP_TYPE_MANUAL (Include) and SERVICE_STATUS_RUNNING (Exclude), rest are not selected
		wscParams.setStartupTypeInclude(STARTUP_TYPE_MANUAL);
		wscParams.setServiceStatusExclude(SERVICE_STATUS_RUNNING);
		assertTrue(invokeExecWmicCommand(result, args, regexes, wscParams));
		// every entry should contain STARTUP_TYPE_MANUAL (Include) and should NOT contain SERVICE_STATUS_RUNNING
		// (Exclude)
		String startup = "", status = "";
		for (Entry<String, String[]> entry : result.entrySet()) {
			startup = entry.getValue()[0];
			status = entry.getValue()[1];
			assertTrue(startup.equals(STARTUP_TYPE_MANUAL));
			assertFalse(status.equals(SERVICE_STATUS_RUNNING));
			assertTrue(status.equals(SERVICE_STATUS_START_PENDING) || status.equals(SERVICE_STATUS_CONTINUE_PENDING)
					|| status.equals(SERVICE_STATUS_PAUSE_PENDING) || status.equals(SERVICE_STATUS_PAUSED)
					|| status.equals(SERVICE_STATUS_PENDING) || status.equals(SERVICE_STATUS_STOPPED));
		}
	}

	@Test
	public void execWmicCommandStartupTypeExcludeTest() {
		// STARTUP_TYPE_DISABLED (Exclude) selected, rest are not selected
		wscParams.setStartupTypeExclude(STARTUP_TYPE_DISABLED);
		assertTrue(invokeExecWmicCommand(result, args, regexes, wscParams));
		String startup = "";
		for (Entry<String, String[]> entry : result.entrySet()) {
			startup = entry.getValue()[0];
			assertFalse(startup.equals(STARTUP_TYPE_DISABLED));
			assertTrue(startup.equals(STARTUP_TYPE_MANUAL) || startup.equals(STARTUP_TYPE_AUTO));
		}
	}

	@Test
	public void execWmicCommandStartupTypeExcludeAndServiceStatusIncludeTest() {
		// STARTUP_TYPE_MANUAL (Exclude) and SERVICE_STATUS_RUNNING (Include) are selected.
		wscParams.setStartupTypeExclude(STARTUP_TYPE_MANUAL);
		wscParams.setServiceStatusInclude(SERVICE_STATUS_RUNNING);
		assertTrue(invokeExecWmicCommand(result, args, regexes, wscParams));
		// every entry should NOT contain STARTUP_TYPE_MANUAL (Exclude) and contain SERVICE_STATUS_RUNNING (Include).
		String startup = "", status = "";
		for (Entry<String, String[]> entry : result.entrySet()) {
			startup = entry.getValue()[0];
			status = entry.getValue()[1];
			assertFalse(startup.equals(STARTUP_TYPE_MANUAL));
			assertTrue(startup.equals(STARTUP_TYPE_AUTO) || startup.equals(STARTUP_TYPE_DISABLED));
			assertTrue(status.equals(SERVICE_STATUS_RUNNING));
			assertFalse(status.equals(SERVICE_STATUS_PAUSED) || status.equals(SERVICE_STATUS_PAUSE_PENDING)
					|| status.equals(SERVICE_STATUS_PENDING) || status.equals(SERVICE_STATUS_START_PENDING)
					|| status.equals(SERVICE_STATUS_STOPPED));
		}
	}

	@Test
	public void execWmicCommandStartupTypeExcludeAndServiceStatusExcludeTest() {
		// STARTUP_TYPE_MANUAL (Exclude) and SERVICE_STATUS_RUNNING (Exclude) are selected.
		wscParams.setStartupTypeExclude(STARTUP_TYPE_MANUAL);
		wscParams.setServiceStatusExclude(SERVICE_STATUS_RUNNING);
		assertTrue(invokeExecWmicCommand(result, args, regexes, wscParams));
		// every entry should NOT contain STARTUP_TYPE_MANUAL (Exclude) and SERVICE_STATUS_RUNNING (Exclude).
		String startup = "", status = "";
		for (Entry<String, String[]> entry : result.entrySet()) {
			startup = entry.getValue()[0];
			status = entry.getValue()[1];
			assertFalse(startup.equals(STARTUP_TYPE_MANUAL));
			assertTrue(startup.equals(STARTUP_TYPE_AUTO) || startup.equals(STARTUP_TYPE_DISABLED));
			assertFalse(status.equals(SERVICE_STATUS_RUNNING));
			assertTrue(status.equals(SERVICE_STATUS_PAUSED) || status.equals(SERVICE_STATUS_PAUSE_PENDING)
					|| status.equals(SERVICE_STATUS_PENDING) || status.equals(SERVICE_STATUS_START_PENDING)
					|| status.equals(SERVICE_STATUS_STOPPED));
		}
	}

	@Test
	public void exicWmicCommandServiceStatusIncludeTest() {
		// service include is selected ("Stopped"), rest are not selected
		wscParams.setServiceStatusInclude(SERVICE_STATUS_STOPPED);
		assertTrue(invokeExecWmicCommand(result, args, regexes, wscParams));
		// every entry should contain "Stopped"
		String status = "";
		for (Entry<String, String[]> entry : result.entrySet()) {
			status = entry.getValue()[1];
			assertTrue(status.equals(SERVICE_STATUS_STOPPED));
			assertFalse(status.equals(SERVICE_STATUS_START_PENDING));
			assertFalse(status.equals(SERVICE_STATUS_CONTINUE_PENDING));
			assertFalse(status.equals(SERVICE_STATUS_PAUSE_PENDING));
			assertFalse(status.equals(SERVICE_STATUS_PAUSED));
			assertFalse(status.equals(SERVICE_STATUS_PENDING));
			assertFalse(status.equals(SERVICE_STATUS_RUNNING));
		}
	}

	@Test
	public void exicWmicCommandServiceStatusExcludeTest() {
		// service exclude is selected ("Stopped"), rest are not selected
		wscParams.setServiceStatusExclude(SERVICE_STATUS_STOPPED);
		assertTrue(invokeExecWmicCommand(result, args, regexes, wscParams));
		// every entry should NOT contain "Stopped"
		String status = "";
		for (Entry<String, String[]> entry : result.entrySet()) {
			status = entry.getValue()[1];
			assertFalse(status.equals(SERVICE_STATUS_STOPPED));
			// Since only include "Stopped" is selected, rest should NOT be matched.
			assertTrue(status.equals(SERVICE_STATUS_START_PENDING) || status.equals(SERVICE_STATUS_CONTINUE_PENDING)
					|| status.equals(SERVICE_STATUS_PAUSE_PENDING) || status.equals(SERVICE_STATUS_PAUSED)
					|| status.equals(SERVICE_STATUS_PENDING) || status.equals(SERVICE_STATUS_RUNNING));
		}
	}

	@Test
	public void splitLineAndPutInHashMap() {
		HashMap<String, String[]> result = new HashMap<String, String[]>();
		// To match any Service Display Name.
		String someVerySpecialLine = SystemUtils.IS_OS_WINDOWS ? "DEV-SYOON,Random, Display,  Awesome,RandomDisplayAwesome,"
				+ STARTUP_TYPE_MANUAL + "," + SERVICE_STATUS_STOPPED
				: "Random, Display,  Awesome,RandomDisplayAwesome," + STARTUP_TYPE_MANUAL + ","
						+ SERVICE_STATUS_STOPPED;

		// Testing a special case which Service Display Name contains comma(s).
		// Since Startup Type (Include) : Manual is selected, someVerySpecialLine won't be filtered.
		wscParams.setStartupTypeInclude(STARTUP_TYPE_MANUAL);
		assertTrue(invokeSplitLineAndPutInHashMap(result, someVerySpecialLine, regexes, wscParams));
		for (Entry<String, String[]> entry : result.entrySet()) {
			// The key should be "Random, Display,  Awesome" (aka messed up Service Display Name).
			assertTrue(entry.getKey().equals("Random, Display,  Awesome"));
			assertTrue(entry.getValue()[0].equals(STARTUP_TYPE_MANUAL));
			assertTrue(entry.getValue()[1].equals(SERVICE_STATUS_STOPPED));
		}
	}

	@Test
	public void checkIncludeExcludeAndConvertAutomaticTest() {
		// only Startup Type (Include) is selected
		wscParams.setStartupTypeInclude(STARTUP_TYPE_AUTO);
		assertTrue(invokeCheckIncludeExclude(wscParams));
		// only Startup Type (Exclude) is selected
		wscParams.resetStartupTypeServiceStatusProperties();
		wscParams.setStartupTypeExclude(STARTUP_TYPE_AUTO);
		assertTrue(invokeCheckIncludeExclude(wscParams));
		// only Service Status (Include) is selected
		wscParams.resetStartupTypeServiceStatusProperties();
		wscParams.setServiceStatusInclude(SERVICE_STATUS_STOPPED);
		assertTrue(invokeCheckIncludeExclude(wscParams));
		// only Service Status (Exclude) is selected
		wscParams.resetStartupTypeServiceStatusProperties();
		wscParams.setServiceStatusExclude(SERVICE_STATUS_STOPPED);
		assertTrue(invokeCheckIncludeExclude(wscParams));
		// Startup Type (Include) and Service Statue (Include) are selected
		wscParams.resetStartupTypeServiceStatusProperties();
		wscParams.setStartupTypeInclude(STARTUP_TYPE_AUTO);
		wscParams.setServiceStatusInclude(SERVICE_STATUS_STOPPED);
		assertTrue(invokeCheckIncludeExclude(wscParams));
		// Startup Type (Include) and Service Statue (Exclude) are selected
		wscParams.resetStartupTypeServiceStatusProperties();
		wscParams.setStartupTypeInclude(STARTUP_TYPE_AUTO);
		wscParams.setServiceStatusExclude(SERVICE_STATUS_STOPPED);
		assertTrue(invokeCheckIncludeExclude(wscParams));
		// Startup Type (Exclude) and Service Statue (Include) are selected
		wscParams.resetStartupTypeServiceStatusProperties();
		wscParams.setStartupTypeExclude(STARTUP_TYPE_AUTO);
		wscParams.setServiceStatusInclude(SERVICE_STATUS_STOPPED);
		assertTrue(invokeCheckIncludeExclude(wscParams));
		// Startup Type (Exclude) and Service Statue (Exclude) are selected
		wscParams.resetStartupTypeServiceStatusProperties();
		wscParams.setStartupTypeExclude(STARTUP_TYPE_AUTO);
		wscParams.setServiceStatusExclude(SERVICE_STATUS_STOPPED);
		assertTrue(invokeCheckIncludeExclude(wscParams));
		// both Startup Type (Include) & (Exclude) are selected
		wscParams.resetStartupTypeServiceStatusProperties();
		wscParams.setStartupTypeInclude(STARTUP_TYPE_AUTO);
		wscParams.setStartupTypeExclude(STARTUP_TYPE_MANUAL);
		assertFalse(invokeCheckIncludeExclude(wscParams));
		// both Service Status (Include) & (Exclude) are selected
		wscParams.resetStartupTypeServiceStatusProperties();
		wscParams.setServiceStatusInclude(SERVICE_STATUS_STOPPED);
		wscParams.setServiceStatusExclude(SERVICE_STATUS_PENDING);
		assertFalse(invokeCheckIncludeExclude(wscParams));
		// both Service Status (Include) & (Exclude) and Startup Type (Include) are selected
		wscParams.resetStartupTypeServiceStatusProperties();
		wscParams.setStartupTypeInclude(STARTUP_TYPE_AUTO);
		wscParams.setServiceStatusInclude(SERVICE_STATUS_STOPPED);
		wscParams.setServiceStatusExclude(SERVICE_STATUS_PENDING);
		assertFalse(invokeCheckIncludeExclude(wscParams));
		// both Service Status (Include) & (Exclude) and Startup Type (Exclude) are selected
		wscParams.resetStartupTypeServiceStatusProperties();
		wscParams.setStartupTypeExclude(STARTUP_TYPE_AUTO);
		wscParams.setServiceStatusInclude(SERVICE_STATUS_STOPPED);
		wscParams.setServiceStatusExclude(SERVICE_STATUS_PENDING);
		assertFalse(invokeCheckIncludeExclude(wscParams));
		// both Startup Type (Include) & (Exclude) and Service Status (Include) are selected
		wscParams.resetStartupTypeServiceStatusProperties();
		wscParams.setStartupTypeInclude(STARTUP_TYPE_AUTO);
		wscParams.setStartupTypeExclude(STARTUP_TYPE_MANUAL);
		wscParams.setServiceStatusInclude(SERVICE_STATUS_STOPPED);
		assertFalse(invokeCheckIncludeExclude(wscParams));
		// both Startup Type (Include) & (Exclude) and Service Status (Exclude) are selected
		wscParams.resetStartupTypeServiceStatusProperties();
		wscParams.setStartupTypeInclude(STARTUP_TYPE_AUTO);
		wscParams.setStartupTypeExclude(STARTUP_TYPE_MANUAL);
		wscParams.setServiceStatusExclude(SERVICE_STATUS_STOPPED);
		assertFalse(invokeCheckIncludeExclude(wscParams));
	}

	@Test
	public void checkAdminOrPasswordMissingTest() {
		// It is localhost with non-empty userName
		WSCPluginParams temp = new WSCPluginParams("localhost", null, "user", null, null, null, null, null, null);
		assertFalse(invokeCheckAdminOrPasswordMissing(temp));
		// It is localhost with non-empty password
		temp.setUserName(null);
		temp.setPassword("pw");
		assertFalse(invokeCheckAdminOrPasswordMissing(temp));
		// It is localhost with non-empty domain
		temp.setPassword(null);
		temp.setDomainName("domain");
		assertFalse(invokeCheckAdminOrPasswordMissing(temp));
		// It is localhost with non-empty username / password / domain.
		temp.setUserName("user");
		temp.setPassword("pw");
		assertFalse(invokeCheckAdminOrPasswordMissing(temp));
		// all empty.
		temp.setDomainName(null);
		temp.setUserName(null);
		temp.setPassword(null);
		assertTrue(invokeCheckAdminOrPasswordMissing(temp));
		// Non-localhost but one of userName / password / domain or more are empty
		temp.setHostName("non-local");
		// missing domain and pw.
		temp.setUserName("user");
		assertFalse(invokeCheckAdminOrPasswordMissing(temp));
		// missing username and domain
		temp.setUserName(null);
		temp.setPassword("pw");
		assertFalse(invokeCheckAdminOrPasswordMissing(temp));
		// missing all.
		temp.setPassword(null);
		assertFalse(invokeCheckAdminOrPasswordMissing(temp));
		// missing username
		temp.setPassword("pw");
		temp.setDomainName("domain");
		assertFalse(invokeCheckAdminOrPasswordMissing(temp));
		// missing pw.
		temp.setPassword(null);
		temp.setUserName("user");
		assertFalse(invokeCheckAdminOrPasswordMissing(temp));
		// all entered
		temp.setPassword("pw");
		assertTrue(invokeCheckAdminOrPasswordMissing(temp));
	}

	@Test
	public void buildArgsOfProcessBuilderLocalhostTest() {
		ArrayList<String> args = new ArrayList<String>();
		WSCPluginParams temp = new WSCPluginParams("localhost", domainName, userName, password, null, null, null, null,
				null);

		if (SystemUtils.IS_OS_WINDOWS) {
			String[] winArgs = new String[] { "wmic", "/node:\"" + temp.getHostName() + "\"", "Service", "GET",
					"Caption,Name,StartMode,State", "/format:csv" };
			// If it is localhost (Windows monitoring station).
			assertTrue(invokeBuildArgsOfProcessBuilder(args, temp));
			assertTrue(args.size() == winArgs.length);
			for (int i = 0; i < winArgs.length; i++) {
				assertTrue(args.get(i).equals(winArgs[i]));
			}
		} else {
			// localhost is not allowed on Linux monitoring station.
			assertFalse(invokeBuildArgsOfProcessBuilder(args, temp));
		}
	}

	@Test
	public void buildArgsOfProcessBuilderTest() {
		ArrayList<String> args = new ArrayList<String>();
		WSCPluginParams temp = new WSCPluginParams("non-localhost", domainName, userName, password, null, null, null,
				null, null);

		if (SystemUtils.IS_OS_WINDOWS) {
			String[] winArgs = temp.getDomainName() != null ? new String[] { "wmic",
					"/node:\"" + temp.getHostName() + "\"",
					"/user:" + temp.getDomainName() + "\\" + temp.getUserName(), "/password:" + temp.getPassword(),
					"Service", "GET", "Caption,Name,StartMode,State", "/format:csv" }
					: new String[] { "wmic", "/node:\"" + temp.getHostName() + "\"", "/user:" + temp.getUserName(),
							"/password:" + temp.getPassword(), "Service", "GET", "Caption,Name,StartMode,State",
							"/format:csv" };
			// If it is NOT localhost (Windows monitoring station).
			assertTrue(invokeBuildArgsOfProcessBuilder(args, temp));
			assertTrue(args.size() == winArgs.length);
			for (int i = 0; i < winArgs.length; i++) {
				assertTrue(args.get(i).equals(winArgs[i]));
			}
		} else {
			String[] linuxArgs = temp.getDomainName() != null ? new String[] { "wmic", "-U",
					temp.getDomainName() + "/" + temp.getUserName() + "%" + temp.getPassword(),
					"//" + temp.getHostName(), "select Caption,Name,StartMode,State from Win32_Service",
					"--delimiter=," } : new String[] { "wmic", "-U", temp.getUserName() + "%" + temp.getPassword(),
					"//" + temp.getHostName(), "select Caption,Name,StartMode,State from Win32_Service",
					"--delimiter=," };
			// Linux monitoring station.
			assertTrue(invokeBuildArgsOfProcessBuilder(args, temp));
			assertTrue(args.size() == linuxArgs.length);
			for (int i = 0; i < linuxArgs.length; i++) {
				assertTrue(args.get(i).equals(linuxArgs[i]));
			}
		}
	}

	/**
	 * Invoke private checkAdminOrPasswordMissing method by using Java Reflection.
	 * 
	 * @return True if the method call is successful, false otherwise.
	 */
	private boolean invokeCheckAdminOrPasswordMissing(WSCPluginParams wscParams) {
		boolean gotResult = false;
		try {
			Method method = UptimeMonitorWindowsServiceCheckAdvanced.class.getDeclaredMethod(
					"checkAdminOrPasswordMissing", new Class[] { WSCPluginParams.class });
			method.setAccessible(true);
			gotResult = (boolean) method.invoke(UptimeMonitorWindowsServiceCheckAdvanced.class.newInstance(),
					new Object[] { wscParams });
		} catch (NoSuchMethodException | SecurityException | IllegalArgumentException | IllegalAccessException
				| InvocationTargetException | InstantiationException e) {
			System.err.println(e);
		}
		return gotResult;
	}

	/**
	 * Invoke private method checkIncludeExclude by using Reflection.
	 * 
	 * @return True if successful, false otherwise.
	 */
	private boolean invokeCheckIncludeExclude(WSCPluginParams wscParams) {
		boolean okay = false;
		try {
			Method method = UptimeMonitorWindowsServiceCheckAdvanced.class.getDeclaredMethod("checkIncludeExclude",
					new Class[] { WSCPluginParams.class });
			method.setAccessible(true);
			okay = (boolean) method.invoke(UptimeMonitorWindowsServiceCheckAdvanced.class.newInstance(),
					new Object[] { wscParams });
		} catch (NoSuchMethodException | SecurityException | IllegalArgumentException | IllegalAccessException
				| InvocationTargetException | InstantiationException e) {
			System.err.println(e);
		}
		return okay;
	}

	/**
	 * Invoke private execWmicCommand method by using Java Reflection.
	 * 
	 * @return True if executing the wmic command is successful, false otherwise.
	 */
	private boolean invokeExecWmicCommand(HashMap<String, String[]> result, ArrayList<String> wmicCommand,
			HashSet<String> regexes, WSCPluginParams wscParams) {
		boolean gotResult = false;
		try {
			Method method = UptimeMonitorWindowsServiceCheckAdvanced.class.getDeclaredMethod("execWmicCommand",
					new Class[] { HashMap.class, ArrayList.class, HashSet.class, WSCPluginParams.class });
			method.setAccessible(true);
			gotResult = (boolean) method.invoke(UptimeMonitorWindowsServiceCheckAdvanced.class.newInstance(),
					new Object[] { result, wmicCommand, regexes, wscParams });
		} catch (NoSuchMethodException | SecurityException | IllegalArgumentException | IllegalAccessException
				| InvocationTargetException | InstantiationException e) {
			System.err.println(e);
		}
		return gotResult;
	}

	/**
	 * Invoke private method splitLineAndPutInHashMap by using Reflection.
	 * 
	 * @return True if successful, false otherwise.
	 */
	private boolean invokeSplitLineAndPutInHashMap(HashMap<String, String[]> result, String line,
			HashSet<String> regexes, WSCPluginParams wscParams) {
		boolean gotResult = false;
		try {
			Method method = UptimeMonitorWindowsServiceCheckAdvanced.class.getDeclaredMethod(
					"splitLineAndPutInHashMap", new Class[] { HashMap.class, String.class, HashSet.class,
							WSCPluginParams.class });
			method.setAccessible(true);
			gotResult = (boolean) method.invoke(UptimeMonitorWindowsServiceCheckAdvanced.class.newInstance(),
					new Object[] { result, line, regexes, wscParams });
		} catch (NoSuchMethodException | SecurityException | IllegalArgumentException | IllegalAccessException
				| InvocationTargetException | InstantiationException e) {
			System.err.println(e);
		}
		return gotResult;
	}

	/**
	 * Invoke private checkRegex method by using Java Reflection.
	 * 
	 * @return True if the given regex is valid, false otherwise.
	 */
	private boolean invokeCheckRegexAndAdd(HashSet<String> regexes, WSCPluginParams wscParams) {
		boolean regexValid = false;
		try {
			Method method = UptimeMonitorWindowsServiceCheckAdvanced.class.getDeclaredMethod("checkRegexAndAdd",
					new Class[] { HashSet.class, WSCPluginParams.class });
			method.setAccessible(true);
			regexValid = (boolean) method.invoke(UptimeMonitorWindowsServiceCheckAdvanced.class.newInstance(),
					new Object[] { regexes, wscParams });
		} catch (NoSuchMethodException | SecurityException | IllegalArgumentException | IllegalAccessException
				| InvocationTargetException | InstantiationException e) {
			System.err.println(e);
		}
		return regexValid;
	}

	/**
	 * Invoke private buildArgsOfProcessBuilder method by using Java Reflection.
	 * 
	 * @return True if building args is successful, false otherwise.
	 */
	private boolean invokeBuildArgsOfProcessBuilder(ArrayList<String> args, WSCPluginParams wscParams) {
		boolean okay = false;
		try {
			Method method = UptimeMonitorWindowsServiceCheckAdvanced.class.getDeclaredMethod(
					"buildArgsOfProcessBuilder", new Class[] { ArrayList.class, WSCPluginParams.class });
			method.setAccessible(true);
			okay = (boolean) method.invoke(UptimeMonitorWindowsServiceCheckAdvanced.class.newInstance(), new Object[] {
					args, wscParams });
		} catch (NoSuchMethodException | SecurityException | IllegalArgumentException | IllegalAccessException
				| InvocationTargetException | InstantiationException e) {
			System.err.println(e);
		}
		return okay;
	}
}
