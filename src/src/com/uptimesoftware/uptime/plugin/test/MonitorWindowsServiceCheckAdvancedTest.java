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

public class MonitorWindowsServiceCheckAdvancedTest {

	// Configure these private variables with your inputs. Default is 'null' not empty string "".
	private String hostname = "localhost";
	// When you don't need adminName, password, domainName make it null, not empty string "".
	private String adminName = null;
	private String password = null;
	private String domainName = null;

	private static final boolean IS_IT_LOCALHOST = true;
	private static final boolean STARTUP_TYPE_INCLUDE_SELECTED = true;
	private static final boolean STARTUP_TYPE_EXCLUDE_SELECTED = true;
	private static final boolean SERVICE_STATUS_INCLUDE_SELECTED = true;
	private static final boolean SERVICE_STATUS_EXCLUDE_SELECTED = true;
	private static final String STARTUP_TYPE_INCLUDE_NOT_ENTERED = null;
	private static final String STARTUP_TYPE_EXCLUDE_NOT_ENTERED = null;
	private static final String SERVICE_STATUS_INCLUDE_NOT_ENTERED = null;
	private static final String SERVICE_STATUS_EXCLUDE_NOT_ENTERED = null;

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

		if (hostname.equals("localhost")) {
			// If it's running on Linux, all testcases will fail.
			assertTrue(invokeBuildArgsOfProcessBuilder(args, IS_IT_LOCALHOST, hostname, adminName, password, domainName));
		} else {
			assertTrue(invokeBuildArgsOfProcessBuilder(args, !IS_IT_LOCALHOST, hostname, adminName, password,
					domainName));
		}

