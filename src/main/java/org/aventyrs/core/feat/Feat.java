package org.aventyrs.core.feat;

import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.aventyrs.core.effect.CriticalEffect;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.item.RegaliaGrade;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.TitleArchetype;

/**
 * A Talento. <b>Sealed</b>, which is what lets {@link FeatCatalog} enumerate every authored
 * Talento reflectively via {@link Class#getPermittedSubclasses()} — see that class for why this
 * beats scanning the classpath. The compiler enforces the permits clause, so a new Talento tree
 * physically cannot exist without appearing in the catalog.
 *
 * <p>{@link AbstractFeat} is deliberately {@code non-sealed}: a consumer of this library that
 * needs a Talento of its own (a homebrew rule, a test double) extends that instead of
 * implementing this directly. Such a Talento is a real {@code Feat} everywhere — {@code
 * FeatService#grantFeat} takes it, {@link #isEligible} works — it simply isn't part of the
 * authored catalog, which is correct: {@code FeatCatalog} lists the ruleset, not every {@code
 * Feat} that could ever be constructed.
 */
public sealed interface Feat permits AnaoFeat, ArtesMarciaisFeat, ArtificeFeat, ArtilhariaFeat, AssassinoFeat, AvianoFeat, BestialFeat, CavalariaFeat, DestinoFeat, DraconicoFeat, DuelistaFeat, ElementalFeat, ElficoFeat, EscudeiroFeat, FadasFeat, FeericoFeat, FeralFeat, FuriasFeat, GiganteFeat, GnomoFeat, GoblinFeat, GorgonaFeat, HumanoFeat, IndomitoFeat, MesticoFeat, MobilidadeFeat, MonstruosoFeat, OgricoFeat, OrquicoFeat, PequeninoFeat, TrollFeat, VampiricoFeat, PeritoFeat, SobrevivenciaFeat, MetamagicoFeat, AbstractFeat {
    FeatCategory getFeatCategory();
    String getDescription();
    FeatRequirements getFeatRequirements();

    /**
     * The authored catalog constant this {@code Feat} <em>is</em> — itself, for an ordinary enum
     * constant. A {@link AbstractFeat}-based Talento that carries an <b>acquisition-time choice</b>
     * (the Perícia picked for {@code PeritoFeat#FOCO_EM_PERICIA}, the terrain for {@code
     * SobrevivenciaFeat#TERRENO_PREDILETO}, …) overrides this to return the bare enum constant it
     * is a per-character form of — the same split {@code
     * org.aventyrs.core.skill.artes.ArtesAprimorarComArteAbility} keeps against {@code
     * ArtesCompetencyAbility#APRIMORAR_COM_ARTE}.
     *
     * <p>It exists because {@code Feat} identity is compared two ways that a hand-written choice
     * instance would otherwise fail: {@link #isEligible}'s {@code requiredFeat} check and {@code
     * FeatCatalog#availableFor}'s "not already held" filter. Both compare against the catalog
     * constant, so both go through this rather than reference-equality on the held object. A
     * choice instance is deliberately <em>not</em> given a custom {@code equals} — an asymmetric
     * "instance equals the constant it wraps" is fragile; this one-directional accessor is not.
     */
    default Feat catalogEntry() {
        return this;
    }

    /**
     * Whether character currently satisfies every prerequisite named in {@link
     * #getFeatRequirements()} — an Attribute's base reaching {@code requiredAttributeValue}
     * (skipped when {@code attributeDomain} is unset), a Perícia's Graduação reaching {@code
     * requiredSkillGraduation} (skipped when {@code requiredSkillType} is unset; an untrained
     * Perícia reads as Graduação 0, same as everywhere else in this core), {@code
     * requiredFeat} already being held (skipped when unset), {@code
     * requiredSkillCompetencyAbility} already being held — racial Habilidades included, via
     * {@code SkillCompetencyAbility#allFor} (skipped when unset), enough Títulos Aventyr Despertos
     * — optionally of one {@link TitleArchetype} — for {@code requiredAwakenedTitles} (skipped
     * when zero, which is every non-Aventyr-tier Talento), the holder's Race matching {@code
     * requiredRace} (skipped when unset), the holder's {@code CreatureType} matching {@code
     * requiredCreatureType} (skipped when unset), the holder's devotion matching {@code requiredDeity}
     * (skipped when unset), enough already-held Talentos of {@code
     * requiredFeatCategory} (skipped when unset), and enough Regalias of {@code
     * craftedRegaliaGrade} already forged by the holder (skipped when unset — the "criação de 3 ou
     * mais Regalias" gate). Every clause is independent — a requirement
     * left unset never blocks eligibility — and, when more than one is set, all must hold at
     * once, mirroring {@code AventyrTitleAbility#isEligible}'s identical
     * combine-every-set-prerequisite shape. Checked by {@code
     * org.aventyrs.core.character.services.FeatService#grantFeat} before granting.
     */
    default boolean isEligible(final Character character) {
        FeatRequirements requirements = getFeatRequirements();

        boolean attributeSatisfied = requirements.attributeDomain() == null
                || character.getAttributes().getAttribute(requirements.attributeDomain()).getBase() >= requirements.requiredAttributeValue();

        boolean skillSatisfied = requirements.requiredSkillType() == null
                || graduationOf(character, requirements.requiredSkillType()) >= requirements.requiredSkillGraduation();

        boolean featSatisfied = requirements.requiredFeat() == null
                || character.getFeats().stream()
                        .anyMatch(held -> held.catalogEntry() == requirements.requiredFeat());

        boolean competencySatisfied = requirements.requiredSkillCompetencyAbility() == null
                || SkillCompetencyAbility.allFor(character)
                        .contains(requirements.requiredSkillCompetencyAbility());

        boolean titlesSatisfied = countAwakenedTitles(character, requirements.requiredTitleArchetype())
                >= requirements.requiredAwakenedTitles();

        boolean raceSatisfied = requirements.requiredRace() == null
                || requirements.requiredRace().isInstance(character.getRace());

        boolean creatureTypeSatisfied = requirements.requiredCreatureType() == null
                || requirements.requiredCreatureType() == character.getRace().getPrerequisiteCreatureType();

        boolean deitySatisfied = requirements.requiredDeity() == null
                || requirements.requiredDeity() == character.getDeity();

        boolean categoryCountSatisfied = requirements.requiredFeatCategory() == null
                || countFeatsOfCategory(character, requirements.requiredFeatCategory())
                        >= requirements.requiredFeatCategoryCount();

        boolean regaliaCraftHistorySatisfied = requirements.craftedRegaliaGrade() == null
                || character.getRegaliasCrafted(requirements.craftedRegaliaGrade())
                        >= requirements.craftedRegaliaCount();

        return attributeSatisfied && skillSatisfied && featSatisfied && competencySatisfied
                && titlesSatisfied && raceSatisfied && creatureTypeSatisfied && deitySatisfied
                && categoryCountSatisfied && regaliaCraftHistorySatisfied;
    }

