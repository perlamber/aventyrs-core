package org.aventyrs.core.feat;

import org.aventyrs.core.character.MovementMode;
import java.util.List;
import java.util.Optional;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.FormType;
import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.MimetizedSpell;
import org.aventyrs.core.character.Alignment;
import org.aventyrs.core.character.Dice;
import org.aventyrs.core.character.FlightActivation;
import org.aventyrs.core.race.Aviano;
import org.aventyrs.core.race.Fada;
import org.aventyrs.core.race.Furia;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CharacterSheet;
import java.util.Set;
import org.aventyrs.core.race.Bestial;
import org.aventyrs.core.race.CreatureType;
import org.aventyrs.core.race.Satiro;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.race.RacialTraitSuppression;
import org.aventyrs.core.sheet.CombatantSheet;

/**
 * Talentos Feéricos — the largest racial tree, spanning wings, tree-bonded Dríades, Pixies,
 * Sirenídeos and the two Sátiro lineages.
 *
 * <p>Four constants carry a real <b>unconditional Vantagem on named Perícias</b>, through {@code
 * Feat#resolveSkillRollBonus}: {@link #NINFA} (Empatia Selvagem), {@link #SIRENIDEO} (Artes),
 * {@link #FAUNO} (Furtividade e Persuasão) and {@link #LUPERCAL} (Artes e Atenção). {@link #NINFA}
 * and {@link #SIRENIDEO} additionally grant a <b>fixed-Atributo +1</b> through {@code
 * Feat#resolveAttributeBonus} (Carisma, Vigor) — that half is now real too, the same as every
 * {@code BestialFeat} Herança. {@link #LUPERCAL} additionally sets its holder's Categoria de
 * Tamanho outright ({@code Feat#resolveSizeCategoryOverride}), as {@link #PIXIE} does, and both
 * Sátiro lineages ease their rolls against a target of one Tendência ({@code
 * SceneContext#getOpposedCharacter()}'s band).
 *
 * <p><b>The Efeito de Voo is real</b> (core 0.1.7.8): {@link #ASAS} and {@link #PIXIE} price their
 * flight through {@code Feat#resolveFlightActivation}, {@link #BENCAO_DE_FLORA} adjusts it, and
 * {@code FlightService} spends it. {@link #DRIADE} is the one constant left unbuilt.
 *
 * <p><b>This tree is why {@code FeatRequirements#requiredCreatureType} exists.</b> "Apenas
 * personagens de raça Feérica" spans Fada, Fúria, Sátiro, Nascido da Floresta and Górgona — five
 * classes with no common supertype — so {@code requiredRace} cannot express it. {@link
 * CreatureType#FEERICO} can, in one clause.
 *
 * <p>Where the text narrows further to named races ("apenas Fadas e Fúrias", {@link #PIXIE} and
 * {@link #SIRENIDEO}; "Fadas, Fúrias e Sátiros", {@link #ESPIRITO_DA_FLORESTA}) the gate names them,
 * as one {@code anyOf} branch per Raça.
 */
public enum FeericoFeat implements Feat {

