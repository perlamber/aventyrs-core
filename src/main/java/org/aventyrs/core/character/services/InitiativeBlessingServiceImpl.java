package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.ego.EgoAdvantage;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.skill.SkillCompetencyAbility;

import java.util.ArrayList;
import java.util.List;

public class InitiativeBlessingServiceImpl implements InitiativeBlessingService {

    @Override
    public List<Blessing> resolveBlessings(final Character character) {
        List<Blessing> blessings = new ArrayList<>();
        for (EgoAdvantage advantage : character.getEgoAdvantages().values()) {
            blessings.addAll(advantage.resolveInitiativeBlessings());
        }
        for (AttributeAbility ability : character.getAttributeAbilities()) {
            blessings.addAll(ability.resolveInitiativeBlessings());
        }
        for (SkillCompetencyAbility ability : SkillCompetencyAbility.allFor(character)) {
            blessings.addAll(ability.resolveInitiativeBlessings());
        }
        return blessings;
    }
}
