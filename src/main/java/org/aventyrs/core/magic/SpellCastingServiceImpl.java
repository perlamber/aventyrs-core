package org.aventyrs.core.magic;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.effect.ConditionCleansingEffect;
import org.aventyrs.core.effect.SpellEffect;
import org.aventyrs.core.effect.SpellHealingEffect;
import org.aventyrs.core.rest.RestService;
import org.aventyrs.core.rest.RestServiceImpl;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.scene.ActiveAreaSpellEffect;
import org.aventyrs.core.skill.AbstractSkillInteraction;
import org.aventyrs.core.skill.dominiodomana.DominioDoManaInteraction;

import java.util.Optional;
import java.util.OptionalInt;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_SPELL_CAST_TARGET;
import static org.aventyrs.core.util.TranslatableMessages.SPELL_CASTING_PREVENTED;

public class SpellCastingServiceImpl implements SpellCastingService {

    private final Interaction<CombatantSheet> dominioDoManaInteraction;
    private final AbstractSkillInteraction dominioDoManaContextInteraction;
    private final SpellDurationService spellDurationService;

    /** Supplies a healing Magia's "como se passasse por um Descanso X" figure — see {@link SpellHealing}. */
    private final RestService restService;

    public SpellCastingServiceImpl() {
        this(new DominioDoManaInteraction(), new SpellDurationServiceImpl());
    }

    public SpellCastingServiceImpl(final Interaction<CombatantSheet> dominioDoManaInteraction) {
        this.dominioDoManaInteraction = dominioDoManaInteraction;
        this.dominioDoManaContextInteraction = dominioDoManaInteraction instanceof AbstractSkillInteraction interaction
                ? interaction
                : new DominioDoManaInteraction();
        this.spellDurationService = new SpellDurationServiceImpl();
        this.restService = new RestServiceImpl();
    }

    public SpellCastingServiceImpl(final AbstractSkillInteraction dominioDoManaInteraction,
                                   final SpellDurationService spellDurationService) {
        this(dominioDoManaInteraction, spellDurationService, new RestServiceImpl());
    }

    public SpellCastingServiceImpl(final AbstractSkillInteraction dominioDoManaInteraction,
                                   final SpellDurationService spellDurationService,
                                   final RestService restService) {
        this.dominioDoManaInteraction = dominioDoManaInteraction;
        this.dominioDoManaContextInteraction = dominioDoManaInteraction;
        this.spellDurationService = spellDurationService;
        this.restService = restService;
    }

    @Override
    public SpellCastingResult castSpell(final SpellCastRequest request) {
        validateRequest(request);

        InteractionResult deliveryResult = request.getSpell().getAttackSkillType().newInteraction()
                .applyTo(request.getCaster(), request.getSceneContext(), null, request.getCombatantTarget(),
                        request.getSpell());
        InteractionResult dominioDoManaResult = dominioDoManaContextInteraction.applyTo(request.getCaster(),
                request.getSceneContext());
        OptionalInt durationInRounds = spellDurationService.resolveDurationInRounds(request.getSpell(),
                request.getCaster().getCharacter(),
                request.getCombatantTarget() == null ? null : request.getCombatantTarget().getCharacter());
        ActiveAreaSpellEffect areaSpellEffect = registerAreaSpellEffect(request, durationInRounds);

        return SpellCastingResult.builder()
                .deliveryResult(deliveryResult)
                .dominioDoManaResult(dominioDoManaResult)
                .durationInRounds(durationInRounds.isPresent() ? durationInRounds.getAsInt() : null)
                .areaSpellEffect(areaSpellEffect)
                .primaryDamage(resolvePrimaryDamage(request.getSpell(), request.getCaster()).orElse(null))
                .spellEffect(resolveEffect(request.getSpell(), isHostileTarget(request)).orElse(null))
                .recordedAction(recordedAction(request, deliveryResult))
                .build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Healing is checked before cleansing because no authored Magia does both — a branch either
     * restores PV or lifts Malefícios. Should one ever do both, this is the line that has to
     * become a chain rather than a choice.
     */
    @Override
    public Optional<SpellEffect> resolveEffect(final Spell spell, final boolean hostileTarget) {
        Optional<SpellHealing> healing = spell.getHealing();
        if (healing.isPresent()) {
            return Optional.of(new SpellHealingEffect(spell, healing.get(), hostileTarget, restService));
        }
        if (!spell.getCleansedConditions().isEmpty()) {
            return Optional.of(new ConditionCleansingEffect(spell, spell.getCleansedConditions()));
        }
        return Optional.empty();
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
                && request.getSceneContext() != null
                && request.getSceneContext().getEnemies().contains(request.getCombatantTarget());
    }

    @Override
    public Optional<ResolvedSpellDamage> resolvePrimaryDamage(final Spell spell, final CombatantSheet caster) {
        return spell.getPrimaryDamage().map(damage -> resolve(damage, caster));
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
    private CombatantAction recordedAction(final SpellCastRequest request, final InteractionResult deliveryResult) {
        return new CombatantAction(
                request.getSpell().getAttackSkillType(),
                deliveryResult.getGoverningAttributeDomain(),
                request.getSpell(),
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

    private void validateRequest(final SpellCastRequest request) {
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

        SpellTargeting targeting = request.getSpell().getTargeting();
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

    private ActiveAreaSpellEffect registerAreaSpellEffect(final SpellCastRequest request,
                                                           final OptionalInt durationInRounds) {
        if (!request.getSpell().getTargeting().isAreaOfEffect()
                || durationInRounds.isEmpty()
                || durationInRounds.getAsInt() == 0 && !request.getSpell().getDuration().concentration()) {
            return null;
        }
        ActiveAreaSpellEffect effect = new ActiveAreaSpellEffect(request.getSpell(), request.getCaster(),
                request.getPositionTarget(), durationInRounds.getAsInt());
        request.getScene().addAreaSpellEffect(effect);
        return effect;
    }
}