    /**
     * How many Títulos Aventyr character currently has Desperto — every filled slot of {@code
     * Character#getAllTitles()}, narrowed to one {@link TitleArchetype} when archetype is
     * non-null. "Desperto" is simply "held": there is no separate awakening step, and no
     * Talento in the catalog gates on a Título's own Especializações or Supremas.
     */
    private static long countAwakenedTitles(final Character character, final TitleArchetype archetype) {
        return character.getAllTitles().stream()
                .filter(title -> archetype == null || title.getArchetype() == archetype)
                .count();
    }

    /**
     * How many Talentos of featCategory character already holds — the "2 outros Talentos de
     * Destino" clause. The Talento being tested is never among them, since eligibility is only
     * ever asked before it is granted.
     */
    private static long countFeatsOfCategory(final Character character, final FeatCategory featCategory) {
        return character.getFeats().stream()
                .filter(feat -> feat.getFeatCategory() == featCategory)
                .count();
    }

    /**
     * How many Dano Base scale-ups this Talento grants character right now. The shorter form of
     * {@link #resolveDamageBaseIncrease(Character, Weapon)} — it delegates there with a {@code
     * null} weapon, so a constant whose grant is unconditional overrides <em>this</em> one and
     * still gets routed correctly for a weapon attack.
     *
     * <p>A scale-up is a row of {@link org.aventyrs.core.character.DamageBase}'s table, never a
     * flat addend — see that type's javadoc for why "+1 Dano Base" and "+1 aos Danos" are
     * different mechanics that must not be summed together.
     */
    default int resolveDamageBaseIncrease(final Character character) {
        return resolveDamageBaseIncrease(character, null);
    }

    /**
     * How many Dano Base scale-ups this Talento grants character for an attack made with weapon
     * — {@code null} when it is an Ataque Desarmado. Summed by {@code
     * org.aventyrs.core.character.services.DamageBaseService#getDamageBase} across {@code
     * Character#getFeats()}; zero by default.
     *
     * <p>This is the longer overload (the cascading-overloads convention): a constant scoped to
     * <em>what the attack was made with</em> overrides THIS one, never the shorter form.
     * {@code ArtesMarciaisFeat#ARTISTA_MARCIAL} grants only to an Ataque Desarmado or an Arma
     * Natural; {@code AnaoFeat#FILHO_DE_YMIR}'s "de armas" grants to any wielded weapon but not
     * to bare hands. Both read {@code weapon == null} as "Ataque Desarmado" and {@code
     * weapon.getCategory() == }{@link ItemCategory#NATURAL_WEAPON} as "Arma Natural" — a
     * distinction that <em>is</em> reliable here, because {@code DamageBaseService}'s two
     * overloads make it explicit (unlike {@code AbstractSkillInteraction#applyTo}'s 5th
     * parameter, where a {@code null} {@code AttackSource} also means "caller didn't say").
     */
    /**
     * Whether this Talento makes weapon count as an <b>Arma Natural for its holder</b>, even
     * though its own {@link ItemCategory} says otherwise — {@code
     * ArtesMarciaisFeat#DOMINAR_ARTE_MARCIAL_FERROADA_ESMAGADORA}'s "armas leves de combate
     * corpo-a-corpo que você utilizar são consideradas Armas Naturais para você". False by
     * default.
     *
     * <p><b>Never call this directly — ask {@link Character#treatsAsNaturalWeapon(Weapon)}.</b>
     * That is the single per-character view every Arma-Natural check consults, and the reason
     * this hook exists rather than each clause testing the raw category itself: a reclassifying
     * Talento is only meaningful if the checks it is supposed to reach actually see it, and the
     * rules text says so outright ("permite que você utilize quaisquer outros Talentos ou
     * Habilidades que afetem Armas Naturais para afetar sua arma").
     *
     * <p>Reclassification is <em>additive</em> and one-directional: a Talento can make a weapon
     * count as natural, never make a real Arma Natural stop counting. Nothing in the catalogue
     * takes the classification away, and a hook that could would need a precedence rule between
     * two Talentos disagreeing.
     */
    default boolean reclassifiesAsNaturalWeapon(final Weapon weapon, final Character character) {
        return false;
    }

    /**
     * Extra damage this Talento adds to a <b>dano roll</b> — the {@code Feat} counterpart of
     * {@code SkillCompetencyAbility#resolveDamageBonus}, with the same four parameters, summed by
     * {@code AbstractSkillInteraction} across {@code Character#getFeats()} alongside every other
     * dano source. Empty by default.
     *
     * <p><b>A dano bonus is not a Dano Base increase</b> — see {@link #resolveDamageBaseIncrease}
     * and {@code DamageBase}'s own javadoc. This one is a flat number added to an already-rolled
     * total; that one moves the attack up the dice scale, and a scale-up can be worth a whole
     * extra die. A clause saying "+N em rolagens de Danos" belongs here; one saying "+N de Dano
     * Base" belongs there. Never both.
     *
     * <p>"Vantagem em rolagens de Dano" is a flat {@code Skill#ADVANTAGE_BONUS} (+2), the same
     * figure Vantagem is on a Perícia roll, and a Desvantagem the same -2.
     *
     * <p>{@code attackTarget} is {@code null} outside an attack against a specific combatant, and
     * on the bonuses-only preview path; an override conditioned on the target must read that as
     * "condition not met". {@code actor} is the roller, for an amount scaling off their own state.
     */
    default Optional<DamageBonus> resolveDamageBonus(final SkillType attackingSkillType, final SceneContext sceneContext,
                                                      final CombatantSheet attackTarget, final Character actor) {
        return Optional.empty();
    }

