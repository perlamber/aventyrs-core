package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

/**
 * Acquiring a Magia — resolving the Conjurador's general Árvore de Magia cap, and enforcing
 * every acquisition rule before a Magia is added to {@code Character#getSpells()}.
 *
 * <p>This is acquisition only; casting is {@code org.aventyrs.core.magic.SpellCastingService}'s
 * concern and is untouched by any of it.
 */
public interface SpellService {

    /** Where every Conjurador starts before a Talento raises them — a tree's own entry rung. */
    BranchLevel BASE_BRANCH_LEVEL = BranchLevel.SEMENTE;

    /**
     * How deep into any Árvore de Magia character may currently acquire — {@link
     * #BASE_BRANCH_LEVEL} advanced by the summed {@link Feat#resolveBranchLevelIncrease} across
     * {@code character.getFeats()}, clamped to {@link BranchLevel#FLORESCENTE} by {@code
     * BranchLevel#advancedBy}.
     *
     * <p>Talentos are the only source, matching the rules as described: no {@code ModifierType}
     * exists for this and no {@code ModifierResolver} scan runs, so a Habilidade de Atributo or
     * an Excelência cannot raise it. Widen this only when a trait outside {@code
     * FeatCategory#METAMAGICO} is confirmed to.
     */
    BranchLevel getMaxBranchLevel(Character character);

    /**
     * Grants spell to character after checking all three of {@link Spell#isEligible}'s gates
     * against the cap resolved by {@link #getMaxBranchLevel} — throwing {@link
     * IllegalOperationException} ({@code SPELL_PREREQUISITE_NOT_MET}) and mutating nothing if
     * any fails. Same validate-then-mutate order as {@code FeatService#grantFeat}.
     *
     * <p><b>No XP is spent</b>, deliberately: no acquisition cost for a Magia has been specified,
     * and inventing one would bake a number in that rules text then has to override.
     * {@code characterSheet} is taken all the same — it is where {@code unUsedExperience} lives,
     * so adding a cost later is a one-line change here rather than a signature change rippling
     * out to every caller. This is the one open question in this service.
     */
    Spell grantSpell(Character character, CharacterSheet characterSheet, Spell spell) throws IllegalOperationException;
}
