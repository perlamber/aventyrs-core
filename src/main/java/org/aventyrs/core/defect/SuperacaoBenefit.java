package org.aventyrs.core.defect;

import java.util.Arrays;
import java.util.List;

/**
 * The Benefícios de Superação — what a Defeito taken at creation lets its holder acquire, "um dos
 * seguintes benefícios" per gravidade. Only creation Defeitos give one.
 *
 * <p>Each says what it hands over as counts, and what it still needs picked:
 * <ul>
 *   <li>Qualidades ({@link #getMenorQualities()}/{@link #getMaiorQualities()}) are chosen as {@link
 *       HeldQuality} entries with source {@link QualitySource#SUPERACAO}, matched against these counts.</li>
 *   <li>A Talento is an extra starting slot ({@link #getFeatSlot()}), filled in the Talentos step like
 *       any other — "devem ter seus pré-requisitos preenchidos" is that slot's own eligibility check.</li>
 *   <li>A Habilidade de Atributo is an extra slot the Habilidades step fills.</li>
 *   <li>A Perícia, a Vantagem de Ego or a Habilidade de Competência is picked here, into {@link
 *       HeldDefect#superacaoPicks()} — see {@link #getPick()}.</li>
 * </ul>
 */
public enum SuperacaoBenefit {

    // ---- Leve ---------------------------------------------------------------------------------
    /** "1 Qualidade Menor". */
    QUALIDADE_MENOR(DefectSeverity.LEVE, 1, 0, null, Pick.NONE),
    /** "Treinamento em uma Perícia adicional" — an untrained Perícia, trained at Graduação 1. */
    TREINAMENTO_ADICIONAL(DefectSeverity.LEVE, 0, 0, null, Pick.UNTRAINED_SKILL),
    /** "+1 graduação em uma Perícia Treinada que possua". */
    GRADUACAO_ADICIONAL(DefectSeverity.LEVE, 0, 0, null, Pick.TRAINED_SKILL),

    // ---- Moderado -----------------------------------------------------------------------------
    /** "1 Qualidade Maior". */
    QUALIDADE_MAIOR(DefectSeverity.MODERADO, 0, 1, null, Pick.NONE),
    /** "2 Qualidades Menores". */
    DUAS_QUALIDADES_MENORES(DefectSeverity.MODERADO, 2, 0, null, Pick.NONE),
    /** "1 Talento Geral". */
    TALENTO_GERAL(DefectSeverity.MODERADO, 0, 0, FeatSlot.GERAL, Pick.NONE),

    // ---- Grave --------------------------------------------------------------------------------
    /** "1 Qualidade Maior + 1 Qualidade Menor". */
    QUALIDADE_MAIOR_E_MENOR(DefectSeverity.GRAVE, 1, 1, null, Pick.NONE),
    /** "1 Talento qualquer + 1 Graduação ou Treinamento em Perícia" — any Perícia: +1 if trained, trained if not. */
    TALENTO_E_PERICIA(DefectSeverity.GRAVE, 0, 0, FeatSlot.QUALQUER, Pick.ANY_SKILL),
    /** "1 Habilidade de Atributo … adicional" — a bonus Habilidade de Atributo slot, any Atributo. */
    HABILIDADE_DE_ATRIBUTO(DefectSeverity.GRAVE, 0, 0, null, Pick.NONE),
    /** "1 Habilidade … de Ego … adicional" — a second Vantagem de Ego (a table ruling), in an Ego that has none. */
    VANTAGEM_DE_EGO(DefectSeverity.GRAVE, 0, 0, null, Pick.EGO_ADVANTAGE),
    /** "1 Habilidade … de Competência adicional" — of a trained Perícia. */
    HABILIDADE_DE_COMPETENCIA(DefectSeverity.GRAVE, 0, 0, null, Pick.COMPETENCY_ABILITY);

    /** What a Superação's Talento slot accepts. */
    public enum FeatSlot {
        /** Talentos Gerais only. */
        GERAL,
        /** "Talento qualquer" — General or Racial. */
        QUALQUER
    }

    /** What must be picked with this benefit, into {@link HeldDefect#superacaoPicks()} (one value). */
    public enum Pick {
        NONE, UNTRAINED_SKILL, TRAINED_SKILL, ANY_SKILL, EGO_ADVANTAGE, COMPETENCY_ABILITY
    }

    private final DefectSeverity severity;
    private final int menorQualities;
    private final int maiorQualities;
    private final FeatSlot featSlot;
    private final Pick pick;

    SuperacaoBenefit(final DefectSeverity severity, final int menorQualities, final int maiorQualities,
                     final FeatSlot featSlot, final Pick pick) {
        this.severity = severity;
        this.menorQualities = menorQualities;
        this.maiorQualities = maiorQualities;
        this.featSlot = featSlot;
        this.pick = pick;
    }

    public DefectSeverity getSeverity() {
        return severity;
    }

    public int getMenorQualities() {
        return menorQualities;
    }

    public int getMaiorQualities() {
        return maiorQualities;
    }

    /** The extra starting Talento slot this benefit grants, or {@code null}. */
    public FeatSlot getFeatSlot() {
        return featSlot;
    }

    public Pick getPick() {
        return pick;
    }

    /** The benefits a Defeito of severity offers. */
    public static List<SuperacaoBenefit> optionsFor(final DefectSeverity severity) {
        return Arrays.stream(values()).filter(benefit -> benefit.severity == severity).toList();
    }
}