    /**
     * "Você tem asas e possui Movimento Base de Voo. Enquanto voando seu Movimento Base aumenta
     * em +2UD."
     */
    // The Movimento Base de Voo is real, and so is the +2UD: ⚠️ "enquanto voando seu Movimento Base
    // aumenta em +2UD" is read as the flight axis (the Movimento used while flying), not as land
    // Movimento.
    // The Efeito de Voo is real (Feat#resolveFlightActivation): 3PD — 3PM for a Feérico — less 1
    // per Título, 1d6 + metade do Vigor Rodadas, landing safely as it lapses.
    // The disjunctive Pré-requisito is real in full: "apenas Avianos e Bestiais" is the data
    // (two anyOf branches), and "ou personagens recém-criados de raça Feérica" is the starting-slot
    // branch (Feat#isEligibleAtCreation), so a Feérico takes it only at creation.
    ASAS(
            "Você tem asas e possui Movimento Base de Voo. Enquanto voando seu Movimento Base "
                    + "aumenta em +2UD. Iniciar uma ação de voo em situações estressantes, como as "
                    + "Cenas de Combate, exige o uso de 3PD, Feéricos usam 3PM. Este Custo é "
                    + "reduzido em 1 para cada Título Aventyr que o personagem possua. A Duração "
                    + "do Efeito de Voo é igual à 1d6+Metade do Vigor Rodadas.",
            FeatRequirements.builder()
                    .alternative(FeatRequirements.builder().requiredRace(Aviano.class).build())
                    .alternative(FeatRequirements.builder().requiredRace(Bestial.class).build())
                    .build()) {
        @Override
        public boolean grantsMovementMode(final MovementMode mode, final Character character,
                                          final CombatantSheet holder) {
            return mode == MovementMode.FLIGHT;
        }

        @Override
        public int resolveModeMovementIncrease(final MovementMode mode, final Character character) {
            return mode == MovementMode.FLIGHT ? ASAS_FLIGHT_BONUS : 0;
        }

        /** "… ou personagens recém-criados de raça Feérica." */
        @Override
        public boolean isEligibleAtCreation(final Character character, final CharacterSheet sheet) {
            return isEligible(character, sheet) || Feat.meetsRequirements(FEERICO_ONLY, character, sheet);
        }

        /**
         * "Exige o uso de 3PD, Feéricos usam 3PM. Este Custo é reduzido em 1 para cada Título Aventyr
         * que o personagem possua. A Duração do Efeito de Voo é igual à 1d6+Metade do Vigor Rodadas."
         * ⚠️ No Tempo de Ativação is stated, so none is charged. "Feéricos" is any Feérico prerequisite
         * type (a Pequenino of Linhagem de Flora too).
         */
        @Override
        public Optional<FlightActivation> resolveFlightActivation(final Character character,
                                                                  final CombatantSheet holder) {
            int cost = ASAS_FLIGHT_COST - character.getAllTitles().size();
            boolean feerico = character.getPrerequisiteCreatureTypes().contains(CreatureType.FEERICO);
            return Optional.of(new FlightActivation("Asas", ActionCost.NONE, feerico ? 0 : cost, feerico ? cost : 0,
                    Dice.of(1, holder.getAttributeTotal(AttributeDomain.VIGOR) / 2)));
        }
    },

    /**
     * "O Custo de Ativação do Efeito de Voo é reduzido em -1PM. A Duração de seu Efeito de Voo muda
     * para 2d6+Metade do Foco Rodadas." Both halves are real, on every Efeito de Voo its holder
     * starts ({@code FlightService#getFlightActivations}); the −1PM never takes a cost below 0.
     */
    BENCAO_DE_FLORA(
            "O Custo de Ativação do Efeito de Voo é reduzido em -1PM. A Duração de seu Efeito de "
                    + "Voo muda para 2d6+Metade do Foco Rodadas.",
            FeatRequirements.builder()
                    .requiredCreatureType(CreatureType.FEERICO)
                    .requiredFeat(ASAS)
                    .build()) {
        @Override
        public int resolveFlightMagicPointReduction(final Character character) {
            return BENCAO_DE_FLORA_PM_REDUCTION;
        }

        @Override
        public Dice resolveFlightDurationOverride(final Character character, final CombatantSheet holder) {
            return Dice.of(2, holder.getAttributeTotal(AttributeDomain.FOCUS) / 2);
        }
    },

