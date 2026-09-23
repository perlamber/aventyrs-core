package org.aventyrs.core.magic;

import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.sheet.FrenzyMode;
import org.aventyrs.core.sheet.AreaDamage;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.effect.SpellEffect;
import org.aventyrs.core.effect.SpellEffectContext;
import org.aventyrs.core.effect.SpellEffectFactory;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.scene.ActiveAreaSpellEffect;
import org.aventyrs.core.skill.AbstractSkillInteraction;
import org.aventyrs.core.skill.dominiodomana.DominioDoManaInteraction;

import java.util.Optional;
import java.util.OptionalInt;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_SPELL_CAST_TARGET;
import static org.aventyrs.core.util.TranslatableMessages.NO_ALTERNATE_SPELL_VERSION;
import static org.aventyrs.core.util.TranslatableMessages.SPELL_CASTING_PREVENTED;

public class SpellCastingServiceImpl implements SpellCastingService {

    /** The highest face a d6 shows — what a maximised die deals. */
    private static final int MAXIMUM_D6_FACE = 6;

    /** The floor a reduced Tempo de Ativação stops at. */
    private static final int MINIMUM_ACTION_POINTS = 1;

    private final Interaction<CombatantSheet> dominioDoManaInteraction;
    private final AbstractSkillInteraction dominioDoManaContextInteraction;
    private final SpellDurationService spellDurationService;

    public SpellCastingServiceImpl() {
        this(new DominioDoManaInteraction(), new SpellDurationServiceImpl());
    }

    public SpellCastingServiceImpl(final Interaction<CombatantSheet> dominioDoManaInteraction) {
        this.dominioDoManaInteraction = dominioDoManaInteraction;
        this.dominioDoManaContextInteraction = dominioDoManaInteraction instanceof AbstractSkillInteraction interaction
                ? interaction
                : new DominioDoManaInteraction();
        this.spellDurationService = new SpellDurationServiceImpl();
    }

    public SpellCastingServiceImpl(final AbstractSkillInteraction dominioDoManaInteraction,
                                   final SpellDurationService spellDurationService) {
        this.dominioDoManaInteraction = dominioDoManaInteraction;
        this.dominioDoManaContextInteraction = dominioDoManaInteraction;
        this.spellDurationService = spellDurationService;
    }

    @Override
    public SpellCastingResult castSpell(final SpellCastRequest request) {
        // The version is resolved first, and everything downstream uses it: an Efeito Alternativo
        // may state its own Alcance, so validating the request against the base version's reach
        // would refuse a cast the second version permits (Procrastinar Ferimento is Distância
        // Curta where Aliviar a Dor is Pessoal).
        Spell spell = resolveVersion(request);
        validateRequest(request, spell);

        InteractionResult deliveryResult = spell.getAttackSkillType().newInteraction()
                .applyTo(request.getCaster(), request.getSceneContext(), null, request.getCombatantTarget(),
                        spell);
        InteractionResult dominioDoManaResult = withOffensiveCastingAdvantage(
                dominioDoManaContextInteraction.applyTo(request.getCaster(), request.getSceneContext()),
                request.getCaster(), spell);
        SpellEmpowerment empowerment = consumeEmpowerment(request.getCaster());
        OptionalInt durationInRounds = extendDuration(spellDurationService.resolveDurationInRounds(spell,
                request.getCaster().getCharacter(),
                request.getCombatantTarget() == null ? null : request.getCombatantTarget().getCharacter()),
                spell, empowerment);
        ActiveAreaSpellEffect areaSpellEffect = registerAreaSpellEffect(request, spell, durationInRounds);
        int castingDifficultyReduction = resolveCastingDifficultyReduction(spell, request.getCaster());
        DifficultyLevel authoredDifficulty = spell.getCastingDifficultyLevel();

        return SpellCastingResult.builder()
                .deliveryResult(deliveryResult)
                .dominioDoManaResult(dominioDoManaResult)
                .castingDifficultyReduction(castingDifficultyReduction)
                .castingDifficultyLevel(authoredDifficulty == null ? null
                        : authoredDifficulty.easier(castingDifficultyReduction))
                .activationTime(surcharged(resolveActivationTime(spell, request.getCaster(),
                        request.getScene().getCurrentRound()), empowerment))
                .durationInRounds(durationInRounds.isPresent() ? durationInRounds.getAsInt() : null)
                .areaSpellEffect(areaSpellEffect)
                .primaryDamage(resolvePrimaryDamage(spell, request.getCaster())
                        .map(damage -> empowered(damage, empowerment)).orElse(null))
                .empowerment(empowerment)
                .mustOvercomeMagicDefense(mustOvercomeMagicDefense(request))
                .areaDamage(spell.getDuration() != null && spell.getDuration().kind() == DurationKind.INSTANTANEA
                        ? AreaDamage.cataclysm(request.getCaster(), request.getSceneContext())
                        : null)
                .spellEffect(resolveEffect(spell, SpellEffectContext.of(isHostileTarget(request)))
                        .orElse(null))
                .recordedAction(recordedAction(request, spell, deliveryResult))
                .build();
    }

