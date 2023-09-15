package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeDomain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@Getter
@AllArgsConstructor
public abstract class BasicSkill implements Skill {
    @NonNull
    protected AttributeDomain attributreUsed;
}
