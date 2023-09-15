package org.aventyrs.core.character;

/**
 *
 *
 */
public class Human implements Race {

    @Override
    public Character.CharacterBuilder generateEmptyCharacter() {
        return Character.builder();
    }

}
