package com.denizenscript.depenizen.bukkit.events.pvparena;

import com.denizenscript.depenizen.bukkit.objects.pvparena.PVPArenaArenaTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import net.slipcor.pvparena.events.PALeaveEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PlayerLeavesPVPArenaScriptEvent extends BukkitScriptEvent implements Listener {

    // <--[event]
    // @Events
    // pvparena player leaves
    //
    // @Regex ^on pvparena player leaves$
    //
    // @Triggers when a player leaves a pvparena.
    //
    // @Context
    // <context.arena> returns the arena denizen object.
    //
    // @Plugin Depenizen, PVPArena
    //
    // @Player Always.
    //
    // @Group Depenizen
    //
    // -->

    public static PlayerLeavesPVPArenaScriptEvent instance;
    public PALeaveEvent event;
    public PVPArenaArenaTag arena;

    public PlayerLeavesPVPArenaScriptEvent() {
        instance = this;
    }

    @Override
    public boolean couldMatch(ScriptPath path) {
        return path.eventLower.startsWith("pvparena player leaves");
    }

    @Override
    public String getName() {
        return "PlayerLeavesPVPArena";
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(new PlayerTag(event.getPlayer()), null);
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("arena")) {
            return arena;
        }
        return super.getContext(name);
    }

    @EventHandler
    public void onPlayerLeavesPVPArena(PALeaveEvent event) {
        arena = new PVPArenaArenaTag(event.getArena());
        cancelled = false;
        this.event = event;
        fire(event);
    }
}
