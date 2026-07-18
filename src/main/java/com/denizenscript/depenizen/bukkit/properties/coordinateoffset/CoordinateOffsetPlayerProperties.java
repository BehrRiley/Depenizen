package com.denizenscript.depenizen.bukkit.properties.coordinateoffset;

import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.properties.Property;
import com.denizenscript.denizencore.tags.Attribute;
import com.jtprince.coordinateoffset.FixedOffset;
import com.jtprince.coordinateoffset.Offset;
import com.jtprince.coordinateoffset.api.CoordinateOffset;
import com.jtprince.coordinateoffset.adapter.OffsetPlayer;

public class CoordinateOffsetPlayerProperties implements Property {

    @Override
    public String getPropertyString() {
        return null;
    }

    @Override
    public String getPropertyId() {
        return "CoordinateOffsetPlayer";
    }

    public static boolean describes(ObjectTag object) {
        return object instanceof PlayerTag;
    }

    public static CoordinateOffsetPlayerProperties getFrom(ObjectTag object) {
        if (!describes(object)) {
            return null;
        }
        else {
            return new CoordinateOffsetPlayerProperties((PlayerTag) object);
        }
    }

    public static final String[] handledTags = new String[] {
            "coordinate_offset"
    };

    public static final String[] handledMechs = new String[] {
            "coordinate_offset"
    };

    public CoordinateOffsetPlayerProperties(PlayerTag player) {
        this.player = player;
    }

    PlayerTag player;

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        if (attribute == null) {
            return null;
        }

        // <--[tag]
        // @attribute <PlayerTag.coordinate_offset>
        // @returns LocationTag
        // @mechanism PlayerTag.coordinate_offset
        // @plugin Depenizen, CoordinateOffset
        // @description
        // Returns the player's current coordinate offset as a LocationTag (x, y, z).
        // -->
        if (attribute.startsWith("coordinate_offset") && player.isOnline()) {
            attribute = attribute.fulfill(1);
            OffsetPlayer offsetPlayer = CoordinateOffset.api().adaptPlayer(player.getPlayerEntity());
            FixedOffset offset = CoordinateOffset.api().getOffset(offsetPlayer);
            if (offset.isZero()) {
                return null;
            }
            return new LocationTag(null, offset.x(), offset.y(), offset.z()).getObjectAttribute(attribute);
        }

        return null;
    }

    @Override
    public void adjust(Mechanism mechanism) {
        // <--[mechanism]
        // @object PlayerTag
        // @name coordinate_offset
        // @input LocationTag
        // @plugin Depenizen, CoordinateOffset
        // @description
        // Sets the player's coordinate offset.
        // @tags
        // <PlayerTag.coordinate_offset>
        // -->
        if (mechanism.matches("coordinate_offset") && mechanism.hasValue() && player.isOnline()) {
            LocationTag loc = mechanism.valueAsType(LocationTag.class);
            if (loc != null) {
                int x = (int) Math.round(loc.getX());
                int y = (int) Math.round(loc.getY());
                int z = (int) Math.round(loc.getZ());
                
                // Align using ConstantOffsetProvider logic (multiples of configured blocks)
                int alignedX = Offset.alignComponentToConfiguredMultiple(x);
                int alignedZ = Offset.alignComponentToConfiguredMultiple(z);
                
                OffsetPlayer offsetPlayer = CoordinateOffset.api().adaptPlayer(player.getPlayerEntity());
                CoordinateOffset.api().setOffset(offsetPlayer, Offset.scalable(alignedX, y, alignedZ));
            }
        }
    }
}
