package org.aventyrs.core.ability;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.InitiativeBlessing;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.SkillTraitKind;
import org.aventyrs.core.skill.SkillType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

public interface AttributeAbility {
    AttributeDomain getAttributeDomain();
    String getDescription();

    /**
     * Which Ego domains this ability grants a non-cumulative temporary point in when the
     * holder's own Perícia roll resolves to criticalResult — e.g. {@code
     * CharismaAbility#DESTINO_FAVORAVEL} granting Sorte and Autocontrole on {@link
     * CriticalResult#ACERTO_CRITICO_MAIOR}. Empty by default; only override on a constant
     * whose rules text reacts to the roller's own critical result this way. "Non-cumulative"
     * per that kind of ability's own rules text: applied via {@code CharacterSheet
     * #gainNonCumulativeTemporaryEgoPoints}, not a plain additive gain — repeated triggers
     * don't stack a point on top of one already held.
     */
    default List<EgoDomain> resolveCriticalSuccessEgoGain(CriticalResult criticalResult) {
        return List.of();
    }

    /**
     * Which single Ego domain this ability permanently grants +1 to (in {@code
     * EgoValue#variable}, never {@code #base} — see {@code EgoValue}'s own javadoc) the
     * moment it's acquired — e.g. {@code CharismaAbility#DESTINO_FAVORAVEL}'s "Você adquire
     * um ponto de Sorte permanentemente." Empty by default; only override on a constant
     * whose rules text grants a permanent Ego point this way. Applied by {@code
     * org.aventyrs.core.character.services.AttributeAbilityService#grantAttributeAbility},
     * not by this method itself — same "resolve, don't mutate" shape as {@link
     * #resolveCriticalSuccessEgoGain}.
     */
    default Optional<EgoDomain> resolvePermanentEgoGain() {
        return Optional.empty();
    }

    /**
     * The {@link ActiveAbility} this ability grants the character the moment it's acquired —
     * e.g. {@link FocusAbility#CONCENTRACAO_PROFUNDA}'s own activatable state. Empty by
     * default; only override on a constant whose rules text describes something the holder
     * must actively spend Pontos de Ação/Magia to trigger, rather than an always-on passive
     * effect. Applied by {@code
     * org.aventyrs.core.character.services.AttributeAbilityService#grantAttributeAbility},
     * not by this method itself — same "resolve, don't mutate" shape as {@link
     * #resolvePermanentEgoGain}.
     */
    default Optional<ActiveAbility> resolveActiveAbility() {
        return Optional.empty();
    }

    /**
     * {@link #resolvePendingSkillTraitChoices(Collection)} against character's own currently
     * trained Perícias — the overload to call when a full {@link Character} is in hand. Never
     * override this one: the rule lives on the {@link Collection} overload, which is all any
     * of these abilities actually needs (see its javadoc for why that matters).
     */
    default List<SkillType> resolvePendingSkillTraitChoices(Character character) {
        return resolvePendingSkillTraitChoices(character.getSkills().keySet());
    }

    /**
     * Every {@link SkillType} this ability still owes the holder a trait choice for, given
     * trainedSkills — e.g. {@link CharismaAbility#CHARME}'s own "pick a {@code
     * SkillCompetencyAbility} <b>and</b> a {@code SkillSpecialization} for each trained Carisma
     * Perícia," or {@link GnoseAbility#DOMINIO_DO_CONHECIMENTO}'s own specialization-<b>only</b>
     * "pick a {@code SkillSpecialization} for each known Perícia." These are <i>candidates</i>,
     * not a quota: how many of them the player may actually resolve is {@link
     * #resolvePendingSkillTraitChoiceLimit()}, and which trait(s) each one owes is {@link
     * #resolvePendingSkillTraitKinds()} — neither is carried by this list itself. Empty by
     * default; only override on a constant whose rules text requires the player to pick one or
     * both of these per Perícia at acquisition time.
     *
     * <p>Takes the trained {@link SkillType}s rather than a {@link Character} — unlike {@link
     * #resolvePermanentEgoGain}/{@link #resolveActiveAbility}, which Perícias are owed a choice
     * depends on which ones are currently trained, but on <i>nothing else</i> about the
     * character. Keeping it to that lets a caller that has no {@code Character} yet — a
     * character-creation wizard, where the abilities and the Perícias are both still being
     * picked — ask the same question the acquisition path asks, instead of duplicating the rule
     * or fabricating a throwaway {@code Character} to hold it.
     *
     * <p>Reported by {@code
     * org.aventyrs.core.character.services.AttributeAbilityService#grantAttributeAbility} via
     * {@code AttributeAbilityGrantResult#getPendingSkillTraitChoices()} — a caller (an API/UI
     * layer) is expected to resolve each one, once the player picks, via {@code
     * AttributeAbilityService#grantCompetencyAbilityChoice} and/or {@code
     * AttributeAbilityService#grantSpecializationChoice}; this method itself never mutates
     * anything.
     */
    default List<SkillType> resolvePendingSkillTraitChoices(Collection<SkillType> trainedSkills) {
        return List.of();
    }

