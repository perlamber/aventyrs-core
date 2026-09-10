package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Alignment;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.effect.Definhar;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.magic.ActivationTime;
import org.aventyrs.core.magic.MimetizedSpell;
import org.aventyrs.core.magic.SpellDuration;
import org.aventyrs.core.magic.catalog.AliadosDaNaturezaSpell;
import org.aventyrs.core.magic.catalog.RegeneracaoSpell;
import org.aventyrs.core.magic.catalog.VooSpell;
import org.aventyrs.core.race.Elfo;
import org.aventyrs.core.race.MeioElfo;
import org.aventyrs.core.race.CreatureType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.Altitude;
import org.aventyrs.core.scene.EnvironmentalState;
import org.aventyrs.core.scene.LightLevel;
import org.aventyrs.core.scene.TerrainType;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.conhecimentos.ConhecimentosSpecialization;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.TitleIdentity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Talentos Élficos — four <b>Guardiões</b>, each adapting the holder to one environment, plus
 * three that reach past the Elfo's ordinary limits.
 *
 * <p>Three constants carry real effects, which makes this the richest racial tree so far:
 * {@link #GUARDIAO_DOS_BOSQUES} and {@link #GUARDIAO_DAS_DUNAS} grant terrain-conditioned
 * Vantagem through the new {@code Feat#resolveSkillRollBonus}, and {@link #SENTIDOS_ABSOLUTOS}
 * takes a nível off every Atenção roll.
 *
 * <p><b>The Guardiões are the clause that earned the roll-bonus hook.</b> All four grant Vantagem
 * on the same four Perícia scopes while in their own environment, and two of those environments
 * are exactly a {@code TerrainType} — so the condition is expressible today, unlike the lighting,
 * altitude and submersion states the other constants need. Their "Conhecimentos: Natureza" scope
 * is resolved through {@code requestedAbility}, the identical technique {@code
 * AnoesRacialAbility#FILHOS_DA_MONTANHA} uses for the very same clause.
 *
 * <p><b>Name collision worth knowing:</b> {@link #SENTIDOS_ABSOLUTOS} shares its name with {@code
 * ElfosRacialAbility#SENTIDOS_ABSOLUTOS}, which every Elfo already has. They are different
 * things — the racial ability grants Vantagem em Atenção, this Talento reduces that roll's GD —
 * and a character holding both gets both, correctly.
 *
 * <p>"Elfos podem possuir até 2 talentos Guardião, Meio-Elfos apenas um" is <b>not enforced</b>:
 * it is a per-race cap on how many Talentos of a sub-type may be held, and {@code
 * FeatRequirements} carries only thresholds that must be met, never a ceiling. {@code
 * ArtesMarciaisFeat} enforces its own sub-type cap ("nenhum outro Talento Dominar Arte Marcial")
 * with a local {@code isEligible} override; this one differs — the cap depends on the holder's
 * Race, not on another held Talento — so the same shape does not transfer, and it stays a
 * comment for now.
 */
public enum ElficoFeat implements Feat {

    /**
     * "Enquanto estiver em uma floresta ou bosque você recebe Vantagem em rolagens nas Perícias
     * de Ataque, 'Empatia Selvagem', 'Conhecimentos: Natureza' e em 'Furtividade'." Real.
     *
     * <p>"Uma floresta ou bosque" is exactly {@link TerrainType#FOREST}, so this is the cleanest
     * mapping of the four Guardiões.
     */
    GUARDIAO_DOS_BOSQUES(
            "Você possui pele em tom claro. Enquanto estiver em uma floresta ou bosque você "
                    + "recebe Vantagem em rolagens nas Perícias de Ataque, 'Empatia Selvagem', "
                    + "'Conhecimentos: Natureza' e em 'Furtividade'. Você também pode mimetizar a "
                    + "magia 'Cativar Animal' ao custo de 2PD. Guardiões dos Bosques não podem "
                    + "adquirir o título Bruxo.",
            FeatRequirements.builder().build()) {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character) {
            return guardiaoBonus(skillType, sceneContext, requestedAbility, TerrainType.FOREST);
        }

        @Override
        public List<MimetizedSpell> getGrantedMimetizedSpells(final Character character) {
            return List.of(mimetize(AliadosDaNaturezaSpell.CATIVAR_ANIMAL, 2));
        }

        @Override
        public TitleAcquisitionPermission resolveTitleAcquisitionPermission(
                final Optional<TitleIdentity> title, final Character character) {
            return prohibitsBruxo(title);
        }
    },

    /**
     * "Você recebe vantagem em rolagens nas Perícias de Ataque, 'Empatia Selvagem',
     * 'Conhecimentos: Natureza' e 'Furtividade', mas apenas enquanto estiver em desertos ou
     * outros lugares de temperaturas elevadas." Real.
     *
     * <p>Read as {@link TerrainType#DESERT}. "Ou outros lugares de temperaturas elevadas" is a
     * wider scope than the enum carries — nothing models ambient temperature — so this grants on
     * the desert half alone, which is narrower than the text rather than wider.
     */
    // TODO: "Dádiva de Undine" does not appear in the authored spell catalog. The similarly
    // named Lágrima de Undine is a different Muda spell, so it must not be substituted silently.
    GUARDIAO_DAS_DUNAS(
            "Você possui pele negra ou outro tom escuro, o forte sol do deserto com suas altas "
                    + "temperaturas pouco lhe incomodam. Você recebe vantagem em rolagens nas "
                    + "Perícias de Ataque, 'Empatia Selvagem', 'Conhecimentos: Natureza' e "
                    + "'Furtividade', mas apenas enquanto estiver em desertos ou outros lugares de "
                    + "temperaturas elevadas. Você também pode mimetizar a magia 'Dádiva de "
                    + "Undine' ao custo de 2PD. Guardiões das Dunas não podem adquirir o título "
                    + "Bruxo.",
            FeatRequirements.builder().build()) {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character) {
            return guardiaoBonus(skillType, sceneContext, requestedAbility, TerrainType.DESERT);
        }

        @Override
        public TitleAcquisitionPermission resolveTitleAcquisitionPermission(
                final Optional<TitleIdentity> title, final Character character) {
            return prohibitsBruxo(title);
        }
    },

    /**
     * "Enquanto estiver em locais de grande altitude, ou voando, você recebe vantagem em rolagens
     * nas Perícias de Ataque, em 'Empatia Selvagem', 'Conhecimentos: Natureza' e 'Furtividade'."
     */
    // TODO: withheld, unlike its two siblings, because its condition is not a TerrainType.
    //  "Locais de grande altitude" is not MOUNTAIN — a mountain Scene is not necessarily at
    //  altitude and vice versa — and "ou voando" needs the flight state Aviano's Braços Alados
    //  records as missing. Mapping it to MOUNTAIN would grant the bonus in caves-and-crags Scenes
    //  the clause does not cover and withhold it while flying, which it does. Granting nothing is
    //  the honest reading until either state exists.
    GUARDIAO_DAS_NUVENS(
            "Você possui pele em tom acinzentado e um corpo adaptado ao frio das Montanhas. "
                    + "Enquanto estiver em locais de grande altitude, ou voando, você recebe "
                    + "vantagem em rolagens nas Perícias de Ataque, em 'Empatia Selvagem', "
                    + "'Conhecimentos: Natureza' e 'Furtividade'. Você também pode mimetizar a "
                    + "magia 'Voo' em você mesmo, com Tempo de Conjuração de 1PA e Duração de 2 "
                    + "Rodadas, ao custo de 3PD.",
            FeatRequirements.builder().build()) {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character) {
            if (sceneContext == null) {
                return 0;
            }
            EnvironmentalState state = sceneContext.getEnvironmentalState();
            return state.altitude() == Altitude.HIGH || state.flying()
                    ? guardiaoBonus(skillType, requestedAbility)
                    : 0;
        }

        @Override
        public List<MimetizedSpell> getGrantedMimetizedSpells(final Character character) {
            return List.of(MimetizedSpell.builder()
                    .spell(VooSpell.VOO_LIVRE)
                    .determinationPointCost(3)
                    .activationTimeOverride(ActivationTime.pa(1))
                    .durationOverride(SpellDuration.rodadas(2))
                    .selfOnly(true)
                    .build());
        }
    },

    /**
     * "Enquanto estiverem com pelo menos metade do seu corpo submerso, recebem Vantagem nas
     * rolagens de Perícias de Ataque e 'Furtividade'."
     */
    // TODO: Conhecimentos and Empatia
    //  Selvagem only "para informações referente a vida e hábitos marinhos" — a narrative purpose
    //  this core does not track.
    // TODO: breathing underwater has no state to toggle.
    GUARDIAO_DAS_PROFUNDEZAS(
            "Com pele em tom azulado e brânquias no pescoço, os Guardiões das Profundezas são "
                    + "Elfos de características anfíbias, capazes de viver na água e em terra "
                    + "firme. Possuem a capacidade de Respirar na Água e, enquanto estiverem com "
                    + "pelo menos metade do seu corpo submerso, recebem Vantagem nas rolagens de "
                    + "Perícias de Ataque e 'Furtividade', também recebem Vantagem em suas "
                    + "rolagens de 'Conhecimento: Natureza' e Empatia Selvagem para informações "
                    + "referente a vida e hábitos marinhos. Também podem mimetizar a magia "
                    + "'Regeneração', ao custo de 2PD.",
            FeatRequirements.builder().build()) {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character) {
            return sceneContext != null && sceneContext.getEnvironmentalState().atLeastHalfSubmerged()
                    && (skillType.isAttackSkill() || skillType == SkillType.FURTIVIDADE)
                    ? Skill.ADVANTAGE_BONUS
                    : 0;
        }

        @Override
        public boolean allowsUnderwaterBreathing(final Character character) {
            return true;
        }

        @Override
        public List<MimetizedSpell> getGrantedMimetizedSpells(final Character character) {
            return List.of(mimetize(RegeneracaoSpell.REGENERACAO, 2));
        }

        @Override
        public TitleAcquisitionPermission resolveTitleAcquisitionPermission(
                final Optional<TitleIdentity> title, final Character character) {
            return prohibitsBruxo(title);
        }
    },

    /**
     * "Você recebe Vantagem em suas rolagens de Perícias efetuadas enquanto estiver sob a
     * cobertura de uma sombra ou durante a noite… Você Recebe Roubo de Vida 1."
     */
    // TODO: Definhar reaches physical attacks through Feat#resolveEffectChains and AttackDelivery,
    //  but SpellCastingService has no post-delivery EffectChain pipeline to attach it to Magias.
    CORRUPTOR_SOMBRIO(
            "Sua pele lentamente começa a escurecer, até se tornar preta como ébano. Você recebe "
                    + "Vantagem em suas rolagens de Perícias efetuadas enquanto estiver sob a "
                    + "cobertura de uma sombra ou durante a noite, mas é quase cego em locais "
                    + "luminosos, por isso sofre Desvantagem em rolagens de Perícias efetuadas em "
                    + "locais muito claros ou sob a luz do sol. Elfos com este talento perdem a "
                    + "limitação racial para adquirir o Título de Aventyr Bruxo. Você recebe Roubo "
                    + "de Vida 1 e seus ataques e Magias recebem Corrente de Efeito – Definhar.",
            FeatRequirements.builder()
                    .requiredFeatCategory(FeatCategory.ELFICO)
                    .requiredFeatCategoryCount(1)
                    .requiredAlignments(Set.of(Alignment.NEUTRAL, Alignment.EVIL))
                    .build()) {
        @Override
        public int resolveGrantedLifeSteal(final Character character) {
            return LIFE_STEAL;
        }

        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character) {
            if (sceneContext == null) {
                return 0;
            }
            return sceneContext.getEnvironmentalState().lightLevel().isDarkOrShadowed()
                    ? Skill.ADVANTAGE_BONUS
                    : sceneContext.getEnvironmentalState().lightLevel() == LightLevel.BRIGHT
                    ? Skill.DISADVANTAGE_MALUS
                    : 0;
        }

        @Override
        public List<EffectChain> resolveEffectChains(final Character attacker, final SkillType attackSkill,
                                                      final org.aventyrs.core.skill.AttackSource attackSource) {
            return List.of(new Definhar());
        }

        @Override
        public TitleAcquisitionPermission resolveTitleAcquisitionPermission(
                final Optional<TitleIdentity> title, final Character character) {
            return title.filter(TitleIdentity.BRUXO::equals)
                    .map(ignored -> TitleAcquisitionPermission.ALLOW)
                    .orElse(TitleAcquisitionPermission.NO_OPINION);
        }
    },

    /**
     * "Você é considerado um personagem Feérico para requisitos de Talentos e Habilidades.
     * Escolha uma Árvore de Magia Natural, você pode mimetizar as magias Broto e Muda da árvore
     * escolhida."
     */
    // TODO: mimetizar has no mechanism, and spending PD in place of PM has no cost step to
    //  redirect — SpellCastingService spends nothing at all. (The chosen Árvore could be
    //  recorded now — a choice-carrying AbstractFeat subclass, see FocoEmPericiaFeat — but
    //  mimetizar is the blocker, not the choice.)
    ALMA_FEERICA(
            "Você é considerado um personagem Feérico para requisitos de Talentos e Habilidades. "
                    + "Escolha uma Árvore de Magia Natural, você pode mimetizar as magias Broto e "
                    + "Muda da árvore escolhida. Caso possua 2 Títulos Despertos também poderá "
                    + "mimetizar Emergentes. Magias Mimetizadas desta forma utilizam PD em "
                    + "substituição aos PM.",
            FeatRequirements.builder()
                    .requiredRace(Elfo.class)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public Set<CreatureType> getGrantedPrerequisiteCreatureTypes(final Character character) {
            return Set.of(CreatureType.FEERICO);
        }
    },

    /**
     * "A GD de suas rolagens de Atenção é reduzida em -1 Nível." Real — the third Talento in the
     * catalog to reduce a roll's GD, and the shape {@code Feat#resolveDifficultyReduction} exists
     * for: unconditional, one named Perícia.
     */
    SENTIDOS_ABSOLUTOS(
            "A GD de suas rolagens de Atenção é reduzida em -1 Nível. Sua Margem Crítica Menor de "
                    + "suas rolagens de Atenção aumentam em +2 números para cada Título Aventyr "
                    + "Desperto. Você é sempre considerado bem-sucedido em rolagens de Atenção "
                    + "quando o GD for Médio ou inferior, dispensando a necessidade de rolar os "
                    + "dados.",
            FeatRequirements.builder()
                    .requiredRace(Elfo.class)
                    .attributeDomain(AttributeDomain.INSTINCT)
                    .requiredAttributeValue(3)
                    .requiredSkillType(SkillType.ATTENTION)
                    .requiredSkillGraduation(1)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public int resolveDifficultyReduction(final SkillType skillType, final Character character) {
            return skillType == SkillType.ATTENTION ? ATENCAO_DIFFICULTY_REDUCTION : 0;
        }

        @Override
        public int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext,
                                                 final Character character) {
            return skillType == SkillType.ATTENTION
                    ? CRITICAL_MARGIN_PER_AWAKENED_TITLE * character.getAllTitles().size()
                    : 0;
        }

        @Override
        public boolean resolveAutomaticSuccess(final SkillType skillType, final int targetValue,
                                               final SceneContext sceneContext, final Character character) {
            return skillType == SkillType.ATTENTION
                    && targetValue <= DifficultyLevel.MEDIUM.getBaseValue();
        }
    };

    private static final int ATENCAO_DIFFICULTY_REDUCTION = 1;
    private static final int CRITICAL_MARGIN_PER_AWAKENED_TITLE = 2;
    private static final int LIFE_STEAL = 1;

    /**
     * The Vantagem every Guardião grants, differing only in which {@link TerrainType} unlocks it:
     * every Perícia de Ataque, Empatia Selvagem, Furtividade, and Conhecimentos when the roll
     * names the Natureza Especialização.
     *
     * <p>Scoping Conhecimentos through {@code requestedAbility} rather than granting it to every
     * Conhecimentos roll is what keeps this faithful — the clause says "Conhecimentos: Natureza",
     * and {@code AnoesRacialAbility#FILHOS_DA_MONTANHA} resolves the identical clause the same
     * way.
     */
    private static int guardiaoBonus(final SkillType skillType, final SceneContext sceneContext,
                                      final SkillTrait requestedAbility, final TerrainType terrain) {
        if (sceneContext == null || !sceneContext.isTerrain(terrain)) {
            return 0;
        }
        return guardiaoBonus(skillType, requestedAbility);
    }

    private static int guardiaoBonus(final SkillType skillType, final SkillTrait requestedAbility) {
        if (skillType.isAttackSkill()
                || skillType == SkillType.EMPATIA_SELVAGEM
                || skillType == SkillType.FURTIVIDADE) {
            return Skill.ADVANTAGE_BONUS;
        }
        boolean naturezaRequested = skillType == SkillType.CONHECIMENTOS
                && requestedAbility == ConhecimentosSpecialization.NATUREZA;
        return naturezaRequested ? Skill.ADVANTAGE_BONUS : 0;
    }

    private final String description;
    private final FeatRequirements featRequirements;

    ElficoFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.ELFICO;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public FeatRequirements getFeatRequirements() {
        return featRequirements;
    }

    @Override
    public boolean isEligible(final Character character, final CharacterSheet sheet) {
        if (!Feat.super.isEligible(character, sheet)) {
            return false;
        }
        if (!isGuardian()) {
            return true;
        }
        int maximum = character.getRace() instanceof Elfo ? 2
                : character.getRace() instanceof MeioElfo ? 1 : 0;
        long heldGuardians = character.getFeats().stream()
                .filter(feat -> feat.catalogEntry() instanceof ElficoFeat elfico && elfico.isGuardian())
                .count();
        return heldGuardians < maximum;
    }

    private boolean isGuardian() {
        return this == GUARDIAO_DOS_BOSQUES || this == GUARDIAO_DAS_DUNAS
                || this == GUARDIAO_DAS_NUVENS || this == GUARDIAO_DAS_PROFUNDEZAS;
    }

    private static MimetizedSpell mimetize(final org.aventyrs.core.magic.Spell spell,
                                           final int determinationPointCost) {
        return MimetizedSpell.builder()
                .spell(spell)
                .determinationPointCost(determinationPointCost)
                .build();
    }

    private static TitleAcquisitionPermission prohibitsBruxo(final Optional<TitleIdentity> title) {
        return title.filter(TitleIdentity.BRUXO::equals)
                .map(ignored -> TitleAcquisitionPermission.PROHIBIT)
                .orElse(TitleAcquisitionPermission.NO_OPINION);
    }
}