    /**
     * The same dano bonus, for a clause conditioned on <b>what the attack was made with</b> —
     * {@code MonstruosoFeat#FEROCIDADE}'s "+1 em rolagens de danos de suas Armas Naturais". {@code
     * attackSource} is the wielded {@link Weapon} or cast {@code Spell} itself; a natural-weapon
     * clause narrows it with {@code actor.treatsAsNaturalWeapon(weapon)}, the same per-character
     * view {@link #resolveDamageBaseIncrease(Character, Weapon)}'s Arma-Natural overriders use.
     *
     * <p><b>Defaults to the 4-arg form, not the other way round</b> — the same defaulting
     * relationship every other trailing-{@code AttackSource} {@code Feat} overload has (see {@link
     * #resolveSkillRollBonus(SkillType, SceneContext, SkillTrait, Character, AttackSource)}): an
     * existing 4-arg overrider keeps working untouched, and a constant overriding <em>this</em>
     * one must return its own unconditional value too if it has one. {@code null} {@code
     * attackSource} means the caller didn't say — read as "no scope matched".
     */
    default Optional<DamageBonus> resolveDamageBonus(final SkillType attackingSkillType, final SceneContext sceneContext,
                                                      final CombatantSheet attackTarget, final Character actor,
                                                      final AttackSource attackSource) {
        return resolveDamageBonus(attackingSkillType, sceneContext, attackTarget, actor);
    }

    /**
     * The same dano bonus, for a clause conditioned on <b>how many targets this one attack is
     * affecting</b> — {@code ArtesMarciaisFeat#DOMINAR_ARTE_MARCIAL_ARTE_FLUIDA}'s "enquanto
     * houver mais de um alvo você sofre Desvantagem em rolagens de Danos". One attack against
     * several targets still makes a <em>single</em> dano roll, so this is a property of the roll
     * rather than of any one target, which is why it is a count here and not something resolved
     * per {@code attackTarget}.
     *
     * <p>{@code targetCount} is how many targets the attack was declared against — {@code 0} on
     * the bonuses-only preview path and on any call that named no target at all, which an
     * override must read as "condition not met" rather than as "one target". {@code attackTarget}
     * stays the <b>primary</b> target, the one every target-conditioned hook resolves against.
     *
     * <p><b>Defaults to the 5-arg form</b>, the same defaulting relationship every other longer
     * {@code Feat} overload has: an existing overrider keeps working untouched, and a constant
     * overriding <em>this</em> one must return its own unconditional value too if it has one.
     */
    default Optional<DamageBonus> resolveDamageBonus(final SkillType attackingSkillType, final SceneContext sceneContext,
                                                      final CombatantSheet attackTarget, final Character actor,
                                                      final AttackSource attackSource, final int targetCount) {
        return resolveDamageBonus(attackingSkillType, sceneContext, attackTarget, actor, attackSource);
    }

    /**
     * How many targets <b>beyond the primary one</b> an attack of attackingSkillType made by
     * character may affect — {@code ArtesMarciaisFeat#DOMINAR_ARTE_MARCIAL_ARTE_FLUIDA}'s "seus
     * ataques afetam um alvo adicional". Zero by default, and summed by {@code
     * org.aventyrs.core.character.services.AttackTargetingService#getMaximumTargets} across
     * {@code Character#getFeats()} on top of the one target every attack already has.
     *
     * <p>Any condition the clause carries is the override's own to check — {@code ARTE_FLUIDA}
     * returns 0 while its holder wields a non-natural weapon or a Escudo, exactly as it does for
     * its dano Desvantagem, so the two halves can never disagree.
     *
     * <p><b>Which</b> combatants those extra targets are is deliberately not decided here. The
     * rules require them to be adjacent to the primary target, and this core does no geometry
     * (a {@code SceneContext} only holds distances measured from its own holder, never between
     * two other combatants) — so the caller picks the targets and {@code
     * org.aventyrs.core.combat.AttackDelivery} only enforces the count.
     */
    default int resolveAdditionalTargets(final SkillType attackingSkillType, final Character character) {
        return 0;
    }

    default int resolveDamageBaseIncrease(final Character character, final Weapon weapon) {
        return 0;
    }

    /**
     * How many bands up the {@link org.aventyrs.core.scene.Range} ladder this Talento widens the
     * maximum distance of an attack made with attackSource — summed by {@code
     * org.aventyrs.core.character.services.AttackRangeService#getEffectiveRange} across {@code
     * Character#getFeats()} and applied with {@code Range#increasedBy}. Zero by default.
     *
     * <p>The <b>first {@code Feat} hook to take an {@link AttackSource}</b> (the wielded {@code
     * Weapon} or cast {@code Spell}), because a range clause is routinely scoped to <em>how</em>
     * the attack is delivered rather than to a Perícia as a whole: {@code
     * ArtilhariaFeat#TIRO_LONGO}'s "ataques à Distância, físicos e Mágicos" checks {@code
     * attackSource.getAttackSkillType() == }{@link SkillType#ATAQUE_A_DISTANCIA}, which is true
     * of an arco and of a ranged Magia alike. {@code null} means the caller didn't say, and every
     * override reads it as "no scope matched" — the same convention as {@code
     * AbstractSkillInteraction#applyTo}'s 5th parameter; it does <em>not</em> mean "Ataque
     * Desarmado".
     *
     * <p>A range step is a whole {@code Range} band (a nível/passo de distância), never a UD
     * count — which is why this is an {@code int} step count and not a {@code ModifierType}
     * bonus, the same reasoning {@code SpellService#getMaxBranchLevel} uses for tree depth.
     */
    default int resolveAttackRangeIncrease(final Character character, final AttackSource attackSource) {
        return 0;
    }

