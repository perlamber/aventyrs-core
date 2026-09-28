package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.SpellTree;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.math.BigDecimal;
import java.util.List;
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
     * How many Árvores de Magia character may conhecer — the summed {@link
     * Feat#resolveKnownTreeCapacity} across {@code character.getFeats()}. {@code
     * MetamagicoFeat#ARCANISTA} is the only source ("uma quantidade de árvores de magia igual ao
     * seu Conhecimento Metamágico"), and it grows with the Graduação, so this is resolved on
     * every call rather than fixed when the Talento was acquired.
     */
    int getKnownTreeCapacity(Character character);

    /**
     * {@link #getKnownTreeCapacity} minus {@link #getKnownTrees}, floored at zero — how many
     * new Árvores character may still open with {@link #learnTree}. A UI prompts for these; a
     * raised Graduação opens one ("Ao adquirir novas graduações novas Árvores de Magia também
     * podem ser escolhidas").
     */
    int getOpenTreeSlots(Character character);

    /**
     * Every catalog Árvore character could open right now: every {@code MagicTree} not already
     * known, or empty when {@link #getOpenTreeSlots} is zero.
     */
    List<SpellTree> getLearnableTrees(Character character);

    /**
     * Opens tree for character — "você conhece estas árvores de magias e sabe utilizar todas
     * as magias do tipo Semente" — granting every {@link BranchLevel#SEMENTE} of it through
     * {@link #grantSpell}, and returns them. Throws {@code SPELL_TREE_CAPACITY_REACHED} when no
     * slot is open, and {@code SPELL_PREREQUISITE_NOT_MET} when tree is already known; mutates
     * nothing either way.
     */
    List<Spell> learnTree(Character character, CharacterSheet characterSheet, SpellTree tree)
            throws IllegalOperationException;

    /**
     * Per rung, how many free Magias character is still owed: the summed {@link
     * Feat#resolveFreeSpellPicks} for that rung, minus the number of distinct Árvores in which
     * character already holds a Magia of it, floored at zero. Rungs owing nothing are absent.
     * Derived from {@code getSpells()}, never stored — spending a pick is simply holding one
     * more Árvore's Magia at that rung.
     */
    Map<BranchLevel, Integer> getOwedFreeSpells(Character character);

    /**
     * The catalog Magias of rung that character could take as a free pick now: nothing owed
     * at rung means none; otherwise every non-alternate Magia of rung that passes {@link
     * Spell#isEligible} under {@link #getMaxBranchLevel}, from an Árvore holding no Magia of
     * rung yet. In an Árvore that diverges at rung both ramificações' Magias are offered, so
     * picking one <em>is</em> choosing the branch.
     */
    List<Spell> getFreeSpellOptions(Character character, BranchLevel rung);

    /**
     * The XP {@link #grantSpell} will spend to give character spell:
     *
     * <ol>
     *   <li>{@link BigDecimal#ZERO} if a held Talento grants this Magia outright ({@link
     *       Feat#grantsFreeSpellAcquisition}), or if it would spend one of {@link
     *       #getOwedFreeSpells}' picks — its rung is owed and its Árvore holds no Magia of that
     *       rung yet. Checked first, short-circuits the rest;</li>
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
     * mutates nothing if a gate fails ({@code SPELL_PREREQUISITE_NOT_MET}), if spell's Árvore
     * is not yet known and {@link #getOpenTreeSlots} is zero ({@code SPELL_TREE_CAPACITY_REACHED}),
     * or characterSheet
     * cannot afford the cost ({@code NOT_ENOUGH_EXPERIENCE}). Same validate-then-spend-then-mutate
     * order as {@code FeatService#grantFeat}.
     */
    Spell grantSpell(Character character, CharacterSheet characterSheet, Spell spell) throws IllegalOperationException;
}
