package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.scripts.util.Diableavionics_stringsManager;
import data.scripts.util.MagicAnim;
import java.awt.Color;
import java.util.List;

public class Diableavionics_godhead_drift extends BaseShipSystemScript {

    private final Integer TURN_ACC_BUFF = 500;
    private final Integer TURN_RATE_BUFF = 250;
    private final Integer ACCEL_BUFF = 250;
    private final Integer DECCEL_BUFF = 150;
    private final Integer SPEED_BUFF = 100;
    private final Integer TIME_BUFF = 500;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        float effect = Math.min(1, Math.max(0,
                MagicAnim.smoothReturnNormalizeRange(effectLevel, 0, 1) / 2
                        + MagicAnim.smoothReturnNormalizeRange(effectLevel * 1.5f, 0, 1) / 2
                        + MagicAnim.smoothReturnNormalizeRange(effectLevel * 2, 0, 1) / 2));

        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship != null) {
            // Apply visuals and phasing to parent
            applyVisuals(ship, id, effect);

            // Apply visuals and stats to each child module
            List<ShipAPI> modules = ship.getChildModulesCopy();
            if (modules != null) {
                for (ShipAPI module : modules) {
                    if(!module.getHullSpec().getHullId().equals("diableavionics_godhead_base_l") && !module.getHullSpec().getHullId().equals("diableavionics_godhead_base_r")) { //Don't animate under layers - Weird glitch occurs that prevents them from de-spawning
                        applyVisuals(module, id, effect);
                        applyStats(module.getMutableStats(), id, effect);
                    }
                }
            }
        }

        // Apply stats to parent
        applyStats(stats, id, effect);
    }

    private void applyVisuals(ShipAPI ship, String id, float effect) {
        ship.setJitterUnder(
                ship,
                Color.CYAN,
                0.5f * effect,
                5,
                5 + 5f * effect,
                5 + 10f * effect
        );
        if (Math.random() > 0.9f) {
            ship.addAfterimage(
                    new Color(0, 200, 255, 64),
                    0, 0,
                    -ship.getVelocity().x, -ship.getVelocity().y,
                    5 + 50 * effect, 0, 0,
                    2 * effect,
                    false, false, false
            );
        }

        if (!ship.getMutableStats().getTimeMult().getPercentMods().containsKey(id)) {
            // Only play sound once from the parent — handled in apply() directly
            Global.getSoundPlayer().playSound("diableavionics_drift", 1, 1.66f, ship.getLocation(), ship.getVelocity());
            ship.setPhased(true);
        } else if (ship.isPhased()) {
            ship.setPhased(false);
        }
    }

    private void applyStats(MutableShipStatsAPI stats, String id, float effect) {
        stats.getTurnAcceleration().modifyPercent(id, TURN_ACC_BUFF * effect);
        stats.getMaxTurnRate().modifyPercent(id, TURN_RATE_BUFF * effect);
        stats.getMaxSpeed().modifyPercent(id, SPEED_BUFF * effect);
        stats.getAcceleration().modifyPercent(id, ACCEL_BUFF);
        stats.getDeceleration().modifyPercent(id, DECCEL_BUFF);
        stats.getTimeMult().modifyPercent(id, TIME_BUFF * effect);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();

        // Unapply from parent
        unapplyStats(stats, id);

        // Unapply from all child modules
        if (ship != null) {
            List<ShipAPI> modules = ship.getChildModulesCopy();
            if (modules != null) {
                for (ShipAPI module : modules) {
                    unapplyStats(module.getMutableStats(), id);
                    if (module.isPhased()) {
                        module.setPhased(false);
                    }
                }
            }
        }
    }

    private void unapplyStats(MutableShipStatsAPI stats, String id) {
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getTimeMult().unmodify(id);
    }

    private final String TXT = Diableavionics_stringsManager.txt("drift");

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData(TXT, false);
        }
        return null;
    }
}