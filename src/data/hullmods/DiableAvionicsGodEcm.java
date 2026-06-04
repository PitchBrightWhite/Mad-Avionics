package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
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

import static com.fs.starfarer.api.impl.campaign.missions.academy.GAPZPostEncounters.id;

//import static data.scripts.util.Diableavionics_stringsManager.txt;

// Abominal intelligence used in the creation of this code teehee :p
// Frankensteined Code from Scalar tech gownmodule.java

public class DiableAvionicsGodEcm extends BaseHullMod {

    // --- PD weapon range bonus ---
    public static final float PD_RANGE_BONUS_PERCENT = 20f;        // % more range for PD weapons

    // Tooltip highlight color — matches Starsector's standard stat highlight
    private static final Color HIGHLIGHT = new Color(255, 210, 0, 255);


    private static void advanceChild(ShipAPI child, ShipAPI parent) {
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

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        ShipAPI parent = ship.getParentStation();
        if (parent != null) {
            advanceChild(ship, parent);

            if (ship.isAlive()) {

                parent.getMutableStats().getDynamic().getMod(Stats.ELECTRONIC_WARFARE_FLAT).modifyFlat("diableavionics_godhead_ecm", 5f);
                parent.getMutableStats().getDynamic().getMod(Stats.COORDINATED_MANEUVERS_FLAT).modifyFlat("diableavionics_godhead_ecm", 30f);
            } else {
                parent.getMutableStats().getDynamic().getMod(Stats.ELECTRONIC_WARFARE_FLAT).unmodify("diableavionics_godhead_ecm");
                parent.getMutableStats().getDynamic().getMod(Stats.COORDINATED_MANEUVERS_FLAT).unmodify("diableavionics_godhead_ecm");
            }
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

        stats.getNonBeamPDWeaponRangeBonus().modifyPercent(id, PD_RANGE_BONUS_PERCENT);
        stats.getBeamPDWeaponRangeBonus().modifyPercent(id, PD_RANGE_BONUS_PERCENT);
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        // These map to the %s placeholders in hullmod.csv description, in order:
        // 0: Nav rating
        // 1: Ecm Rating
        // 2: PD weapon range bonus
        switch (index) {
            case 0:
                return "" + (int) 30 + "%";
            case 1:
                return "" + (int) 5 + "%";
            case 2:
                return "" + (int) PD_RANGE_BONUS_PERCENT + "%";
            default:
                return null;
        }
    }
}
