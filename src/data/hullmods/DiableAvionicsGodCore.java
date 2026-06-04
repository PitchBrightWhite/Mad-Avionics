package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

//import static data.scripts.util.Diableavionics_stringsManager.txt;

// Abominal intelligence used in the creation of this code teehee :p
public class DiableAvionicsGodCore extends BaseHullMod {

    // --- Sensor / Profile bonus ---
    public static final float SENSOR_PROFILE_BONUS = 100f;   // flat units added to profile & sensor strength

    // --- Sight radius bonus ---
    public static final float SIGHT_RADIUS_BONUS = 2000f;    // flat units added to combat sight radius

    // --- Non-missile, non-PD weapon bonuses ---
    public static final float WEAPON_HEALTH_BONUS_PERCENT = 50f;   // % more weapon health
    public static final float WEAPON_RANGE_BONUS_PERCENT  = 30f;   // % more range for non-missile, non-PD

    // --- PD weapon range bonus ---
    public static final float PD_RANGE_BONUS_PERCENT = 10f;        // % more range for PD weapons

    // --- Zero flux threshold
    public static final float THRESHOLD = 0.05f; // 5%

    // Tooltip highlight color — matches Starsector's standard stat highlight
    private static final Color HIGHLIGHT = new Color(255, 210, 0, 255);

    private static final Set<String> BLOCKED_FRONT = new HashSet<>();

    static {
        /* Prohibit certain shield mods */
        BLOCKED_FRONT.add("shield_shunt");
        BLOCKED_FRONT.add("frontshield");
        BLOCKED_FRONT.add("adaptiveshields");
    }

    private static final Set<String> FRONT_MODULE_IDS = new HashSet<>();
    static {
        FRONT_MODULE_IDS.add("diableavionics_godhead_l_f");
        FRONT_MODULE_IDS.add("diableavionics_godhead_r_f");
        FRONT_MODULE_IDS.add("diableavionics_godhead_c");
    }

    //Shield shrink vars
    public static final float SHORTER_SHIELD_RADIUS = 0.6f;
    public static final float SHORTER_SHIELD_ARC = 180F;
    public static final float SHRINK_TIME_SECONDS = 3f;

    // Derived decay constant: k = -ln(0.05) / time  →  ~3 / time
    private static final float DECAY_K = (float)(-Math.log(0.05) / SHRINK_TIME_SECONDS);

    // Tracks ships whose front modules are all destroyed
    private final Set<ShipAPI> triggered = new HashSet<>();

    // Tracks the original shield radius per ship before shrinking begins
    private final Map<ShipAPI, Float> originalRadius = new HashMap<>();
    // Tracks the original shield arc per ship before shrinking begins
    private final Map<ShipAPI, Float> originalArc = new HashMap<>();

    // Tracks elapsed time since trigger per ship
    private final Map<ShipAPI, Float> elapsed = new HashMap<>();
    // Tracks whether arc bonus has been applied
    private final Set<ShipAPI> arcApplied = new HashSet<>();

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!ship.isAlive() || ship.getShield() == null) return;

        if (!triggered.contains(ship)) {
            boolean allDestroyed = true;
            for (ShipAPI module : ship.getChildModulesCopy()) {
                String hullId = module.getHullSpec().getHullId();
                if (FRONT_MODULE_IDS.contains(hullId) && module.isAlive()) {
                    allDestroyed = false;
                    break;
                }
            }

            if (allDestroyed) {
                triggered.add(ship);
                originalRadius.put(ship, ship.getShield().getRadius());
                originalArc.put(ship, ship.getShield().getArc());
                elapsed.put(ship, 0f);
            }
        }

        if (triggered.contains(ship)) {
            // Apply arc bonus once immediately on trigger
            if (!arcApplied.contains(ship)) {
                float newArc = 170f;
                ship.getShield().setArc(newArc);
                arcApplied.add(ship);
            }

            // Advance elapsed time
            float t = elapsed.get(ship) + amount;
            elapsed.put(ship, t);

            // Exponential interpolation toward target radius
            // r(t) = target + (original - target) * e^(-k*t)
            float baseRadius = originalRadius.get(ship); //~468
            float targetRadius = 261;
            float currentRadius = targetRadius + (baseRadius - targetRadius) * (float) Math.exp(-DECAY_K * t);

            float baseArc = originalArc.get(ship);
            float targetArc = 170f;
            float currentArc = targetArc + (baseArc - targetArc) * (float) Math.exp(-DECAY_K * t);

            ship.getShield().setRadius(currentRadius);
            ship.getShield().setArc(currentArc);
        }
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return ship.getHullSpec().getHullId().startsWith("diableavionics_godhead");
    }

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {

        // Sensor profile increase (makes ship easier to detect)
        stats.getSensorProfile().modifyFlat(id, SENSOR_PROFILE_BONUS);

        // Sensor strength increase (makes ship better at detecting others)
        stats.getSensorStrength().modifyFlat(id, SENSOR_PROFILE_BONUS);

        // Sight radius in combat
        stats.getSightRadiusMod().modifyFlat(id, SIGHT_RADIUS_BONUS);

        // Weapon health for all non-missile weapons
        stats.getWeaponHealthBonus().modifyPercent(id, WEAPON_HEALTH_BONUS_PERCENT);
        stats.getBallisticWeaponRangeBonus().modifyPercent(id, WEAPON_RANGE_BONUS_PERCENT);
        stats.getEnergyWeaponRangeBonus().modifyPercent(id, WEAPON_RANGE_BONUS_PERCENT);

        stats.getNonBeamPDWeaponRangeBonus().modifyPercent(id, PD_RANGE_BONUS_PERCENT);
        stats.getBeamPDWeaponRangeBonus().modifyPercent(id, PD_RANGE_BONUS_PERCENT);
        stats.getZeroFluxMinimumFluxLevel().modifyFlat(id, THRESHOLD);
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        // These map to the %s placeholders in hullmod.csv description, in order:
        // 0: sensor/profile bonus
        // 1: sight radius bonus
        // 2: weapon health bonus
        // 3: non-PD weapon range bonus
        // 4: PD weapon range bonus
        switch (index) {
            case 0:
                return "" + (int) SENSOR_PROFILE_BONUS;
            case 1:
                return "" + (int) SIGHT_RADIUS_BONUS;
            case 2:
                return "" + (int) WEAPON_HEALTH_BONUS_PERCENT + "%";
            case 3:
                return "" + (int) WEAPON_RANGE_BONUS_PERCENT + "%";
            case 4:
                return "" + (int) PD_RANGE_BONUS_PERCENT + "%";
            case 5:
                return "" + (int) THRESHOLD + "%";
            default:
                return null;
        }
    }
}
