package org.aventyrs.core.magic;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
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
                .build();
    }

    @Override
    public Optional<ResolvedSpellDamage> resolvePrimaryDamage(final Spell spell, final CombatantSheet caster) {
        return spell.getPrimaryDamage().map(damage -> resolve(damage, caster));
    }

    private ResolvedSpellDamage resolve(final SpellDamage damage, final CombatantSheet caster) {
        int focusTotal = caster.getCharacter().getAttributes().getAttribute(AttributeDomain.FOCUS).getTotal();
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
