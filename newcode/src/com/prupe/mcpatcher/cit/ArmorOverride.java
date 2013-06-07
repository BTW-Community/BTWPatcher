package com.prupe.mcpatcher.cit;

import java.util.Properties;

final class ArmorOverride extends OverrideBase {
    ArmorOverride(String propertiesName, Properties properties) {
        super(propertiesName, properties);

        if (itemsIDs == null) {
            error("no matching items specified");
        }
    }

    @Override
    String getType() {
        return "armor";
    }
}
