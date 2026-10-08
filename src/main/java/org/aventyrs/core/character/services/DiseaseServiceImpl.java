package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.Disease;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class DiseaseServiceImpl implements DiseaseService {

    @Override
    public Optional<Disease> spreadingDisease(@NonNull final CombatantSheet holder) {
        return holder.getHeldConditions().stream()
                .filter(Disease.class::isInstance)
                .map(Disease.class::cast)
                .filter(Disease::isPropagates)
                .findFirst();
    }

    @Override
    public List<CombatantSheet> propagationCandidates(@NonNull final CombatantSheet holder,
                                                      @NonNull final Collection<CombatantSheet> adjacent) {
        return spreadingDisease(holder)
                .map(disease -> adjacent.stream()
                        .filter(other -> other != holder && other != disease.getSource())
                        .filter(other -> !other.hasCondition(ConditionType.DOENTE, null))
                        .toList())
                .orElse(List.of());
    }

    @Override
    public boolean resolvePropagation(@NonNull final CombatantSheet holder, @NonNull final CombatantSheet target,
                                      final int face) {
        if (face != INFECTING_FACE || !propagationCandidates(holder, List.of(target)).contains(target)) {
            return false;
        }
        target.applyCondition(spreadingDisease(holder).orElseThrow().caught());
        return target.hasCondition(ConditionType.DOENTE, null);
    }
}
