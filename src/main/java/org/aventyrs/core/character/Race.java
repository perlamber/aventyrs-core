package org.aventyrs.core.character;

import org.aventyrs.core.feat.FeatCategory;
import org.aventyrs.core.sheet.DlcRuleset;

import java.util.List;

public interface Race {
    int BASE_ATTRIBUTE_MODIFIER = 1;
    int BASE_NEW_FEAT_COST = 3;
    int BASE_NEW_SKILL_COST = 2;
    public default int getVigorModifier(){ return BASE_ATTRIBUTE_MODIFIER;}
    public default int getStrengthModifier(){ return BASE_ATTRIBUTE_MODIFIER;}
    public default int getDexterityModifier(){ return BASE_ATTRIBUTE_MODIFIER;}
    public default int getFocusModifier(){ return BASE_ATTRIBUTE_MODIFIER;}
    public default int getInstinctModifier(){ return BASE_ATTRIBUTE_MODIFIER;}
    public default int getGnoseModifier(){ return BASE_ATTRIBUTE_MODIFIER;}
    public default int getCharismaModifier(){ return BASE_ATTRIBUTE_MODIFIER;}

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
