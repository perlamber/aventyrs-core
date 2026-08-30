package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.race.Elfo;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.TerrainType;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.conhecimentos.ConhecimentosSpecialization;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;

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
 * FeatRequirements} carries only thresholds that must be met, never a ceiling. Same family as
 * the exclusion clauses recorded in {@code docs/rules/talentos-index.md}.
 */
public enum ElficoFeat implements Feat {

    /**
     * "Enquanto estiver em uma floresta ou bosque você recebe Vantagem em rolagens nas Perícias
     * de Ataque, 'Empatia Selvagem', 'Conhecimentos: Natureza' e em 'Furtividade'." Real.
     *
     * <p>"Uma floresta ou bosque" is exactly {@link TerrainType#FOREST}, so this is the cleanest
     * mapping of the four Guardiões.
     */
    // TODO: mimetizar 'Cativar Animal' at 2PD has no mechanism — SpellCastingService cannot cast
    //  a Magia the caster does not know. Same gap NascidoDoDragao's Magia Dracônica cites.
    // TODO: "não podem adquirir o título Bruxo" is an exclusion on a *Título*, and nothing
    //  validates Título acquisition against a held Talento.
    GUARDIAO_DOS_BOSQUES(
            "Você possui pele em tom claro. Enquanto estiver em uma floresta ou bosque você "
                    + "recebe Vantagem em rolagens nas Perícias de Ataque, 'Empatia Selvagem', "
                    + "'Conhecimentos: Natureza' e em 'Furtividade'. Você também pode mimetizar a "
                    + "magia 'Cativar Animal' ao custo de 2PD. Guardiões dos Bosques não podem "
                    + "adquirir o título Bruxo.",
            FeatRequirements.builder()
                    .requiredRace(Elfo.class)
                    .build()) {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character) {
            return guardiaoBonus(skillType, sceneContext, requestedAbility, TerrainType.FOREST);
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
    // TODO: mimetizar 'Dádiva de Undine' — same missing mimicry mechanism as its sibling.
    // TODO: the Bruxo exclusion is unenforceable, same as its sibling.
    GUARDIAO_DAS_DUNAS(
            "Você possui pele negra ou outro tom escuro, o forte sol do deserto com suas altas "
                    + "temperaturas pouco lhe incomodam. Você recebe vantagem em rolagens nas "
                    + "Perícias de Ataque, 'Empatia Selvagem', 'Conhecimentos: Natureza' e "
                    + "'Furtividade', mas apenas enquanto estiver em desertos ou outros lugares de "
                    + "temperaturas elevadas. Você também pode mimetizar a magia 'Dádiva de "
                    + "Undine' ao custo de 2PD. Guardiões das Dunas não podem adquirir o título "
                    + "Bruxo.",
            FeatRequirements.builder()
                    .requiredRace(Elfo.class)
                    .build()) {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character) {
            return guardiaoBonus(skillType, sceneContext, requestedAbility, TerrainType.DESERT);
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
    // TODO: mimetizar 'Voo' — same missing mimicry mechanism as its siblings.
    GUARDIAO_DAS_NUVENS(
            "Você possui pele em tom acinzentado e um corpo adaptado ao frio das Montanhas. "
                    + "Enquanto estiver em locais de grande altitude, ou voando, você recebe "
                    + "vantagem em rolagens nas Perícias de Ataque, em 'Empatia Selvagem', "
                    + "'Conhecimentos: Natureza' e 'Furtividade'. Você também pode mimetizar a "
                    + "magia 'Voo' em você mesmo, com Tempo de Conjuração de 1PA e Duração de 2 "
                    + "Rodadas, ao custo de 3PD.",
            FeatRequirements.builder()
                    .requiredRace(Elfo.class)
                    .build()),

    /**
     * "Enquanto estiverem com pelo menos metade do seu corpo submerso, recebem Vantagem nas
     * rolagens de Perícias de Ataque e 'Furtividade'."
     */
    // TODO: withheld for the same reason as GUARDIAO_DAS_NUVENS — "com pelo menos metade do corpo
    //  submerso" is a per-character state, not a property of the Scene, so TerrainType.AQUATIC
    //  would grant it to an Elfo standing dry on a boat. Note this Guardião's scopes also differ
    //  from its siblings': Ataque and Furtividade unconditionally, but Conhecimentos and Empatia
    //  Selvagem only "para informações referente a vida e hábitos marinhos" — a narrative purpose
    //  this core does not track.
    // TODO: breathing underwater has no state to toggle, and mimetizar 'Regeneração' has no
    //  mechanism.
    GUARDIAO_DAS_PROFUNDEZAS(
            "Com pele em tom azulado e brânquias no pescoço, os Guardiões das Profundezas são "
                    + "Elfos de características anfíbias, capazes de viver na água e em terra "
                    + "firme. Possuem a capacidade de Respirar na Água e, enquanto estiverem com "
                    + "pelo menos metade do seu corpo submerso, recebem Vantagem nas rolagens de "
                    + "Perícias de Ataque e 'Furtividade', também recebem Vantagem em suas "
                    + "rolagens de 'Conhecimento: Natureza' e Empatia Selvagem para informações "
                    + "referente a vida e hábitos marinhos. Também podem mimetizar a magia "
                    + "'Regeneração', ao custo de 2PD.",
            FeatRequirements.builder()
                    .requiredRace(Elfo.class)
                    .build()),

    /**
     * "Você recebe Vantagem em suas rolagens de Perícias efetuadas enquanto estiver sob a
     * cobertura de uma sombra ou durante a noite… Você Recebe Roubo de Vida 1."
     */
    // TODO: the Vantagem and the paired Desvantagem are both conditioned on lighting — shadow,
    //  night, bright sun — and nothing models light or time of day. Both halves are withheld
    //  together, so the Talento is neither better nor worse than written.
    // TODO: Roubo de Vida is real (LifeStealService), but it is sourced from an active LifeSteal
    //  effect on the sheet plus AttributeAbility#resolveLifeStealBonus — a Feat has no hook, and
    //  LifeSteal is deliberately kept off ModifierType.
    // TODO: Corrente de Efeitos – Definhar exists as an effect class, but nothing attaches a
    //  Corrente to every attack a character makes.
    // TODO: "possuir tendência neutra ou maligna" is unenforced — Tendência is a plain
    //  unvalidated 1-10 value and FeatRequirements has no clause for it. So is "perde a limitação
    //  racial para adquirir o Título Bruxo", which is a Título-side restriction nothing validates.
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
                    .build()),

    /**
     * "Você é considerado um personagem Feérico para requisitos de Talentos e Habilidades.
     * Escolha uma Árvore de Magia Natural, você pode mimetizar as magias Broto e Muda da árvore
     * escolhida."
     */
    // TODO: a per-character CreatureType — Race#getCreatureType() takes no Character, so a type
    //  that changes with what its holder acquired is inexpressible. Same gap PequeninoFeat's two
    //  Linhagem Talentos and Indomito's Monstros em Potencial cite.
    // TODO: mimetizar has no mechanism, the Árvore is an acquisition-time choice a flat enum
    //  constant cannot hold, and spending PD in place of PM has no cost step to redirect —
    //  SpellCastingService spends nothing at all.
    ALMA_FEERICA(
            "Você é considerado um personagem Feérico para requisitos de Talentos e Habilidades. "
                    + "Escolha uma Árvore de Magia Natural, você pode mimetizar as magias Broto e "
                    + "Muda da árvore escolhida. Caso possua 2 Títulos Despertos também poderá "
                    + "mimetizar Emergentes. Magias Mimetizadas desta forma utilizam PD em "
                    + "substituição aos PM.",
            FeatRequirements.builder()
                    .requiredRace(Elfo.class)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "A GD de suas rolagens de Atenção é reduzida em -1 Nível." Real — the third Talento in the
     * catalog to reduce a roll's GD, and the shape {@code Feat#resolveDifficultyReduction} exists
     * for: unconditional, one named Perícia.
     */
    // TODO: "Margem Crítica Menor +2 para cada Título Aventyr Desperto" needs
    //  resolveCriticalMarginIncrease, which lives on EgoAdvantage/AttributeAbility/
    //  SkillCompetencyAbility but not on Feat.
    // TODO: "sempre considerado bem-sucedido quando o GD for Médio ou inferior" is an
    //  auto-success hook, which this core has never had — the same still-unbuilt piece
    //  MedicinaECuraExcellency#FOCADO and AttentionCompetencyAbility#PERCEPCAO_DE_FOXM wait on.
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
    };

    private static final int ATENCAO_DIFFICULTY_REDUCTION = 1;

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
}
