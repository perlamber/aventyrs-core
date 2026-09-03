package org.aventyrs.core.skill;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.ability.PeritoTeoricoAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.services.CharacterSizeService;
import org.aventyrs.core.character.services.CharacterSizeServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.character.services.CharacterSkillServiceImpl;
import org.aventyrs.core.ego.EgoAdvantage;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.skill.artes.ArtesInteraction;
import org.aventyrs.core.skill.dominiodomana.DominioDoManaInteraction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.aventyrs.core.skill.Skill.UNTRAINED_PENALTY;
import static org.aventyrs.core.util.TranslatableMessages.REQUIRED_SKILL_TRAIT_NOT_HELD;

/**
 * The {@code applyTo}/{@code findCharacterSkill} machinery every {@code <Skill>Interaction}
 * needs, factored out into one place — see CLAUDE.md's "Adding a new Perícia" checklist item
 * 5. Every concrete subclass (e.g. {@link ArtesInteraction}) was, before this class existed,
 * an almost byte-for-byte copy of every other one, varying only by which {@link SkillType} it
 * targeted; that's now the *only* thing a subclass supplies, via its constructor.
 *
 * <p>Computes {@code skillRollBonus} from {@link CharacterSkillService#getValueForRoll} (using
 * whichever Attribute currently governs this Perícia — its own, a fixed-Perícia substitution
 * (see {@link SkillCompetencyAbility#resolveAttributeDomain}), or the per-{@link SkillType}
 * fixed-constant substitution granted by {@link org.aventyrs.core.ability.PeritoTeoricoAbility}
 * (see {@link PeritoTeoricoAbility#resolveAttributeDomain}, consulted first and fed in as the
 * fixed-Perícia resolution's own default, so a {@code SkillCompetencyAbility} substitution
 * still wins if one somehow also targets the same Perícia); both resolutions are safe to run
 * unconditionally, since each is a no-op — falls back to the Perícia's own Attribute — for
 * every skillType with no substituting ability/constant held) plus four sources, each summed
 * for *both* {@code
 * ModifierType#SKILL_ROLL_BONUS} (applies to every Perícia's roll) and {@code
 * skillType.getRollBonusType()} (applies only to this one) — see {@link
 * #sumSkillRollBonusModifiers}: {@code attributeAbilities}, {@code skillCompetencyAbilities}
 * *plus* {@code character.getRace().getRacialAbilities()} (see {@link #allSkillCompetencyAbilities}
 * and CLAUDE.md's "Racial Abilities reuse SkillCompetencyAbility" section — a racial ability
 * like {@code ElfosRacialAbility#SENTIDOS_ABSOLUTOS} contributes here identically to an
 * acquired one), unlocked {@link SkillExcellency} tiers, and the target {@link CombatantSheet}'s
 * own {@code TemporaryBonus} pool (see CLAUDE.md's "Temporary bonuses from other Characters"
 * section) — plus a separate source summed via {@link #sumConditionalRollBonuses}: {@link
 * SkillCompetencyAbility#resolveConditionalRollBonus}, for a bonus conditioned on {@code
 * sceneContext}/the roll's own requested trait rather than reflection-discoverable via
 * {@code @Modifier} (e.g. {@code AnoesRacialAbility#FILHOS_DA_MONTANHA}) — plus, via {@link
 * #sumEgoAdvantageRollBonuses}, every held {@code character.getEgoAdvantages()}' own {@link
 * org.aventyrs.core.ego.EgoAdvantage#resolveConditionalRollBonus} (e.g. {@code
 * InitiativeAdvantage#IMPETO}'s Vantagem during a Cena de Combate's first two Rounds) — plus,
 * via {@link #sumEgoAdvantageSkillSpecificRollBonuses}, every held {@code EgoAdvantage}'s own
 * {@link org.aventyrs.core.ego.EgoAdvantage#resolveSkillSpecificRollBonus} (e.g. {@code
 * ResourcesAdvantage#MORAL_HERDADA}'s bonus scoped to just Artes and Persuasão, unlike {@code
 * resolveConditionalRollBonus}'s every-skill scope) — plus,
 * via {@link #sizeCategoryRollBonus}, whichever {@link SizeCategory} modifier this Perícia is
 * affected by (resolved through {@link CharacterSizeService#getEffectiveSizeCategory}, so a
 * size-shifting ability like Sangue de Gigante is reflected here too): Ataque à Distância/
 * Ataque Corpo-a-Corpo use {@link SizeCategory#getAttackAndDamageModifier()}, Atenção/
 * Furtividade use {@link SizeCategory#getStealthAndAttentionModifier()}, and Esquiva e Aparar
 * uses {@link SizeCategory#getDefenseModifier()} — every other Perícia gets 0 — plus, via
 * {@link #sumFirstRollOfTurnBonuses}, every held {@code AttributeAbility}'s own {@link
 * AttributeAbility#resolveFirstRollOfTurnBonus} (e.g. {@code DexterityAbility#PRECISAO}'s
 * Vantagem), but only once {@link CombatantSheet#isFirstRollOfTurnFor} confirms this
 * actual roll (skillRoll non-{@code null}) is target's first this Turn governed by whichever
 * Attribute domain is resolved above — a non-mutating read; the API records the action
 * afterwards via {@code CombatantSheet#recordAction}. Computes {@code
 * difficultyReduction} from unlocked {@code SkillExcellency} tiers
 * plus every entry of that same combined acquired-plus-racial list's own {@link
 * SkillCompetencyAbility#getDifficultyReduction()}. {@link SkillCompetencyAbility
 * #resolveAttributeDomain} is resolved against the combined list too, for the same reason.
 * For an attack-skill roll ({@link SkillType#isAttackSkill()}), also sets {@link
 * InteractionResult#getDamageBonus()} from the first non-empty {@link
 * org.aventyrs.core.ego.EgoAdvantage#resolveDamageBonus} across the same held Vantagens de Ego
 * (see {@link #resolveEgoAdvantageDamageBonus}) — the {@code EgoAdvantage} counterpart to
 * {@code AtaqueADistanciaInteraction}'s own {@code SkillCompetencyAbility}-based wiring for
 * {@code AtaqueADistanciaCompetencyAbility#FRIEZA}, resolved here generically instead since no
 * {@code EgoAdvantage} granting this needs an explicit {@code attackTarget}.
 *
 * <p>{@code applyTo} has five overloads, each just delegating down to the next one with
 * {@code null} for the newly-added parameter — {@code applyTo(target)} → {@code +sceneContext}
 * → {@code +skillRoll} → {@code +attackTarget} → {@code +attackSource}, the last one holding
 * all the real logic. A subclass with something genuinely skill-specific to add overrides the
 * **longest** overload (not a shorter one — {@link ArtesInteraction} overrides the 5-arg one
 * even though it touches neither {@code attackTarget} nor {@code attackSource}, simply because
 * that's where {@code applyTo}'s real logic lives, and an override placed any higher would be
 * skipped by a caller using a longer form) and calls {@code
 * super.applyTo(...)} first, then layers its own addition on top of the result — most skills
 * need nothing extra (e.g. {@link DominioDoManaInteraction}'s "this is always the second of
 * two rolls" note is documentation, not behavior). Every shorter overload still reaches a
 * subclass's override correctly through ordinary virtual dispatch, so callers using
 * {@code applyTo(target)} keep working unchanged no matter which overload a subclass defines
 * its logic on. Three subclasses currently override: {@link ArtesInteraction} (below), {@code EsquivaEApararInteraction} (its own 4-arg overload adding the DF/DM pool and the armour-Categoria Destreza penalty), and — through this class's own 4-arg {@code attackTarget} overload rather than an override — both Perícias de Ataque. {@link ArtesInteraction}'s own case: a character
 * holding {@code ArtesCompetencyAbility#DOM_BARDICO} gets {@code temporaryBonusModifierType}/
 * {@code temporaryBonusRounds}/{@code temporaryBonusScope} set on the result — see that class.
 *
 * <p>When {@code skillRoll} names a {@link SkillRoll#getRequestedAbility()}, the 3-arg
 * {@code applyTo} validates the target's Character actually holds it (and that it belongs to
 * this same {@code skillType}) before doing anything else, throwing {@link
 * IllegalOperationException} otherwise — this stops a caller from requesting a maneuver-style
 * {@link SkillCompetencyAbility} (checked against {@code character.getSkillCompetencyAbilities()}/
 * {@code character.getRace().getRacialAbilities()}) or a held {@link SkillSpecialization}
 * (checked against {@code characterSkill.getSpecializations()}) the character doesn't actually
 * have — see {@link #validateRequestedTrait}. A {@code null} requestedAbility (a plain roll, the
 * common case) skips this check entirely. When the validated trait is a {@link
 * SkillSpecialization}, the roll's reached {@link DifficultyLevel} is resolved via {@link
 * DifficultyLevel#reachedByAsExpert} instead of {@link DifficultyLevel#reachedBy} — the
 * Especialização's easier threshold finally has a real consumer.
 */
