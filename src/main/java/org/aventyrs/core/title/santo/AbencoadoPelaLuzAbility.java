package org.aventyrs.core.title.santo;

import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.AventyrTitleSpecialization;

/**
 * The Habilidades/Suprema gated on holding the {@link SantoSpecialization#ABENCOADO_PELA_LUZ}
 * Especialização specifically — as opposed to {@link SantoAbility}, whose own prerequisites
 * only ever name "1 Especialização" generically (either of Santo's two). Kept in its own enum,
 * in the same {@code santo} subpackage, rather than folded into {@code SantoAbility}: a
 * constant's prerequisite here always names this one specific Especialização by name, and a
 * second Título-family enum keeps that distinction legible as more Especializações (and their
 * own gated abilities) are added. Every constant here overrides {@code
 * AventyrTitleAbility#getRequiredSpecialization()} with {@code SantoSpecialization
 * #ABENCOADO_PELA_LUZ} for exactly this reason (real, enforced data now — see
 * {@code AventyrTitleAbility#isEligible}), and {@code GLORIA_RELAMPEJANTE_DE_TESLA} additionally
 * overrides {@code getRequiredOtherAbilities()} for its own "2 Habilidades de 'Abençoado pela
 * Luz'" clause, counted only against sibling constants of this same enum — never {@code
 * SantoAbility}'s own, even though both live in the same held Título's ability list.
 */
@Getter
@AllArgsConstructor
public enum AbencoadoPelaLuzAbility implements AventyrTitleAbility {

    // Requer Especialização 'Abençoado pela Luz' — enforced (see class javadoc). Otherwise
    // fully TODO'd: "obrigar" enemies to target
    // the holder is a forced-attack-targeting mechanism this core has no equivalent of (the
    // same "no attack-targeting orchestration"/"nothing models an attack as a stateful
    // transaction another Character can interject into" gap SantoAbility#GUARDA_VIDAS already
    // cites); the Desvantagem-on-subsequent-attacks clause needs a flat Desvantagem constant,
    // which doesn't exist anywhere in this core (confirmed: race/Bestial.java's own citation —
    // "Desvantagem itself has no flat-bonus constant to mirror Skill#ADVANTAGE_BONUS... this
    // codebase's 'Vantagem is a flat +2' convention has never needed a symmetric Desvantagem
    // constant before") — even if it did, scoping it to "attacks against the *new* target
    // specifically" needs the same "doesn't track what a roll is *for*" gap documented
    // elsewhere; and "Efeito de Encantamento" + "Área de Efeito" are both unmodeled
    // classifications (Encantamento — see SantoSpecialization#ABENCOADO_PELA_LUZ's own TODO;
    // Área de Efeito — cited but unbuilt, see EsquivaEApararCompetencyAbility#EVASAO's own
    // TODO). getPDCost() reports the rules text's stated *minimum* (1) — the actual cost is
    // "Variável", chosen by the activating player above that floor, which this int-returning
    // method has no way to represent beyond the floor itself.
    ORGULHO_ELDURIANO(
            "Todos os inimigos em Distância Curta são obrigados a desferir o primeiro ataque " +
            "efetuada no Turno deles em você, podendo escolher outros alvos apenas se você " +
            "não for um alvo válido. Após te atacarem, caso seus inimigos escolham outros " +
            "alvos para seus ataques conseguintes realizados na mesma Rodada, eles sofrerão " +
            "Desvantagem em todas as suas Rolagens de Perícias contra os novos alvos. Esta " +
            "Habilidade não afeta os mesmos personagens mais de uma vez, até que eles passem " +
            "por um Descanso Longo, e tem como Duração uma quantidade de Rodadas igual a " +
            "quantidade de PD usados em sua ativação. Esta Habilidade é um Efeito de " +
            "encantamento e sempre afeta os alvos em sua Área de Efeito.",
            false, 1, 2, false, Optional.empty(), Optional.of(SantoSpecialization.ABENCOADO_PELA_LUZ), 0),