    /**
     * "Você pode, com auxílio de um ritual, transferir sua essência vital para uma árvore."
     */
    // TODO: deliberately left unbuilt (table decision, 2026-10-16) — the densest single clause of
    //  the Feérico tree. It
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
    // The Categoria de Tamanho set is real, through Feat#resolveSizeCategoryOverride.
    // "Movimento Base de Voo 8UD" is real, an absolute figure (Feat#resolveMovementBaseOverride).
    // The Efeito de Voo is real (Feat#resolveFlightActivation): 2PM ao Tempo de Ação Livre,
    // 2d6 + metade do Carisma Rodadas + 3 per Título Desperto, then "retornam ao solo lentamente".
    // "Apenas Fadas e Fúrias" is real (one anyOf branch per Raça). The exclusion runs one way only,
    // exactly as the rules text writes it: Sirenídeo forbids Pixie, Pixie forbids nothing.
    PIXIE(
            "Sua categoria de tamanho muda para -3, você tem asas e Movimento Base de Voo 8UD. "
                    + "Voar em Cenas estressantes, como Combates, exige o uso de 2PM ao Tempo de "
                    + "Ação Livre, a capacidade de voo poderá ser usada por 2d6+Metade do Carisma "
                    + "Rodadas, +3 Rodadas para cada Título Aventyr Desperto que possuir.",
            fadasEFurias().build()) {
        @Override
        public Optional<FlightActivation> resolveFlightActivation(final Character character,
                                                                  final CombatantSheet holder) {
            int bonus = holder.getAttributeTotal(AttributeDomain.CHARISMA) / 2
                    + PIXIE_FLIGHT_ROUNDS_PER_TITLE * character.getAllTitles().size();
            return Optional.of(new FlightActivation("Pixie", ActionCost.FREE_ACTION, 0, PIXIE_FLIGHT_COST,
                    Dice.of(2, bonus)));
        }

        @Override
        public Integer resolveMovementBaseOverride(final MovementMode mode, final Character character) {
            return mode == MovementMode.FLIGHT ? PIXIE_FLIGHT_MOVEMENT : null;
        }

        /** "Apenas … recém-criados" — only a starting Talento slot can take it. */
        @Override
        public boolean isAcquirableOnlyAtCreation() {
            return true;
        }

        @Override
        public SizeCategory resolveSizeCategoryOverride(final Character character) {
            return SizeCategory.MINUS_THREE;
        }
    },

