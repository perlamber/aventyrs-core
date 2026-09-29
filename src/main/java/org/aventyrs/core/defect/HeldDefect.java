package org.aventyrs.core.defect;

import lombok.NonNull;
import org.aventyrs.core.feat.DefeitoFeat;

import java.util.List;
import java.util.Optional;

/**
 * A Defeito one character holds. What {@code Character#getDefects()} stores and a consumer persists.
 *
 * @param defect         the Defeito
 * @param severity       its gravidade
 * @param choices        answers to {@link Defect#resolveChoices}, in order (the limb, the sense, the fobia…)
 * @param fromCreation   taken at creation — only those give a Benefício de Superação; a Defeito the
 *                       Narrador imposes during play gives none
 * @param superacao      the Benefício de Superação chosen, or {@code null} (a mid-campaign Defeito)
 * @param superacaoPicks the benefit's own pick, per {@link SuperacaoBenefit#getPick()} — a Perícia, a
 *                       Vantagem de Ego or a Habilidade de Competência; empty when it asks none
 */
public record HeldDefect(@NonNull Defect defect, @NonNull DefectSeverity severity, List<Object> choices,
                         boolean fromCreation, SuperacaoBenefit superacao, List<Object> superacaoPicks) {

    public HeldDefect {
        choices = choices == null ? List.of() : List.copyOf(choices);
        superacaoPicks = superacaoPicks == null ? List.of() : List.copyOf(superacaoPicks);
    }

    /** A creation Defeito. */
    public static HeldDefect atCreation(final Defect defect, final DefectSeverity severity, final List<Object> choices,
                                        final SuperacaoBenefit superacao, final List<Object> superacaoPicks) {
        return new HeldDefect(defect, severity, choices, true, superacao, superacaoPicks);
    }

    /** This Defeito's effect — folded into {@code Character#getFeats()}. */
    public DefeitoFeat effect() {
        return defect.effectAt(severity);
    }

    /** The first choice of type, if any — Deficiência Física's {@link Limb}, the {@link Sense}… */
    public <T> Optional<T> choice(final Class<T> type) {
        return choices.stream().filter(type::isInstance).map(type::cast).findFirst();
    }

    /** Every choice of type, in order — Vulnerabilidade's two Elementos. */
    public <T> List<T> choicesOf(final Class<T> type) {
        return choices.stream().filter(type::isInstance).map(type::cast).toList();
    }
}
