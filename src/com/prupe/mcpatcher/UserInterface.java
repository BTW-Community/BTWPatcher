package com.prupe.mcpatcher;

import java.io.File;
import java.util.ArrayList;

abstract public class UserInterface {
    abstract boolean shouldExit();

    void show() {
    }

    File chooseMinecraftDir(File enteredMCDir) {
        return null;
    }

    boolean locateMinecraftDir(String enteredMCDir) {
        ArrayList<File> mcDirs = new ArrayList<File>();
        if (enteredMCDir == null) {
            mcDirs.add(MCPatcherUtils.getDefaultGameDir());
            mcDirs.add(new File("."));
            mcDirs.add(new File(".."));
        } else {
            mcDirs.add(new File(enteredMCDir).getAbsoluteFile());
        }

        for (File dir : mcDirs) {
            if (MCPatcherUtils.setGameDir(dir)) {
                return true;
            }
        }

        File minecraftDir = mcDirs.get(0);
        while (true) {
            minecraftDir = chooseMinecraftDir(minecraftDir);
            if (minecraftDir == null) {
                return false;
            }
            if (MCPatcherUtils.setGameDir(minecraftDir) ||
                MCPatcherUtils.setGameDir(minecraftDir.getParentFile())) {
                return true;
            }
        }
    }

    abstract boolean go(ProfileManager profileManager);

    public void updateProgress(int value, int max) {
    }

    public void setStatusText(String format, Object... params) {
    }

    void showBetaWarning() {
    }

    static class CLI extends UserInterface {
        CLI() {
            Config.setReadOnly(true);
            Config.getInstance().selectPatchedProfile = false;
        }

        @Override
        boolean shouldExit() {
            return true;
        }

        @Override
        boolean go(ProfileManager profileManager) {
            boolean ok = false;
            try {
                profileManager.refresh(this);
                MCPatcher.refreshMinecraftPath();
                MCPatcher.refreshModList();
                MCPatcher.checkModApplicability();
                System.out.println();
                System.out.println("#### Class map:");
                MCPatcher.showClassMaps(System.out, false);
                MCPatcher.patch();
                System.out.println();
                System.out.println("#### Patch summary:");
                MCPatcher.showPatchResults(System.out);
                ok = true;
            } catch (Throwable e) {
                Logger.log(e);
            }
            return ok;
        }
    }
}
