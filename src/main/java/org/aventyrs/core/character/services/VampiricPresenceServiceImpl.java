package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.VampiricoFeat;
import org.aventyrs.core.race.CreatureType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CarmillaPresence;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.HealingSource;

import java.util.stream.Stream;

public class VampiricPresenceServiceImpl implements VampiricPresenceService {

    /** "Em Distância Muito Curta." */
    private static final Range BASE_REACH = Range.DISTANCIA_MUITO_CURTA;

    @Override
    public int drain(@NonNull final CombatantSheet vampire, final SceneContext vampireContext) {
        if (vampireContext == null || !vampire.hasActiveEffect(CarmillaPresence.class)) {
            return 0;
        }
        Range reach = BASE_REACH.increasedBy(vampire.getCharacter().getAllTitles().size());
        long drained = Stream.concat(vampireContext.getAlliesWithin(reach).stream(),
                        vampireContext.getEnemiesWithin(reach).stream())
                .filter(other -> other != vampire && other.getDamageTaken() > 0 && isLiving(other))
                .count();
        if (drained == 0) {
            return 0;
        }
        int before = vampire.getDamageTaken();
        vampire.heal((int) drained, new HealingSource(VampiricoFeat.PRESENCA_DE_CARMILLA, true, vampire, null, null));
        return before - vampire.getDamageTaken();
    }

    /** "Vivo": anyone whose Raça is not a Renascido — ⚠️ a foe with no Raça counts as living. */
    private static boolean isLiving(final CombatantSheet sheet) {
        Character character = sheet.getCharacter();
        return character == null || character.getRace() == null
                || character.getRace().getCreatureType() != CreatureType.RENASCIDO;
    }
}