public abstract class AbstractSkillInteraction implements Interaction<CombatantSheet> {

    private final SkillType skillType;
    private final CharacterSkillService characterSkillService;
    private final ModifierResolver modifierResolver;
    private final CharacterSizeService characterSizeService;
    private final HitPointsService hitPointsService;

    protected AbstractSkillInteraction(final SkillType skillType) {
        this(skillType, new CharacterSkillServiceImpl(), new ModifierResolverImpl());
    }

    protected AbstractSkillInteraction(final SkillType skillType, final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        this.skillType = skillType;
        this.characterSkillService = characterSkillService;
        this.modifierResolver = modifierResolver;
        this.characterSizeService = new CharacterSizeServiceImpl(modifierResolver);
        this.hitPointsService = new HitPointsServiceImpl();
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        return applyTo(target, null);
    }

    /**
     * Same as {@link #applyTo(CombatantSheet)}, but also given sceneContext — nearby allies/
     * enemies and their {@code Range} — for a subclass whose bonus is conditioned on
     * proximity (e.g. {@code MedicinaECuraExcellency#FOCADO}'s "se não tiver inimigos
     * próximos") to consult by overriding this method (or the 3-arg one below) instead of the
     * 1-arg one. sceneContext may be {@code null} (the 1-arg {@link #applyTo(CombatantSheet)}
     * always passes {@code null}) when the caller doesn't have one — e.g. no active {@code
     * Scene}, or this roll isn't happening in an encounter.
     */
    public InteractionResult applyTo(final CombatantSheet target, final SceneContext sceneContext) {
        return applyTo(target, sceneContext, null);
    }

