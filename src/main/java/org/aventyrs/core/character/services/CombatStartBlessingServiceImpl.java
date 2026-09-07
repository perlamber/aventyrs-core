package org.aventyrs.core.character.services;

import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;

import java.util.ArrayList;
import java.util.List;

public class CombatStartBlessingServiceImpl implements CombatStartBlessingService {

    @Override
    public List<Blessing> applyCombatStartBlessings(final CombatantSheet combatant) {
        List<Blessing> granted = new ArrayList<>();
        for (Feat feat : combatant.getCharacter().getFeats()) {
            for (Blessing blessing : feat.resolveCombatStartBlessings(combatant.getCharacter())) {
                combatant.grantTemporaryBonus(blessing.getModifierType(), blessing.getValue(), blessing.getRounds());
                granted.add(blessing);
            }
        }
        return granted;
    }
}