    /**
     * The version this request casts — the Magia's {@code Efeito Alternativo} when it asked for
     * one, otherwise the Magia itself.
     *
     * @throws IllegalOperationException when the request asks for a second version of a Magia that
     *                                   has none
     */
    private Spell resolveVersion(final SpellCastRequest request) {
        if (!request.isUseAlternateVersion()) {
            return request.getSpell();
        }
        return request.getSpell().getAlternateVersion()
                .orElseThrow(() -> new IllegalOperationException(NO_ALTERNATE_SPELL_VERSION));
    }

    @Override
    public Optional<SpellEffect> resolveEffect(final Spell spell, final SpellEffectContext context) {
        return SpellEffectFactory.create(spell, context);
    }

    /**
     * Whether the request's single named target counts as an enemy of the caster, for a Magia that
     * gives hostiles less ({@code VidaSpell#NOVA_REJUVENESCEDORA}). Read off the caster's own
     * {@code SceneContext}, the only sub-group information in reach.
     *
     * <p>A cast with no named target is not hostile: an Área de Efeito names none, and its real
     * per-target answer is the caller's to give — this core resolves no footprint, so it builds
     * the effect for the primary target alone and a caller sweeping an area constructs its own
     * per target via {@link #resolveEffect}.
     */
    private boolean isHostileTarget(final SpellCastRequest request) {
        return request.getCombatantTarget() != null
                && (request.getSceneContext() != null
                        && request.getSceneContext().getEnemies().contains(request.getCombatantTarget())
                    || mustOvercomeMagicDefense(request));
    }

    /**
     * Fanático de Cyt: a Magia cast on the Fanático by anyone else counts as hostile, and a
     * beneficial one must overcome their DM to land — so the caller rolls it as an attack against DM.
     */
    private static boolean mustOvercomeMagicDefense(final SpellCastRequest request) {
        CombatantSheet target = request.getCombatantTarget();
        return target != null && target != request.getCaster() && target.treatsBeneficialSpellsAsHostile();
    }

    /**
     * Frenesi Esmeralda: "Vantagem em Rolagens de … Conjuração de Magias Ofensivas (que inflijam danos
     * em seus alvos)" — the flat +{@value Skill#ADVANTAGE_BONUS} on the Domínio do Mana roll of a Magia
     * that deals damage.
     */
    private static InteractionResult withOffensiveCastingAdvantage(final InteractionResult dominioDoMana,
                                                                   final CombatantSheet caster, final Spell spell) {
        boolean esmeralda = caster.getFrenzy().map(frenzy -> frenzy.hasMode(FrenzyMode.FRENESI_ESMERALDA))
                .orElse(false);
        if (!esmeralda || spell.getPrimaryDamage().isEmpty() || dominioDoMana.getSkillRollBonus() == null) {
            return dominioDoMana;
        }
        return dominioDoMana.toBuilder()
                .skillRollBonus(dominioDoMana.getSkillRollBonus() + Skill.ADVANTAGE_BONUS)
                .build();
    }

    /** Spends a pending Frenesi Arcano option, if the caster holds one. */
    private static SpellEmpowerment consumeEmpowerment(final CombatantSheet caster) {
        for (SpellEmpowerment empowerment : SpellEmpowerment.values()) {
            if (caster.consumeEnhancedAttack(empowerment)) {
                return empowerment;
            }
        }
        return null;
    }

