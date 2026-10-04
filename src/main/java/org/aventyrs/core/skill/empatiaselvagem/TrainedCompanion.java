package org.aventyrs.core.skill.empatiaselvagem;

import lombok.NonNull;
import org.aventyrs.core.subordinate.SubordinateBenefit;
import org.aventyrs.core.subordinate.SubordinateGrade;

import java.util.Set;

/**
 * A creature trained through {@link EmpatiaSelvagemCompetencyAbility#ALIADO_DA_NATUREZA} — "ele será considerado um
 * Subordinado do tipo Cavaleiro, Peão ou Torre, a sua escolha". Lasting, kept on the trainer's {@code Character}
 * (table ruling); in each Cena the trainer calls one of them, whose {@link #benefit()} then applies as a Subordinado.
 *
 * @param name    what the trainer calls the creature
 * @param benefit the Subordinado benefit chosen when it was trained — a Cavaleiro's, Peão's or Torre's
 */
public record TrainedCompanion(@NonNull String name, @NonNull SubordinateBenefit benefit) {

    /** The three grades "Cavaleiro, Peão ou Torre" a trained creature may serve as. */
    public static final Set<SubordinateGrade> GRADES =
            Set.of(SubordinateGrade.CAVALEIRO, SubordinateGrade.PEAO, SubordinateGrade.TORRE);

    public TrainedCompanion {
        if (name.isBlank() || !GRADES.contains(benefit.getGrade())) {
            throw new IllegalArgumentException("A trained creature needs a name and a Cavaleiro, Peão or Torre benefit");
        }
    }
}
