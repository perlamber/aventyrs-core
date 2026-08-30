package org.aventyrs.core.feat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.scene.SceneContext;
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
     * (skipped when unset), and enough already-held Talentos of {@code
     * requiredFeatCategory} (skipped when unset). Every clause is independent — a requirement
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
                || character.getFeats().contains(requirements.requiredFeat());

        boolean competencySatisfied = requirements.requiredSkillCompetencyAbility() == null
                || SkillCompetencyAbility.allFor(character)
                        .contains(requirements.requiredSkillCompetencyAbility());

        boolean titlesSatisfied = countAwakenedTitles(character, requirements.requiredTitleArchetype())
                >= requirements.requiredAwakenedTitles();

        boolean raceSatisfied = requirements.requiredRace() == null
                || requirements.requiredRace().isInstance(character.getRace());

        boolean creatureTypeSatisfied = requirements.requiredCreatureType() == null
                || requirements.requiredCreatureType() == character.getRace().getCreatureType();

        boolean deitySatisfied = requirements.requiredDeity() == null
                || requirements.requiredDeity() == character.getDeity();

        boolean categoryCountSatisfied = requirements.requiredFeatCategory() == null
                || countFeatsOfCategory(character, requirements.requiredFeatCategory())
                        >= requirements.requiredFeatCategoryCount();

        return attributeSatisfied && skillSatisfied && featSatisfied && competencySatisfied
                && titlesSatisfied && raceSatisfied && creatureTypeSatisfied && deitySatisfied
                && categoryCountSatisfied;
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
     * How many Dano Base scale-ups this Talento grants character right now — e.g. {@code
     * ArtesMarciaisFeat#ARTISTA_MARCIAL}'s "+1, cumulativamente +1 para cada Título Aventyr
     * Desperto". Summed by {@code
     * org.aventyrs.core.character.services.DamageBaseService#getDamageBase} across {@code
     * Character#getFeats()}. Zero by default; only override on a constant whose rules text
     * raises Dano Base.
     *
     * <p>A scale-up is a row of {@link org.aventyrs.core.character.DamageBase}'s table, never a
     * flat addend — see that type's javadoc for why "+1 Dano Base" and "+1 aos Danos" are
     * different mechanics that must not be summed together.
     */
    default int resolveDamageBaseIncrease(final Character character) {
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
     * DestinoFeat#CORACAO_DE_FERRO}'s "seu multiplicador de PD aumenta em +1".
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