    /** Frenesi Arcano (Duração): "+2 Rodadas" on an Encantamento or Maldição with a Duração in Rodadas. */
    private static OptionalInt extendDuration(final OptionalInt rounds, final Spell spell,
                                              final SpellEmpowerment empowerment) {
        boolean enchantmentOrCurse = spell.getPrimaryType() == MagicType.ENCANTAMENTO
                || spell.getPrimaryType() == MagicType.MALDICAO
                || spell.getSecondaryType() == MagicType.ENCANTAMENTO
                || spell.getSecondaryType() == MagicType.MALDICAO;
        if (empowerment != SpellEmpowerment.DURACAO || !enchantmentOrCurse || rounds.isEmpty()) {
            return rounds;
        }
        return OptionalInt.of(rounds.getAsInt() + SpellEmpowerment.DURATION_BONUS);
    }

    /** Frenesi Arcano (Dano): +1d6, or the dice maximised once that would pass 3d6. */
    private static ResolvedSpellDamage empowered(final ResolvedSpellDamage damage, final SpellEmpowerment empowerment) {
        if (empowerment != SpellEmpowerment.DANO) {
            return damage;
        }
        if (damage.diceCount() + 1 <= SpellEmpowerment.MAXIMUM_DICE) {
            return new ResolvedSpellDamage(damage.deterministicAmount(), damage.diceCount() + 1,
                    damage.damageType(), damage.elementalType(), damage.focusFullyApplied());
        }
        return new ResolvedSpellDamage(damage.deterministicAmount() + damage.diceCount() * MAXIMUM_D6_FACE, 0,
                damage.damageType(), damage.elementalType(), damage.focusFullyApplied());
    }

    /** The cast's Tempo de Ativação plus Frenesi Arcano's "+3PA", when this cast spends it. */
    private static ActivationTime surcharged(final ActivationTime time, final SpellEmpowerment empowerment) {
        if (empowerment == null || time == null || time.type() != ActivationType.PONTOS_DE_ACAO) {
            return time;
        }
        return ActivationTime.pa(time.actionPoints() + SpellEmpowerment.ACTION_POINT_SURCHARGE);
    }

    @Override
    public Optional<ResolvedSpellDamage> resolvePrimaryDamage(final Spell spell, final CombatantSheet caster) {
        return spell.getPrimaryDamage().map(damage -> resolve(damage, caster));
    }

    @Override
    public int resolveCastingDifficultyReduction(final Spell spell, final CombatantSheet caster) {
        Character character = caster.getCharacter();
        return character.getFeats().stream()
                .mapToInt(feat -> feat.resolveCastingDifficultyReduction(spell, character))
                .sum();
    }

    @Override
    public ActivationTime resolveActivationTime(final Spell spell, final CombatantSheet caster,
                                                final int currentRound) {
        ActivationTime authored = spell.getActivationTime();
        if (authored == null || authored.type() != ActivationType.PONTOS_DE_ACAO) {
            return authored;
        }
        Character character = caster.getCharacter();
        int reduction = character.getFeats().stream()
                .mapToInt(feat -> feat.resolveCastingActionPointReduction(spell, character, currentRound,
                        caster.getActionsThisRound()))
                .sum();
        // "(mínimo 1PA)", as MetamagicoFeat#PROCRASTINAR_CONJURACAO states — and an
        // ActivationTime of 0PA is not constructible anyway.
        ActivationTime reduced = reduction <= 0 ? authored
                : ActivationTime.pa(Math.max(MINIMUM_ACTION_POINTS, authored.actionPoints() - reduction));
        // Titã Enlouquecido: "Conjurar Magias … tem o tempo de ação aumentado em +1PA".
        int surcharge = caster.getConcentrationActionPointSurcharge();
        return surcharge == 0 ? reduced : ActivationTime.pa(reduced.actionPoints() + surcharge);
    }

    private ResolvedSpellDamage resolve(final SpellDamage damage, final CombatantSheet caster) {
        int focusTotal = caster.getCharacter().getEffectiveAttributeTotal(AttributeDomain.FOCUS);
        boolean upgradeHalfToFull = isFirstSpellCastOfRound(caster)
                && caster.getCharacter().getAttributeAbilities().stream()
                        .anyMatch(AttributeAbility::upgradesFirstSpellOfRoundFocusScaling);

        int focusContribution = switch (damage.focusScaling()) {
            case NONE -> 0;
            case FULL -> focusTotal;
            case HALF -> upgradeHalfToFull ? focusTotal : focusTotal / 2;
        };
        boolean focusFullyApplied = switch (damage.focusScaling()) {
            case NONE -> false;
            case FULL -> true;
            case HALF -> upgradeHalfToFull;
        };

        return new ResolvedSpellDamage(damage.flatBonus() + focusContribution, damage.diceCount(),
                damage.damageType(), damage.elementalType(), focusFullyApplied);
    }

