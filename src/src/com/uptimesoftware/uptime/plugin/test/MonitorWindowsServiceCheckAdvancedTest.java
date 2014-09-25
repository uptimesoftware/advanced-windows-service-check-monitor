package com.uptimesoftware.uptime.plugin.test;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.lang.SystemUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import com.uptimesoftware.uptime.plugin.MonitorWindowsServiceCheckAdvanced.UptimeMonitorWindowsServiceCheckAdvanced;

public class MonitorWindowsServiceCheckAdvancedTest {

	// Configure these private variables with your credentials.
	private String hostname = "localhost";
	private String adminName = "";
	private String password = "";
	private static String serviceDisplayNameWithMultipleRegexes = "Win.*,gefe.*,VM.*,   roflcopter*";
	private static String serviceDisplayNameWithSingleRegex = "regex.*";

	private HashMap<String, String[]> result = new HashMap<String, String[]>();
	private static HashSet<String> regexes = new HashSet<String>();
	private static final String SERVICE_NAME_SEPARATOR = ",";

	@BeforeClass
	public static void setUpBeforeClass() {
		// If Service Display Name input contains comma(s), separate them.
		String[] temp = serviceDisplayNameWithMultipleRegexes.split(SERVICE_NAME_SEPARATOR);
		for (String regex : temp) {
			// Check regex syntax and put it in the HashSet for later use.
			assertFalse(!invokeCheckRegex(regex));
			regexes.add(regex);
		}

		// No need to split by ','.
		assertFalse(!invokeCheckRegex(serviceDisplayNameWithSingleRegex));
		regexes.add(serviceDisplayNameWithSingleRegex);
	}

	@Test
	public void execWmicCommandTest() {
		assertFalse(!invokeExecWmicCommand());
	}

	/**
	 * Invoke private getRemoteConnection method by using Java Reflection.
	 * 
	 * @return True if executing the wmic command is successful, false otherwise.
	 */
	private boolean invokeExecWmicCommand() {
		boolean gotResult = false;
		try {
			Method method = UptimeMonitorWindowsServiceCheckAdvanced.class.getDeclaredMethod("execWmicCommand",
					new Class[] { HashMap.class, ArrayList.class, HashSet.class });
			method.setAccessible(true);
			gotResult = (boolean) method.invoke(UptimeMonitorWindowsServiceCheckAdvanced.class.newInstance(),
					new Object[] { result, whichWmicCommands(), regexes });
		} catch (NoSuchMethodException | SecurityException | IllegalArgumentException | IllegalAccessException
				| InvocationTargetException | InstantiationException e) {
			System.err.println(e);
		}

		// just testing
		// for (String key : result.keySet()) {
		// System.out.println(key + " : " + result.get(key).toString());
		// }

		return gotResult;
	}

	/**
	 * Invoke private checkRegex method by using Java Reflection.
	 * 
	 * @return True if the given regex is valid, false otherwise.
	 */
	private static boolean invokeCheckRegex(String regex) {
		boolean regexValid = false;
		try {
			Method method = UptimeMonitorWindowsServiceCheckAdvanced.class.getDeclaredMethod("checkRegex",
					new Class[] { String.class });
			method.setAccessible(true);
			regexValid = (boolean) method.invoke(UptimeMonitorWindowsServiceCheckAdvanced.class.newInstance(),
					new Object[] { regex });
		} catch (NoSuchMethodException | SecurityException | IllegalArgumentException | IllegalAccessException
				| InvocationTargetException | InstantiationException e) {
			System.err.println(e);
		}
		return regexValid;
	}

	/**
	 * Decide which version of args to use.
	 * 
	 * @return Win / Linux wmic command.
	 */
	private ArrayList<String> whichWmicCommands() {
		ArrayList<String> args = new ArrayList<String>();
		if (SystemUtils.IS_OS_WINDOWS) {
			if (hostname.equals("localhost")) {
				args.add("wmic");
				args.add("/node:\"" + hostname + "\"");
				args.add("service");
				args.add("GET");
				args.add("Caption,Name,StartMode,State");
			} else {
				args.add("wmic");
				args.add("/node:\"" + hostname + "\"");
				args.add("/user:" + adminName);
				args.add("/password:" + password);
				args.add("Service");
				args.add("GET");
				args.add("Caption,Name,StartMode,State");
			}
		} else {
			args.add("wmic");
			args.add("-U");
			args.add(adminName + "%" + password);
			args.add("//" + hostname);
			args.add("select Caption,Name,StartMode,State from Win32_Service");
		}
		return args;
	}

}
