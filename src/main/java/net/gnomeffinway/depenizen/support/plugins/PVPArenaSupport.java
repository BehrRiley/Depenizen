package net.gnomeffinway.depenizen.support.plugins;

import net.aufdemrand.denizen.objects.dPlayer;
import net.aufdemrand.denizencore.tags.Attribute;
import net.gnomeffinway.depenizen.events.PVPArena.PVPArenaStartsScriptEvent;
import net.gnomeffinway.depenizen.extensions.pvparena.PVPArenaPlayerExtension;
import net.gnomeffinway.depenizen.objects.pvparena.PVPArenaArena;
import net.gnomeffinway.depenizen.support.Support;

public class PVPArenaSupport extends Support {

    public PVPArenaSupport() {
        registerScriptEvents(new PVPArenaStartsScriptEvent());
        registerProperty(PVPArenaPlayerExtension.class, dPlayer.class);
        registerObjects(PVPArenaArena.class);
        registerAdditionalTags("pvparena");
    }

    @Override
    public String additionalTags(Attribute attribute) {

        if (attribute.startsWith("pvparena") && attribute.hasContext(1)) {
            PVPArenaArena arena = PVPArenaArena.valueOf(attribute.getContext(1));
            if (arena == null) {
                return null;
            }
            return arena.getAttribute(attribute.fulfill(1));
        }

        return null;

    }
}
