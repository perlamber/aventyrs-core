package org.aventyrs.core.race;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.feat.FeatCategory;
import org.aventyrs.core.sheet.DlcRuleset;
import org.aventyrs.core.skill.SkillCompetencyAbility;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Race {
    int BASE_NEW_FEAT_COST = 3;
    int BASE_NEW_SKILL_COST = 2;

    /**
     * The broad creature classification this race belongs to — every implementor must declare
     * one (no default), since this is what a Mestiço race's constructor validates a chosen
     * parent {@link Race} against (see {@code AbstractMesticoRace}/{@code MeioElfo}).
     */
    CreatureType getCreatureType();

    /**
     * Whether this race is itself a Mestiço (mixed-blood) race — e.g. {@code MeioElfo} or any
     * of the 6 Elemental Mestiços. {@code false} by default so none of the pre-existing races
     * need to change; only a Mestiço race overrides this to {@code true}, which its own
     * constructor validation then uses to reject chaining one Mestiço as another's parent race
     * ("que não seja mestiça").
     */
    default boolean isMestico() { return false; }

    /**
     * The {@link SizeCategory} this race normally assigns at creation — {@link
     * SizeCategory#ZERO} by default (the baseline every race without an override already
     * produces via {@link #generateEmptyCharacter}). Races that set a different fixed size
     * (e.g. {@code Anao}'s {@code MINUS_ONE}) override this instead of hardcoding the literal
     * inside {@link #generateEmptyCharacter} a second time; a Mestiço race also uses this to
     * read its chosen parent race's own base size (`parentRace.getBaseSizeCategory()`) without
     * needing to fully assemble a {@link Character} just to read one field back.
     */
    default SizeCategory getBaseSizeCategory() { return SizeCategory.ZERO; }

    /**
     * Racial attribute bonuses every individual of this race is automatically granted,
     * consequence of its culture or environment. Empty by default: most races, and
     * Humans in particular as the size/attribute baseline, have no fixed bonus.
     */
    public default Map<AttributeDomain, Integer> getFixedAttributeBonuses() { return Map.of(); }

    /**
     * How many racial bonus points a player may freely assign among
     * {@link #getChoosableAttributes()} at character creation.
     */
    public default int getChoosableAttributeBonusPoints() { return 0; }

    /**
     * Attributes eligible to receive the race's choosable bonus points. Only meaningful
     * when {@link #getChoosableAttributeBonusPoints()} is greater than zero.
     */
    public default Set<AttributeDomain> getChoosableAttributes() { return Set.of(); }

    /**
     * Habilidades Raciais automatically granted to every member of this race — e.g. Anões'
     * Abatedores de Gigantes. Modeled as {@link SkillCompetencyAbility} instances on purpose:
     * a racial ability contributes to a roll the exact same way an acquired one does, so it
     * reuses that same interface (and its {@code resolve*} hooks) rather than a parallel
     * duplicate type — a consumer that wants "every ability of this kind, acquired or
     * racial" just concatenates this list with {@link Character#getSkillCompetencyAbilities()}
     * (see {@code AtaqueADistanciaInteraction}). Unlike that per-Character list, this one is
     * fixed per race (not a player's acquisition choice), so it lives here instead. Empty by
     * default; most races have none.
     */
    public default List<SkillCompetencyAbility> getRacialAbilities() { return List.of(); }

    /**
     * Cost in XP to learn a new Feat
     *
     * @param featCategory
     * @return int Cost to learn a new Feat based on this character's spec
     */
    public default int getNewFeatCost(FeatCategory featCategory){ return BASE_NEW_FEAT_COST;}

    /**
     * Cost in XP to learn a new Skill
     * @return int Cost to learn a new skill based on this character's spec
     */
    public default int getNewSkillCost(){ return BASE_NEW_SKILL_COST;}

    /**
     *
     * @param dlcRulesetList
     * @return
     */
    public Character.CharacterBuilder generateEmptyCharacter(List<DlcRuleset> dlcRulesetList);
}
