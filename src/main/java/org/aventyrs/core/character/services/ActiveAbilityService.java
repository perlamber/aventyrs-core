package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

public interface ActiveAbilityService {
    /**
     * Activates ability on character's behalf, applying its effect to characterSheet — the
     * single entry point for triggering an {@link ActiveAbility}, mirroring the
     * check-then-spend-then-mutate shape {@code CharacterAttributeService#upgradeBase}/
     * {@code SkillGraduationService#upgradeGraduation} already use for a resource-gated
     * character action.
     *
     * <p>Validates, in order: that character actually holds ability (present in
     * {@link Character#getActiveAbilities()}, by reference — the same instance granted at
     * acquisition, not just an equal one); that character can afford its
     * {@link ActiveAbility#getActionPointCost()} on turnNumber (via {@code
     * ActionPointsService#getMaxActionPoints(CombatantSheet, int)} — the {@code CombatantSheet}
     * overload, so a granted {@code ModifierType.ACTION_POINTS} {@code TemporaryBonus} counts,
     * mirroring the Turn-max comparison {@link
     * org.aventyrs.core.action.ActionPointsService#canAffordSkillRoll} already uses for a
     * Perícia roll's own PA cost;
     * this core still has no persisted "PA already spent this Turn" pool, so this checks the
     * Turn's max, not a running spent total); that characterSheet currently has enough Magic
     * Points (via {@link MagicPointsService#getCurrentMagicPoints}) to afford its {@link
     * ActiveAbility#getMagicPointCost()}; and, when {@link ActiveAbility#getHitPointCost()} is
     * positive, that spending it would not drop the holder to 0 PV or below (a Poder Vampírico
     * "consome 3PV" but cannot be self-fatal). Throws {@link IllegalOperationException} on any
     * failure, leaving characterSheet untouched.
     *
     * <p>On success: spends the Magic Point cost (via {@link CombatantSheet#spendMagicPoints}),
     * the Hit Point cost if any (via {@link CombatantSheet#applyDamage} — plain damage, its
     * "recuperados exclusivamente com Roubo de Vida" provenance untracked), and applies every
     * effect from {@code ability.resolveEffects(character)} (via {@link
     * CombatantSheet#applyEffect}) — a caller never resolves or applies the granted effects
     * itself.
     *
     * @throws IllegalOperationException if ability isn't held, or character/characterSheet
     *         can't currently afford its cost — characterSheet is left untouched
     */
    void activate(Character character, CombatantSheet characterSheet, ActiveAbility ability, int turnNumber) throws IllegalOperationException;
}
