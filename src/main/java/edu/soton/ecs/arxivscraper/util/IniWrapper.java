package edu.soton.ecs.arxivscraper.util;

import org.ini4j.Config;
import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;

public class IniWrapper {

    private static Wini ini = new Wini();

    public static void load(String file) throws IOException {
        load(new File(file));
    }

    public static void load(File file) throws IOException {
        Config conf = new Config();
        conf.setMultiOption(true);
        conf.setMultiSection(true);
        ini.setConfig(conf);
        ini.load(file);
    }

    public static boolean getBoolean(String section, String option)
            throws Exception {
        String result = getString(section, option);
        if (result.equalsIgnoreCase("true")) {
            return true;
        } else if (result.equalsIgnoreCase("false")) {
            return false;
        }
        throw new Exception("Configuration[" + section + ","
                + "] is not a Boolean");
    }

    public static boolean optBoolean(String section, String option) {
        return optBoolean(section, option, false);
    }

    public static boolean optBoolean(String section, String option,
                                     boolean defaultValue) {
        try {
            return getBoolean(section, option);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static String getString(String section, String option)
            throws Exception {
        String result = ini.get(section, option);
        if (result != null) {
            return result;
        }
        throw new Exception("Configuration[" + section + ","
                + "] is not a String");
    }

    public static String optString(String section, String option) {
        return optString(section, option, "");
    }

    public static String optString(String section, String option,
                                   String defaultValue) {
        try {
            return getString(section, option);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static int getInt(String section, String option) throws Exception {
        String result = getString(section, option);
        try {
            return Integer.parseInt(result);
        } catch (NumberFormatException e) {
            throw new Exception("Configuration[" + section + ","
                    + "] is not an Integer");
        }
    }

    public static int optInt(String section, String option) {
        return optInt(section, option, 0);
    }

    public static int optInt(String section, String option, int defaultValue) {
        try {
            return getInt(section, option);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static long getLong(String section, String option) throws Exception {
        String result = getString(section, option);
        try {
            return Long.parseLong(result);
        } catch (NumberFormatException e) {
            throw new Exception("Configuration[" + section + ","
                    + "] is not a Long");
        }
    }

    public static long optLong(String section, String option) {
        return optLong(section, option, 0L);
    }

    public static long optLong(String section, String option, long defaultValue) {
        try {
            return getLong(section, option);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
