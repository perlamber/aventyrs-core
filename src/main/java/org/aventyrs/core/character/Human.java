package org.aventyrs.core.character;

import org.aventyrs.core.sheet.DlcRuleset;

import java.util.List;

/**
 * Defines what the Human race can do under each rule-set
 *
 */
public class Human implements Race {

    @Override
    public Character.CharacterBuilder generateEmptyCharacter(List<DlcRuleset> dlcRulesetList) {
        return Character.builder();
    }

}
