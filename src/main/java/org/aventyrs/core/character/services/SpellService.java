package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.SpellTree;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * Acquiring a Magia — resolving the Conjurador's general Árvore de Magia cap, pricing the
 * acquisition, and enforcing every acquisition rule before a Magia is added to {@code
 * Character#getSpells()}.
 *
 * <p>This is acquisition only; casting is {@code org.aventyrs.core.magic.SpellCastingService}'s
 * concern and is untouched by any of it.
 */
public interface SpellService {

    /** Where every Conjurador starts before a Talento raises them — a tree's own entry rung. */
    BranchLevel BASE_BRANCH_LEVEL = BranchLevel.SEMENTE;

    /**
     * XP an <em>unaided</em> Magia acquisition costs {@link #grantSpell}, keyed on the Magia's
     * {@link BranchLevel}. Only the four deeper figures are authored anywhere: {@code
     * MetamagicoFeat}'s ladder clauses state "1 exp" to learn an extra Broto, "2 exp" a Muda,
     * "3 exp" an Emergente, "5 exp" a Florescente, and that is the ruleset's <b>only</b> stated
     * spell-acquisition cost. A {@link BranchLevel#SEMENTE} is treated as the free foothold — an
     * inference, the same shape as {@code WeaponDrawService.DEFAULT_DRAW_COST}: it is what {@code
     * MetamagicoFeat#ARCANISTA} hands out automatically, and a tree's entry rung "rests on
     * nothing" (see {@code BranchLevel}). This is <b>not</b> {@link BranchLevel#getManaCost()} —
     * that ladder is 0/1/3/5/7, this one 0/1/2/3/5.
     */
    Map<BranchLevel, BigDecimal> ACQUISITION_EXPERIENCE_COST = Map.of(
            BranchLevel.SEMENTE, BigDecimal.ZERO,
            BranchLevel.BROTO, BigDecimal.ONE,
            BranchLevel.MUDA, BigDecimal.valueOf(2),
            BranchLevel.EMERGENTE, BigDecimal.valueOf(3),
            BranchLevel.FLORESCENTE, BigDecimal.valueOf(5));

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
     * Every Árvore de Magia character <b>conhece</b> — the distinct {@link SpellTree}s across
     * {@code character.getSpells()}. "Conhecer uma Árvore" in this core is simply holding at
     * least one Magia of it (the climb gate makes that first Magia a {@link
     * BranchLevel#SEMENTE}), so this is <b>derived, never stored</b> — the same
     * recompute-on-demand discipline as {@link Spell#isEligible}'s branch resolution. This is
     * what {@code MetamagicoFeat#ARCANISTA}'s "conhece [Conhecimento Metamágico] Árvores de
     * Magia" is asked against.
     */
    Set<SpellTree> getKnownTrees(Character character);

    /**
     * The XP {@link #grantSpell} will spend to give character spell:
     *
     * <ol>
     *   <li>{@link BigDecimal#ZERO} if a held Talento grants this Magia outright ({@link
     *       Feat#grantsFreeSpellAcquisition}) — checked first, short-circuits the rest;</li>
     *   <li>otherwise the rung's {@link #ACQUISITION_EXPERIENCE_COST}, minus every discount the
     *       character has — each held Talento's {@link
     *       Feat#resolveSpellAcquisitionCostReduction} plus the Race's {@code
     *       Race#resolveSpellAcquisitionCostReduction} (Agástias' "Magia é Ciência") — summed and
     *       then <b>floored at zero</b>, so stacked discounts can make a Magia free but never
     *       negative.</li>
     * </ol>
     *
     * <p>A UI listing acquirable Magias with their price must call this rather than reading
     * {@link #ACQUISITION_EXPERIENCE_COST} directly, so every waiver and discount is reflected.
     */
    BigDecimal getAcquisitionCost(Character character, Spell spell);

    /**
     * Grants spell to character after checking all three of {@link Spell#isEligible}'s gates
     * against the cap resolved by {@link #getMaxBranchLevel}, then spending {@link
     * #getAcquisitionCost} from characterSheet. Throws {@link IllegalOperationException} and
     * mutates nothing if a gate fails ({@code SPELL_PREREQUISITE_NOT_MET}) or characterSheet
     * cannot afford the cost ({@code NOT_ENOUGH_EXPERIENCE}). Same validate-then-spend-then-mutate
     * order as {@code FeatService#grantFeat}.
     */
    Spell grantSpell(Character character, CharacterSheet characterSheet, Spell spell) throws IllegalOperationException;
}
