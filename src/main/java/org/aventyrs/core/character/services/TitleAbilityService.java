package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.title.AventyrTitle;
import org.aventyrs.core.title.AventyrTitleAbility;

import java.math.BigDecimal;

/**
 * Grants a held Título Aventyr an Especialização/Habilidade/Suprema post-acquisition — a
 * Character-progression action, so, like {@link CharacterAttributeService#upgradeBase}/{@link
 * SkillGraduationService#upgradeGraduation}, it can only happen with a {@link CharacterSheet}
 * in hand to spend XP from whenever the specific ability being granted actually costs any.
 */
public interface TitleAbilityService {
    /** Flat XP cost of CENTELHA_SUPERIOR's own "uma Suprema adicional" grant. */
    BigDecimal EXTRA_SUPREMA_COST = BigDecimal.valueOf(5);

    /**
     * How many more Supremas title may receive right now — the normal one-per-Título
     * allotment (this codebase's own established restraint: unenforced anywhere a Título is
     * constructed directly, so this is a read-only report, not a hard cap on {@link
     * AventyrTitle#grantAbility}), plus one more while {@code
     * org.aventyrs.core.ability.InstinctAbility#CENTELHA_SUPERIOR}'s own one-time extra grant
     * hasn't been spent yet (see {@code Character#isCentelhaSuperiorSelected()}) — every held
     * Título is equally eligible for that extra slot until it's actually spent on one of them
     * (see {@link #grantTitleAbility}), never clamped below 0.
     */
    int getAvailableSupremaSlots(Character character, AventyrTitle title);

    /**
     * Grants ability to title — any {@link AventyrTitleAbility}, Habilidade or Suprema alike,
     * not just one requiring CENTELHA_SUPERIOR's own extra slot. This is the single entry point
     * for validating a given ability's own prerequisites before it's actually added to title's
     * held list: {@link AventyrTitleAbility#isEligible} checks its
     * {@code getRequiredSpecializations()}/{@code getRequiredOtherAbilities()} prerequisite
     * (e.g. {@code SantoAbility#GUARDA_VIDAS}'s own "Requer 1 Especialização e 2 outras
     * Habilidades de Santo") — the first "Requer N..." acquisition prerequisite this codebase
     * actually enforces, rather than leaving it an unenforced comment like most elsewhere.
     *
     * <p>A <b>Suprema</b> that would exceed title's normal one-per-Título allotment (i.e.
     * {@link #getAvailableSupremaSlots} would be 0 without it) additionally needs
     * CENTELHA_SUPERIOR specifically — held, its one-time grant not yet spent — and spends
     * {@link #EXTRA_SUPREMA_COST} XP from characterSheet, marking {@code
     * Character#isCentelhaSuperiorSelected()} so a later call for <i>any</i> Título character
     * holds is rejected the same way. A plain Habilidade, or a Suprema that still fits within
     * the normal allotment, has no XP cost or CENTELHA_SUPERIOR requirement documented
     * anywhere in the rules text, so neither applies.
     *
     * @throws IllegalOperationException if character doesn't hold title, ability's own {@link
     *                                    AventyrTitleAbility#isEligible} prerequisite isn't met
     *                                    yet; or, only when ability is a Suprema exceeding the
     *                                    normal allotment, if character doesn't hold {@code
     *                                    InstinctAbility#CENTELHA_SUPERIOR}, its one-time extra
     *                                    grant has already been spent, or characterSheet
     *                                    doesn't have enough unused experience
     */
    AventyrTitleAbility grantTitleAbility(Character character, CharacterSheet characterSheet,
                                           AventyrTitle title, AventyrTitleAbility ability) throws IllegalOperationException;
}
