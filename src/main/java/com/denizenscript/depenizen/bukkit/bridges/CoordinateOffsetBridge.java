package com.denizenscript.depenizen.bukkit.bridges;

import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizencore.objects.properties.PropertyParser;
import com.denizenscript.depenizen.bukkit.Bridge;
import com.denizenscript.depenizen.bukkit.properties.coordinateoffset.CoordinateOffsetPlayerProperties;

public class CoordinateOffsetBridge extends Bridge {

    @Override
    public void init() {
        PropertyParser.registerProperty(CoordinateOffsetPlayerProperties.class, PlayerTag.class);
    }
}
