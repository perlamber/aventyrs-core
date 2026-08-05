package org.aventyrs.core.race;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
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
