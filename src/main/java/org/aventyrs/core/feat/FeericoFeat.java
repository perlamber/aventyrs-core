package org.aventyrs.core.feat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.race.CreatureType;
import org.aventyrs.core.race.Satiro;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;

/**
 * Talentos Feéricos — the largest racial tree, spanning wings, tree-bonded Dríades, Pixies,
 * Sirenídeos and the two Sátiro lineages.
 *
 * <p>Four constants carry real effects, all of them the same shape: an <b>unconditional Vantagem
 * on named Perícias</b>, through {@code Feat#resolveSkillRollBonus}. {@link #NINFA} (Empatia
 * Selvagem), {@link #SIRENIDEO} (Artes), {@link #FAUNO} (Furtividade e Persuasão) and {@link
 * #LUPERCAL} (Artes e Atenção). Each also opens with an Atributo bonus a Talento cannot grant, so
 * every one of the four is half-implemented and says so.
 *
 * <p><b>This tree is why {@code FeatRequirements#requiredCreatureType} exists.</b> "Apenas
 * personagens de raça Feérica" spans Fada, Fúria, Sátiro, Nascido da Floresta and Górgona — five
 * classes with no common supertype — so {@code requiredRace} cannot express it. {@link
 * CreatureType#FEERICO} can, in one clause.
 *
 * <p>Where the text narrows further to two named races ("apenas Fadas e Fúrias", {@link #PIXIE}
 * and {@link #SIRENIDEO}), the gate stays at {@code FEERICO} and is therefore <b>looser than
 * written</b> — the direction every unexpressible clause in this catalog errs in.
 */
public enum FeericoFeat implements Feat {

    /**
     * "Você tem asas e possui Movimento Base de Voo. Enquanto voando seu Movimento Base aumenta
     * em +2UD."
     */
    // TODO: needs a flight state and a Movimento Base de Voo, which is a different sub-stat from
    //  ordinary Movimento Base — see Aviano's Braços Alados and BestialFeat's class javadoc.
    // TODO: its Pré-requisito is a disjunction — "apenas Avianos e Bestiais, OU personagens
    //  recém-criados de raça Feérica" — and every FeatRequirements clause combines with and. The
    //  Feérico branch is recorded, so an Aviano or Bestial is wrongly refused; one of the eight
    //  constants docs/rules/talentos-index.md lists under "Disjunctions".
    ASAS(
            "Você tem asas e possui Movimento Base de Voo. Enquanto voando seu Movimento Base "
                    + "aumenta em +2UD. Iniciar uma ação de voo em situações estressantes, como as "
                    + "Cenas de Combate, exige o uso de 3PD, Feéricos usam 3PM. Este Custo é "
                    + "reduzido em 1 para cada Título Aventyr que o personagem possua. A Duração "
                    + "do Efeito de Voo é igual à 1d6+Metade do Vigor Rodadas.",
            FeatRequirements.builder()
                    .requiredCreatureType(CreatureType.FEERICO)
                    .build()),

    /** "O Custo de Ativação do Efeito de Voo é reduzido em -1PM." */
    // TODO: adjusts the Custo and Duração of a flight effect that cannot be activated.
    BENCAO_DE_FLORA(
            "O Custo de Ativação do Efeito de Voo é reduzido em -1PM. A Duração de seu Efeito de "
                    + "Voo muda para 2d6+Metade do Foco Rodadas.",
            FeatRequirements.builder()
                    .requiredCreatureType(CreatureType.FEERICO)
                    .requiredFeat(ASAS)
                    .build()),

