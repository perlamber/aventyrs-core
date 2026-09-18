package org.aventyrs.core.title.santo;

import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.AventyrTitleSpecialization;
import org.aventyrs.core.title.PDCost;

import static org.aventyrs.core.title.PDCost.fixed;
import static org.aventyrs.core.title.PDCost.variable;

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

    // Requer Especialização 'Abençoado pela Luz' — enforced (see class javadoc). Real through
    // Santo#activateOrgulhoElduriano → OrgulhoEldurianoInteraction: the "Variável" cost is
    // PDCost.variable(1), and the PD actually spent is the Duração in Rodadas of a
    // scene.ActiveAura at Distância Curta. Scene#refreshAura binds every non-group combatant
    // that comes in range (caller-driven — this core does no geometry) and marks it with
    // CombatantSheet#markAffectedUntilRest, which is "não afeta os mesmos personagens mais de uma
    // vez, até que eles passem por um Descanso Longo". AttackDelivery/AttackReceiver then refuse
    // a bound attacker's first attack of the Rodada unless it targets the holder (the caller
    // passes forcedTargetUnavailable when "você não for um alvo válido", which this core can't
    // judge), and apply Skill#DISADVANTAGE_MALUS to its later attacks that Rodada against anyone
    // else — once the caller files Scene#recordAttack. Still TODO:
    // - the Desvantagem is applied to *attack* rolls only; "todas as suas Rolagens de Perícias
    //   contra os novos alvos" also covers non-attack rolls, and nothing tracks what those are for;
    // - "Efeito de Encantamento" is unmodeled (see SantoSpecialization#ABENCOADO_PELA_LUZ's own
    //   TODO — nothing tracks which Encantamentos affect a combatant);
    // - "Área de Efeito": the Aura isn't classified as one for EsquivaEApararCompetencyAbility
    //   #EVASAO-style clauses (no incoming effect carries that flag).
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
            false, variable(1), ActionCost.ofActionPoints(2), Optional.of(OrgulhoEldurianoInteraction.class),
            Optional.of(SantoSpecialization.ABENCOADO_PELA_LUZ), 0),

    // Requer Especialização 'Abençoado pela Luz' — enforced (see class javadoc). Otherwise,
    // two separate halves, only one still TODO'd.
    // The "Vantagem em rolagens de Perícias de Ataque" half is real, via
    // GritoDeGuerraVulcanoInteraction (Santo#activateGritoDeGuerraVulcano is the entry point):
    // Vantagem is just Skill.ADVANTAGE_BONUS (CLAUDE.md's "Vantagem is a flat +2 bonus"
    // section), ModifierType.ATAQUE_A_DISTANCIA_ROLL_BONUS/ATAQUE_CORPO_A_CORPO_ROLL_BONUS are
    // already summed by AbstractSkillInteraction via CombatantSheet#getTemporaryBonus, and
    // SceneContext#getAlliesWithin(Range.ADJACENTE) resolves "self + aliados adjacentes" as an
    // actual List<CombatantSheet> to grant CombatantSheet#grantTemporaryBonus to directly. The
    // "+2 em Defesas" half stays TODO'd — the Defesas stat itself now exists, so what's left is
    // granting it: this is an activated, target-scoped bonus, so it wants a DEFESAS-typed
    // Blessing at activation rather than a passive @Modifier (see
    // Santo's own TODO) — see GritoDeGuerraVulcanoInteraction's own class javadoc for the
    // current split.
    GRITO_DE_GUERRA_VULCANO(
            "Você e seus aliados adjacentes recebem Bônus de +2 em Defesas e Vantagem em " +
            "rolagens de Perícias de Ataque por 2 Rodadas.",
            false, fixed(3), ActionCost.ofActionPoints(1), Optional.of(GritoDeGuerraVulcanoInteraction.class),
            Optional.of(SantoSpecialization.ABENCOADO_PELA_LUZ), 0),

    // Requer Especialização 'Abençoado pela Luz' — enforced (see class javadoc). Otherwise still
    // TODO'd, and now for one reason rather than two: *granting* RA for N Rodadas is expressible
    // since DamageServiceImpl reads a timed ABSOLUTE_DAMAGE_REDUCTION bonus (see
    // GLORIA_RELAMPEJANTE_DE_TESLA), but this clause's RA is conditioned on "após" the first
    // attack having already been negated, and the negation itself has no mechanism: DamageService
    // computes each hit independently, with no "the next hit against this target is free" state to
    // consult, and nothing tracks whether a one-time negation has already fired. Granting the RA
    // unconditionally for the full 2 Rodadas would misrepresent the text, so this stays unwired
    // until the negation exists.
    PELE_ROCHOSA_DE_EPONA(
            "Sua pele é transformada em pedra por 2 Rodadas. O primeiro ataque que lhe " +
            "causaria Danos é reduzido à zero, após isso você recebe RA.",
            false, fixed(2), ActionCost.ofActionPoints(2), Optional.empty(), Optional.of(SantoSpecialization.ABENCOADO_PELA_LUZ), 0),

    // Requer 2 Habilidades de 'Abençoado pela Luz' — enforced (see class javadoc). Its own
    // comment never repeats "Requer Especialização 'Abençoado pela Luz'" the way its three
    // siblings' do, but this whole catalog's class-level javadoc already states every
    // constant here is gated on that Especialização, and you can't hold 2 sibling Habilidades
    // from this catalog without already holding it — so getRequiredSpecialization() is set
    // here too, for consistency with that reading; flagged as an inference from the class-wide
    // statement, not text repeated on this specific constant, same "flag it, don't silently
    // assume" discipline this codebase applies elsewhere. Otherwise fully TODO'd, two separate
    // Fully real now, through GloriaRelampejanteDeTeslaInteraction, which reports both halves as
    // SELF_AND_ALLIES Blessings for the caller to grant to self + SceneContext#getAlliesWithin(
    // Range.DISTANCIA_CURTA). Both land: +1PA via ActionPointsServiceImpl#getMaxActionPoints's
    // CombatantSheet-taking overloads (a caller reading PA through the Character-only overload
    // still won't see it — that overload has no sheet to ask), and RA via DamageServiceImpl's
    // timed-RA branch, which reads CombatantSheet#getTemporaryBonus(ABSOLUTE_DAMAGE_REDUCTION)
    // beside the continuously-scanned passives. That branch was added *for* this clause: the
    // older comment here correctly said RA had no TemporaryBonus path, which is no longer true.
    // A numberless "recebem RA" is one instance, DamageService.DEFAULT_DAMAGE_REDUCTION.
    GLORIA_RELAMPEJANTE_DE_TESLA(
            "Você e seus aliados em Distância Curta recebem Bônus de +1PA e RA por 1 Rodada.",
            true, fixed(2), ActionCost.FREE_ACTION, Optional.of(GloriaRelampejanteDeTeslaInteraction.class),
            Optional.of(SantoSpecialization.ABENCOADO_PELA_LUZ), 2);

    private final String description;
    private final boolean supreme;
    private final PDCost PDCost;
    private final ActionCost actionPointCost;
    private final Optional<Class<? extends Interaction>> interactionClass;
    private final Optional<AventyrTitleSpecialization> requiredSpecialization;
    private final int requiredOtherAbilities;
}