    /**
     * How many of {@link #resolvePendingSkillTraitChoices}' candidates the player may actually
     * resolve — the "até 3" in {@link GnoseAbility#DOMINIO_DO_CONHECIMENTO}'s own rules text,
     * against a candidate list of every Perícia the character knows. Empty means "one per
     * candidate, no cap of its own," which is what an ability like {@link
     * CharismaAbility#CHARME} owes ("de cada Perícia baseada em Carisma em que for treinado" —
     * every candidate it reports, by construction); only override on a constant whose rules
     * text names a number smaller than the candidate list it offers.
     *
     * <p>Deliberately <b>not</b> a function of the candidate list: a caller wanting the number
     * of choices actually owed takes {@code min} of this and that list's own size, which is
     * exactly what {@code AttributeAbilityGrantResult#getPendingSkillTraitChoiceLimit()}
     * already reports. Nothing enforces the cap at resolution time — {@code
     * AttributeAbilityService#grantSpecializationChoice} records whatever it's handed, same
     * unenforced-prerequisite restraint already applied to "Requer N Graduações"-style clauses
     * elsewhere in this core.
     */
    default OptionalInt resolvePendingSkillTraitChoiceLimit() {
        return OptionalInt.empty();
    }

    /**
     * Which {@link SkillTraitKind}s each of {@link #resolvePendingSkillTraitChoices}' entries
     * owes — {@link SkillTraitKind#SPECIALIZATION} alone for {@link
     * GnoseAbility#DOMINIO_DO_CONHECIMENTO}, both for {@link CharismaAbility#CHARME}, which
     * owes an Especialização <i>and</i> an Habilidade de Competência per Perícia. This is what
     * tells a caller which of {@code AttributeAbilityService#grantCompetencyAbilityChoice}/
     * {@code #grantSpecializationChoice} to call for an entry, without a per-ability switch of
     * its own. Empty by default, and expected to stay in lockstep with {@link
     * #resolvePendingSkillTraitChoices}: a constant that overrides one overrides the other.
     */
    default Set<SkillTraitKind> resolvePendingSkillTraitKinds() {
        return Set.of();
    }

    /**
     * Extra PM a Rest of restType recovers on top of {@code RestService}'s own Foco-times-
     * multiplier calculation — e.g. {@link FocusAbility#CANALIZADOR_DE_MANA}'s "Descansos
     * Verdadeiros Longos ou superiores permitem que você recupere +2PM adicionais." Zero by
     * default; only override on a constant whose rules text grants a Rest-tier-conditioned PM
     * bonus like this. Takes restType explicitly (rather than being a reflection-invoked
     * {@code @Modifier} method) because the bonus is conditioned on *which* Rest tier is being
     * applied, which a no-arg method has no way to see — same reasoning as {@code
     * SkillCompetencyAbility#resolveDamageBonus}/{@code #resolveAttackRollBonus} needing their
     * own explicit parameters instead of {@code @Modifier}. Summed by {@code
     * org.aventyrs.core.rest.RestService#getRecoveredMagicPoints} across {@code
     * Character#getAttributeAbilities()}.
     */
    default int resolveRestMagicPointsBonus(RestType restType) {
        return 0;
    }

    /**
     * Every {@link InitiativeBlessing} this Habilidade grants the moment its holder wins
     * initiative for their group — mirrors {@code org.aventyrs.core.ego.EgoAdvantage
     * #resolveInitiativeBlessings}'s own shape (see that method's javadoc for the full
     * mechanism, and {@code org.aventyrs.core.character.services.InitiativeBlessingService}
     * for how this is scanned alongside {@code EgoAdvantage}/{@code
     * org.aventyrs.core.skill.SkillCompetencyAbility}). Empty by default; only override on a
     * constant whose rules text grants a bonus specifically for winning initiative.
     */
    default List<InitiativeBlessing> resolveInitiativeBlessings() {
        return List.of();
    }
}