    /**
     * "Você pode, com auxílio de um ritual, transferir sua essência vital para uma árvore."
     */
    // TODO: the densest single clause of the Feérico tree, and none of it is expressible. It
    //  needs an age concept (não envelhece; envelhecem metade do tempo ao desconectar), a
    //  disease classification, a bound external object with its own location and PV, a
    //  kilometre-denominated distance (this core has Range bands within a Scene and nothing
    //  else), a halving of Descanso recovery (RestService has no multiplier stage), and a
    //  death-replacement trigger that rewrites current PV to 10 and permanently drops the PV
    //  multiplier by 1.
    DRIADE(
            "Você pode, com auxílio de um ritual, transferir sua essência vital para uma árvore. "
                    + "Enquanto estiver a uma distância igual ou inferior a 'Instinto' quilômetros "
                    + "de sua árvore você não envelhece, se torna imune a doenças não mágicas e "
                    + "não pode morrer por causas naturais. Enquanto a distância for maior, a "
                    + "quantidade de PV, PD e PM que recuperam a cada Descanso é reduzida pela "
                    + "metade. Se algum efeito lhe causar danos suficientes para reduzir seus PV à "
                    + "zero ou menos, ao invés disso sua árvore morrerá e sua essência vital "
                    + "voltará para o seu corpo: você sofre redutor permanente de -1 Multiplicador "
                    + "de PV e seus PV atuais mudam para 10. Se a árvore for destruída de outra "
                    + "forma, você também morrerá.",
            FeatRequirements.builder()
                    .requiredCreatureType(CreatureType.FEERICO)
                    .build()),

    /**
     * "Sua categoria de tamanho muda para -3, você tem asas e Movimento Base de Voo 8UD."
     */
    // TODO: a Talento cannot set Categoria de Tamanho (no hook, and this is an absolute set
    //  rather than the shift ModifierType.SIZE_CATEGORY expresses), and flight does not exist.
    // TODO: gated at FEERICO where the text says "apenas Fadas e Fúrias" — looser than written,
    //  see the class javadoc. Its exclusion of SIRENIDEO is unenforceable, as every exclusion is.
    PIXIE(
            "Sua categoria de tamanho muda para -3, você tem asas e Movimento Base de Voo 8UD. "
                    + "Voar em Cenas estressantes, como Combates, exige o uso de 2PM ao Tempo de "
                    + "Ação Livre, a capacidade de voo poderá ser usada por 2d6+Metade do Carisma "
                    + "Rodadas, +3 Rodadas para cada Título Aventyr Desperto que possuir.",
            FeatRequirements.builder()
                    .requiredCreatureType(CreatureType.FEERICO)
                    .build()),

