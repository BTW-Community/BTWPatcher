package com.prupe.mcpatcher.cit;

import java.util.Properties;

class ArmorOverride extends OverrideBase {
    ArmorOverride(String propertiesName, Properties properties) {
        super(propertiesName, properties);
    }

    @Override
    String getType() {
        return "armor";
    }
}
