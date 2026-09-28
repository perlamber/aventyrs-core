package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.VampiricoFeat;
import org.aventyrs.core.race.Race;
import org.aventyrs.core.race.Vampiro;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.aventyrs.core.util.TranslatableMessages.PROGENY_NOT_PERMITTED;

public class BloodBondServiceImpl implements BloodBondService {

    @Override
    public Optional<UUID> getMaster(@NonNull final Character vampire) {
        if (!(vampire.getRace() instanceof Vampiro vampiro) || holds(vampire, VampiricoFeat.LACOS_ROMPIDOS)) {
            return Optional.empty();
        }
        return Optional.ofNullable(vampiro.getSireId());
    }

    @Override
    public boolean isBoundTo(@NonNull final Character vampire, @NonNull final Character master) {
        return getMaster(vampire).filter(master.getId()::equals).isPresent();
    }

    @Override
    public boolean canSireProgeny(@NonNull final Character master) {
        return master.getRace() instanceof Vampiro vampiro
                && vampiro.getLineage() != Vampiro.VampiroLineage.DAMPIRO
                && holds(master, VampiricoFeat.MESTRE_VAMPIRO);
    }

    @Override
    public Vampiro sire(@NonNull final Character master, @NonNull final Race parentRace) {
        if (!canSireProgeny(master)) {
            throw new IllegalOperationException(PROGENY_NOT_PERMITTED);
        }
        Vampiro.VampiroLineage lineage = ((Vampiro) master.getRace()).getLineage();
        return new Vampiro(lineage, parentRace, null, List.of(), master.getId());
    }

    private static boolean holds(final Character character, final VampiricoFeat talento) {
        return character.getFeats().stream().anyMatch(feat -> feat.catalogEntry() == talento);
    }
}
