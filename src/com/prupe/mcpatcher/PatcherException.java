package com.prupe.mcpatcher;

import java.io.File;
import java.net.URL;

abstract public class PatcherException extends Exception {
    static final String FORUM_URL = "http://www.minecraftforum.net/topic/1496369-";
    static final String LAUNCHER_URL = "https://mojang.com/2013/07/minecraft-1-6-2-pre-release/";
    static final String OLD_PATCHER_URL = "http://bitbucket.org/prupe/mcpatcher/downloads/";

    PatcherException() {
    }

    PatcherException(Throwable cause) {
        super(cause);
    }

    String getMessageBoxTitle() {
        return "Error";
    }

    abstract String getMessageBoxText();

    static class InstallationNotFound extends PatcherException {
        private final File dir;

        InstallationNotFound(File dir) {
            this.dir = dir;
        }

        @Override
        String getMessageBoxTitle() {
            return "Minecraft not found";
        }

        @Override
        String getMessageBoxText() {
            return String.format(
                "Minecraft installation not found in\n" +
                    "%1$s\n\n" +
                    "This version of MCPatcher supports the new Minecraft launcher only.\n" +
                    "You must run the game from the new Minecraft launcher at least once before\n" +
                    "starting MCPatcher.  The new launcher can be downloaded from Mojang at\n" +
                    "%2$s\n\n" +
                    "For the old launcher, use MCPatcher 3.x (for Minecraft 1.5.2) or MCPatcher 2.x\n" +
                    "(for Minecraft 1.4.7 and earlier) available from\n" +
                    "%3$s\n\n" +
                    "If the game is installed somewhere else, please select the game folder now.\n" +
                    "The game folder is the one containing the assets, libraries, and versions subfolders.",
                dir.getPath(),
                LAUNCHER_URL,
                OLD_PATCHER_URL
            );
        }
    }

    static class CorruptJarFile extends PatcherException {
        private final File jar;

        CorruptJarFile(File jar) {
            this.jar = jar;
        }

        @Override
        String getMessageBoxTitle() {
            return "Invalid or Corrupt minecraft jar";
        }

        @Override
        String getMessageBoxText() {
            return String.format(
                "There was an error opening %1$s. This may be because:\n" +
                    " - The file has already been patched.\n" +
                    " - There was an update that this patcher cannot handle.\n" +
                    " - There is another, conflicting mod applied.\n" +
                    " - The jar file is invalid or corrupt.\n\n" +
                    "You can re-download the original %1$s using the Minecraft Launcher from\n" +
                    "%2$s",
                jar.getName(),
                LAUNCHER_URL
            );
        }
    }

    static class DownloadException extends PatcherException {
        private final URL url;
        private final File local;

        DownloadException(Throwable cause, URL url, File local) {
            super(cause);
            this.url = url;
            this.local = local;
        }

        DownloadException(URL url, File local) {
            this.url = url;
            this.local = local;
        }

        @Override
        String getMessageBoxTitle() {
            return "Download error";
        }

        @Override
        String getMessageBoxText() {
            return String.format(
                "There was an error downloading\n" +
                    "%1$s\n\n" +
                    "Check your system's proxy server settings and try running MCPatcher again.\n\n" +
                    "You may also try downloading the file manually and saving it to\n" +
                    "%2$s",
                url,
                local
            );
        }
    }

    static class PatchError extends PatcherException {
        PatchError(Throwable cause) {
            super(cause);
        }

        @Override
        String getMessageBoxText() {
            return "There was an error during patching.  See log for more information.\n\n" +
                "Your original minecraft jar has been restored.";
        }
    }
}