    /**
     * Same as {@link #applyTo(CombatantSheet, SceneContext)}, but also given skillRoll — the
     * already-rolled dice behind this Perícia test (this core never rolls dice itself, see
     * the {@code skill} package-info). When non-{@code null}, sets {@link InteractionResult
     * #reachedDifficultyLevel} (via {@link DifficultyLevel#reachedBy} against {@code
     * skillRollBonus + skillRoll.getTotal()}) and {@link InteractionResult#criticalResult}
     * (from {@link SkillRoll#getCriticalResult(int)}, given the combined Margem Crítica Menor
     * widening summed via {@link #sumCriticalMarginIncrease} — see that method's own javadoc);
     * both stay {@code null} when skillRoll is {@code null} (the 1-/2-arg overloads always pass
     * {@code null}), same as every other not-applicable {@code InteractionResult} field. When
     * {@link CriticalResult
     * #isCriticalSuccess()} — the only outcome this can ever apply to, so every other
     * criticalResult skips the scan below entirely rather than walking every held {@code
     * AttributeAbility} for nothing — this also grants (directly on target, the same
     * unambiguous-recipient shape {@code org.aventyrs.core.effect.Primor} uses to mutate its
     * own target) a non-cumulative temporary Ego point, via {@link
     * CombatantSheet#grantTemporaryEgoPointBonus} — which raises that domain's temporary
     * <em>ceiling</em> rather than handing over a free-floating point — for every domain any
     * held {@code AttributeAbility}'s {@link org.aventyrs.core.ability.AttributeAbility
     * #resolveCriticalSuccessEgoGain} returns against this same criticalResult — passing that
     * ability itself as the grant's source, so one ability's own repeat triggers don't stack
     * past 1 point while an unrelated source's own grant still adds normally (e.g. {@code
     * CharismaAbility#DESTINO_FAVORAVEL} on {@link CriticalResult#ACERTO_CRITICO_MAIOR}). The
     * granted domains are also reported on {@link InteractionResult#egoGainDomains} for
     * visibility — stays {@code null} when no held ability grants one. A subclass whose bonus
     * is conditioned on
     * proximity *and* needs the roll's own result (none currently does) overrides this
     * 3-arg method and calls {@code super.applyTo(target, sceneContext, skillRoll)} first —
     * the same cascading-delegation shape as the 1-/2-arg overloads above, so a subclass only
     * ever needs to override the *one* method with everything it needs; the shorter overloads
     * still reach it correctly through ordinary virtual dispatch (see {@link ArtesInteraction},
     * which overrides this method, not the 2-arg one, even though it doesn't touch skillRoll
     * itself yet).
     */
    public InteractionResult applyTo(final CombatantSheet target, final SceneContext sceneContext, final SkillRoll skillRoll) {
        return applyTo(target, sceneContext, skillRoll, null);
    }

    /**
     * Same as {@link #applyTo(CombatantSheet, SceneContext, SkillRoll, CombatantSheet,
     * AttackSource)} with nothing known about how the attack is being delivered.
     */
    public InteractionResult applyTo(final CombatantSheet target, final SceneContext sceneContext, final SkillRoll skillRoll, final CombatantSheet attackTarget) {
        return applyTo(target, sceneContext, skillRoll, attackTarget, null);
    }

    /**
     * The overload that holds all the real logic — every shorter one delegates down to it with
     * {@code null} for the parameters it doesn't carry, so a subclass with something to add
     * overrides <b>this</b> one (see {@link ArtesInteraction}) and every shorter form still
     * reaches the override through ordinary virtual dispatch.
     *
     * <p>attackTarget is the combatant this attack is actually being made against, so a held
     * ability like {@code AtaqueADistanciaCompetencyAbility#FRIEZA} (conditioned on the target's
     * distance) or {@code AnoesRacialAbility#ABATEDORES_DE_GIGANTES} (on its {@link
     * SizeCategory}) can resolve against the real target rather than a generic fact about the
     * encounter. Neither is reachable from the shared scan, because a no-arg {@code @Modifier}
     * method can't see a target. That half lives here rather than on one Interaction because
     * the rules text covers every <b>Perícia de Ataque</b>, not just Ataque à Distância — it was
     * on {@code AtaqueADistanciaInteraction} only because that was the first one wired, leaving
     * Ataque Corpo a Corpo silently missing both bonuses.
     *
     * <p>attackSource is what the attack is being delivered <em>with</em> — see {@link
     * AttackSource}. It reaches {@link SkillCompetencyAbility#resolveAttributeDomain(
     * java.util.Collection, SkillType, AttributeDomain, AttackSource)} <b>before</b> the roll is
     * computed, which is the whole reason it's a parameter here rather than a field on {@link
     * SkillRoll} or something layered onto the result afterwards: the resolved {@link
     * AttributeDomain} feeds {@code getValueForRoll}, both {@code sumAttributeDomain*} scans and
     * the {@link CombatantSheet#isFirstRollOfTurnFor} check, and is reported on {@link
     * InteractionResult#getGoverningAttributeDomain()} so the API can build the matching {@code
     * CombatantAction}. Being a parameter also keeps the substitution visible on the bonuses-only
     * path, where no dice have been rolled yet.
     *
     * <p>Neither extra parameter is gated: a {@code null} attackSource, or one handed to a
     * non-attack Perícia, is harmless rather than an error — nothing about a delivery method or
     * a target is meaningful for an Atletismo roll, and refusing it would only push the check
     * onto callers.
     */
    public InteractionResult applyTo(final CombatantSheet target, final SceneContext sceneContext, final SkillRoll skillRoll, final CombatantSheet attackTarget, final AttackSource attackSource) {
        Character character = target.getCharacter();
        CharacterSkill characterSkill = findCharacterSkill(character);
        if (skillRoll != null) {
            validateRequestedTrait(character, characterSkill, skillRoll.getRequestedAbility());
        }
        int graduationValue = characterSkill.getGraduation().getGraduationValue();
        List<SkillCompetencyAbility> skillCompetencyAbilities = allSkillCompetencyAbilities(character);

        AttributeDomain naturalDomain = characterSkill.getSkill().getAttributeDomain();
        AttributeDomain peritoTeoricoDomain = PeritoTeoricoAbility.resolveAttributeDomain(character.getAttributeAbilities(), skillType, naturalDomain);
        AttributeDomain attributeDomain = SkillCompetencyAbility.resolveAttributeDomain(
                skillCompetencyAbilities, skillType, peritoTeoricoDomain, attackSource);

        int bonus = characterSkillService.getValueForRoll(characterSkill, character.getAttributes(), character.getRace(), attributeDomain);
        bonus += sumSkillRollBonusModifiers(character.getAttributeAbilities());
        bonus += sumSkillRollBonusModifiers(skillCompetencyAbilities);
        List<SkillExcellency> unlockedExcellencies = SkillExcellency.unlockedBy(skillType.getExcellencyClass(), graduationValue);
        bonus += sumSkillRollBonusModifiers(unlockedExcellencies);
        bonus += target.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS);
        bonus += target.getTemporaryBonus(skillType.getRollBonusType());
        // Malefício maluses — Caído/Agarrado's blanket Desvantagem, Desarmado's Ataque-only one,
        // and the fear ladder's, which applies only while within reach of the fear's origin.
        bonus += target.getConditionBonus(ModifierType.SKILL_ROLL_BONUS, sceneContext);
        bonus += target.getConditionBonus(skillType.getRollBonusType(), sceneContext);
        bonus += sumEquipmentRollBonuses(character);
        bonus += sumConditionalRollBonuses(skillCompetencyAbilities, sceneContext, skillRoll);
        bonus += sumFeatRollBonuses(target, sceneContext, skillRoll, attackSource);
        bonus += sumEgoAdvantageRollBonuses(character.getEgoAdvantages().values(), sceneContext);
        bonus += sumEgoAdvantageSkillSpecificRollBonuses(character.getEgoAdvantages().values(), sceneContext, target);
        bonus += sizeCategoryRollBonus(characterSizeService.getEffectiveSizeCategory(character));
        bonus += sumAttributeDomainRollBonuses(character.getAttributeAbilities(), attributeDomain, character);
        if (skillRoll != null && target.isFirstRollOfTurnFor(attributeDomain)) {
            bonus += sumFirstRollOfTurnBonuses(character.getAttributeAbilities(), attributeDomain);
        }

