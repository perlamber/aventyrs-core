package org.aventyrs.core.character.services;

import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;

import java.util.ArrayList;
import java.util.List;

public class DefeatBlessingServiceImpl implements DefeatBlessingService {

    @Override
    public List<Blessing> applyDefeatBlessings(final CombatantSheet attacker, final CombatantSheet defeated,
                                               final boolean viaCriticalHit) {
        List<Blessing> granted = new ArrayList<>();
        for (Feat feat : attacker.getCharacter().getFeats()) {
            for (Blessing blessing : feat.resolveDefeatBlessings(attacker.getCharacter(), defeated, viaCriticalHit)) {
                attacker.grantTemporaryBonus(blessing.getModifierType(), blessing.getValue(), blessing.getRounds());
                granted.add(blessing);
            }
        }
        return granted;
    }
}
