package com.cleanroommc.flare.common.component.os;

/**
 * Small utility to query the operating system name & version.
 */
public final class OperatingSystemInfo {

    private final String name;
    private final String version;
    private final String arch;

    public OperatingSystemInfo(String name, String version, String arch) {
        this.name = name;
        this.version = version;
        this.arch = arch;
    }

    public String name() {
        return this.name;
    }

    public String version() {
        return this.version;
    }

    public String arch() {
        return this.arch;
    }

    public static OperatingSystemInfo poll() {
        String name = null;
        String version = null;

        for (String line : LinuxProc.OSINFO.read()) {
            if (line.startsWith("PRETTY_NAME") && line.length() > 13) {
                name = line.substring(13).replace('"', ' ').trim();
            }
        }

        String winName = null;
        String winBuild = null;

        for (String line : WindowsReg.OS_GET_INFO.read()) {
            String trimmed = line.trim();

            if (trimmed.startsWith("ProductName")) {
                int index = trimmed.indexOf("REG_SZ");
                if (index != -1) {
                    winName = trimmed.substring(index + 6).trim();
                }
            }
            else if (trimmed.startsWith("CurrentBuild")) {
                int index = trimmed.indexOf("REG_SZ");
                if (index != -1) {
                    winBuild = trimmed.substring(index + 6).trim();
                }
            }

            if (winName != null && winBuild != null) {
                break;
            }
        }

        if (winName != null) {
            if (winBuild != null && !winBuild.isEmpty()) {
                name = winName + " (Build " + winBuild + ")";
            } else {
                name = winName;
            }
        }

        if (name == null) {
            name = System.getProperty("os.name");
        }

        if (version == null) {
            version = System.getProperty("os.version");
        }

        String arch = System.getProperty("os.arch");

        return new OperatingSystemInfo(name, version, arch);
    }

}