    /**
     * A flat bonus this Talento adds to a skillType roll — summed by {@code
     * AbstractSkillInteraction#applyTo} across {@code Character#getFeats()}, alongside the
     * {@code SKILL_ROLL_BONUS}/per-Perícia {@code @Modifier} scans it already runs. Zero by
     * default.
     *
     * <p><b>Vantagem is a flat +2 ({@code Skill#ADVANTAGE_BONUS}), never a reroll</b> — see
     * CLAUDE.md's "Vantagem is a flat +2 bonus" section. Talentos are outside every {@code
     * ModifierResolver} scan (nothing scans them reflectively), which is why they need an
     * explicit hook where an ability would simply carry a {@code @Modifier} method.
     *
     * <p>The four parameters mirror {@code SkillCompetencyAbility#resolveConditionalRollBonus}'s
     * own, because the clauses are the same shape: {@code skillType} is the Perícia being rolled
     * (each override checks it — the service applies no filter, so a constant scoped to one
     * Perícia must say so); {@code sceneContext} carries the terrain or proximity a clause is
     * conditioned on, and is {@code null} whenever there is no active Scene, which every override
     * must read as "condition not met"; {@code requestedAbility} is what lets a clause scope
     * itself to an Especialização ("Conhecimentos: Natureza"), exactly as {@code
     * AnoesRacialAbility#FILHOS_DA_MONTANHA} does for the identical clause; and {@code character}
     * is the holder, for an amount that scales off their own live state.
     *
     * <p>Only for a bonus this core can actually scope. A Vantagem conditioned on lighting, on
     * being unseen, or on a narrative purpose still grants nothing, and each such constant says
     * so on itself.
     */
    default int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                       final SkillTrait requestedAbility, final Character character) {
        return 0;
    }

    /**
     * The same bonus, for a clause conditioned on <b>what the attack was made with</b> —
     * {@code DuelistaFeat#ESPECIALISTA_EM_ARMA}'s "escolha entre um tipo de arma, armas naturais
     * ou magias ofensivas. Receba vantagem nas rolagens de ataque com a arma escolhida" and
     * {@code ArtilhariaFeat#ATIRADOR_PERFEITO}'s identical shape. attackSource is the wielded
     * {@code Weapon} or cast {@code Spell} itself, matched against an {@code AttackMethod} choice
     * via {@code AttackMethod#matches} — {@code null} means the caller didn't say, which every
     * override must read as "no scope matched", the same convention {@code
     * SkillCompetencyAbility#resolveSubstituteAttributeDomain(AttackSource)} uses.
     *
     * <p><b>This defaults to the unconditional answer, rather than the other way round</b> — the
     * same defaulting relationship {@code resolveSubstituteAttributeDomain(AttackSource)} has
     * against {@code getSubstituteAttributeDomain()}: every existing 4-arg overrider keeps
     * working untouched, because this longer form falls through to it. Overriding <i>this</i> one
     * is what an attack-method-conditioned clause does, and such a constant must then return its
     * unconditional value itself if it has one — there is no partial inheritance.
     */
    default int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                       final SkillTrait requestedAbility, final Character character,
                                       final AttackSource attackSource) {
        return resolveSkillRollBonus(skillType, sceneContext, requestedAbility, character);
    }

    /**
     * The longest form of the Perícia-roll bonus, adding the roller's own {@link CombatantSheet}.
     *
     * <p><b>Defaults to the sheet-less form, not the other way round</b> — the same defaulting
     * relationship the {@code AttackSource} overloads have, so every existing overrider keeps
     * working untouched. Override <i>this</i> one for a clause conditioned on the holder's
     * live combat state; such a constant must then return its unconditional value itself.
     *
     * <p>{@code holder} is the roller's own {@link CombatantSheet} — what a {@link Character}
     * alone cannot reach: held {@code Condição}s ({@code CombatantSheet#hasCondition}), the
     * per-Rodada action log, temporary bonuses, current PV. {@code null} whenever the caller has
     * only a {@code Character} (e.g. {@code DefenseService}'s {@code Character} overload), which
     * every override must read as "condition not met".
     */
    default int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                       final SkillTrait requestedAbility, final Character character,
                                       final AttackSource attackSource, final CombatantSheet holder) {
        return resolveSkillRollBonus(skillType, sceneContext, requestedAbility, character, attackSource);
    }

    /**
     * How many GD (níveis de {@code DifficultyLevel}) this Talento takes off a skillType roll —
     * summed by {@code AbstractSkillInteraction#applyTo} across {@code Character#getFeats()},
     * alongside the {@code SkillCompetencyAbility#getDifficultyReduction()} and {@code
     * SkillExcellency} scans it already runs. Positive means <i>easier</i>, the same sign
     * convention those two use. Zero by default.
     *
     * <p>Takes the {@link SkillType} because a Talento's GD clause always names one Perícia
     * ("a GD de suas rolagens de Atenção é reduzida em -1 Nível"), and the character because a
     * clause may scale off the holder's own live state — the same two reasons {@link
     * #resolveDefenseBonus} takes its pair. Each override checks skillType itself; the service
     * applies no filter.
     *
     * <p><b>Only for an unconditional reduction on a named Perícia.</b> Most Talentos that
     * mention a GD do not qualify, and each says so on its own constant: one scoped to a
     * narrative purpose ("para criar equipamento"), one bought with a resource ("gastar 2PD
     * para reduzir"), one scoped to a single roll of a Turn, and one on a Conjuração roll that
     * {@code SpellCastingService} does not resolve at all. This hook exists because several
     * racial trees state the plain, unconditional form.
     */
    default int resolveDifficultyReduction(final SkillType skillType, final Character character) {
        return 0;
    }

    /**
     * How many GD (níveis de {@code DifficultyLevel}) this Talento takes off a skillType
     * <b>attack</b> roll, for a clause conditioned on <b>what the attack cost</b> and on the
     * Rodada's prior actions — {@code AssassinoFeat#SAQUE_RELAMPAGO}'s "a cada Rodada, a primeira
     * vez que fizer um ataque utilizando apenas 1PA ou Ação Livre a GD da Perícia de Ataque é
     * reduzida em -1 nível". Summed by {@code AbstractSkillInteraction#applyTo} across {@code
     * Character#getFeats()}, gated on a {@code SkillRoll} being present, and folded into the same
     * {@code difficultyReduction} total as {@link #resolveDifficultyReduction} — so it is applied
     * through {@code DifficultyLevel#easier} on the direct skill-roll path and by {@code
     * AttackReceiver}, and reported (not applied) on the {@code AttackDelivery} flat-Defesa path,
     * like every other attacker-side reduction there. Positive = easier. Zero by default.
     *
     * <p><b>Distinct from {@link #resolveDifficultyReduction}</b>, which is documented for the
     * unconditional case. This is the <b>first consumer</b> of a conditional GD reduction scoped
     * to an attack's Pontos de Ação cost, so the shape is deliberately raw: {@code actionCost}
     * ({@code null} when the caller didn't say — read as "condition not met") and the whole
     * {@code actionsThisRound} list are passed through rather than a pre-computed "is this the
     * first cheap attack" boolean, because the "primeira vez" predicate filters that history by a
     * feat-specific choice (Armas vs Magias) and cannot be generically hoisted. A second consumer
     * with the identical predicate would justify moving the predicate into {@code
     * AbstractSkillInteraction}.
     */
    default int resolveAttackCostDifficultyReduction(final SkillType skillType, final SceneContext sceneContext,
            final Character character, final AttackSource attackSource, final ActionCost actionCost,
            final List<CombatantAction> actionsThisRound) {
        return 0;
    }

    /**
     * Whether this Talento hands spell to character <b>for free</b> as part of its own authored
     * benefit, so {@code org.aventyrs.core.character.services.SpellService#grantSpell} spends no
     * XP for it — {@code MetamagicoFeat#ARCANISTA}'s automatically-learned Sementes and its two
     * chosen trees' Brotos ("suas magias do tipo Semente são automaticamente aprendidas", "você
     * aprende a conjurar as Magias Brotos destas árvores"). Consulted by {@code
     * SpellService#getAcquisitionCost}, and a UI pricing Magia acquisition must apply the same
     * waiver so its numbers match what {@code grantSpell} will actually charge.
     *
     * <p>False by default. Wired <b>ahead of its first real consumer</b>: ARCANISTA is still a
     * plain enum constant, and expressing "the trees I picked" needs the acquisition-time-choice
     * class it does not have yet (see the {@code adding-a-feat} skill) — the same way {@link
     * #resolveBranchLevelIncrease} and its {@code SpellService} scan were wired before {@code
     * MetamagicoFeat} existed.
     */
    default boolean grantsFreeSpellAcquisition(final Character character, final Spell spell) {
        return false;
    }

    /**
     * EXP this Talento takes off {@code SpellService#getAcquisitionCost} for spell — {@code
     * ElementalFeat#ARCANISMO_ELEMENTAL}'s "Aprender magias Elementais de seu elemento custa
     * 0.5EXP a menos". Summed across {@code character.getFeats()} together with {@code
     * org.aventyrs.core.race.Race#resolveSpellAcquisitionCostReduction}, and the aggregate is
     * floored so a Magia never costs below zero. {@link BigDecimal#ZERO} by default.
     *
     * <p>Distinct from {@link #grantsFreeSpellAcquisition}, an all-or-nothing waiver checked
     * first: this is a partial discount that <b>stacks</b> with every other source. {@code
     * BigDecimal} because these clauses are routinely fractional (0.5), the same reason {@code
     * SkillGraduationService}'s cost is one.
     */
    default BigDecimal resolveSpellAcquisitionCostReduction(final Character character, final Spell spell) {
        return BigDecimal.ZERO;
    }

    /**
     * How many {@link org.aventyrs.core.magic.BranchLevel} rungs this Talento raises its
     * holder's general Árvore de Magia cap by. Summed by {@code
     * org.aventyrs.core.character.services.SpellService#getMaxBranchLevel} across {@code
     * Character#getFeats()}, advancing from {@code BranchLevel#SEMENTE} — the cap is what {@code
     * org.aventyrs.core.magic.Spell#isEligible}'s first gate checks, so until a Talento here
     * raises it a Conjurador can only acquire more Magias at their current depth, from other
     * Árvores.
     *
     * <p>Zero by default; only override on a constant whose rules text raises the cap. Talentos
     * that do belong in {@code FeatCategory#METAMAGICO}, which has no enum authored yet — this
     * hook and its service are wired ahead of the first one, so it drops in with no further
     * changes. Same default-hook shape as {@link #resolveDamageBaseIncrease} above.
     */
    default int resolveBranchLevelIncrease(final Character character) {
        return 0;
    }

    /**
     * How many numbers this Talento widens the holder's Margem Crítica Menor by on a skillType
     * roll — summed by {@code AbstractSkillInteraction#sumCriticalMarginIncrease} across {@code
     * Character#getFeats()}, alongside the {@code AttributeAbility}/{@code
     * SkillCompetencyAbility}/{@code EgoAdvantage} scans it already runs, and fed to {@code
     * SkillRoll#getCriticalResult(int)}. Zero by default.
     *
     * <p>The {@code Feat} counterpart of those three hooks, with {@code character} added for the
     * same reason every other {@code Feat} hook takes it: a Talento's figure routinely scales off
     * the holder's own live state. A clause conditioned on the opponent reads {@code
     * SceneContext#getOpposedCharacter()} — that is what makes {@code
     * AnaoFeat#GLORIA_YMIRIANA}'s "para atacar alvos maiores que você" expressible.
     */
    default int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext,
                                               final Character character) {
        return 0;
    }

    /**
     * The same widening, for a clause conditioned on <b>what the attack was made with</b> —
     * {@code AssassinoFeat#ACERTO_CRITICO_APRIMORADO}'s "Escolha entre um Tipo de Arma ou
     * Conjuração de Magias. Sua Margem Crítica Menor com o tipo de arma escolhida, ou das magias
     * que você conjurar, é aumentada". Same {@code attackSource}/{@code null} convention and the
     * same defaulting relationship (falls through to the 3-arg form) as {@link
     * #resolveSkillRollBonus(SkillType, SceneContext, SkillTrait, Character, AttackSource)}.
     */
    default int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext,
                                               final Character character, final AttackSource attackSource) {
        return resolveCriticalMarginIncrease(skillType, sceneContext, character);
    }

    /**
     * The longest form, adding the roller's own {@link CombatantSheet} — for a Margem Crítica
     * clause conditioned on the holder's live combat state that a {@link Character} alone cannot
     * see: held {@code Condição}s, the per-Rodada action log ({@code
     * CombatantSheet#getActionsThisRound()}), current PV. {@code AssassinoFeat#ACERTO_CRITICO_RELAMPAGO}'s
     * "primeiro ataque de cada Rodada" is what this exists for.
     *
     * <p><b>Defaults to the sheet-less {@code AttackSource} form</b>, so every existing overrider
     * keeps working. {@code holder} is {@code null} whenever the caller has only a {@code
     * Character}, which every override must read as "condition not met".
     */
    default int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext,
                                               final Character character, final AttackSource attackSource,
                                               final CombatantSheet holder) {
        return resolveCriticalMarginIncrease(skillType, sceneContext, character, attackSource);
    }

    /**
     * Extra {@link CriticalEffect}s this Talento adds to an attack the holder lands as a critical
     * — {@code AssassinoFeat#ABRIR_FERIDAS}'s "seus ataques recebem 'Sangramento' como Efeito
     * Crítico adicional". Merged with the caller-supplied list and then filtered by the victim's
     * immunities in {@code AttackDelivery}. Empty by default; only called when {@code
     * criticalResult} is an Acerto Crítico and {@code attackSkill.isAttackSkill()}.
     */
    default List<CriticalEffect> resolveExtraCriticalEffects(final Character attacker, final SkillType attackSkill,
                                                             final AttackSource attackSource,
                                                             final CriticalResult criticalResult) {
        return List.of();
    }

    /**
     * The {@link ActiveAbility} this Talento grants its holder — a Poder Vampírico (see {@code
     * org.aventyrs.core.feat.PoderVampiricoActiveAbility}), triggered through {@code
     * org.aventyrs.core.character.services.ActiveAbilityService#activate}. Empty by default.
     *
     * <p>Mirrors {@code org.aventyrs.core.ability.AttributeAbility#resolveActiveAbility}, with
     * one wiring difference: an {@code AttributeAbility}'s active ability is copied onto {@code
     * Character.activeAbilities} at acquisition, whereas a Talento's is surfaced live by {@code
     * Character#getActiveAbilities()} aggregating {@code getFeats()}. So a constant overriding
     * this <b>must return a stable singleton</b> (an enum-constant field) — {@code
     * ActiveAbilityService#activate} identifies the held ability by reference.
     */
    default Optional<ActiveAbility> resolveActiveAbility() {
        return Optional.empty();
    }

    /**
     * How much this Talento adds to the holder's Roubo de Vida total — summed by {@code
     * org.aventyrs.core.character.services.LifeStealService#getTotalLifeSteal} alongside {@code
     * AttributeAbility#resolveLifeStealBonus}, and, like that scan, applied <b>only when the
     * holder already has an active {@code LifeSteal} effect</b> ("amplify, never grant from
     * nothing"). {@code VampiricoFeat#SEDE_DE_SANGUE} returns its Títulos Despertos count.
     * Zero by default.
     *
     * <p>Real, computable data with no automatic combat caller — nothing in this core resolves a
     * dealt hit and reads {@code getTotalLifeSteal} yet, the same status the rest of the Roubo de
     * Vida infrastructure has.
     */
    default int resolveLifeStealBonus(final Character character) {
        return 0;
    }

    /**
     * A flat bonus this Talento adds to one Atributo — {@code VampiricoFeat#MESTRE_VAMPIRO}'s
     * "+1 ao Bônus Racial em Atributo ganho por ser um Vampiro". Overriding CLAUDE.md's "a
     * Talento cannot grant an Atributo bonus" for the first time, so the reach is deliberately
     * narrow: <b>only {@code AbstractSkillInteraction} reads it</b> — the bonus reaches a Perícia
     * roll governed by domain and nothing else (HP/PM/PD/Defesa/Conjuração still read {@code
     * AttributeValue#getTotal()} directly). Document that partial reach on any constant that
     * overrides this. Zero by default.
     *
     * <p>For a <b>round-scoped</b> Atributo bonus (a Poder Vampírico like {@code DOM_DE_MIRCALLA})
     * grant a {@code TemporaryBonus} of {@code domain.getBonusModifierType()} instead — the same
     * one reader picks both up.
     */
    default int resolveAttributeBonus(final AttributeDomain domain, final Character character) {
        return 0;
    }

    /**
     * The {@link RegaliaGrade} this Talento currently permits holder to forge — the {@code
     * ArtificeFeat} ladder's whole payload, and {@code null} for every other Talento.
     *
     * <p><b>A permission, not a prerequisite.</b> A Talento's {@link FeatRequirements} say what
     * a character must have to <i>acquire</i> it; this says what holding it lets them <i>do</i>,
     * and the two are genuinely different questions. {@code ARTESAO_DE_REGALIAS_MENOR} is a case
     * where both exist and only one used to be modeled: Profissão 7 is what you need to learn it
     * (a requirement), while "a Regalia em sua posse" is what you need in hand each time you
     * forge (a condition of use). Gating acquisition on possession would have meant a crafter
     * who sold their Regalia had never learned anything.
     *
     * <p>So this hook answers "may this holder forge that grade <em>right now</em>", and returns
     * {@code null} when the use-conditions aren't met — which is why it takes {@code holder} at
     * all. Callers must therefore treat {@code null} as "not permitted", never as "no such
     * Talento". {@code org.aventyrs.core.item.ItemForgery} is the consumer; it scans every held
     * Talento and refuses the forge when none permits the grade asked for.
     *
     * <p>Deliberately one grade, not a set: each rung of the ladder permits exactly its own
     * grade, and a crafter reaches the higher rungs holding the lower Talentos too (each names
     * the one below in {@code requiredFeat}), so the union falls out of the scan.
     */
    default RegaliaGrade itsAllowedToCraftRegalia(final Character holder) {
        return null;
    }

    /**
     * The Armas Naturais ({@link NaturalWeapon}) this Talento grants its holder — a Chifres
     * Poderosos from {@code BestialFeat#HERANCA_BOVIDEA}, the two picked by {@code
     * org.aventyrs.core.feat.ArmamentoDraconicoFeat}. Aggregated by {@code
     * Character#getNaturalWeapons()} and, through it, the single answer to "what can this
     * character strike with unarmed". Empty by default.
     *
     * <p>Same default-empty-list shape as {@link #resolveExtraCriticalEffects} / {@link
     * #resolveDefeatBlessings}, and takes {@code character} for the same reason every {@code
     * Feat} hook does — a future clause could scale the grant off holder state, though none does
     * yet. A granted Arma Natural needs no possession gate on the attack path: {@code
     * DamageBaseService} takes the {@link Weapon} as a parameter and never looks it up, and
     * {@link Character#treatsAsNaturalWeapon(Weapon)} already recognises any {@link
     * ItemCategory#NATURAL_WEAPON} weapon.
     */
    default List<NaturalWeapon> getGrantedNaturalWeapons(final Character character) {
        return List.of();
    }

    /**
     * {@link Blessing}s this Talento grants its holder the moment one of their attacks drops a
     * target to 0 PV or less — {@code AssassinoFeat#SANGUE_QUENTE} ("+1PA e então +1PA para cada
     * Título Aventyr"), {@code VIOLENCIA_DESCOMUNAL} and {@code ARCANISMO_AVASSALADOR} (both
     * gated on {@code viaCriticalHit}). Resolved and applied by {@code
     * org.aventyrs.core.character.services.DefeatBlessingService#applyDefeatBlessings}, which the
     * caller invokes after an attack it has determined was fatal — this core has no defeat
     * observer, the same caller-drives-it shape as {@code recordAction} / session recovery.
     * Empty by default.
     */
    default List<Blessing> resolveDefeatBlessings(final Character attacker, final CombatantSheet defeated,
                                                  final boolean viaCriticalHit) {
        return List.of();
    }

    /**
     * Whether this Talento lets its holder draw a weapon as an <b>Ação Livre</b> instead of
     * paying {@code WeaponDrawService#DEFAULT_DRAW_COST} — {@code AssassinoFeat#SAQUE_RAPIDO}'s
     * "você pode sacar uma arma como Ação Livre". False by default.
     *
     * <p>A boolean rather than a cost, because no clause in the catalogue makes drawing
     * <i>cheaper</i> by some amount — they make it free outright. Resolved by {@code
     * WeaponDrawService#getDrawCost}, which takes the cheapest answer across every held Talento.
     */
    default boolean drawsWeaponAsFreeAction(final Character character) {
        return false;
    }

    /**
     * Flat, unconditional Redução de Danos (RD, also written RDS — Redução de Danos Sofridos)
     * this Talento grants — summed by {@code DamageServiceImpl#getTotalDamageReduction} across
     * {@code Character#getFeats()}, alongside the usual three-source {@code
     * ModifierType#DAMAGE_REDUCTION} scan and the equipment pass. Zero by default.
     *
     * <p>Talentos are outside every {@code ModifierResolver} scan, so they need an explicit hook
     * where an ability would carry a {@code @Modifier} method — the same reason {@link
     * #resolveSkillRollBonus} and {@link #resolveDifficultyReduction} exist.
     *
     * <p><b>Only for an unconditional grant.</b> Most RD clauses in the catalog are conditioned
     * on something this core cannot see — the holder's current PV, a form they are in, an active
     * effect, or which attack of the Rodada this is — and each such constant says so on itself
     * rather than overriding here. A clause stating no number uses {@code
     * DamageService#DEFAULT_DAMAGE_REDUCTION}; one stating its own uses that.
     *
     * <p>Deliberately RD and not {@code ABSOLUTE_DAMAGE_REDUCTION}: no Talento in the catalog
     * describes mitigation an attack cannot be allowed to bypass, and RD is what {@code
     * calculateFinalDamage}'s {@code ignoreDamageReduction} flag exists to skip.
     */
    default int resolveDamageReduction(final Character character) {
        return 0;
    }

    /**
     * The longer form of the RD grant, adding the holder's own {@link CombatantSheet} — what
     * {@code AssassinoFeat#ESCUDO_DE_SOMBRAS}'s "RDS enquanto estiver escondido" needs to ask
     * whether its holder is currently Escondido.
     *
     * <p><b>Defaults to the sheet-less form, not the other way round</b> — the same defaulting
     * relationship every other {@code Feat} overload here uses, so existing overriders keep
     * working untouched. {@code holder} is {@code null} whenever the caller has only a {@link
     * Character}, which every override must read as "condition not met".
     */
    default int resolveDamageReduction(final Character character, final CombatantSheet holder) {
        return resolveDamageReduction(character);
    }

    /**
     * A flat, unconditional bonus this Talento grants to defenseType — summed by {@code
     * org.aventyrs.core.character.services.DefenseService#getTotalDefense} across {@code
     * Character#getFeats()}, alongside the usual three-source {@code @Modifier} scan.
     *
     * <p>Takes both the {@link DefenseType} and the character because a Talento's Defesa clause
     * is routinely scoped to one of DF/DM and scaled off the holder's own live state — {@code
     * MetamagicoFeat#ARCANISTA}'s "Bônus em sua DM igual a metade de suas Graduações em Domínio
     * do Mana" is both at once, which no compile-time-fixed {@code @Modifier} value could
     * express. Zero by default.
     *
     * <p>Only for an <b>unconditional</b> bonus. A Defesa scoped to what is being resisted
     * ("para resistir aos efeitos de magias que seja capaz de conjurar") can't be expressed
     * here — nothing classifies an incoming effect that way; see {@code
     * EsquivaEApararCompetencyAbility#EVASAO}'s identical blocker.
     */
    default int resolveDefenseBonus(final DefenseType defenseType, final Character character) {
        return 0;
    }

    /**
     * The same bonus, for a clause conditioned on <b>who is attacking</b> — {@code
     * AnaoFeat#VANTAGEM_DE_TAMANHO}'s "+Metade do Vigor em Defesas para resistir aos ataques de
     * oponentes de Categorias de Tamanhos superiores à sua". The attacker is {@code
     * SceneContext#getOpposedCharacter()}, which on a defence roll is the combatant being
     * defended against.
     *
     * <p><b>This defaults to {@link #resolveDefenseBonus(DefenseType, Character)}, it does not
     * cascade into it.</b> The direction is deliberate and is the same one {@code
     * SkillCompetencyAbility#resolveSubstituteAttributeDomain(AttackSource)} uses: every
     * constant that already overrides the unconditional form keeps working untouched, because
     * this longer form falls through to it. Overriding <i>this</i> one is what a
     * context-conditioned clause does, and such a constant must then return its unconditional
     * value itself if it has one — there is no partial inheritance.
     *
     * <p>{@code sceneContext} is {@code null} whenever there is no active Scene, and every
     * override must read that as "condition not met".
     */
    default int resolveDefenseBonus(final DefenseType defenseType, final Character character,
                                     final SceneContext sceneContext) {
        return resolveDefenseBonus(defenseType, character);
    }

    /**
     * The longest form of the Defesa bonus, adding the holder's own {@link CombatantSheet} —
     * what {@code AssassinoFeat#ESCUDO_DE_SOMBRAS} needs to ask whether its holder is currently
     * Escondido.
     *
     * <p><b>Defaults to the sheet-less form, not the other way round</b> — the same defaulting
     * relationship the {@code AttackSource} overloads have, so every existing overrider keeps
     * working untouched. Override <i>this</i> one for a clause conditioned on the holder's
     * live combat state; such a constant must then return its unconditional value itself.
     *
     * <p>{@code holder} is the roller's own {@link CombatantSheet} — what a {@link Character}
     * alone cannot reach: held {@code Condição}s ({@code CombatantSheet#hasCondition}), the
     * per-Rodada action log, temporary bonuses, current PV. {@code null} whenever the caller has
     * only a {@code Character} (e.g. {@code DefenseService}'s {@code Character} overload), which
     * every override must read as "condition not met".
     */
    default int resolveDefenseBonus(final DefenseType defenseType, final Character character,
                                     final SceneContext sceneContext, final CombatantSheet holder) {
        return resolveDefenseBonus(defenseType, character, sceneContext);
    }

    /**
     * How much this Talento raises the holder's Mana Multiplier — summed by {@code
     * MagicPointsService#getManaMultiplier} across {@code Character#getFeats()}, on top of
     * {@code Character#getManaMultiplier()} and the {@code ModifierType#MANA_MULTIPLIER} scan.
     * Zero by default.
     */
    default int resolveManaMultiplierIncrease(final Character character) {
        return 0;
    }

    /**
     * Extra Pontos de Mana this Talento recovers on a Descanso of restType — summed by {@code
     * org.aventyrs.core.rest.RestService#getRecoveredMagicPoints} across {@code
     * Character#getFeats()}, alongside {@code AttributeAbility#resolveRestMagicPointsBonus}'s
     * own scan.
     *
     * <p>Takes the character too, unlike the {@code AttributeAbility} hook it mirrors, because a
     * Talento's recovery clause may scale off the holder's own state — {@code
     * MetamagicoFeat#MENTE_EXPANDIDA}'s "+1PM para cada Título Aventyr que tenha desperto".
     * Zero by default.
     */
    default int resolveRestMagicPointsBonus(final RestType restType, final Character character) {
        return 0;
    }

    /**
     * How many UD this Talento adds to its holder's Movimento Base — summed by {@code
     * org.aventyrs.core.character.services.MovementService#getMovementBase} across {@code
     * Character#getFeats()}, alongside the usual three-source {@code ModifierType#MOVEMENT} scan.
     * Zero by default.
     *
     * <p>Like that {@code ModifierType}, this is a figure <b>per Ponto de Ação</b>, not a
     * whole-Turn allowance — see {@code MovementService}'s own javadoc for why the two readings
     * land differently, and only override here for a clause of the "seu Movimento Base aumenta em
     * +NUD" shape.
     */
    default int resolveMovementIncrease(final Character character) {
        return 0;
    }

    /**
     * Extra Movimento Base, in UD, this Talento grants on one specific movement of the Rodada —
     * for a clause scoped to <i>which</i> movement it is, e.g. {@code
     * MobilidadeFeat#VELOCISTA}'s "aumenta cumulativamente em +1UD para cada outro movimento
     * feito no mesmo Turno". movementIndex is 0-based, so the first movement of the Rodada is 0
     * and the count of movements already made before this one is movementIndex itself.
     *
     * <p><b>Per Ponto de Ação, like every other movement figure in this core</b> — see {@code
     * org.aventyrs.core.character.services.MovementService#getMovementBase}. Summed by {@code
     * MovementServiceImpl} across {@code Character#getFeats()}, the same explicit fourth pass
     * {@link #resolveMovementIncrease} already gets, since Talentos sit outside every {@code
     * ModifierResolver} scan.
     *
     * <p>Takes character for the same reason every other {@code Feat} hook does: a Talento's
     * figure routinely scales off the holder's own live state. Zero by default; an
     * unconditional "+NUD ao Movimento Base" belongs on {@link #resolveMovementIncrease}.
     */
    default int resolveRoundMovementIncrease(int movementIndex, Character character) {
        return 0;
    }

    /**
     * How many Pontos de Ação this Talento permanently adds — summed by {@code
     * org.aventyrs.core.character.services.ActionPointsService#getMaxActionPoints} across {@code
     * Character#getFeats()}, alongside the usual three-source {@code ModifierType#ACTION_POINTS}
     * scan. Zero by default.
     *
     * <p>Only for a <b>permanent</b> grant ("você adquire permanentemente +1PA"). A Talento
     * granting Pontos de Ação for a Turn or a Rodada is a {@code Blessing} of {@code
     * ModifierType#ACTION_POINTS} instead, which only the {@code CombatantSheet} overloads read.
     */
    default int resolveActionPointsIncrease(final Character character) {
        return 0;
    }

    /**
     * How much this Talento raises the holder's Determination Multiplier — summed by {@code
     * org.aventyrs.core.character.services.DeterminationPointsService#getDeterminationMultiplier}
     * across {@code Character#getFeats()}, on top of {@code Character#getDeterminationMultiplier()}
     * and the {@code ModifierType#DETERMINATION_MULTIPLIER} scan. Zero by default.
     *
     * <p>The Determinação twin of {@link #resolveManaMultiplierIncrease}, added for {@code
     * DestinoFeat#CORACAO_DE_FERRO_DO_DESTINO}'s "seu multiplicador de PD aumenta em +1".
     */
    default int resolveDeterminationMultiplierIncrease(final Character character) {
        return 0;
    }

    /**
     * How much this Talento raises the holder's Life Multiplier — summed by {@code
     * org.aventyrs.core.character.services.HitPointsService#getLifeMultiplier} across {@code
     * Character#getFeats()}, on top of {@code Character#getLifeMultiplier()} and the {@code
     * ModifierType#LIFE_MULTIPLIER} scan. Zero by default.
     *
     * <p>Not interchangeable with a flat {@code ModifierType#HIT_POINTS} grant — see {@code
     * HitPointsService} for why a stated PV amount and a multiplier uplift only agree at one
     * particular Vigor. Use this only for rules text that says "Multiplicador de PV".
     */
    default int resolveLifeMultiplierIncrease(final Character character) {
        return 0;
    }

    private static int graduationOf(final Character character, final SkillType skillType) {
        CharacterSkill characterSkill = character.getSkills().get(skillType);
        return characterSkill == null ? 0 : characterSkill.getGraduation().getGraduationValue();
    }
}
