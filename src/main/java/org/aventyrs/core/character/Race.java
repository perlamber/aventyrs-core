package org.aventyrs.core.character;

public interface Race {
    public default int getVigorModifier(){ return 0;}
    public default int getStrengthModifier(){ return 0;}
    public default int getDexterityModifier(){ return 0;}
    public default int getFocusModifier(){ return 0;}
    public default int getInstinctModifier(){ return 0;}
    public default int getGnoseModifier(){ return 0;}
    public default int getCharismaModifier(){ return 0;}

    /**
     * Cost in XP to learn a new Feat
     */
    public default int getNewFeatCost(){ return 3;}
    /**
     * Cost in XP to learn a new Skill
     */
    public default int getNewSkilltCost(){ return 2;}
    public Character.CharacterBuilder generateEmptyCharacter();
}