    /**
     * Bundles the cast as a ready-to-file {@link CombatantAction} for {@link
     * SpellCastingResult#getRecordedAction()} — the {@link Spell} as {@code attackSource} is the
     * part {@code isFirstSpellCastOfRound} reads back once this is recorded. Partial: no roll is
     * supplied to {@code castSpell}, so the governing domain and verdict stay unset. Not recorded
     * here — the caller files it via {@code scene.recordAction(caster, action)}.
     */
    private CombatantAction recordedAction(final SpellCastRequest request, final Spell spell,
                                           final InteractionResult deliveryResult) {
        return new CombatantAction(
                spell.getAttackSkillType(),
                deliveryResult.getGoverningAttributeDomain(),
                spell,
                null,
                request.getScene().getCurrentRound(),
                null);
    }

    private static boolean isFirstSpellCastOfRound(final CombatantSheet caster) {
        return caster.getActionsThisRound().stream()
                .noneMatch(action -> action.attackSource() instanceof Spell);
    }

    @Override
    public SpellCastingResult castSpell(final CombatantSheet target, final Interaction<CombatantSheet> deliveryInteraction) {
        InteractionResult deliveryResult = target.receiveInteraction(deliveryInteraction);
        InteractionResult dominioDoManaResult = target.receiveInteraction(dominioDoManaInteraction);
        return SpellCastingResult.builder()
                .deliveryResult(deliveryResult)
                .dominioDoManaResult(dominioDoManaResult)
                .build();
    }

    /**
     * @param spell the version actually being cast — see {@link #resolveVersion}. Its own Alcance
     *              is what the target shape below is judged against, never the base version's.
     */
    private void validateRequest(final SpellCastRequest request, final Spell spell) {
        // Silêncio: "não podem Conjurar Magias". Refused rather than resolved-and-discarded,
        // since casting spends Pontos de Mana — the caster must not pay for a Magia that
        // cannot happen.
        if (request.getCaster().isSpellCastingPrevented(request.getSceneContext())) {
            throw new org.aventyrs.core.sheet.IllegalOperationException(SPELL_CASTING_PREVENTED);
        }
        if (!request.getScene().getAllParticipants().contains(request.getCaster())
                || request.getCombatantTarget() != null
                && !request.getScene().getAllParticipants().contains(request.getCombatantTarget())) {
            throw new org.aventyrs.core.sheet.IllegalOperationException(INVALID_SPELL_CAST_TARGET);
        }

        SpellTargeting targeting = spell.getTargeting();
        boolean hasCombatantTarget = request.getCombatantTarget() != null;
        boolean hasPositionTarget = request.getPositionTarget() != null;
        boolean validTarget = switch (targeting.reach()) {
            case PESSOAL, PLANAR -> !hasCombatantTarget && !hasPositionTarget;
            case TOQUE, DISTANCIA -> hasCombatantTarget && !hasPositionTarget;
            case AREA_DE_EFEITO -> !hasCombatantTarget
                    && (targeting.isCenteredOnCaster() ? !hasPositionTarget : hasPositionTarget);
        };
        if (!validTarget) {
            throw new org.aventyrs.core.sheet.IllegalOperationException(INVALID_SPELL_CAST_TARGET);
        }
    }

    private ActiveAreaSpellEffect registerAreaSpellEffect(final SpellCastRequest request, final Spell spell,
                                                           final OptionalInt durationInRounds) {
        if (!spell.getTargeting().isAreaOfEffect()
                || durationInRounds.isEmpty()
                || durationInRounds.getAsInt() == 0 && !spell.getDuration().concentration()) {
            return null;
        }
        ActiveAreaSpellEffect effect = new ActiveAreaSpellEffect(spell, request.getCaster(),
                request.getPositionTarget(), durationInRounds.getAsInt());
        request.getScene().addAreaSpellEffect(effect);
        return effect;
    }
}