    /**
     * "Você recebe Bônus de +1 Racial em Carisma e vantagem em rolagens de 'Empatia Selvagem'."
     * Both halves are real — the Carisma +1 through {@link Feat#resolveAttributeBonus}, the
     * Vantagem through {@link Feat#resolveSkillRollBonus}.
     */
    // "Do sexo feminino" is real: FeatRequirements#requiredSexo, which an unrecorded Sexo fails.
    NINFA(
            "Você recebe Bônus de +1 Racial em Carisma e vantagem em rolagens de 'Empatia "
                    + "Selvagem'.",
            FeatRequirements.builder()
                    .requiredCreatureType(CreatureType.FEERICO)
                    .requiredSexo(Character.Sexo.FEMININO)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public int resolveAttributeBonus(final AttributeDomain domain, final Character character) {
            return domain == AttributeDomain.CHARISMA ? FEERICO_ATTRIBUTE_BONUS : 0;
        }

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
    // Real: every Broto of every Árvore Natural, mimetized at 2PD (getGrantedMimetizedSpells,
    // derived live) — "mesmo que você não conheça as magias e não cumpra com seus pré-requisitos",
    // which is exactly what a MimetizedSpell is. "Apenas Fadas, Fúrias e Sátiros" names the three
    // Raças; the Título comes from the Talento's TALENTOS RACIAIS AVENTYRS heading.
    ESPIRITO_DA_FLORESTA(
            "Você pode Mimetizar Magias Naturais, do tipo Broto, ao custo de 2PD. Esta ação pode "
                    + "ser efetuada mesmo que você não conheça as magias e não cumpra com seus "
                    + "pré-requisitos.",
            fadasEFurias()
                    .alternative(FeatRequirements.builder().requiredRace(Satiro.class).build())
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public List<MimetizedSpell> getGrantedMimetizedSpells(final Character character) {
            return ArvoresMimetizadasFeat.naturalTrees().stream()
                    .flatMap(tree -> tree.getSpells().stream())
                    .filter(spell -> spell.getBranchLevel() == BranchLevel.BROTO)
                    .map(spell -> MimetizedSpell.builder().spell(spell).determinationPointCost(ESPIRITO_BROTO_COST).build())
                    .toList();
        }
    },

    /**
     * "Sempre que outros personagens conjurarem magias em Distância Curta você irá identificá-los
     * automaticamente."
     */
    // Real as a sense (Feat#resolveSpellcastingSenseRange): the caller, which sees every cast,
    // tells the holder who cast what within Distância Curta and marks a caster hidden from them as
    // detected (table ruling, 2026-10-16: "identificá-los" = notice + reveal).
    SENTIR_O_FLUXO_DE_MANA(
            "Você é capaz de sentir mudanças no Mana ao seu redor, sempre que outros personagens "
                    + "conjurarem magias em Distância Curta você irá identificá-los "
                    + "automaticamente.",
            FeatRequirements.builder()
                    .requiredCreatureType(CreatureType.FEERICO)
                    .requiredSkillType(SkillType.DOMINIO_DO_MANA)
                    .requiredSkillGraduation(1)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public Range resolveSpellcastingSenseRange(final Character character) {
            return Range.DISTANCIA_CURTA;
        }
    },

    /**
     * "Temporariamente você pode mudar sua forma física, se transformando em um Anciente, uma
     * árvore viva, abandonando seus traços raciais."
     */
    // The transformation itself is real — FormaActiveAbility, the same template DRACONATO uses:
    // 3PA + 3PD to enter FormType.ANCIENTE for 3 Rodadas, gated on a Descanso Longo before it can
    // be used again (ActiveAbility#getReactivationRest).
    // The per-Título Defesas (+2), Categoria de Tamanho (+2) and Carisma/Foco (+1) uplifts are
    // granted, as round-scoped TemporaryBonuses lasting the Duração — the Atributo half with the
    // usual partial reach (a Perícia roll governed by it, not PV/PM/Conjuração).
    // The "Multiplicador de PV aumenta em +2 para cada Título" half is real too, as a
    // LIFE_MULTIPLIER TemporaryBonus read by HitPointsService#getMaxHitPoints(Character,
    // CombatantSheet) — so a transformed Anciente's max PV genuinely rises and falls back when
    // the Duração lapses. Damage on the sheet is not re-scaled, so current PV follows the max;
    // see that method for what the fall-back can do to a holder damaged while transformed.
    // "Abandonando seus traços raciais" is real: RacialTraitSuppression.ALL while the Forma
    // holds. Only the Race term of each trait goes, and the Habilidade/Atributo halves carry the
    // same sheet-reach limit noted above (the Perícia-roll path, not PV/PM/Conjuração).
    // "Nascidos da Floresta permanecem nesta forma por +2 Rodadas" is real — FormaActiveAbility's
    // per-holder extra Rodadas, which the Uplift bonuses and the FormEffect both last.
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
                    .build()) {
        private final ActiveAbility transformation =
                new FormaActiveAbility(this, FormType.ANCIENTE,
                        // "suas Defesas e Multiplicador de PV, Categoria de Tamanho aumentam em
                        // +2, enquanto seu Carisma e Foco aumentam em +1" — per Título Desperto.
                        new FormaActiveAbility.Uplift(2, 2, 1,
                                List.of(AttributeDomain.CHARISMA, AttributeDomain.FOCUS)),
                        holder -> holder.getRace() instanceof org.aventyrs.core.race.NascidoDaFloresta
                                ? NASCIDO_DA_FLORESTA_EXTRA_ROUNDS : 0);

        @Override
        public Optional<ActiveAbility> resolveActiveAbility() {
            return Optional.of(transformation);
        }

        /** "Abandonando seus traços raciais" — while an Anciente, and only then. */
        @Override
        public RacialTraitSuppression resolveRacialTraitSuppression(final Character character,
                                                                    final CombatantSheet sheet) {
            return sheet != null && sheet.isInForm(FormType.ANCIENTE)
                    ? RacialTraitSuppression.ALL
                    : RacialTraitSuppression.NONE;
        }

        /**
         * "Para cada Título Aventyr Desperto … Multiplicador de PV … aumenta em +2."
         *
         * <p>Resolved from the worn Forma rather than granted as a {@code TemporaryBonus} beside
         * it, unlike the Defesas/Categoria/Atributo halves of the same sentence. A countdown is
         * tied to itself, not to the shape: a holder leaving early through {@code
         * enterForm(null)} would keep the uplift, and one still transformed when it lapsed would
         * lose it. Max PV is too visible a number to let drift apart from the shape granting it.
         */
        @Override
        public int resolveLifeMultiplierIncrease(final Character character, final CombatantSheet sheet) {
            return sheet != null && sheet.isInForm(FormType.ANCIENTE)
                    ? ANCIENTE_LIFE_MULTIPLIER_PER_TITLE * character.getAllTitles().size()
                    : 0;
        }
    },

