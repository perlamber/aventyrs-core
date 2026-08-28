package org.aventyrs.core.feat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.skill.SkillCompetencyAbility;
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
public sealed interface Feat permits ArtesMarciaisFeat, ArtificeFeat, ArtilhariaFeat, AssassinoFeat, CavalariaFeat, DestinoFeat, DuelistaFeat, EscudeiroFeat, MobilidadeFeat, PeritoFeat, SobrevivenciaFeat, MetamagicoFeat, AbstractFeat {
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
     * requiredRace} (skipped when unset), and enough already-held Talentos of {@code
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

        boolean categoryCountSatisfied = requirements.requiredFeatCategory() == null
                || countFeatsOfCategory(character, requirements.requiredFeatCategory())
                        >= requirements.requiredFeatCategoryCount();

        return attributeSatisfied && skillSatisfied && featSatisfied && competencySatisfied
                && titlesSatisfied && raceSatisfied && categoryCountSatisfied;
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