        int difficultyReduction = SkillExcellency.totalDifficultyReduction(skillType.getExcellencyClass(), graduationValue);
        difficultyReduction += skillCompetencyAbilities.stream()
                .mapToInt(SkillCompetencyAbility::getDifficultyReduction)
                .sum();
        difficultyReduction += sumAttributeDomainDifficultyReductions(character.getAttributeAbilities(), attributeDomain, character);
        difficultyReduction += sumFeatDifficultyReductions(character);
        if (skillRoll != null) {
            difficultyReduction += sumFeatAttackCostDifficultyReductions(character, sceneContext, attackSource,
                    skillRoll.getActionCost(), target.getActionsThisRound());
        }

        InteractionResult.InteractionResultBuilder result = InteractionResult.builder()
                .resultStatus(hitPointsService.getStatus(target))
                .skillRollBonus(bonus)
                .difficultyReduction(difficultyReduction)
                .governingAttributeDomain(skillRoll != null ? attributeDomain : null);

        if (skillType.isAttackSkill()) {
            sumDamageBonus(target, sceneContext, null).ifPresent(result::damageBonus);
        }

        if (skillRoll != null) {
            boolean expert = skillRoll.getRequestedAbility() instanceof SkillSpecialization;
            Optional<DifficultyLevel> reached = expert
                    ? DifficultyLevel.reachedByAsExpert(bonus + skillRoll.getTotal())
                    : DifficultyLevel.reachedBy(bonus + skillRoll.getTotal());
            int criticalMarginIncrease = sumCriticalMarginIncrease(target, skillCompetencyAbilities, sceneContext, attackSource);
            CriticalResult criticalResult = skillRoll.getCriticalResult(criticalMarginIncrease);
            result.reachedDifficultyLevel(reached.orElse(null))
                    .criticalResult(criticalResult);
            resolveOutcome(bonus + skillRoll.getTotal(), skillRoll.getTargetValue(), difficultyReduction,
                    skillCompetencyAbilities, sceneContext).ifPresent(outcome -> {
                        result.succeeded(outcome.succeeded()).margin(outcome.margin());
                        if (outcome.succeeded()) {
                            List<Blessing> earned = skillCompetencyAbilities.stream()
                                    .flatMap(ability -> ability.resolveSuccessBlessings(
                                            skillType, skillRoll.getRequestedAbility(), sceneContext).stream())
                                    .toList();
                            if (!earned.isEmpty()) {
                                result.blessings(earned);
                            }
                        }
                    });

            if (criticalResult.isCriticalSuccess()) {
                List<EgoDomain> egoGainDomains = new ArrayList<>();
                for (AttributeAbility ability : character.getAttributeAbilities()) {
                    for (EgoDomain domain : ability.resolveCriticalSuccessEgoGain(criticalResult)) {
                        target.grantTemporaryEgoPointBonus(domain, ability, 1);
                        if (!egoGainDomains.contains(domain)) {
                            egoGainDomains.add(domain);
                        }
                    }
                }
                if (!egoGainDomains.isEmpty()) {
                    result.egoGainDomains(List.copyOf(egoGainDomains));
                }
            }
        }