		// Get a list of ALL Windows service.
		assertTrue(invokeCheckRegexAndAdd(regexes, ".*,Win.*"));
	}

	@Test
	public void checkRegexAndAddTest() {
		LinkedList<String> temp = new LinkedList<String>();
		HashSet<String> regexTemp = new HashSet<String>();

		temp.add("Win.*");
		temp.add("gefe.*");
		temp.add("   roflcopter*");
		// Valid regexes. Comma is a separator.
		assertTrue(invokeCheckRegexAndAdd(regexTemp, "Win.*,gefe.*,   roflcopter*"));
		assertTrue(regexTemp.size() == temp.size());
		for (String regex : regexTemp) {
			assertTrue(temp.contains(regex));
		}

		// Invalid regexes. Comma is a separator.
		assertFalse(invokeCheckRegexAndAdd(regexTemp, "*,*"));
		assertFalse(invokeCheckRegexAndAdd(regexTemp, "*"));
	}

	@Test
	public void execWmicCommandStartupTypeIncludeTest() {
		// STARTUP_TYPE_MANUAL (Include) is selected.
		assertTrue(invokeExecWmicCommand(result, args, regexes, STARTUP_TYPE_INCLUDE_SELECTED,
				!STARTUP_TYPE_EXCLUDE_SELECTED, !SERVICE_STATUS_INCLUDE_SELECTED, !SERVICE_STATUS_EXCLUDE_SELECTED,
				STARTUP_TYPE_MANUAL, STARTUP_TYPE_EXCLUDE_NOT_ENTERED, SERVICE_STATUS_INCLUDE_NOT_ENTERED,
				SERVICE_STATUS_EXCLUDE_NOT_ENTERED));
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
		assertTrue(invokeExecWmicCommand(result, args, regexes, STARTUP_TYPE_INCLUDE_SELECTED,
				!STARTUP_TYPE_EXCLUDE_SELECTED, SERVICE_STATUS_INCLUDE_SELECTED, !SERVICE_STATUS_EXCLUDE_SELECTED,
				STARTUP_TYPE_MANUAL, STARTUP_TYPE_EXCLUDE_NOT_ENTERED, SERVICE_STATUS_RUNNING,
				SERVICE_STATUS_EXCLUDE_NOT_ENTERED));
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
		assertTrue(invokeExecWmicCommand(result, args, regexes, STARTUP_TYPE_INCLUDE_SELECTED,
				!STARTUP_TYPE_EXCLUDE_SELECTED, !SERVICE_STATUS_INCLUDE_SELECTED, SERVICE_STATUS_EXCLUDE_SELECTED,
				STARTUP_TYPE_MANUAL, STARTUP_TYPE_EXCLUDE_NOT_ENTERED, SERVICE_STATUS_INCLUDE_NOT_ENTERED,
				SERVICE_STATUS_RUNNING));
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
		assertTrue(invokeExecWmicCommand(result, args, regexes, !STARTUP_TYPE_INCLUDE_SELECTED,
				STARTUP_TYPE_EXCLUDE_SELECTED, !SERVICE_STATUS_INCLUDE_SELECTED, !SERVICE_STATUS_EXCLUDE_SELECTED,
				STARTUP_TYPE_INCLUDE_NOT_ENTERED, STARTUP_TYPE_DISABLED, SERVICE_STATUS_INCLUDE_NOT_ENTERED,
				SERVICE_STATUS_EXCLUDE_NOT_ENTERED));
		String startup = "";
		for (Entry<String, String[]> entry : result.entrySet()) {
			startup = entry.getValue()[0];
			assertFalse(startup.equals(STARTUP_TYPE_DISABLED));
			assertTrue(startup.equals(STARTUP_TYPE_MANUAL) || startup.equals(STARTUP_TYPE_AUTO));
		}
	}

	@Test
	public void execWmicCommandStartupTypeExcludeAndServiceStatusIncludeTest() {
		// STARTUP_TYPE_MANUAL (Include) and SERVICE_STATUS_RUNNING (Exclude) are selected.
		assertTrue(invokeExecWmicCommand(result, args, regexes, !STARTUP_TYPE_INCLUDE_SELECTED,
				STARTUP_TYPE_EXCLUDE_SELECTED, SERVICE_STATUS_INCLUDE_SELECTED, !SERVICE_STATUS_EXCLUDE_SELECTED,
				STARTUP_TYPE_INCLUDE_NOT_ENTERED, STARTUP_TYPE_MANUAL, SERVICE_STATUS_RUNNING,
				SERVICE_STATUS_EXCLUDE_NOT_ENTERED));
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
		// STARTUP_TYPE_MANUAL (Include) and SERVICE_STATUS_RUNNING (Exclude) are selected.
		assertTrue(invokeExecWmicCommand(result, args, regexes, !STARTUP_TYPE_INCLUDE_SELECTED,
				STARTUP_TYPE_EXCLUDE_SELECTED, !SERVICE_STATUS_INCLUDE_SELECTED, SERVICE_STATUS_EXCLUDE_SELECTED,
				STARTUP_TYPE_INCLUDE_NOT_ENTERED, STARTUP_TYPE_MANUAL, SERVICE_STATUS_INCLUDE_NOT_ENTERED,
				SERVICE_STATUS_RUNNING));
		// every entry should NOT contain STARTUP_TYPE_MANUAL (Exclude) and contain SERVICE_STATUS_RUNNING (Include).
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
		assertTrue(invokeExecWmicCommand(result, args, regexes, !STARTUP_TYPE_INCLUDE_SELECTED,
				!STARTUP_TYPE_EXCLUDE_SELECTED, SERVICE_STATUS_INCLUDE_SELECTED, !SERVICE_STATUS_EXCLUDE_SELECTED,
				STARTUP_TYPE_INCLUDE_NOT_ENTERED, STARTUP_TYPE_EXCLUDE_NOT_ENTERED, SERVICE_STATUS_STOPPED,
				SERVICE_STATUS_EXCLUDE_NOT_ENTERED));
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
		assertTrue(invokeExecWmicCommand(result, args, regexes, !STARTUP_TYPE_INCLUDE_SELECTED,
				!STARTUP_TYPE_EXCLUDE_SELECTED, !SERVICE_STATUS_INCLUDE_SELECTED, SERVICE_STATUS_EXCLUDE_SELECTED,
				STARTUP_TYPE_INCLUDE_NOT_ENTERED, STARTUP_TYPE_EXCLUDE_NOT_ENTERED, SERVICE_STATUS_INCLUDE_NOT_ENTERED,
				SERVICE_STATUS_STOPPED));
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
		assertTrue(invokeSplitLineAndPutInHashMap(result, someVerySpecialLine, regexes, STARTUP_TYPE_INCLUDE_SELECTED,
				!STARTUP_TYPE_EXCLUDE_SELECTED, !SERVICE_STATUS_INCLUDE_SELECTED, !SERVICE_STATUS_EXCLUDE_SELECTED,
				STARTUP_TYPE_MANUAL, STARTUP_TYPE_EXCLUDE_NOT_ENTERED, SERVICE_STATUS_INCLUDE_NOT_ENTERED,
				SERVICE_STATUS_EXCLUDE_NOT_ENTERED));
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
		assertTrue(invokeCheckIncludeExclude(STARTUP_TYPE_INCLUDE_SELECTED, !STARTUP_TYPE_EXCLUDE_SELECTED,
				!SERVICE_STATUS_INCLUDE_SELECTED, !SERVICE_STATUS_EXCLUDE_SELECTED));
		// only Startup Type (Exclude) is selected
		assertTrue(invokeCheckIncludeExclude(!STARTUP_TYPE_INCLUDE_SELECTED, STARTUP_TYPE_EXCLUDE_SELECTED,
				!SERVICE_STATUS_INCLUDE_SELECTED, !SERVICE_STATUS_EXCLUDE_SELECTED));
		// only Service Status (Include) is selected
		assertTrue(invokeCheckIncludeExclude(!STARTUP_TYPE_INCLUDE_SELECTED, !STARTUP_TYPE_EXCLUDE_SELECTED,
				SERVICE_STATUS_INCLUDE_SELECTED, !SERVICE_STATUS_EXCLUDE_SELECTED));
		// only Service Status (Exclude) is selected
		assertTrue(invokeCheckIncludeExclude(!STARTUP_TYPE_INCLUDE_SELECTED, !STARTUP_TYPE_EXCLUDE_SELECTED,
				!SERVICE_STATUS_INCLUDE_SELECTED, SERVICE_STATUS_EXCLUDE_SELECTED));
		// Startup Type (Include) and Service Statue (Include) are selected
		assertTrue(invokeCheckIncludeExclude(STARTUP_TYPE_INCLUDE_SELECTED, !STARTUP_TYPE_EXCLUDE_SELECTED,
				SERVICE_STATUS_INCLUDE_SELECTED, !SERVICE_STATUS_EXCLUDE_SELECTED));
		// Startup Type (Include) and Service Statue (Exclude) are selected
		assertTrue(invokeCheckIncludeExclude(STARTUP_TYPE_INCLUDE_SELECTED, !STARTUP_TYPE_EXCLUDE_SELECTED,
				!SERVICE_STATUS_INCLUDE_SELECTED, SERVICE_STATUS_EXCLUDE_SELECTED));
		// Startup Type (Exclude) and Service Statue (Include) are selected
		assertTrue(invokeCheckIncludeExclude(!STARTUP_TYPE_INCLUDE_SELECTED, STARTUP_TYPE_EXCLUDE_SELECTED,
				SERVICE_STATUS_INCLUDE_SELECTED, !SERVICE_STATUS_EXCLUDE_SELECTED));
		// Startup Type (Exclude) and Service Statue (Exclude) are selected
		assertTrue(invokeCheckIncludeExclude(!STARTUP_TYPE_INCLUDE_SELECTED, STARTUP_TYPE_EXCLUDE_SELECTED,
				!SERVICE_STATUS_INCLUDE_SELECTED, SERVICE_STATUS_EXCLUDE_SELECTED));
		// both Startup Type (Include) & (Exclude) are selected
		assertFalse(invokeCheckIncludeExclude(STARTUP_TYPE_INCLUDE_SELECTED, STARTUP_TYPE_EXCLUDE_SELECTED,
				!SERVICE_STATUS_INCLUDE_SELECTED, !SERVICE_STATUS_EXCLUDE_SELECTED));
		// both Service Status (Include) & (Exclude) are selected
		assertFalse(invokeCheckIncludeExclude(!STARTUP_TYPE_INCLUDE_SELECTED, !STARTUP_TYPE_EXCLUDE_SELECTED,
				SERVICE_STATUS_INCLUDE_SELECTED, SERVICE_STATUS_EXCLUDE_SELECTED));
		// both Service Status (Include) & (Exclude) and Startup Type (Include) are selected
		assertFalse(invokeCheckIncludeExclude(STARTUP_TYPE_INCLUDE_SELECTED, !STARTUP_TYPE_EXCLUDE_SELECTED,
				SERVICE_STATUS_INCLUDE_SELECTED, SERVICE_STATUS_EXCLUDE_SELECTED));
		// both Service Status (Include) & (Exclude) and Startup Type (Exclude) are selected
		assertFalse(invokeCheckIncludeExclude(!STARTUP_TYPE_INCLUDE_SELECTED, STARTUP_TYPE_EXCLUDE_SELECTED,
				SERVICE_STATUS_INCLUDE_SELECTED, SERVICE_STATUS_EXCLUDE_SELECTED));
		// both Startup Type (Include) & (Exclude) and Service Status (Include) are selected
		assertFalse(invokeCheckIncludeExclude(STARTUP_TYPE_INCLUDE_SELECTED, STARTUP_TYPE_EXCLUDE_SELECTED,
				SERVICE_STATUS_INCLUDE_SELECTED, !SERVICE_STATUS_EXCLUDE_SELECTED));
		// both Startup Type (Include) & (Exclude) and Service Status (Exclude) are selected
		assertFalse(invokeCheckIncludeExclude(STARTUP_TYPE_INCLUDE_SELECTED, STARTUP_TYPE_EXCLUDE_SELECTED,
				!SERVICE_STATUS_INCLUDE_SELECTED, SERVICE_STATUS_EXCLUDE_SELECTED));
	}

	@Test
	public void checkAdminOrPasswordMissingTest() {
		// It is localhost with non-empty adminName
		assertFalse(invokeCheckAdminOrPasswordMissing(IS_IT_LOCALHOST, "some_admin_name", null, null));
		// It is localhost with non-empty password
		assertFalse(invokeCheckAdminOrPasswordMissing(IS_IT_LOCALHOST, null, "some_pw", null));
		// It is localhost with non-empty domain
		assertFalse(invokeCheckAdminOrPasswordMissing(IS_IT_LOCALHOST, null, null, "some_domain"));
		// It is localhost with non-empty admin / password / domain.
		assertFalse(invokeCheckAdminOrPasswordMissing(IS_IT_LOCALHOST, "some_admin_name", "some_password",
				"some_domain"));
		// all empty.
		assertTrue(invokeCheckAdminOrPasswordMissing(IS_IT_LOCALHOST, null, null, null));
		// Non-localhost but one of adminName / password or both are empty
		assertFalse(invokeCheckAdminOrPasswordMissing(!IS_IT_LOCALHOST, "some_admin_name", null, null));
		assertFalse(invokeCheckAdminOrPasswordMissing(!IS_IT_LOCALHOST, null, "some_password", null));
		assertFalse(invokeCheckAdminOrPasswordMissing(!IS_IT_LOCALHOST, null, null, null));
		assertFalse(invokeCheckAdminOrPasswordMissing(!IS_IT_LOCALHOST, null, "some_password", "some_domain"));
		assertFalse(invokeCheckAdminOrPasswordMissing(!IS_IT_LOCALHOST, "some_admin_name", null, "some_domain"));
		// domain is not required many times for non-localhost
		assertTrue(invokeCheckAdminOrPasswordMissing(!IS_IT_LOCALHOST, "some_admin_name", "some_password", null));
	}

	@Test
	public void buildArgsOfProcessBuilderLocalhostTest() {
		ArrayList<String> args = new ArrayList<String>();
		if (SystemUtils.IS_OS_WINDOWS) {
			String[] winArgs = new String[] { "wmic", "/node:\"" + hostname + "\"", "Service", "GET",
					"Caption,Name,StartMode,State", "/format:csv" };
			// If it is localhost (Windows monitoring station).
			assertTrue(invokeBuildArgsOfProcessBuilder(args, IS_IT_LOCALHOST, hostname, adminName, password, domainName));
			assertTrue(args.size() == winArgs.length);
			for (int i = 0; i < winArgs.length; i++) {
				assertTrue(args.get(i).equals(winArgs[i]));
			}
		} else {
			// localhost is not allowed on Linux monitoring station.
			assertFalse(invokeBuildArgsOfProcessBuilder(args, IS_IT_LOCALHOST, hostname, adminName, password,
					domainName));
		}
	}

	@Test
	public void buildArgsOfProcessBuilderTest() {
		ArrayList<String> args = new ArrayList<String>();
		if (SystemUtils.IS_OS_WINDOWS) {
			String[] winArgs = domainName != null ? new String[] { "wmic", "/node:\"" + hostname + "\"",
					"/user:" + domainName + "\\" + adminName, "/password:" + password, "Service", "GET",
					"Caption,Name,StartMode,State", "/format:csv" } : new String[] { "wmic",
					"/node:\"" + hostname + "\"", "/user:" + adminName, "/password:" + password, "Service", "GET",
					"Caption,Name,StartMode,State", "/format:csv" };
			// If it is NOT localhost (Windows monitoring station).
			assertTrue(invokeBuildArgsOfProcessBuilder(args, !IS_IT_LOCALHOST, hostname, adminName, password,
					domainName));
			assertTrue(args.size() == winArgs.length);
			for (int i = 0; i < winArgs.length; i++) {
				assertTrue(args.get(i).equals(winArgs[i]));
			}
		} else {
			String[] linuxArgs = domainName != null ? new String[] { "wmic", "-U",
					domainName + "/" + adminName + "%" + password, "//" + hostname,
					"select Caption,Name,StartMode,State from Win32_Service", "--delimiter=," } : new String[] {
					"wmic", "-U", adminName + "%" + password, "//" + hostname,
					"select Caption,Name,StartMode,State from Win32_Service", "--delimiter=," };
			// Linux monitoring station.
			assertTrue(invokeBuildArgsOfProcessBuilder(args, !IS_IT_LOCALHOST, hostname, adminName, password,
					domainName));
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
	private boolean invokeCheckAdminOrPasswordMissing(boolean isItLocalhost, String adminName, String password,
			String domainName) {
		boolean gotResult = false;
		try {
			Method method = UptimeMonitorWindowsServiceCheckAdvanced.class.getDeclaredMethod(
					"checkAdminOrPasswordMissing", new Class[] { boolean.class, String.class, String.class,
							String.class });
			method.setAccessible(true);
			gotResult = (boolean) method.invoke(UptimeMonitorWindowsServiceCheckAdvanced.class.newInstance(),
					new Object[] { isItLocalhost, adminName, password, domainName });
		} catch (NoSuchMethodException | SecurityException | IllegalArgumentException | IllegalAccessException
				| InvocationTargetException | InstantiationException e) {
			System.err.println(e);
		}
		return gotResult;
	}

	private boolean invokeCheckIncludeExclude(boolean startupInc, boolean startupExc, boolean serviceInc,
			boolean serviceExc) {
		boolean okay = false;
		try {
			Method method = UptimeMonitorWindowsServiceCheckAdvanced.class.getDeclaredMethod("checkIncludeExclude",
					new Class[] { boolean.class, boolean.class, boolean.class, boolean.class });
			method.setAccessible(true);
			okay = (boolean) method.invoke(UptimeMonitorWindowsServiceCheckAdvanced.class.newInstance(), new Object[] {
					startupInc, startupExc, serviceInc, serviceExc });
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
			HashSet<String> regexes, boolean startupInc, boolean startupExc, boolean serviceInc, boolean serviceExc,
			String typeInc, String typeExc, String statusInc, String statusExc) {
		boolean gotResult = false;
		try {
			Method method = UptimeMonitorWindowsServiceCheckAdvanced.class.getDeclaredMethod("execWmicCommand",
					new Class[] { HashMap.class, ArrayList.class, HashSet.class, boolean.class, boolean.class,
							boolean.class, boolean.class, String.class, String.class, String.class, String.class });
			method.setAccessible(true);
			gotResult = (boolean) method.invoke(UptimeMonitorWindowsServiceCheckAdvanced.class.newInstance(),
					new Object[] { result, wmicCommand, regexes, startupInc, startupExc, serviceInc, serviceExc,
							typeInc, typeExc, statusInc, statusExc });
		} catch (NoSuchMethodException | SecurityException | IllegalArgumentException | IllegalAccessException
				| InvocationTargetException | InstantiationException e) {
			System.err.println(e);
		}

		// just testing
		// for (String key : result.keySet()) {
		// System.out.println(key + " : [ " + result.get(key)[0] + " - " + result.get(key)[1] + " ]");
		// }

		return gotResult;
	}

	private boolean invokeSplitLineAndPutInHashMap(HashMap<String, String[]> result, String line,
			HashSet<String> regexes, boolean startupInc, boolean startupExc, boolean serviceInc, boolean serviceExc,
			String typeInc, String typeExc, String statusInc, String statusExc) {
		boolean gotResult = false;
		try {
			Method method = UptimeMonitorWindowsServiceCheckAdvanced.class.getDeclaredMethod(
					"splitLineAndPutInHashMap", new Class[] { HashMap.class, String.class, HashSet.class,
							boolean.class, boolean.class, boolean.class, boolean.class, String.class, String.class,
							String.class, String.class });
			method.setAccessible(true);
			gotResult = (boolean) method.invoke(UptimeMonitorWindowsServiceCheckAdvanced.class.newInstance(),
					new Object[] { result, line, regexes, startupInc, startupExc, serviceInc, serviceExc, typeInc,
							typeExc, statusInc, statusExc });
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
	private boolean invokeCheckRegexAndAdd(HashSet<String> regexes, String serviceDisplayName) {
		boolean regexValid = false;
		try {
			Method method = UptimeMonitorWindowsServiceCheckAdvanced.class.getDeclaredMethod("checkRegexAndAdd",
					new Class[] { HashSet.class, String.class });
			method.setAccessible(true);
			regexValid = (boolean) method.invoke(UptimeMonitorWindowsServiceCheckAdvanced.class.newInstance(),
					new Object[] { regexes, serviceDisplayName });
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
	private boolean invokeBuildArgsOfProcessBuilder(ArrayList<String> args, boolean isItLocalhost, String hostname,
			String adminName, String password, String domainName) {
		boolean okay = false;
		try {
			Method method = UptimeMonitorWindowsServiceCheckAdvanced.class.getDeclaredMethod(
					"buildArgsOfProcessBuilder", new Class[] { ArrayList.class, boolean.class, String.class,
							String.class, String.class, String.class });
			method.setAccessible(true);
			okay = (boolean) method.invoke(UptimeMonitorWindowsServiceCheckAdvanced.class.newInstance(), new Object[] {
					args, isItLocalhost, hostname, adminName, password, domainName });
		} catch (NoSuchMethodException | SecurityException | IllegalArgumentException | IllegalAccessException
				| InvocationTargetException | InstantiationException e) {
			System.err.println(e);
		}
		return okay;
	}
}
