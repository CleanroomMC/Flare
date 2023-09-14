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

        for (String line : WindowsWmic.OS_GET_CAPTION_AND_VERSION.read()) {
            if (line.startsWith("Caption") && line.length() > 18) {
                // Caption=Microsoft Windows something
                // \----------------/ = 18 chars
                name = line.substring(18).trim();
            } else if (line.startsWith("Version")) {
                // Version=10.0.something
                // \------/ = 8 chars
                version = line.substring(8).trim();
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
