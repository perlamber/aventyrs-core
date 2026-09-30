package org.aventyrs.core.defect;

import lombok.NonNull;
import org.aventyrs.core.feat.DefeitoFeat;

import java.util.List;
import java.util.Optional;

/**
 * A Defeito one character holds. What {@code Character#getDefects()} stores and a consumer persists.
 *
 * <p><b>An overcome Defeito stays on record.</b> Superar removes its <i>effects</i> ("se livrando de seus
 * efeitos") — {@code Character#getFeats()} folds in active ones only — but what a creation Defeito's
 * Benefício de Superação gave is kept (a table ruling, 2026-09-29), and several of those
 * benefits are derived from this record (a Talento slot, a Habilidade de Atributo slot).
 *
 * @param defect         the Defeito
 * @param severity       its gravidade
 * @param choices        answers to {@link Defect#resolveChoices}, in order (the limb, the sense, the fobia…)
 * @param fromCreation   taken at creation — only those give a Benefício de Superação; a Defeito the
 *                       Narrador imposes during play gives none
 * @param superacao      the Benefício de Superação chosen, or {@code null} (a mid-campaign Defeito)
 * @param superacaoPicks the benefit's own pick, per {@link SuperacaoBenefit#getPick()} — a Perícia, a
 *                       Vantagem de Ego or a Habilidade de Competência; empty when it asks none
 * @param overcome       superado — its effects no longer apply
 */
public record HeldDefect(@NonNull Defect defect, @NonNull DefectSeverity severity, List<Object> choices,
                         boolean fromCreation, SuperacaoBenefit superacao, List<Object> superacaoPicks,
                         boolean overcome) {

    public HeldDefect {
        choices = choices == null ? List.of() : List.copyOf(choices);
        superacaoPicks = superacaoPicks == null ? List.of() : List.copyOf(superacaoPicks);
    }

    /** A Defeito still in force. */
    public HeldDefect(final Defect defect, final DefectSeverity severity, final List<Object> choices,
                      final boolean fromCreation, final SuperacaoBenefit superacao, final List<Object> superacaoPicks) {
        this(defect, severity, choices, fromCreation, superacao, superacaoPicks, false);
    }

    /** A creation Defeito. */
    public static HeldDefect atCreation(final Defect defect, final DefectSeverity severity, final List<Object> choices,
                                        final SuperacaoBenefit superacao, final List<Object> superacaoPicks) {
        return new HeldDefect(defect, severity, choices, true, superacao, superacaoPicks);
    }

    /** A Defeito the Narrador imposes during play — no Benefício de Superação. */
    public static HeldDefect duringPlay(final Defect defect, final DefectSeverity severity, final List<Object> choices) {
        return new HeldDefect(defect, severity, choices, false, null, List.of());
    }

    /** Whether its effects still apply — not {@link #overcome()}. */
    public boolean isActive() {
        return !overcome;
    }

    /** This Defeito, superado. */
    public HeldDefect asOvercome() {
        return new HeldDefect(defect, severity, choices, fromCreation, superacao, superacaoPicks, true);
    }

    /** This Defeito at another gravidade and choices, keeping where it came from and its Superação. */
    public HeldDefect changedTo(final DefectSeverity newSeverity, final List<Object> newChoices) {
        return new HeldDefect(defect, newSeverity, newChoices, fromCreation, superacao, superacaoPicks, overcome);
    }

    /** This Defeito's effect — folded into {@code Character#getFeats()} while {@link #isActive()}. */
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
