package org.aventyrs.core.background;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.feat.AntecedenteFeat;
import org.aventyrs.core.feat.FeatChoice;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An Antecedente — {@code docs/rules/antecedentes.txt}. Two catalogs implement it, one per
 * {@link BackgroundKind}: {@link OriginBackground} (Naturalidade) and {@link CareerBackground}
 * (Carreira). Every character picks one of each, <b>last</b> in creation, permanently.
 *
 * <p>An Antecedente is two things, modelled apart:
 * <ul>
 *   <li><b>What it hands over at creation</b> — Graduações ({@link #getGraduationGrants()}),
 *       Especializações and Habilidades de Competência ({@link #resolveTraitGrants}), and Ego
 *       ({@link #getEgoBonuses()}). {@code CharacterCreationService#applyBackground}
 *       <em>materializes</em> these into the character's own skills, competency list and Egos, so
 *       every reader of a Graduação, a held trait or an Ego total sees them with no change.</li>
 *   <li><b>Its named Benefício</b> ({@link #getBenefit()}) — Prontidão, Sortudo, Treinamento
 *       Atlético… — an {@link AntecedenteFeat} constant that {@code Character#getFeats()} folds in
 *       live for every held Antecedente, so it rides every Talento hook, the roll-activation check
 *       and a client's activation menu as-is. The player's picks it depends on ("a Perícia
 *       escolhida") are read back off the stored {@link AcquiredBackground}.</li>
 * </ul>
 *
 * <p>Sealed so {@link Backgrounds} can enumerate the catalog from the permits clause.
 */
public sealed interface Background permits OriginBackground, CareerBackground {

    /** The enum constant's name — the stable id a consumer persists. */
    String name();

    BackgroundKind getKind();

    /** The rules-text name, e.g. "Deciembrano", "Acadêmico Aventyr". */
    String getDisplayName();

    /** The rules text's flavour paragraph. */
    String getDescription();

    /** The named Benefício — see the class javadoc. */
    AntecedenteFeat getBenefit();

    /**
     * The "Idioma adicional" a Naturalidade teaches, or {@code null} ("Nenhum", and every
     * Carreira). Informational only: this core has no Idiomas.
     */
    default String getAdditionalLanguage() {
        return null;
    }

    /** The Perícias line's "+1 Graduação" clauses, in rules-text order. */
    List<GraduationGrant> getGraduationGrants();

    /**
     * The Especializações and Habilidades de Competência this Antecedente hands over, resolved
     * against trained — the character <em>with this Antecedente's Graduações already applied</em>,
     * since "Se treinado em X" is read as trained in X at all at the moment the Antecedente is
     * picked (a table ruling), which the Antecedente's own +1 counts toward. graduationSkills is
     * every Perícia those Graduações went to, for the clauses scoped to "a Perícia escolhida".
     * Options never include a trait trained already holds. Empty by default.
     */
    default List<TraitGrant> resolveTraitGrants(final Character trained, final Set<SkillType> graduationSkills) {
        return List.of();
    }

    /**
     * Ego points this Antecedente adds ("Sorte +1"). A full Ego point (a table ruling): it raises
     * the Ego's base, so the pool grows and it counts toward the Vantagem de Ego threshold.
     */
    default Map<EgoDomain, Integer> getEgoBonuses() {
        return Map.of();
    }

    /**
     * Picks the Benefício itself needs beyond the Perícias line — Escudeiro's "escolha entre
     * Esquiva e Aparar ou uma Perícia de Ataque", Estudioso Arcano's Árvore de Magias. Answered
     * in order into {@link AcquiredBackground#benefitChoices()}. Empty by default.
     */
    default List<FeatChoice<?>> resolveBenefitChoices(final Character character) {
        return List.of();
    }
}
