package com.prupe.mcpatcher.launcher.version;

class Rule {
    private String action;
    private OS os;

    boolean evaluate(boolean allow) {
        if (!match()) {
            return allow;
        } else if ("allow".equals(action)) {
            return true;
        } else if ("disallow".equals(action)) {
            return false;
        } else {
            return allow;
        }
    }

    private boolean match() {
        return os == null || os.match();
    }
}