    /**
     * "Você recebe Bônus de +1 Racial em Carisma e vantagem em rolagens de 'Empatia Selvagem'."
     * The Vantagem is real.
     */
    // TODO: a Talento cannot grant an Atributo bonus — see BestialFeat's class javadoc.
    // TODO: "do sexo feminino" is unenforced — Character#getSexo() exists and is nullable, but
    //  FeatRequirements has no clause for it and one constant is short of the bar for adding one.
    NINFA(
            "Você recebe Bônus de +1 Racial em Carisma e vantagem em rolagens de 'Empatia "
                    + "Selvagem'.",
            FeatRequirements.builder()
                    .requiredCreatureType(CreatureType.FEERICO)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character) {
            return skillType == SkillType.EMPATIA_SELVAGEM ? Skill.ADVANTAGE_BONUS : 0;
        }
    },

    /**
     * "Você pode Mimetizar Magias Naturais, do tipo Broto, ao custo de 2PD. Esta ação pode ser
     * efetuada mesmo que você não conheça as magias e não cumpra com seus pré-requisitos."
     */
    // TODO: mimetizar has no mechanism — SpellCastingService cannot cast a Magia the caster does
    //  not know, and there is no cost step to redirect from PM to PD. Note this constant states
    //  the mimicry contract most explicitly of any in the catalog ("mesmo que você não conheça as
    //  magias e não cumpra com seus pré-requisitos"), which is worth reading first if the
    //  mechanism is ever built.
    ESPIRITO_DA_FLORESTA(
            "Você pode Mimetizar Magias Naturais, do tipo Broto, ao custo de 2PD. Esta ação pode "
                    + "ser efetuada mesmo que você não conheça as magias e não cumpra com seus "
                    + "pré-requisitos.",
            FeatRequirements.builder()
                    .requiredCreatureType(CreatureType.FEERICO)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Sempre que outros personagens conjurarem magias em Distância Curta você irá identificá-los
     * automaticamente."
     */
    // TODO: an automatic detection with no roll and no state — nothing observes another
    //  combatant casting (this codebase has no event or observer mechanism anywhere), and
    //  "identificá-los" has no knowledge state to record into.
    SENTIR_O_FLUXO_DE_MANA(
            "Você é capaz de sentir mudanças no Mana ao seu redor, sempre que outros personagens "
                    + "conjurarem magias em Distância Curta você irá identificá-los "
                    + "automaticamente.",
            FeatRequirements.builder()
                    .requiredCreatureType(CreatureType.FEERICO)
                    .requiredSkillType(SkillType.DOMINIO_DO_MANA)
                    .requiredSkillGraduation(1)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Temporariamente você pode mudar sua forma física, se transformando em um Anciente, uma
     * árvore viva, abandonando seus traços raciais."
     */
    // TODO: needs a form state — the same missing piece DraconicoFeat#DRACONATO and HomemFera's
    //  Forma Híbrida are blocked on, including the same "abandona seus traços raciais"
    //  suppression that Race#getRacialAbilities() cannot be suspended for.
    // TODO: "Nascidos da Floresta permanecem +2 Rodadas" would be a per-race Duração branch, and
    //  "não pode ser reativado até um Descanso Longo" a per-Descanso activation counter.
    ANCIENTEFORME(
            "Temporariamente você pode mudar sua forma física, se transformando em um Anciente, "
                    + "uma árvore viva, abandonando seus traços raciais. Transformar-se em um "
                    + "Anciente requer 3PA e 3PD; para cada Título Aventyr Desperto suas Defesas, "
                    + "Multiplicador de PV e Categoria de Tamanho aumentam em +2, enquanto seu "
                    + "Carisma e Foco aumentam em +1. Ancienteforme dura por apenas 3 Rodadas, "
                    + "Nascidos da Floresta permanecem nesta forma por +2 Rodadas. Após utilizar "
                    + "deste Efeito ele não pode ser ativado novamente até que você passe por um "
                    + "Descanso Longo.",
            FeatRequirements.builder()
                    .requiredFeat(DRIADE)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Personagens com este Talento recebem Bônus Racial de +1 em Vigor e Vantagem nas rolagens
     * de Artes." The Vantagem is real.
     */
    // TODO: a Talento cannot grant an Atributo bonus.
    // TODO: Movimento Base de Natação 6UD and a *reduced* land Movimento of 2UD are both absolute
    //  sets rather than the increment resolveMovementIncrease expresses, and swim movement is a
    //  separate sub-stat. Withheld together so the Talento is neither better nor worse than
    //  written.
    // TODO: gated at FEERICO where the text says "apenas Fadas e Fúrias" — see the class javadoc.
    SIRENIDEO(
            "A parte inferior de teu corpo, no lugar das pernas, é similar ao de uma criatura "
                    + "marinha. Você possui guelras e pode respirar dentro e fora d'agua, possui "
                    + "também Movimento Base de Natação 6UD, mas em terra seu Movimento Base é "
                    + "reduzido para apenas 2UD. Personagens com este Talento recebem Bônus Racial "
                    + "de +1 em Vigor e Vantagem nas rolagens de Artes.",
            FeatRequirements.builder()
                    .requiredCreatureType(CreatureType.FEERICO)
                    .build()) {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character) {
            return skillType == SkillType.ARTES ? Skill.ADVANTAGE_BONUS : 0;
        }
    },

    /**
     * "Escolha entre Artes, Atenção e Persuasão… Você recebe uma Habilidade de Competência de
     * cada Perícia escolhida e Vantagem em suas rolagens."
     */
    // TODO: the Vantagem is real in shape but its scope is an acquisition-time *choice* — one
    //  Perícia per Título Desperto, from a list of three — and a Feat is a flat catalog constant
    //  carrying no per-acquisition data. The two shapes that could hold it both exist (an
    //  instance-based class, or one constant per option) but neither fits a tree that is one
    //  enum class. Same blocker as HumanoFeat#LIMIAR_DA_EVOLUCAO.
    // TODO: the free Habilidade de Competência per chosen Perícia is the acquisition-slot gap.
    ADOTADO_POR_SYLPH(
            "Escolha entre Artes, Atenção e Persuasão. Você pode escolher uma desta Perícias para "
                    + "cada Título Aventyr Desperto. Personagens Sátiros que possuam um Título "
                    + "Aventyr Especialista Desperto podem escolher uma Perícia listada adicional. "
                    + "Você recebe uma Habilidade de Competência de cada Perícia escolhida e "
                    + "Vantagem em suas rolagens.",
            FeatRequirements.builder()
                    .requiredCreatureType(CreatureType.FEERICO)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Recebe Monstruoso com um tipo de criatura adicional, também recebe Vantagem em suas
     * rolagens de Furtividade e Persuasão." The Vantagem is real.
     */
    // TODO: "Monstruoso com um tipo de criatura adicional" needs a character to hold *two*
    //  CreatureTypes at once — Race#getCreatureType() returns one and takes no Character, so this
    //  is the per-character CreatureType gap in its hardest form.
    // TODO: the GD half is scoped to the *target's* Tendência, and resolveDifficultyReduction
    //  carries no target — the roll's opponent is not a parameter of a GD reduction anywhere in
    //  this core. Tendência itself is a plain unvalidated 1-10 value with no Bondosa/Maligna
    //  banding.
    FAUNO(
            "Você tem o corpo coberto de pelos e maior número de feições caprinas. Recebe "
                    + "Monstruoso com um tipo de criatura adicional, também recebe Vantagem em "
                    + "suas rolagens de Furtividade e Persuasão. O GD de suas rolagens de Artes, "
                    + "Atenção e Persuasão efetuadas contra Personagens de Tendência Bondosa é "
                    + "reduzido em -1.",
            FeatRequirements.builder()
                    .requiredRace(Satiro.class)
                    .build()) {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character) {
            return skillType == SkillType.FURTIVIDADE || skillType == SkillType.PERSUASAO
                    ? Skill.ADVANTAGE_BONUS : 0;
        }
    },

    /**
     * "Sua Categoria de Tamanho muda para 0, também recebe Vantagem em suas rolagens de Artes e
     * Atenção." The Vantagem is real.
     */
    // TODO: a Talento cannot set Categoria de Tamanho. Note the effect happens to be a +1 step
    //  for a Sátiro, whose base is MINUS_ONE — the same coincidence GnomoFeat#DUENDE records, and
    //  equally not something to rely on.
    // TODO: the GD half is target-Tendência-scoped, same blocker as FAUNO's.
    LUPERCAL(
            "Sua Categoria de Tamanho muda para 0, também recebe Vantagem em suas rolagens de "
                    + "Artes e Atenção. O GD de suas rolagens de Artes, Atenção e Persuasão "
                    + "efetuadas contra Personagens de Tendência Maligna é reduzido em -1.",
            FeatRequirements.builder()
                    .requiredRace(Satiro.class)
                    .build()) {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character) {
            return skillType == SkillType.ARTES || skillType == SkillType.ATTENTION
                    ? Skill.ADVANTAGE_BONUS : 0;
        }
    };

    private final String description;
    private final FeatRequirements featRequirements;

    FeericoFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.FEERICO;
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
