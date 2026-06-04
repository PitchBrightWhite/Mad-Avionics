package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.*;

import java.util.List;
import java.awt.Color;

//import static data.scripts.util.Diableavionics_stringsManager.txt;

// Abominal intelligence used in the creation of this code teehee :p
public class DiableAvionicsJunk extends BaseHullMod {

    private static final String ID_PARENT      = "diableavionics_godhead";

    /** Hull (variant) IDs of the five modules. */
    private static final String ID_CENTER      = "diableavionics_godhead_c";
    private static final String ID_LEFT        = "diableavionics_godhead_l_f";
    private static final String ID_RIGHT       = "diableavionics_godhead_r_f";
    private static final String ID_UNDER_LEFT  = "diableavionics_godhead_base_l";
    private static final String ID_UNDER_RIGHT = "diableavionics_godhead_base_r";

    @Override
    public void advanceInCombat(ShipAPI self, float amount) {

        self.setSpawnDebris(false);
        if (!self.isAlive()) return;            // already gone – nothing to do
        if (self.getParentStation() == null) {
            destroySelf(self);
            return;
        }

        ShipAPI parent = self.getParentStation();

        // --- 1. Parent destruction triggers both under-modules ---------------
        if (parent.getHitpoints() < 1000) {
            destroySelf(self);
            return;
        }

        // --- 2. Identify which under-module we are and act accordingly -------
        String myId = self.getHullSpec().getHullId();

        if (myId.equals(ID_UNDER_LEFT)) {
            if (isModuleDeadOrGone(parent, ID_LEFT) && isModuleDeadOrGone(parent, ID_CENTER)) {
                destroySelf(self);
            }
        } else if (myId.equals(ID_UNDER_RIGHT)) {
            if (isModuleDeadOrGone(parent, ID_RIGHT) && isModuleDeadOrGone(parent, ID_CENTER)) {
                destroySelf(self);
            }
        }
        // If this hullmod were somehow placed on another module, nothing happens.
    }

    private boolean isModuleDeadOrGone(ShipAPI parent, String moduleHullId) {
        List<ShipAPI> modules = parent.getChildModulesCopy();
        for (ShipAPI module : modules) {
            if (module.getHullSpec().getHullId().equals(moduleHullId)) {
                // Found it – is it alive?
                return !module.isAlive();
            }
        }
        // Module not found in the list at all → treat as gone
        return true;
    }

    /**
     * "Disappear" this module by destroying it instantly with zero explosion.
     * Starsector has no built-in "remove silently" API for modules, so we use
     * the lowest-drama destruction available: zero-radius, no fx.
     *
     * If you prefer a visual effect swap out the applyDamage call for whatever
     * suits your ship's aesthetic.
     */
    private void destroySelf(ShipAPI self) {
        Global.getCombatEngine().removeEntity(self);
        // Apply enough damage to destroy the module cleanly.
        // Using a very high number guarantees destruction regardless of hull points
        // Alternatively, for a more explicit destroy with no explosion:
        //   // (just silences engines)
        // The engine will register the ship as destroyed on next frame.
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        return "Internual logic mod, If you are reading please submit some kind of bug report";
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getHullDamageTakenMult().modifyMult(id, 0f);
        stats.getEngineDamageTakenMult().modifyMult(id, 0f);
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        return null;
    }
}
