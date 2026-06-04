package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.util.MagicIncompatibleHullmods;

import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.awt.Color;

//import static data.scripts.util.Diableavionics_stringsManager.txt;

// Abominal intelligence used in the creation of this code teehee :p
// Frankensteined Code from Scalar tech gownmodule.java

public class DiableAvionicsGodMod extends BaseHullMod {

    // --- Non-missile, non-PD weapon bonuses ---
    public static final float WEAPON_HEALTH_BONUS_PERCENT = 50f;   // % more weapon health
    public static final float WEAPON_RANGE_BONUS_PERCENT  = 30f;   // % more range for non-missile, non-PD

    // --- PD weapon range bonus ---
    public static final float PD_RANGE_BONUS_PERCENT = 10f;        // % more range for PD weapons

    // Tooltip highlight color — matches Starsector's standard stat highlight
    private static final Color HIGHLIGHT = new Color(255, 210, 0, 255);
    public static final float THRESHOLD = 0.05f; // 5%


    private static void advanceChild(ShipAPI child, ShipAPI parent) {
        ShipEngineControllerAPI ec = parent.getEngineController();
        if (ec != null) {
            if (parent.isAlive()) {
                if (ec.isAccelerating()) {
                    child.giveCommand(ShipCommand.ACCELERATE, null, 0);
                }
                if (ec.isAcceleratingBackwards()) {
                    child.giveCommand(ShipCommand.ACCELERATE_BACKWARDS, null, 0);
                }
                if (ec.isDecelerating()) {
                    child.giveCommand(ShipCommand.DECELERATE, null, 0);
                }
                if (ec.isStrafingLeft()) {
                    child.giveCommand(ShipCommand.STRAFE_LEFT, null, 0);
                }
                if (ec.isStrafingRight()) {
                    child.giveCommand(ShipCommand.STRAFE_RIGHT, null, 0);
                }
                if (ec.isTurningLeft()) {
                    child.giveCommand(ShipCommand.TURN_LEFT, null, 0);
                }
                if (ec.isTurningRight()) {
                    child.giveCommand(ShipCommand.TURN_RIGHT, null, 0);
                }
            }

            ShipEngineControllerAPI cec = child.getEngineController();
            if (cec != null) {
                if ((ec.isFlamingOut() || ec.isFlamedOut()) && !cec.isFlamingOut() && !cec.isFlamedOut()) {
                    child.getEngineController().forceFlameout(true);
                }
            }
        }

        if (parent.getVariant().hasHullMod("unstableinjector")) {
            child.getMutableStats().getBallisticWeaponRangeBonus().modifyMult("diableavionics_godhead_module", 0.85f);
            child.getMutableStats().getEnergyWeaponRangeBonus().modifyMult("diableavionics_godhead_module", 0.85f);
            child.getMutableStats().getFighterRefitTimeMult().modifyPercent("diableavionics_godhead_module", 25f);
        } else {
            child.getMutableStats().getBallisticWeaponRangeBonus().unmodify("diableavionics_godhead_module");
            child.getMutableStats().getEnergyWeaponRangeBonus().unmodify("diableavionics_godhead_module");
            child.getMutableStats().getFighterRefitTimeMult().unmodify("diableavionics_godhead_module");
        }

        /* Mirror Parent's targeting */
        if (parent.getShipTarget() != null) {
            child.setShipTarget(parent.getShipTarget());

        }

        /* Mirror parent's fighter commands */
        if (child.hasLaunchBays()) {
            if (child.isPullBackFighters() ^ parent.isPullBackFighters()) {
                child.giveCommand(ShipCommand.PULL_BACK_FIGHTERS, null, 0);
            }
            if (child.getAIFlags() != null) {
                if (((Global.getCombatEngine().getPlayerShip() == parent) || (parent.getAIFlags() == null))
                        && (parent.getShipTarget() != null)) {
                    child.getAIFlags().setFlag(AIFlags.CARRIER_FIGHTER_TARGET, 1f, parent.getShipTarget());
                } else if ((parent.getAIFlags() != null)
                        && parent.getAIFlags().hasFlag(AIFlags.CARRIER_FIGHTER_TARGET)
                        && (parent.getAIFlags().getCustom(AIFlags.CARRIER_FIGHTER_TARGET) != null)) {
                    child.getAIFlags().setFlag(AIFlags.CARRIER_FIGHTER_TARGET, 1f, parent.getAIFlags().getCustom(AIFlags.CARRIER_FIGHTER_TARGET));
                }
            }
        }

        //Fucky 0-flux boost mirroring that mostly works
        if (parent.getFluxLevel() > parent.getMutableStats().getZeroFluxMinimumFluxLevel().getModifiedValue()) {
            child.getMutableStats().getZeroFluxMinimumFluxLevel().modifyFlat("zerofluxmirror", -2f);
        } else {
            child.getMutableStats().getZeroFluxMinimumFluxLevel().modifyFlat("zerofluxmirror", 2f);
        }
    }