        return applyAttackTargetBonuses(result.build(), target, sceneContext, attackTarget);
    }

    /**
     * The attackTarget-conditioned half of the roll, layered onto an otherwise-complete result:
     * the first non-empty {@link SkillCompetencyAbility#resolveDamageBonus} ({@code FRIEZA}), and
     * the <em>sum</em> of {@link SkillCompetencyAbility#resolveAttackRollBonus} ({@code
     * ABATEDORES_DE_GIGANTES}) — summed rather than first-non-empty because, unlike a dano bonus,
     * more than one is expected to apply at once.
     *
     * <p>Unlike the substituted {@link AttributeDomain}, both of these genuinely can be applied
     * after the fact: neither feeds anything the main body already consumed. A non-attack skill
     * or a {@code null} attackTarget returns result untouched.
     */
    private InteractionResult applyAttackTargetBonuses(final InteractionResult built, final CombatantSheet target, final SceneContext sceneContext, final CombatantSheet attackTarget) {
        InteractionResult result = built;
        if (!skillType.isAttackSkill() || attackTarget == null) {
            return result;
        }
        List<SkillCompetencyAbility> abilities = allSkillCompetencyAbilities(target.getCharacter());

        // Recomputed rather than added to what the main body already put there: the same four
        // sources are scanned again, now with the real attackTarget, so a target-conditioned
        // bonus (FRIEZA) joins the sum instead of the whole total being replaced by it.
        Optional<DamageBonus> damageBonus = sumDamageBonus(target, sceneContext, attackTarget);
        if (damageBonus.isPresent()) {
            result = result.toBuilder().damageBonus(damageBonus.get()).build();
        }

        int attackRollBonus = abilities.stream()
                .map(ability -> ability.resolveAttackRollBonus(target, attackTarget))
                .flatMap(Optional::stream)
                .mapToInt(Integer::intValue)
                .sum();
        if (attackRollBonus != 0) {
            result = result.toBuilder().skillRollBonus(result.getSkillRollBonus() + attackRollBonus).build();
        }
        return result;
    }

    /**
     * A {@code null} requestedTrait (a plain roll) is always fine. A non-{@code null} one must
     * match this {@code skillType} (via {@link SkillTrait#matchesSkillType}, not raw equality —
     * see that method for why) and actually be held by the character — otherwise this roll is
     * trying to invoke a maneuver or Especialização the character never acquired.
     * Branches on the concrete {@link SkillTrait} kind: a {@link SkillCompetencyAbility} is
     * checked against the Character's own acquired/racial ability lists (it doesn't need
     * {@code characterSkill}); a {@link SkillSpecialization} is checked against {@code
     * characterSkill.getSpecializations()} instead. This only validates the character actually
     * holds the named trait — it doesn't validate that an Especialização is actually the right
     * fit for whatever the roll is being used for narratively; that judgment stays with the
     * caller, the same restraint this codebase already applies everywhere it doesn't track what
     * a roll is *for*.
     */
    private void validateRequestedTrait(final Character character, final CharacterSkill characterSkill, final SkillTrait requestedTrait) {
        if (requestedTrait == null) {
            return;
        }
        boolean held;
        if (requestedTrait instanceof SkillCompetencyAbility ability) {
            held = character.getSkillCompetencyAbilities().contains(ability)
                    || character.getRace().getRacialAbilities().contains(ability);
        } else if (requestedTrait instanceof SkillSpecialization specialization) {
            held = characterSkill.getSpecializations().contains(specialization);
        } else {
            held = false;
        }
        if (!requestedTrait.matchesSkillType(skillType) || !held) {
            throw new IllegalOperationException(REQUIRED_SKILL_TRAIT_NOT_HELD);
        }
    }

    /**
     * {@code character.getSkillCompetencyAbilities()} (acquired) plus {@code
     * character.getRace().getRacialAbilities()} (fixed per race) — see CLAUDE.md's "Racial
     * Abilities reuse SkillCompetencyAbility" section. Every place in this class that used to
     * scan only the acquired list now scans this combined one instead, so a racial ability
     * contributes to {@code skillRollBonus}/{@code difficultyReduction}/attribute substitution
     * exactly like an acquired one would, with no special-casing at any call site.
     */
    /**
     * Sums {@link Feat#resolveSkillRollBonus(SkillType, SceneContext, SkillTrait, Character,
     * AttackSource)} across every held Talento — the {@code Feat} counterpart to {@link
     * #sumConditionalRollBonuses}, and needed for the same reason {@link
     * #sumFeatDifficultyReductions} is: Talentos are outside every {@code ModifierResolver} scan,
     * so they get an explicit pass. Safe to call unconditionally — {@code sceneContext}/{@code
     * skillRoll}/{@code attackSource} may each be {@code null}, which every override reads as
     * "condition not met".
     */
    private int sumFeatRollBonuses(final CombatantSheet holder, final SceneContext sceneContext,
                                    final SkillRoll skillRoll, final AttackSource attackSource) {
        Character character = holder.getCharacter();
        SkillTrait requestedAbility = skillRoll == null ? null : skillRoll.getRequestedAbility();
        return character.getFeats().stream()
                .mapToInt(feat -> feat.resolveSkillRollBonus(
                        skillType, sceneContext, requestedAbility, character, attackSource, holder))
                .sum();
    }

    /** Equipment-held bonuses are data, so they have an explicit pass rather than a modifier scan. */
    private int sumEquipmentRollBonuses(final Character character) {
        return character.getEquipment().stream()
                .mapToInt(item -> item.resolveEnhancementBonus(skillType.getRollBonusType(), skillType, character))
                .sum();
    }

    /**
     * Sums {@link Feat#resolveDifficultyReduction} across every held Talento. Talentos are
     * outside every {@code ModifierResolver} scan (nothing scans them reflectively), so they get
     * an explicit pass here — the same shape {@code DefenseServiceImpl}/{@code
     * MovementServiceImpl} already use for their own {@code Feat} hooks.
     */
    private int sumFeatDifficultyReductions(final Character character) {
        return character.getFeats().stream()
                .mapToInt(feat -> feat.resolveDifficultyReduction(skillType, character))
                .sum();
    }

    /**
     * Sums {@link Feat#resolveAttackCostDifficultyReduction} across every held Talento — the
     * conditional GD hook scoped to an attack's Pontos de Ação cost and the Rodada's prior
     * actions ({@code AssassinoFeat#SAQUE_RELAMPAGO}). Only called when a {@code SkillRoll} was
     * supplied; {@code actionsThisRound} is read, never written — the API records the action
     * afterwards via {@code CombatantSheet#recordAction}.
     */
    private int sumFeatAttackCostDifficultyReductions(final Character character, final SceneContext sceneContext,
                                                      final AttackSource attackSource, final ActionCost actionCost,
                                                      final List<CombatantAction> actionsThisRound) {
        return character.getFeats().stream()
                .mapToInt(feat -> feat.resolveAttackCostDifficultyReduction(
                        skillType, sceneContext, character, attackSource, actionCost, actionsThisRound))
                .sum();
    }

    private List<SkillCompetencyAbility> allSkillCompetencyAbilities(final Character character) {
        return SkillCompetencyAbility.allFor(character);
    }

    /**
     * Sums {@code ModifierType#SKILL_ROLL_BONUS} (every Perícia's roll) *and* {@code
     * skillType.getRollBonusType()} (only this Perícia's) across sources — the fix for a
     * previous bug where {@code AbstractSkillInteraction} only ever summed the generic type,
     * so a bonus meant for one specific Perícia had no way to avoid leaking into every other
     * Perícia's roll too.
     */
    private int sumSkillRollBonusModifiers(final Collection<?> sources) {
        return modifierResolver.sumModifiers(sources, ModifierType.SKILL_ROLL_BONUS)
                + modifierResolver.sumModifiers(sources, skillType.getRollBonusType());
    }

    /**
     * Sums {@link SkillCompetencyAbility#resolveConditionalRollBonus} across every held
     * ability (acquired plus racial) — the additive counterpart to {@link
     * #sumSkillRollBonusModifiers} for a bonus conditioned on {@code sceneContext}/the roll's
     * own requested trait rather than a reflection-discoverable {@code @Modifier} method. Safe
     * to call unconditionally: {@code sceneContext}/{@code skillRoll} may each be {@code null}
     * (no active Scene, or a plain {@code applyTo(target)} call) — every ability's own override
     * is expected to treat that as "condition not met," the same restraint {@code
     * resolveDamageBonus}/{@code resolveAttackRollBonus} already apply.
     */
    private int sumConditionalRollBonuses(final List<SkillCompetencyAbility> skillCompetencyAbilities, final SceneContext sceneContext, final SkillRoll skillRoll) {
        SkillTrait requestedAbility = skillRoll == null ? null : skillRoll.getRequestedAbility();
        return skillCompetencyAbilities.stream()
                .map(ability -> ability.resolveConditionalRollBonus(sceneContext, requestedAbility))
                .flatMap(Optional::stream)
                .mapToInt(Integer::intValue)
                .sum();
    }

    /**
     * Sums {@link EgoAdvantage#resolveConditionalRollBonus} across every held Vantagem de Ego
     * (e.g. {@link org.aventyrs.core.ego.InitiativeAdvantage#IMPETO}) — the {@code EgoAdvantage}
     * counterpart to {@link #sumConditionalRollBonuses}: a Vantagem de Ego isn't tied to one
     * Perícia the way a {@code SkillCompetencyAbility} usually is, so this applies identically
     * for every skill's own {@code applyTo}, the same additive convention every other {@code
     * skillRollBonus} source already uses. Safe to call unconditionally: {@code sceneContext}
     * may be {@code null}, same restraint as {@link #sumConditionalRollBonuses}.
     */
    private int sumEgoAdvantageRollBonuses(final Collection<EgoAdvantage> egoAdvantages, final SceneContext sceneContext) {
        return egoAdvantages.stream()
                .map(advantage -> advantage.resolveConditionalRollBonus(sceneContext))
                .flatMap(Optional::stream)
                .mapToInt(Integer::intValue)
                .sum();
    }

    /**
     * Sums {@link EgoAdvantage#resolveSkillSpecificRollBonus} across every held Vantagem de Ego
     * — the skill-scoped counterpart to {@link #sumEgoAdvantageRollBonuses}, for a Vantagem
     * whose bonus (e.g. {@link org.aventyrs.core.ego.ResourcesAdvantage#MORAL_HERDADA}) only
     * applies to specific named skills rather than every Perícia. target is this Interaction's
     * own roller, passed through unconditionally — safe even for a Vantagem with no
     * skill-specific bonus, since it just falls through to {@code Optional.empty()}.
     */
    private int sumEgoAdvantageSkillSpecificRollBonuses(final Collection<EgoAdvantage> egoAdvantages, final SceneContext sceneContext, final CombatantSheet target) {
        return egoAdvantages.stream()
                .map(advantage -> advantage.resolveSkillSpecificRollBonus(skillType, sceneContext, target))
                .flatMap(Optional::stream)
                .mapToInt(Integer::intValue)
                .sum();
    }

    /**
     * Sums {@link AttributeAbility#resolveFirstRollOfTurnBonus} across every held Habilidade —
     * e.g. {@code DexterityAbility#PRECISAO}'s Vantagem on the first Destreza-based roll each
     * Turn. Only ever called once {@link CombatantSheet#isFirstRollOfTurnFor} has confirmed this
     * is target's first roll governed by rolledDomain this Turn (see that method's own javadoc
     * for the per-Turn slice of the action log it reads) — a constant overriding {@code
     * resolveFirstRollOfTurnBonus} doesn't need to check that condition itself.
     */
    private int sumFirstRollOfTurnBonuses(final Collection<AttributeAbility> attributeAbilities, final AttributeDomain rolledDomain) {
        return attributeAbilities.stream()
                .map(ability -> ability.resolveFirstRollOfTurnBonus(rolledDomain))
                .flatMap(Optional::stream)
                .mapToInt(Integer::intValue)
                .sum();
    }

    /**
     * Sums {@link AttributeAbility#resolveAttributeDomainRollBonus} across every held
     * Habilidade — e.g. {@code InstinctAbility#SENTIR_A_INTENCAO}'s Vantagem on every
     * Instinto-governed roll. Unlike {@link #sumFirstRollOfTurnBonuses}, called
     * unconditionally on every roll, not gated behind {@code CombatantSheet
     * #isFirstRollOfTurnFor}.
     */
    private int sumAttributeDomainRollBonuses(final Collection<AttributeAbility> attributeAbilities, final AttributeDomain rolledDomain, final Character character) {
        return attributeAbilities.stream()
                .mapToInt(ability -> ability.resolveAttributeDomainRollBonus(rolledDomain, character))
                .sum();
    }

    /**
     * Sums {@link AttributeAbility#resolveAttributeDomainDifficultyReduction} across every
     * held Habilidade — the GD-reduction counterpart to {@link #sumAttributeDomainRollBonuses}.
     */
    private int sumAttributeDomainDifficultyReductions(final Collection<AttributeAbility> attributeAbilities, final AttributeDomain rolledDomain, final Character character) {
        return attributeAbilities.stream()
                .mapToInt(ability -> ability.resolveAttributeDomainDifficultyReduction(rolledDomain, character))
                .sum();
    }

    /** One roll's verdict against the GD it was made for — see {@code InteractionResult#succeeded}. */
    private record RollOutcome(boolean succeeded, int margin) {
    }

    /**
     * Compares a roll's finished total against the Grau de Dificuldade it was made against,
     * returning empty when no target was stated — which is what leaves {@code
     * InteractionResult#succeeded} {@code null} rather than {@code false}.
     *
     * <p>difficultyReduction is denominated in <i>níveis</i> and is applied by easing the target
     * that many steps, not by subtracting it as a flat number — a nível is worth a different
     * number of points at each tier. The target is resolved back to a tier via {@link
     * DifficultyLevel#reachedBy} to do that; a target between tiers (a computed "GD 10+Vigor")
     * eases from whichever tier it reaches, and one below the easiest tier cannot be eased at
     * all. This mirrors {@code AttackReceiver#resolve}, which eases the attack's own {@code
     * DifficultyLevel} by the defender's reduction before deriving a threshold.
     *
     * <p>A tie succeeds: a total exactly equal to the target beats it, the same reading {@code
     * AttackReceiver} takes ("a tie is a successful defense").
     *
     * <p>An {@code resolveAutomaticSuccess} ability short-circuits the comparison entirely — see
     * that hook's own javadoc — and reports a margin of 0, since no roll needed to be beaten.
     */
    private Optional<RollOutcome> resolveOutcome(final int total, final Integer targetValue, final int difficultyReduction,
                                                  final List<SkillCompetencyAbility> abilities, final SceneContext sceneContext) {
        if (targetValue == null) {
            return Optional.empty();
        }
        int effectiveTarget = easedTarget(targetValue, difficultyReduction);
        boolean automatic = abilities.stream()
                .anyMatch(ability -> ability.resolveAutomaticSuccess(skillType, effectiveTarget, sceneContext));
        if (automatic) {
            return Optional.of(new RollOutcome(true, 0));
        }
        return Optional.of(new RollOutcome(total >= effectiveTarget, total - effectiveTarget));
    }

    /**
     * targetValue made easier by that many níveis. Resolves the number back to the tier it
     * reaches, steps that tier down, and takes the new tier's own base value; a target too low to
     * reach any tier has nothing to ease and is returned unchanged.
     */
    private int easedTarget(final int targetValue, final int difficultyReduction) {
        if (difficultyReduction <= 0) {
            return targetValue;
        }
        return DifficultyLevel.reachedBy(targetValue)
                .map(tier -> tier.easier(difficultyReduction).getBaseValue())
                .orElse(targetValue);
    }

    /**
     * Sums {@code resolveCriticalMarginIncrease} across all three ability sources this class
     * already scans for everything else — {@code character.getAttributeAbilities()} (e.g.
     * {@code DexterityAbility#LETALIDADE_PROGRESSIVA}), skillCompetencyAbilities (acquired plus
     * racial — e.g. {@code ArtesAprimorarComArteAbility}'s "Margem Crítica Menor" branch), and
     * {@code character.getEgoAdvantages()} (e.g. {@code SorteAdvantage#ACE}) — additively, the
     * same convention every other {@code skillRollBonus}-adjacent sum here already uses. Fed
     * into {@link SkillRoll#getCriticalResult(int)} only when skillRoll is non-{@code null}; safe
     * to call unconditionally otherwise since sceneContext being {@code null} is already handled
     * by every override the same way {@link #sumEgoAdvantageRollBonuses} already relies on.
     */
    private int sumCriticalMarginIncrease(final CombatantSheet holder, final List<SkillCompetencyAbility> skillCompetencyAbilities, final SceneContext sceneContext, final AttackSource attackSource) {
        Character character = holder.getCharacter();
        int total = character.getAttributeAbilities().stream()
                .mapToInt(ability -> ability.resolveCriticalMarginIncrease(skillType, sceneContext))
                .sum();
        total += skillCompetencyAbilities.stream()
                .mapToInt(ability -> ability.resolveCriticalMarginIncrease(skillType, sceneContext))
                .sum();
        total += character.getEgoAdvantages().values().stream()
                .mapToInt(advantage -> advantage.resolveCriticalMarginIncrease(skillType, sceneContext))
                .sum();
        // Talentos are outside every ModifierResolver scan, so they get an explicit fourth pass —
        // the same shape sumFeatRollBonuses/sumFeatDifficultyReductions already use.
        total += character.getFeats().stream()
                .mapToInt(feat -> feat.resolveCriticalMarginIncrease(skillType, sceneContext, character, attackSource, holder))
                .sum();
        return total;
    }

    /**
     * Every dano-roll contribution this roller currently has, summed into one {@link DamageBonus}
     * — see {@link DamageBonus#total}. Four sources, all additive:
     *
     * <ul>
     *   <li>{@code SkillCompetencyAbility#resolveDamageBonus} — e.g. {@code BRUTALIDADE}'s +1.</li>
     *   <li>{@code EgoAdvantage#resolveDamageBonus} — e.g. {@code InitiativeAdvantage#IMPETO}.</li>
     *   <li>{@code Feat#resolveDamageBonus} — Talentos sit outside every {@code ModifierResolver}
     *   scan, so they get their own pass, the same shape {@link #sumFeatRollBonuses} uses.</li>
     *   <li>{@link ModifierType#DAMAGE_ROLL_BONUS} on the sheet — a {@code TemporaryBonus} and any
     *   Condição in force (Caído/Desarmado's Desvantagem, the fear ladder's proximity-scoped one),
     *   which are {@code ModifierType}-typed data rather than typed {@code DamageBonus}es.</li>
     * </ul>
     *
     * <p>Only ever called for a Perícia de Ataque. attackTarget is {@code null} on the main-body
     * pass and real on the {@link #applyAttackTargetBonuses} one, so a target-conditioned bonus
     * simply doesn't contribute until there is a target to test.
     */
    private Optional<DamageBonus> sumDamageBonus(final CombatantSheet target, final SceneContext sceneContext, final CombatantSheet attackTarget) {
        Character character = target.getCharacter();
        List<DamageBonus> typed = new ArrayList<>();
        allSkillCompetencyAbilities(character).stream()
                .map(ability -> ability.resolveDamageBonus(skillType, sceneContext, attackTarget, character))
                .flatMap(Optional::stream)
                .forEach(typed::add);
        character.getEgoAdvantages().values().stream()
                .map(advantage -> advantage.resolveDamageBonus(sceneContext))
                .flatMap(Optional::stream)
                .forEach(typed::add);
        character.getFeats().stream()
                .map(feat -> feat.resolveDamageBonus(skillType, sceneContext, attackTarget, character))
                .flatMap(Optional::stream)
                .forEach(typed::add);
        int flat = target.getTemporaryBonus(ModifierType.DAMAGE_ROLL_BONUS)
                + target.getConditionBonus(ModifierType.DAMAGE_ROLL_BONUS, sceneContext);
        if (attackTarget != null) {
            // Outward-facing: what the *victim's* own Condições hand the attacker (Flanqueado).
            flat += attackTarget.getAttackerDamageBonusFromConditions(sceneContext);
        }
        return DamageBonus.total(typed, flat);
    }

    /**
     * The size-table modifier (see {@link SizeCategory}) this Perícia's roll is affected by,
     * resolved against the character's {@link CharacterSizeService#getEffectiveSizeCategory}
     * (its own {@code sizeCategory} plus any shifting ability, e.g. Sangue de Gigante) rather
     * than the raw {@code Character#getSizeCategory()} — the two Perícias de Ataque use {@link
     * SizeCategory#getAttackAndDamageModifier()}, Atenção and Furtividade use {@link
     * SizeCategory#getStealthAndAttentionModifier()}, and Esquiva e Aparar uses {@link
     * SizeCategory#getDefenseModifier()}; every other Perícia isn't affected by size at all.
     */
    private int sizeCategoryRollBonus(final SizeCategory sizeCategory) {
        if (skillType.isAttackSkill()) {
            return sizeCategory.getAttackAndDamageModifier();
        }
        if (skillType == SkillType.ATTENTION || skillType == SkillType.FURTIVIDADE) {
            return sizeCategory.getStealthAndAttentionModifier();
        }
        if (skillType == SkillType.ESQUIVA_E_APARAR) {
            return sizeCategory.getDefenseModifier();
        }
        return 0;
    }

    /**
     * The Character's own CharacterSkill for this Interaction's Perícia, or a fresh one
     * carrying {@link Skill#UNTRAINED_PENALTY} as its graduation if they never trained it.
     */
    private CharacterSkill findCharacterSkill(final Character character) {
        CharacterSkill trained = character.getSkills().get(skillType);
        if (trained != null) {
            return trained;
        }
        return CharacterSkill.builder()
                .skill(skillType.newSkillInstance())
                .graduation(SkillGraduation.builder().graduationValue(UNTRAINED_PENALTY).build())
                .build();
    }
}
