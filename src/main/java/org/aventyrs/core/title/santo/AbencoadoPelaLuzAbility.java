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
    // judge), and apply Meio-Dano to its later attacks that Rodada against anyone else — once the
    // caller files Scene#recordAttack.
    // V19 changed two things here, and both are now modeled more precisely than before: the
    // penalty on those later attacks is "os danos causados serão reduzidos à metade (efeito de
    // Meio-Dano)", not the previous revision's Desvantagem on every Perícia roll — which retires
    // that whole TODO, since Meio-Dano is a real, first-class stage (DamageService
    // #calculateFinalDamage's halfDamage flag, OR'd in so two sources never quarter). And the PD
    // spent now buys *targets* ("2PD para 1 único inimigo, então +1PD para cada inimigo alvo
    // adicional") rather than Rodadas, the Duração being a flat 2 — see ActiveAura#getMaxTargets
    // and OrgulhoEldurianoInteraction. Still TODO:
    // - "Efeito de Encantamento" is unmodeled (see SantoSpecialization#ABENCOADO_PELA_LUZ's own
    //   TODO — nothing tracks which Encantamentos affect a combatant);
    // - "Área de Efeito": the Aura isn't classified as one for EsquivaEApararCompetencyAbility
    //   #EVASAO-style clauses (no incoming effect carries that flag).
    ORGULHO_ELDURIANO(
            "Todos os inimigos em Distância Curta são obrigados a desferir o primeiro ataque " +
            "efetuada em cada Rodada em você, podendo escolher outros alvos apenas se você " +
            "não for um alvo válido. Quando atacarem outros alvos (com ataques adicionais ou " +
            "por você não estar disponível) os danos causados serão reduzidos à metade " +
            "(efeito de Meio-Dano). Esta Habilidade não afeta os mesmos personagens mais de " +
            "uma vez, até que eles passem por um Descanso Longo. O Custo desta Habilidade é " +
            "de 2PD para 1 único inimigo, então de +1PD para cada inimigo alvo adicional. " +
            "Esta Habilidade é um Efeito de Encantamento, sempre afeta os alvos em sua Área " +
            "de Efeito e tem Duração de 2 Rodadas.",
            false, variable(2), ActionCost.ofActionPoints(3), Optional.of(OrgulhoEldurianoInteraction.class),
            Optional.of(SantoSpecialization.ABENCOADO_PELA_LUZ), 0),

    // Requer Especialização 'Abençoado pela Luz' — enforced (see class javadoc). Both halves are
    // real, via GritoDeGuerraVulcanoInteraction (Santo#activateGritoDeGuerraVulcano is the entry
    // point), which reports all three Blessings for the caller to grant.
    // Vantagem is just Skill.ADVANTAGE_BONUS (CLAUDE.md's "Vantagem is a flat +2 bonus"
    // section), ModifierType.ATAQUE_A_DISTANCIA_ROLL_BONUS/ATAQUE_CORPO_A_CORPO_ROLL_BONUS are
    // already summed by AbstractSkillInteraction via CombatantSheet#getTemporaryBonus, and
    // SceneContext#getAlliesWithin(Range.ADJACENTE) resolves "self + aliados adjacentes" as an
    // actual List<CombatantSheet> for the caller to grant to. The Defesas half is a DEFESAS-typed
    // Blessing granted at activation rather than a passive @Modifier, since it is activated and
    // target-scoped; DefenseService reads it like any other DEFESAS source.
    // V19 raised the Defesas figure from +2 to +3 and lowered the cost from 3PD to 2PD.
    GRITO_DE_GUERRA_VULCANO(
            "Você e seus aliados adjacentes recebem Bônus de +3 em Defesas e Vantagem em " +
            "rolagens de Perícias de Ataque por 2 Rodadas.",
            false, fixed(2), ActionCost.ofActionPoints(1), Optional.of(GritoDeGuerraVulcanoInteraction.class),
            Optional.of(SantoSpecialization.ABENCOADO_PELA_LUZ), 0),

    // Requer Especialização 'Abençoado pela Luz' — enforced (see class javadoc).
    // Real now through CorpoIndestrutivelDeEponaInteraction, which registers a sheet.PeleDePedra —
    // a TemporaryEffect rather than a Blessing, because neither half of this clause is a stat:
    // the first damaging hit is *negated* (no ModifierType expresses "reduce to zero") and what
    // follows is a figure that changes after every hit (a TemporaryBonus holds one figure for its
    // whole Duração). DamageServiceImpl consults it by type, the one place it does so.
    // "O primeiro ataque que lhe *causaria* Danos" is honoured by reading the stone last, after
    // RD/RA/Meio-Dano: a blow those already turned aside never spends the negation.
    // RDS is RD (ModifierType.DAMAGE_REDUCTION), not RA — see GorgonaFeat's own note — so the
    // decaying 5/3/1 lands on the same total every other RD source does.
    // V19 renamed this Habilidade (was "Pele Rochosa de Epona"), raised the cost from 2PD to 4PD,
    // lowered the Tempo de Ativação from 2PA to 1PA, halved the Duração to 1 Rodada, and replaced
    // the previous revision's bare "você recebe RA" with the decaying RDS 5.
    CORPO_INDESTRUTIVEL_DE_EPONA(
            "Sua pele é transformada em pedra por 1 Rodada. O primeiro ataque que lhe " +
            "causaria Danos é reduzido à zero, após isso você recebe RDS 5. A Redução de " +
            "Danos Sofridos é reduzida em -2 para cada dano sofrido.",
            false, fixed(4), ActionCost.ofActionPoints(1),
            Optional.of(CorpoIndestrutivelDeEponaInteraction.class),
            Optional.of(SantoSpecialization.ABENCOADO_PELA_LUZ), 0),

    // Requer 2 Habilidades de 'Abençoado pela Luz' — enforced (see class javadoc). Its own
    // comment never repeats "Requer Especialização 'Abençoado pela Luz'" the way its three
    // siblings' do, but this whole catalog's class-level javadoc already states every
    // constant here is gated on that Especialização, and you can't hold 2 sibling Habilidades
    // from this catalog without already holding it — so getRequiredSpecialization() is set
    // here too, for consistency with that reading; flagged as an inference from the class-wide
    // statement, not text repeated on this specific constant, same "flag it, don't silently
    // assume" discipline this codebase applies elsewhere.
    // Fully real, through GloriaRelampejanteDeTeslaInteraction, which reports the grant as a
    // SELF_AND_ALLIES Blessing for the caller to grant to self + SceneContext#getAlliesWithin(
    // Range.DISTANCIA_CURTA). It lands via ActionPointsServiceImpl#getMaxActionPoints's
    // CombatantSheet-taking overloads (a caller reading PA through the Character-only overload
    // still won't see it — that overload has no sheet to ask).
    // V19 raised the cost from 2PD to 3PD and the grant from +1PA to +2PA, and — note for anyone
    // re-reading the history here — dropped the "e RA" half entirely. This clause grants no
    // Redução Absoluta at all now, so the timed-RA branch in DamageServiceImpl that was added for
    // it is left with AbencoadoPelaLuzAbility no longer among its consumers (it stays real and
    // used: see AbracadoPelaEscuridao's own citations and CLAUDE.md's timed-RA row).
    GLORIA_RELAMPEJANTE_DE_TESLA(
            "Você e seus aliados em Distância Curta recebem Bônus de +2PA por 1 Rodada.",
            true, fixed(3), ActionCost.FREE_ACTION, Optional.of(GloriaRelampejanteDeTeslaInteraction.class),
            Optional.of(SantoSpecialization.ABENCOADO_PELA_LUZ), 2);

    private final String description;
    private final boolean supreme;
    private final PDCost PDCost;
    private final ActionCost actionPointCost;
    private final Optional<Class<? extends Interaction>> interactionClass;
    private final Optional<AventyrTitleSpecialization> requiredSpecialization;
    private final int requiredOtherAbilities;
}