    private static void advanceParent(ShipAPI parent, List<ShipAPI> children) {
        ShipEngineControllerAPI ec = parent.getEngineController();
        if (ec != null && !ec.getShipEngines().isEmpty())  {
            float originalMass;
            int originalEngines;
            switch (parent.getHullSpec().getBaseHullId()) {
                default:
                case "diableavionics_godhead.ship":
                    originalMass = 3500f + (2f*3000f) + 2000f + (2f*2500f); //
                    originalEngines = 8;
                    break;
            }
            float thrustPerEngine = originalMass / originalEngines;

            /* Don't count parent's engines for this stuff - game already affects stats */
            float workingEngines = ec.getShipEngines().size();
            for (ShipAPI child : children) {
                if ((child.getParentStation() == parent) && (child.getStationSlot() != null) && child.isAlive()) {
                    ShipEngineControllerAPI cec = child.getEngineController();
                    if (cec != null) {
                        float contribution = 0f;
                        for (ShipEngineAPI ce : cec.getShipEngines()) {
                            if (ce.isActive() && !ce.isDisabled() && !ce.isPermanentlyDisabled() && !ce.isSystemActivated()) {
                                contribution += ce.getContribution();
                            }
                        }
                        workingEngines += cec.getShipEngines().size() * contribution;
                    }
                }
            }

            // Increases manuverability based on working engines
            float thrust = workingEngines * thrustPerEngine;
            float enginePerformance = thrust / Math.max(1f, parent.getMassWithModules());

            parent.getMutableStats().getAcceleration().modifyMult("godhead_module", enginePerformance);
            parent.getMutableStats().getDeceleration().modifyMult("godhead_module", enginePerformance);
            parent.getMutableStats().getTurnAcceleration().modifyMult("godhead_module", enginePerformance);
            parent.getMutableStats().getMaxTurnRate().modifyMult("godhead_module", enginePerformance);
            parent.getMutableStats().getMaxSpeed().modifyMult("godhead_module", enginePerformance);
            parent.getMutableStats().getZeroFluxSpeedBoost().modifyMult("godhead_module", enginePerformance);

        }
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        ShipAPI parent = ship.getParentStation();
        if (parent != null) {
            advanceChild(ship, parent);
        }

        List<ShipAPI> children = ship.getChildModulesCopy();
        if (children != null && !children.isEmpty()) {
            advanceParent(ship, children);
        }
    }

    private static final Set<String> BLOCKED_FRONT = new HashSet<>();
    private static final Set<String> BLOCKED_OTHER = new HashSet<>();
    private static final Set<String> BLOCKED_OTHER_PLAYER_ONLY = new HashSet<>();

    static {
        /* No shields on my modules */
        BLOCKED_FRONT.add("frontemitter");
        BLOCKED_FRONT.add("frontshield");
        BLOCKED_FRONT.add("adaptiveshields");

        /* Modules don't move on their own */
        BLOCKED_OTHER.add("auxiliarythrusters");
        BLOCKED_OTHER.add("unstable_injector");

        /* Module's can't provide ECM/Nav */
        BLOCKED_OTHER.add("ecm");
        BLOCKED_OTHER.add("nav_relay");

        /* Logistics mods partially or completely don't apply on modules */
        BLOCKED_OTHER.add("operations_center");
        BLOCKED_OTHER.add("recovery_shuttles");
        BLOCKED_OTHER.add("additional_berthing");
        BLOCKED_OTHER.add("augmentedengines");
        BLOCKED_OTHER.add("auxiliary_fuel_tanks");
        BLOCKED_OTHER.add("efficiency_overhaul");
        BLOCKED_OTHER.add("expanded_cargo_holds");
        BLOCKED_OTHER.add("hiressensors");
        //BLOCKED_OTHER.add("insulatedengine"); // Niche use
        BLOCKED_OTHER.add("militarized_subsystems");
        //BLOCKED_OTHER.add("solar_shielding"); // Niche use
        BLOCKED_OTHER.add("surveying_equipment");

        /* Crew penalty doesn't reflect in campaign */
        BLOCKED_OTHER_PLAYER_ONLY.add("converted_hangar");
        //BLOCKED_OTHER_PLAYER_ONLY.add("expanded_deck_crew");
        BLOCKED_OTHER_PLAYER_ONLY.add("TSC_converted_hangar");
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {

        // Weapon health for all non-missile weapons
        stats.getWeaponHealthBonus().modifyPercent(id, WEAPON_HEALTH_BONUS_PERCENT);
        stats.getBallisticWeaponRangeBonus().modifyPercent(id, WEAPON_RANGE_BONUS_PERCENT);
        stats.getEnergyWeaponRangeBonus().modifyPercent(id, WEAPON_RANGE_BONUS_PERCENT);

        stats.getNonBeamPDWeaponRangeBonus().modifyPercent(id, PD_RANGE_BONUS_PERCENT);
        stats.getBeamPDWeaponRangeBonus().modifyPercent(id, PD_RANGE_BONUS_PERCENT);
        stats.getZeroFluxMinimumFluxLevel().modifyFlat(id, THRESHOLD);
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        // These map to the %s placeholders in hullmod.csv description, in order:
        // 0: weapon health bonus
        // 1: non-PD weapon range bonus
        // 2: PD weapon range bonus
        switch (index) {
            case 0:
                return "" + (int) WEAPON_HEALTH_BONUS_PERCENT + "%";
            case 1:
                return "" + (int) WEAPON_RANGE_BONUS_PERCENT + "%";
            case 2:
                return "" + (int) PD_RANGE_BONUS_PERCENT + "%";
            case 3:
                return "" + (int) THRESHOLD + "%";
            default:
                return null;
        }
    }
}
