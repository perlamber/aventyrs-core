package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.action.Manoeuvre;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.sheet.TemporaryBonus;
import org.aventyrs.core.skill.SkillType;

import static org.aventyrs.core.util.TranslatableMessages.CANNOT_ATTACK_WITH_WEAPON;
import static org.aventyrs.core.util.TranslatableMessages.CHARGE_AFTER_REPOSITION;
import static org.aventyrs.core.util.TranslatableMessages.CHARGE_IN_DIFFICULT_TERRAIN;
import static org.aventyrs.core.util.TranslatableMessages.CHARGE_MOVEMENT_PREVENTED;
import static org.aventyrs.core.util.TranslatableMessages.CHARGE_REQUIRES_MELEE_WEAPON;
import static org.aventyrs.core.util.TranslatableMessages.WEAPON_NOT_CARRIED;
import static org.aventyrs.core.util.TranslatableMessages.WEAPON_NOT_DRAWN;

public class ChargeServiceImpl implements ChargeService {

    private final MovementService movementService;
    private final MovementReactionService movementReactionService;

    public ChargeServiceImpl() {
        this(new MovementServiceImpl(), new MovementReactionServiceImpl());
    }

    public ChargeServiceImpl(final MovementService movementService,
                              final MovementReactionService movementReactionService) {
        this.movementService = movementService;
        this.movementReactionService = movementReactionService;
    }

    @Override
    public ActionCost getActionPointCost(@NonNull final Character character) {
        int reduction = character.getFeats().stream()
                .mapToInt((Feat feat) -> feat.resolveChargeActionPointReduction(character))
                .sum();
        return ActionCost.ofActionPoints(
                Math.max(MINIMUM_ACTION_POINT_COST, BASE_ACTION_POINT_COST - reduction));
    }

    @Override
    public int getMovementAllowance(@NonNull final CombatantSheet sheet) {
        int multiplier = BASE_MOVEMENT_MULTIPLIER + sheet.getCharacter().getAttributeAbilities().stream()
                .mapToInt(AttributeAbility::resolveChargeMovementMultiplierIncrease)
                .sum();
        return Math.max(0, multiplier) * movementService.getMovementBase(sheet);
    }

    @Override
    public int getMovementDamageReduction(@NonNull final Character character) {
        return character.getFeats().stream()
                .mapToInt((Feat feat) -> feat.resolveChargeMovementDamageReduction(character))
                .sum();
    }

    @Override
    public boolean canCharge(@NonNull final CombatantSheet sheet, final Weapon weapon,
                             final SceneContext sceneContext) {
        return refusalFor(sheet, weapon, sceneContext) == null;
    }

    @Override
    public ChargeResult begin(@NonNull final CombatantSheet sheet, final Weapon weapon,
                               final SceneContext sceneContext) {
        String refusal = refusalFor(sheet, weapon, sceneContext);
        if (refusal != null) {
            throw new IllegalOperationException(refusal);
        }
        // The allowance is read before the movement is claimed: a per-movement clause
        // (DexterityAbility#PASSOS_LONGOS, MobilidadeFeat#VELOCISTA) applies to *this* movement,
        // which is the one consumeMovementThisRound is about to number.
        int allowance = getMovementAllowance(sheet);
        sheet.consumeMovementThisRound();
        return new ChargeResult(
                getActionPointCost(sheet.getCharacter()),
                allowance,
                getMovementDamageReduction(sheet.getCharacter()),
                movementReactionService.getProvokedReactors(sheet, sceneContext, Manoeuvre.INVESTIDA));
    }

    @Override
    public TemporaryBonus applyOutcome(@NonNull final CombatantSheet sheet, final boolean hit) {
        if (hit || waivesMissPenalty(sheet.getCharacter())) {
            return null;
        }
        // Granted as a Blessing rather than through the bare grantTemporaryBonus path purely for
        // the source: a sourceless bonus stacks without limit, so two failed Investidas would
        // reach -4. With one, applyEffect trims by (source, ModifierType) and the second grant
        // renews the window instead.
        return sheet.grantBlessing(new Blessing(ModifierType.DEFESAS, MISS_DEFENSE_MALUS,
                MISS_DEFENSE_MALUS_ROUNDS, TargetScope.SELF, MISS_PENALTY_SOURCE));
    }

    private boolean waivesMissPenalty(final Character character) {
        return character.getAttributeAbilities().stream()
                .anyMatch(AttributeAbility::waivesChargeMissDefensePenalty);
    }

    /**
     * The one place an Investida is judged legal, shared by {@link #canCharge} and {@link #begin}
     * so the predicate and the mutator can never drift apart — {@code WeaponDrawServiceImpl}'s own
     * shape.
     *
     * <p>A {@code null} weapon is an Ataque Desarmado: nothing to draw, nothing to classify, and
     * {@code DamageBaseService} already reads {@code weapon == null} as exactly that. It still has
     * to clear the Forma and movement gates below.
     */
    private String refusalFor(final CombatantSheet sheet, final Weapon weapon, final SceneContext sceneContext) {
        Character character = sheet.getCharacter();
        if (weapon != null) {
            // "The weapon must already be in hand" — an Investida bundles a movement with an
            // attack and prices neither a draw nor a moment to make one, so WeaponDrawService is
            // deliberately not consulted here: this is a gate, not a price.
            //
            // The test is membership of the equipment list rather than treatsAsNaturalWeapon,
            // and that is the precise reason Armas Naturais are exempt: a body part is not
            // equipment, so there is nothing to draw. It also keeps a Talento that *reclassifies*
            // an ordinary weapon as natural (ArtesMarciaisFeat#DOMINAR_ARTE_MARCIAL_FERROADA_
            // ESMAGADORA) still needing that real weapon in hand, which asking
            // treatsAsNaturalWeapon would have silently excused.
            if (character.getEquipment().contains(weapon)) {
                if (!character.isDrawn(weapon)) {
                    return WEAPON_NOT_DRAWN;
                }
            } else if (!sheet.getNaturalWeapons().contains(weapon)) {
                return WEAPON_NOT_CARRIED;
            }
            // Melee is the Perícia, never the ItemCategory — which is what lets a Chifres
            // Poderosos charge, as Guampo's Chifres Majestosos and the Empalador's Favor require.
            if (weapon.getAttackSkillType() != SkillType.ATAQUE_CORPO_A_CORPO) {
                return CHARGE_REQUIRES_MELEE_WEAPON;
            }
        }
        if (!sheet.canAttackWith(weapon)) {
            return CANNOT_ATTACK_WITH_WEAPON;
        }
        // Agarrado/Imobilizado. The allowance would come back 0 anyway, but checking the
        // prohibition itself is what lets the refusal say why.
        if (sheet.isMovementPrevented(null)) {
            return CHARGE_MOVEMENT_PREVENTED;
        }
        // An Investida is a movement bought with Pontos de Ação, which a Reposicionar already taken
        // this Turn forbids (table ruling) — RepositionService refuses the converse.
        if (sheet.getRepositionsTakenThisRound() > 0) {
            return CHARGE_AFTER_REPOSITION;
        }
        // "Investidas não são possíveis em Terreno Difícil" (table ruling, 2026-09-23) — the
        // starting space here; a path entering one is the caller's to refuse.
        if (sceneContext != null && sceneContext.getEnvironmentalState().inDifficultTerrain()) {
            return CHARGE_IN_DIFFICULT_TERRAIN;
        }
        return null;
    }
}
