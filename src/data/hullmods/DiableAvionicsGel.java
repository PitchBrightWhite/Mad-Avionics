package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
// Abominal intelligence used in the creation of this code teehee :p
    public class DiableAvionicsGel extends BaseHullMod {

        // Max hull points regenerated per second, applied at 100% missing hull.
        // Actual regen = MAX_REGEN_RATE * (missingHullFraction)
        public static final float MAX_REGEN_RATE = 80f;
        public static final float SMOD_MIN_REGEN = 10f;

        @Override
        public void advanceInCombat(ShipAPI ship, float amount) {
            if (!ship.isAlive()) return; // Cant regen if dead kek

            float maxHull = ship.getMaxHitpoints();
            float currentHull = ship.getHitpoints();
            float missingHullFraction = 1f - (currentHull / maxHull);

            if (missingHullFraction <= 0f) return;

            float regenAmnt = MAX_REGEN_RATE * missingHullFraction;

            float minRegen = 10f;

            if (isSMod(ship.getMutableStats())) minRegen = SMOD_MIN_REGEN;
            if (regenAmnt < minRegen) regenAmnt = minRegen; // Regens atleast 10hp/s. 30hp/s if smodded

            float regenThisTick = regenAmnt * amount; // Amount compensates for framerate

            // Clamp so we don't overheal
            float newHull = Math.min(currentHull + regenThisTick, maxHull);
            ship.setHitpoints(newHull);
        }

        @Override
        public String getSModDescriptionParam(int index, HullSize hullSize) {
            if (index == 0) return String.format("%.0f", SMOD_MIN_REGEN);
            return null;
        }

        @Override
        public String getDescriptionParam(int index, HullSize hullSize) {
            if (index == 0) return "10%";
            if (index == 1) return String.format("%.0f", MAX_REGEN_RATE * 0.9f);
            if (index == 2) return "80%";
            if (index == 3) return String.format("%.0f", MAX_REGEN_RATE * 0.2f);
            if (index == 4) return String.format("%.0f", 10f);
            return null;
        }

        @Override
        public boolean isApplicableToShip(ShipAPI ship) {
            return true;
        }
    }