    /**
     * "Personagens com este Talento recebem Bônus Racial de +1 em Vigor e Vantagem nas rolagens
     * de Artes." Both halves are real — the Vigor +1 through {@link Feat#resolveAttributeBonus}
     * (computed even though no Perícia is Vigor-governed, still reaching every Atributo-total
     * reader via {@code Character#getEffectiveAttributeTotal}), the Vantagem through {@link
     * Feat#resolveSkillRollBonus}.
     */
    // Both movement figures are real, as absolute ones (Feat#resolveMovementBaseOverride):
    // Natação 6UD, and land Movimento set to 2UD before every bonus.
    // "Você possui guelras e pode respirar dentro e fora d'agua" is real
    // (Feat#allowsUnderwaterBreathing, read by AsphyxiaService's caller).
    // "Apenas Fadas e Fúrias" is real (one anyOf branch per Raça).
    SIRENIDEO(
            "A parte inferior de teu corpo, no lugar das pernas, é similar ao de uma criatura "
                    + "marinha. Você possui guelras e pode respirar dentro e fora d'agua, possui "
                    + "também Movimento Base de Natação 6UD, mas em terra seu Movimento Base é "
                    + "reduzido para apenas 2UD. Personagens com este Talento recebem Bônus Racial "
                    + "de +1 em Vigor e Vantagem nas rolagens de Artes.",
            fadasEFurias()
                    .forbiddenFeat(PIXIE)
                    .build()) {
        @Override
        public boolean allowsUnderwaterBreathing(final Character character) {
            return true;
        }

        @Override
        public Integer resolveMovementBaseOverride(final MovementMode mode, final Character character) {
            return switch (mode) {
                case SWIM -> SIRENIDEO_SWIM_MOVEMENT;
                case LAND -> SIRENIDEO_LAND_MOVEMENT;
                default -> null;
            };
        }

        /** "Apenas … recém-criados" — only a starting Talento slot can take it. */
        @Override
        public boolean isAcquirableOnlyAtCreation() {
            return true;
        }

        @Override
        public int resolveAttributeBonus(final AttributeDomain domain, final Character character) {
            return domain == AttributeDomain.VIGOR ? FEERICO_ATTRIBUTE_BONUS : 0;
        }

        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character) {
            return skillType == SkillType.ARTES ? Skill.ADVANTAGE_BONUS : 0;
        }
    },

    /**
     * "Escolha entre Artes, Atenção e Persuasão… Você recebe uma Habilidade de Competência de
     * cada Perícia escolhida e Vantagem em suas rolagens."
     *
     * <p><b>Both halves are real</b>, through {@link AdotadoPorSylphFeat} — the acquired,
     * choice-carrying form granted in place of this constant: the Vantagem per chosen Perícia,
     * and the free Habilidade de Competência from each.
     */
    // The free Habilidade de Competência per chosen Perícia is real too, recorded on
    // AdotadoPorSylphFeat and surfaced through Feat#getGrantedSkillTraits.
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
    // ⚠️ "Recebe Monstruoso com um tipo de criatura adicional" is read as "Monstruoso as an
    // additional creature type" — a prerequisite type, the way Alma Feérica grants Feérico
    // (Feat#getGrantedPrerequisiteCreatureTypes), so a Fauno may take Talentos Monstruosos.
    // The GD half is real: SceneContext#getOpposedCharacter()'s Tendência band, GOOD.
    // "Que não possuam o Talento Lupercal" is real both ways (forbiddenFeat on each).
    FAUNO(
            "Você tem o corpo coberto de pelos e maior número de feições caprinas. Recebe "
                    + "Monstruoso com um tipo de criatura adicional, também recebe Vantagem em "
                    + "suas rolagens de Furtividade e Persuasão. O GD de suas rolagens de Artes, "
                    + "Atenção e Persuasão efetuadas contra Personagens de Tendência Bondosa é "
                    + "reduzido em -1.",
            FeatRequirements.builder()
                    .requiredRace(Satiro.class)
                    .build()) {
        /** "Apenas … recém-criados" — only a starting Talento slot can take it. */
        @Override
        public boolean isAcquirableOnlyAtCreation() {
            return true;
        }

        /** "Que não possuam o Talento Lupercal" — a forward reference, so resolved on demand. */
        @Override
        public FeatRequirements getFeatRequirements() {
            return super.getFeatRequirements().toBuilder().forbiddenFeat(LUPERCAL).build();
        }

        @Override
        public Set<CreatureType> getGrantedPrerequisiteCreatureTypes(final Character character) {
            return Set.of(CreatureType.MONSTRUOSO);
        }

        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character) {
            return skillType == SkillType.FURTIVIDADE || skillType == SkillType.PERSUASAO
                    ? Skill.ADVANTAGE_BONUS : 0;
        }

        @Override
        public int resolveDifficultyReduction(final SkillType skillType, final Character character,
                                              final SceneContext sceneContext) {
            return againstTendencia(skillType, sceneContext, Alignment.GOOD);
        }
    },

    /**
     * "Sua Categoria de Tamanho muda para 0, também recebe Vantagem em suas rolagens de Artes e
     * Atenção." The Vantagem is real.
     */
    // The Categoria de Tamanho set is real, through Feat#resolveSizeCategoryOverride — stated as
    // ZERO, the value the clause names, not as the +1 step from a Sátiro's own MINUS_ONE it
    // happens to equal (the same care GnomoFeat#DUENDE takes).
    // The GD half is real: SceneContext#getOpposedCharacter()'s Tendência band, EVIL.
    LUPERCAL(
            "Sua Categoria de Tamanho muda para 0, também recebe Vantagem em suas rolagens de "
                    + "Artes e Atenção. O GD de suas rolagens de Artes, Atenção e Persuasão "
                    + "efetuadas contra Personagens de Tendência Maligna é reduzido em -1.",
            FeatRequirements.builder()
                    .requiredRace(Satiro.class)
                    .forbiddenFeat(FAUNO)
                    .build()) {
        /** "Apenas … recém-criados" — only a starting Talento slot can take it. */
        @Override
        public boolean isAcquirableOnlyAtCreation() {
            return true;
        }

        @Override
        public SizeCategory resolveSizeCategoryOverride(final Character character) {
            return SizeCategory.ZERO;
        }

        @Override
        public int resolveDifficultyReduction(final SkillType skillType, final Character character,
                                              final SceneContext sceneContext) {
            return againstTendencia(skillType, sceneContext, Alignment.EVIL);
        }

        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character) {
            return skillType == SkillType.ARTES || skillType == SkillType.ATTENTION
                    ? Skill.ADVANTAGE_BONUS : 0;
        }
    };

    /** ASAS' "exige o uso de 3PD, Feéricos usam 3PM". */
    private static final int ASAS_FLIGHT_COST = 3;

    /** ASAS' Feérico branch: "personagens recém-criados de raça Feérica". */
    private static final FeatRequirements FEERICO_ONLY = FeatRequirements.builder()
            .requiredCreatureType(CreatureType.FEERICO)
            .build();

    /** BENCAO_DE_FLORA's "reduzido em -1PM". */
    private static final int BENCAO_DE_FLORA_PM_REDUCTION = 1;

    /** PIXIE's "exige o uso de 2PM". */
    private static final int PIXIE_FLIGHT_COST = 2;

    /** PIXIE's "+3 Rodadas para cada Título Aventyr Desperto". */
    private static final int PIXIE_FLIGHT_ROUNDS_PER_TITLE = 3;

    /** ANCIENTEFORME's "Nascidos da Floresta permanecem nesta forma por +2 Rodadas". */
    private static final int NASCIDO_DA_FLORESTA_EXTRA_ROUNDS = 2;

    /** FAUNO's and LUPERCAL's "reduzido em -1" — one nível. */
    private static final int TENDENCIA_DIFFICULTY_REDUCTION = 1;

    /**
     * "O GD de suas rolagens de Artes, Atenção e Persuasão efetuadas contra Personagens de Tendência
     * …" — the opposed combatant's band; no opponent in the context reads as "not met".
     */
    private static int againstTendencia(final SkillType skillType, final SceneContext sceneContext,
                                        final Alignment band) {
        if (skillType != SkillType.ARTES && skillType != SkillType.ATTENTION && skillType != SkillType.PERSUASAO
                || sceneContext == null || sceneContext.getOpposedCharacter() == null) {
            return 0;
        }
        return sceneContext.getOpposedCharacter().getCharacter().getAlignmentBand() == band
                ? TENDENCIA_DIFFICULTY_REDUCTION : 0;
    }

    /** "Apenas Fadas e Fúrias" — one anyOf branch per Raça, to extend or build on. */
    private static FeatRequirements.FeatRequirementsBuilder fadasEFurias() {
        return FeatRequirements.builder()
                .alternative(FeatRequirements.builder().requiredRace(Fada.class).build())
                .alternative(FeatRequirements.builder().requiredRace(Furia.class).build());
    }

    /** The "+1 Bônus Racial" NINFA and SIRENIDEO each grant for their fixed Atributo. */
    private static final int FEERICO_ATTRIBUTE_BONUS = 1;

    /** {@link #ANCIENTEFORME}'s "Multiplicador de PV … aumenta em +2" — per Título Desperto. */
    private static final int ANCIENTE_LIFE_MULTIPLIER_PER_TITLE = 2;

    /** ESPIRITO_DA_FLORESTA's "Mimetizar Magias Naturais, do tipo Broto, ao custo de 2PD". */
    private static final int ESPIRITO_BROTO_COST = 2;

    /** ASAS' "Enquanto voando seu Movimento Base aumenta em +2UD". */
    private static final int ASAS_FLIGHT_BONUS = 2;

    /** PIXIE's "Movimento Base de Voo 8UD". */
    private static final int PIXIE_FLIGHT_MOVEMENT = 8;

    /** SIRENIDEO's "Movimento Base de Natação 6UD". */
    private static final int SIRENIDEO_SWIM_MOVEMENT = 6;

    /** SIRENIDEO's "em terra seu Movimento Base é reduzido para apenas 2UD". */
    private static final int SIRENIDEO_LAND_MOVEMENT = 2;

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