    // Requer Especialização 'Abençoado pela Luz' — enforced (see class javadoc). Otherwise,
    // two separate halves, only one still TODO'd.
    // The "Vantagem em rolagens de Perícias de Ataque" half is real, via
    // GritoDeGuerraVulcanoInteraction (Santo#activateGritoDeGuerraVulcano is the entry point):
    // Vantagem is just Skill.ADVANTAGE_BONUS (CLAUDE.md's "Vantagem is a flat +2 bonus"
    // section), ModifierType.ATAQUE_A_DISTANCIA_ROLL_BONUS/ATAQUE_CORPO_A_CORPO_ROLL_BONUS are
    // already summed by AbstractSkillInteraction via CharacterSheet#getTemporaryBonus, and
    // SceneContext#getAlliesWithin(Range.ADJACENTE) resolves "self + aliados adjacentes" as an
    // actual List<CharacterSheet> to grant CharacterSheet#grantTemporaryBonus to directly. The
    // "+2 em Defesas" half stays TODO'd — it still needs the missing Defesas system (see
    // Santo's own TODO) — see GritoDeGuerraVulcanoInteraction's own class javadoc for the
    // current split.
    GRITO_DE_GUERRA_VULCANO(
            "Você e seus aliados adjacentes recebem Bônus de +2 em Defesas e Vantagem em " +
            "rolagens de Perícias de Ataque por 2 Rodadas.",
            false, 3, 1, false, Optional.of(GritoDeGuerraVulcanoInteraction.class),
            Optional.of(SantoSpecialization.ABENCOADO_PELA_LUZ), 0),

    // Requer Especialização 'Abençoado pela Luz' — enforced (see class javadoc). Otherwise
    // fully TODO'd: the RA half is conditioned on
    // "após" the first attack having already been negated — a sequencing/state-tracking
    // concept ("has this effect's one-time negation already triggered this activation?") this
    // core has no equivalent of, so RA can't just be granted unconditionally for the full 2
    // Rodadas without misrepresenting the rules text. The "primeiro ataque... reduzido à
    // zero" half itself needs a "negate this one specific upcoming hit" mechanism —
    // DamageService computes each hit independently, with no notion of "the next hit against
    // this target is free" state to consult.
    PELE_ROCHOSA_DE_EPONA(
            "Sua pele é transformada em pedra por 2 Rodadas. O primeiro ataque que lhe " +
            "causaria Danos é reduzido à zero, após isso você recebe RA.",
            false, 2, 2, false, Optional.empty(), Optional.of(SantoSpecialization.ABENCOADO_PELA_LUZ), 0),

    // Requer 2 Habilidades de 'Abençoado pela Luz' — enforced (see class javadoc). Its own
    // comment never repeats "Requer Especialização 'Abençoado pela Luz'" the way its three
    // siblings' do, but this whole catalog's class-level javadoc already states every
    // constant here is gated on that Especialização, and you can't hold 2 sibling Habilidades
    // from this catalog without already holding it — so getRequiredSpecialization() is set
    // here too, for consistency with that reading; flagged as an inference from the class-wide
    // statement, not text repeated on this specific constant, same "flag it, don't silently
    // assume" discipline this codebase applies elsewhere. Otherwise fully TODO'd, two separate
    // gaps — unlike GRITO_DE_GUERRA_VULCANO's Vantagem half, resolving "self + aliados em
    // Distância Curta" (the recipient-resolution technique GritoDeGuerraVulcanoInteraction
    // now demonstrates via SceneContext#getAlliesWithin) isn't the blocker for either half
    // here; what each half grants is. The RA half can't reuse
    // CharacterSheet#grantTemporaryBonus at all — confirmed DamageServiceImpl#
    // getTotalAbsoluteDamageReduction never reads CharacterSheet#getTemporaryBonus for
    // ModifierType.ABSOLUTE_DAMAGE_REDUCTION; RA is only ever summed from the reflection-based
    // ability scan and AventyrTitleAbility#resolveAbsoluteDamageReduction, both continuously-
    // scanned passive-style hooks with no "grant a one-time bonus lasting N Rodadas after this
    // activation" shape at all — a genuinely different gap from GRITO's, not the same one. The
    // +1PA half has its own, unrelated problem: ActionPointsServiceImpl#getMaxActionPoints
    // never reads CharacterSheet#getTemporaryBonus(ModifierType.ACTION_POINTS) at all — only
    // Character#getTemporaryActionPointsBonus(), a plain non-Round-scoped int field mutated
    // directly, not a TemporaryEffect that ticks down — so even a single-target
    // grantTemporaryBonus(ACTION_POINTS, ...) call would be silently inert today; PA's
    // "temporary bonus" pathway and this core's general TemporaryBonus/ModifierType
    // machinery haven't been connected.
    GLORIA_RELAMPEJANTE_DE_TESLA(
            "Você e seus aliados em Distância Curta recebem Bônus de +1PA e RA por 1 Rodada.",
            true, 2, 0, false, Optional.empty(), Optional.of(SantoSpecialization.ABENCOADO_PELA_LUZ), 2) {
        @Override
        public boolean isFreeActionActivation() {
            return true;
        }
    };

    private final String description;
    private final boolean supreme;
    private final int PDCost;
    private final int actionPointCost;
    private final boolean reactionActivation;
    private final Optional<Class<? extends Interaction>> interactionClass;
    private final Optional<AventyrTitleSpecialization> requiredSpecialization;
    private final int requiredOtherAbilities;
}
