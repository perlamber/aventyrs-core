package org.aventyrs.core.feat;

import lombok.Getter;
import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CriticalDamage;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.Deity;
import org.aventyrs.core.character.DeityCategory;
import org.aventyrs.core.character.DevotionTier;
import org.aventyrs.core.character.MovementMode;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.effect.Excomungar;
import org.aventyrs.core.effect.ExplosaoCataclismica;
import org.aventyrs.core.effect.RemoverAflicao;
import org.aventyrs.core.effect.ToqueSombrio;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.magic.MagicType;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.scene.InitiativePosition;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.TerrainType;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.ActivationWindow;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.HealingSource;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.sheet.TemporaryEffect;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillTraitCatalog;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.conhecimentos.ConhecimentosSpecialization;
import org.aventyrs.core.skill.furtividade.FurtividadeSpecialization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Talentos de Devoção — Panteão de Tellus ({@code docs/rules/talentos.txt}, "TALENTOS DE DEVOTO"; core 0.0.86).
 * Each is split across three rungs, <b>Adepto</b>, <b>Fiel</b> and <b>Fundamentalista</b>, and a rung applies while
 * the holder's {@code Character#getDevotionTier()} reaches it ({@link #reached}). Table rulings (2026-10-01): the
 * player picks the tier at creation and the Narrador raises or lowers it on role-play alone, so nothing here prices
 * or earns one; a lowered tier silences the rungs above it.
 *
 * <p><b>Rung picks.</b> Astúcia de Sylph, Escolhido de Gaea, Mentalidade de Tesla, Armamento Vulcano and Impacto
 * Ymiriano ask the player to choose at some rung ({@link #resolveRungChoices}). A pick is made when the holder first
 * reaches that rung (at creation for the starting tier) and is <b>kept</b> on the character ({@code
 * Character#getDevotionPicks()}, recorded by {@code DevotionService#recordPicks}), so lowering the tier silences it
 * and raising it back restores it. The bare constant is what is held; its picks are read off the holder.
 *
 * <p>Two prerequisites the Divindade alone decides: "Devoto de X" is {@code FeatRequirements#requiredDeity}, and
 * Cultista Umbral's "Cultista de um Senhor Umbral" is {@code #requiredDeityCategory}. ⚠️ "Devoto de Gaea" and
 * "Devoto de Surt'Eldur" are read as exactly that {@link Deity}; the paired Divindades ({@code GAEA_E_FLORA}, {@code
 * SURT_ELDUR_E_BOROS}…) do not count.
 *
 * <p>⚠️ <b>Umbral</b> (table ruling): an Umbral <i>effect</i> is a Magia of an Árvore de Magia Umbral ({@link
 * MagicType#UMBRAL}). An Umbral <i>character</i> is read as one devoted to a Senhor Umbral or to A Esquecida — the only
 * devotions with access to the Força Umbral ({@link #isUmbral}).
 *
 * <p><b>Several Divindades</b> (core 0.0.87): {@link #SINCRETISMO_RELIGIOSO} adds a second one genuinely, {@link
 * #FALSA_DEVOCAO} one followed falsely; {@link #tierFor} is what a rung reads — the character's tier for a genuine
 * devotion, Adepto for a feigned one. Not authored: the Palavras de Poder, a whole casting subsystem — see {@code
 * docs/plans/devoto-tiers-plan.md}.
 */
@Getter
public enum DevotoFeat implements Feat {

    /**
     * Adepto: "Resistência às Correntes de Efeitos +2" — real, the Resoluto-style margin ({@code
     * Feat#resolveEffectChainResistanceIncrease}). Fiel: "Suas Magias e Habilidades que tenham como alvos um ou mais
     * aliados recebem a Corrente de Efeitos – Remover Aflição" — real for Magias ({@code
     * SpellCastingResult#getGrantedEffectChains()}; ⚠️ any non-hostile combatant target, the caster included).
     * Fundamentalista: "+2 em suas Defesas" (real) "e suas Magias divinas recebem a Corrente de Efeitos – Excomungar"
     * (real, Magias whose Árvore is {@link MagicType#DIVINA}).
     */
    // TODO: Remover Aflição on a Habilidade (de Título) targeting allies — a Título activation reports no Corrente.
    ACOLITO_DA_LUZ_PRIMORDIAL(
            "Adepto: Resistência às Correntes de Efeitos +2. Fiel: Suas Magias e Habilidades que tenham como alvos "
                    + "um ou mais aliados recebem a Corrente de Efeitos – Remover Aflição. Fundamentalista: Você recebe "
                    + "Bônus de +2 em suas Defesas e suas Magias divinas recebem a Corrente de Efeitos – Excomungar.",
            FeatRequirements.builder().requiredDeity(Deity.LUZ_PRIMORDIAL).build()) {
        @Override
        public int resolveEffectChainResistanceIncrease(final Character holder, final InitiativePosition position) {
            return reached(holder, DevotionTier.ADEPTO) ? EFFECT_CHAIN_RESISTANCE : 0;
        }

        @Override
        public List<EffectChain> resolveSpellEffectChains(final Spell spell, final CombatantSheet caster,
                                                          final CombatantSheet target, final boolean hostileTarget) {
            List<EffectChain> chains = new ArrayList<>();
            Character holder = caster.getCharacter();
            if (reached(holder, DevotionTier.FIEL) && target != null && !hostileTarget) {
                chains.add(new RemoverAflicao(caster, spell));
            }
            if (reached(holder, DevotionTier.FUNDAMENTALISTA) && spell.getTree() != null
                    && spell.getTree().hasMagicType(MagicType.DIVINA)) {
                chains.add(new Excomungar());
            }
            return chains;
        }

        @Override
        public int resolveDefenseBonus(final DefenseType defenseType, final Character character) {
            return reached(character, DevotionTier.FUNDAMENTALISTA) ? 2 : 0;
        }
    },

    /**
     * Adepto: "Vantagem em suas Rolagens de Furtividade e Perícias de Ataque durante a noite e em locais com ampla
     * cobertura do sol (como construções e cavernas)" — real for the places, ⚠️ read as a Cena whose terrain is {@link
     * TerrainType#CAVE} or {@link TerrainType#URBAN}. Fiel: "Seus ataques físicos e Mágicos recebem a Corrente de
     * Efeito – Toque Sombrio" (real; "evitar claridades" is the player's to keep). Fundamentalista: "Seus ataques
     * recebem Roubo de Vida 2" (real, {@code Feat#resolveGrantedLifeSteal}) "mas todos os outros efeitos de curas
     * (incluindo Descansos) são reduzidos em -3" (real, {@code Feat#resolveHealingReceivedAdjustment}).
     */
    // TODO: "durante a noite" — this core has no time of day.
    ADEPTO_DA_ESCURIDAO_PROFUNDA(
            "Adepto: Você recebe Vantagem em suas Rolagens de Furtividade e Perícias de Ataque durante a noite e em "
                    + "locais com ampla cobertura do sol (como construções e cavernas). Fiel: Você deve evitar "
                    + "claridades e luz solar, assim como efeitos Divinos e de Fogo. Seus ataques físicos e Mágicos "
                    + "recebem a Corrente de Efeito – Toque Sombrio. Fundamentalista: Você nunca se expõe diretamente a "
                    + "luz ou quaisquer formas de iluminação. Seus ataques recebem Roubo de Vida 2, mas todos os outros "
                    + "efeitos de curas (incluindo Descansos) são reduzidos em -3.",
            FeatRequirements.builder().requiredDeity(Deity.ESCURIDAO_PROFUNDA).build()) {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character) {
            boolean shaded = sceneContext != null
                    && (sceneContext.isTerrain(TerrainType.CAVE) || sceneContext.isTerrain(TerrainType.URBAN));
            return reached(character, DevotionTier.ADEPTO) && shaded
                    && (skillType == SkillType.FURTIVIDADE || skillType.isAttackSkill()) ? Skill.ADVANTAGE_BONUS : 0;
        }

        @Override
        public List<EffectChain> resolveEffectChains(final Character attacker, final SkillType attackSkill,
                                                     final AttackSource attackSource) {
            return reached(attacker, DevotionTier.FIEL) ? List.of(new ToqueSombrio()) : List.of();
        }

        @Override
        public int resolveGrantedLifeSteal(final Character character) {
            return reached(character, DevotionTier.FUNDAMENTALISTA) ? 2 : 0;
        }

        @Override
        public int resolveHealingReceivedAdjustment(final Character holder, final HealingSource source) {
            return reached(holder, DevotionTier.FUNDAMENTALISTA) ? -3 : 0;
        }
    },

    /**
     * Adepto: "O Dano Base de todas as suas armas aumenta em +1" (real). Fiel: "Os Bônus em Defesas de todas as
     * Armaduras ou Escudos (à sua escolha) que você utilizar aumenta em +1" — table ruling: pick Armaduras or Escudos,
     * +1 Defesas <b>in total</b> while wearing that kind. Fundamentalista: "Margem Crítica Menor de seus Ataques
     * aumentam em +1" (real) "Margem Crítica Menor de seus inimigos são reduzidas em -1" — ⚠️ read as against the
     * holder: one number of Resistência a Críticos.
     */
    ARMAMENTO_VULCANO(
            "Adepto: O Dano Base de todas as suas armas aumenta em +1. Fiel: Os Bônus em Defesas de todas as Armaduras "
                    + "ou Escudos (à sua escolha) que você utilizar aumenta em +1. Fundamentalista: Margem Crítica Menor "
                    + "de seus Ataques aumentam em +1, Margem Crítica Menor de seus inimigos são reduzidas em -1.",
            FeatRequirements.builder().requiredDeity(Deity.VULCANO).build()) {
        @Override
        public int resolveDamageBaseIncrease(final Character character, final Weapon weapon) {
            return reached(character, DevotionTier.ADEPTO) && weapon != null ? 1 : 0;
        }

        @Override
        public List<FeatChoice<?>> resolveRungChoices(final DevotionTier rung, final Character holder) {
            return rung == DevotionTier.FIEL
                    ? List.of(FeatChoice.ofOne(VulcanArmament.class, List.of(VulcanArmament.values())))
                    : List.of();
        }

        @Override
        public int resolveDefenseBonus(final DefenseType defenseType, final Character character) {
            return picks(character, DevotionTier.FIEL, VulcanArmament.class).stream()
                    .anyMatch(kind -> kind.isWornBy(character)) ? 1 : 0;
        }

        @Override
        public int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext,
                                                 final Character character) {
            return reached(character, DevotionTier.FUNDAMENTALISTA) && skillType.isAttackSkill() ? 1 : 0;
        }

        @Override
        public int resolveCriticalResistance(final Character character, final SceneContext sceneContext) {
            return reached(character, DevotionTier.FUNDAMENTALISTA) ? 1 : 0;
        }
    },

    /**
     * Adepto: "uma Habilidade de Competência de Atenção". Fiel: "uma Habilidade de Competência de Persuasão".
     * Fundamentalista: "uma segunda Habilidade de Competência de Atenção e de Persuasão". All real, each a rung pick
     * granted while the rung is reached ({@code Feat#getGrantedSkillTraits}). Pré-requisito "3 Graduações em
     * 'Persuasão' e em Atenção" — no Divindade.
     */
    ASTUCIA_DE_SYLPH(
            "A verdade não importa se seu argumento for melhor. Adepto: Você recebe uma Habilidade de Competência de "
                    + "Atenção. Fiel: Você recebe uma Habilidade de Competência de Persuasão. Fundamentalista: Você "
                    + "recebe uma segunda Habilidade de Competência de Atenção e de Persuasão.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.PERSUASAO)
                    .requiredSkillGraduation(3)
                    .alternative(FeatRequirements.builder()
                            .requiredSkillType(SkillType.ATTENTION)
                            .requiredSkillGraduation(3)
                            .build())
                    .build()) {
        @Override
        public List<FeatChoice<?>> resolveRungChoices(final DevotionTier rung, final Character holder) {
            return switch (rung) {
                case ADEPTO -> List.of(competency(SkillType.ATTENTION));
                case FIEL -> List.of(competency(SkillType.PERSUASAO));
                case FUNDAMENTALISTA -> List.of(competency(SkillType.ATTENTION), competency(SkillType.PERSUASAO));
            };
        }

        @Override
        public List<SkillTrait> getGrantedSkillTraits(final Character character) {
            return grantedTraits(character);
        }
    },

    /**
     * Adepto: "Você recebe RM" (real, one instance). Fiel: "Sua resistência à Corrente de Efeitos aumenta em +2"
     * (real). Fundamentalista: "RDS para resistir aos ataques de Monstros e personagens devotos de outras Divindades
     * (exceto Luz Primordial), adicionalmente recebe Roubo de Vida 1 em seus ataques efetuados contra personagens
     * devotos de outras Divindades (exceto Luz Primordial)" — real, read off the attacker / the target ({@link
     * #devotedElsewhere}).
     */
    BENCAO_DE_SURT_ELDUR(
            "Adepto: Você recebe RM. Fiel: Sua resistência à Corrente de Efeitos aumenta em +2. Fundamentalista: Você "
                    + "recebe RDS para resistir aos ataques de Monstros e personagens devotos de outras Divindades "
                    + "(exceto Luz Primordial), adicionalmente recebe Roubo de Vida 1 em seus ataques efetuados contra "
                    + "personagens devotos de outras Divindades (exceto Luz Primordial).",
            FeatRequirements.builder().requiredDeity(Deity.SURT_ELDUR).build()) {
        @Override
        public int resolveMagicReduction(final Character character) {
            return reached(character, DevotionTier.ADEPTO) ? DamageService.DEFAULT_DAMAGE_REDUCTION : 0;
        }

        @Override
        public int resolveEffectChainResistanceIncrease(final Character holder, final InitiativePosition position) {
            return reached(holder, DevotionTier.FIEL) ? EFFECT_CHAIN_RESISTANCE : 0;
        }

        @Override
        public int resolveDamageTakenReductionAgainst(final Character holder, final CombatantSheet holderSheet,
                                                      final CombatantSheet attacker) {
            return reached(holder, DevotionTier.FUNDAMENTALISTA)
                    && (attacker instanceof MonsterSheet || devotedElsewhere(holder, attacker))
                    ? DamageService.DAMAGE_TAKEN_REDUCTION_INSTANCE : 0;
        }

        @Override
        public int resolveTargetedLifeSteal(final Character attacker, final CombatantSheet holder,
                                            final CombatantSheet target) {
            return reached(attacker, DevotionTier.FUNDAMENTALISTA) && devotedElsewhere(attacker, target) ? 1 : 0;
        }
    },

    /**
     * Adepto: "Enquanto em terra seu Movimento Base aumenta em +2UD" — real on the land figure; ⚠️ a mode figure that
     * starts from it (flight, swim, climb) gets the +2 taken back off. Fiel: "Enquanto se movimentando em terra, você
     * recebe Bônus de +2 em Defesas" — ⚠️ read as having moved this Rodada — "e você pode ignorar Terreno Difícil"
     * (real). Fundamentalista: "RDS" (real, one instance).
     */
    CAMINHAR_DE_EPONA(
            "Adepto: Enquanto em terra seu Movimento Base aumenta em +2UD. Fiel: Enquanto se movimentando em terra, "
                    + "você recebe Bônus de +2 em Defesas e você pode ignorar Terreno Difícil. Fundamentalista: RDS.",
            FeatRequirements.builder().requiredDeity(Deity.EPONA).build()) {
        @Override
        public int resolveMovementIncrease(final Character character) {
            return reached(character, DevotionTier.ADEPTO) ? EPONA_LAND_MOVEMENT : 0;
        }

        @Override
        public int resolveModeMovementIncrease(final MovementMode mode, final Character character) {
            return mode != MovementMode.LAND && reached(character, DevotionTier.ADEPTO) ? -EPONA_LAND_MOVEMENT : 0;
        }

        @Override
        public int resolveDefenseBonus(final DefenseType defenseType, final Character character,
                                       final SceneContext sceneContext, final CombatantSheet holder) {
            return reached(character, DevotionTier.FIEL) && holder != null && holder.getMovementsTakenThisRound() > 0
                    ? 2 : 0;
        }

        @Override
        public boolean ignoresDifficultTerrain(final Character character, final CombatantSheet holder) {
            return reached(character, DevotionTier.FIEL);
        }

        @Override
        public int resolveDamageTakenReduction(final Character character) {
            return reached(character, DevotionTier.FUNDAMENTALISTA) ? DamageService.DAMAGE_TAKEN_REDUCTION_INSTANCE : 0;
        }
    },

    /**
     * Adepto: "Vantagem nas rolagens das Perícias 'Conhecimentos: Natureza', 'Empatia Selvagem' e ‘Medicina e Cura’"
     * (real; Conhecimentos only when the roll names its Natureza Especialização). Fiel: "duas Habilidade de
     * Competências escolhidas entre as Perícias Conhecimentos, Empatia Selvagem e Medicina e Cura" (real, a rung
     * pick). Fundamentalista: "A GD de suas rolagens [dessas Perícias] é reduzida em -1 nível" (real).
     */
    ESCOLHIDO_DE_GAEA(
            "Adepto: Você recebe Vantagem nas rolagens das Perícias 'Conhecimentos: Natureza', 'Empatia Selvagem' e "
                    + "‘Medicina e Cura’. Fiel: Você recebe duas Habilidade de Competências escolhidas entre as Perícias "
                    + "Conhecimentos, Empatia Selvagem e Medicina e Cura. Fundamentalista: A GD de suas rolagens das "
                    + "Perícias 'Conhecimentos: Natureza', 'Empatia Selvagem' e ‘Medicina e Cura’ é reduzida em -1 nível.",
            FeatRequirements.builder().requiredDeity(Deity.GAEA).build()) {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character) {
            return reached(character, DevotionTier.ADEPTO) && gaeaScope(skillType, requestedAbility)
                    ? Skill.ADVANTAGE_BONUS : 0;
        }

        @Override
        public List<FeatChoice<?>> resolveRungChoices(final DevotionTier rung, final Character holder) {
            if (rung != DevotionTier.FIEL) {
                return List.of();
            }
            List<SkillTrait> options = new ArrayList<>();
            for (SkillType skill : List.of(SkillType.CONHECIMENTOS, SkillType.EMPATIA_SELVAGEM,
                    SkillType.MEDICINA_E_CURA)) {
                options.addAll(SkillTraitCatalog.competencyAbilitiesOf(skill));
            }
            return List.of(new FeatChoice<>(SkillTrait.class, 2, options));
        }

        @Override
        public List<SkillTrait> getGrantedSkillTraits(final Character character) {
            return grantedTraits(character);
        }

        @Override
        public int resolveDifficultyReduction(final SkillType skillType, final Character character,
                                              final SceneContext sceneContext, final SkillRoll skillRoll) {
            return reached(character, DevotionTier.FUNDAMENTALISTA)
                    && gaeaScope(skillType, skillRoll == null ? null : skillRoll.getRequestedAbility()) ? 1 : 0;
        }
    },

    /**
     * Adepto: "Escolha um efeito entre: Danos físicos causados +1 ou Defesas +1" (real, a rung pick). Fiel: "Ao custo
     * de 2PD e apenas uma vez por Rodada, o tipo de dano de seu ataque muda para Elemental: Gelo (Físico ou Mágico,
     * conforme tipo do ataque) e recebe a Corrente de Efeito – Explosão Cataclísmica" — real through {@link
     * ImpactoYmirianoActiveAbility} (2PD, once per Rodada) and the window it opens. ⚠️ The window lasts the Rodada, so
     * every attack the holder makes in it is Gelo and carries the Corrente. Fundamentalista: "Bônus Variável de +1 em
     * Força ou Vigor, a sua escolha" (real, a rung pick).
     */
    IMPACTO_YMIRIANO(
            "Adepto: Escolha um efeito entre: Danos físicos causados +1 ou Defesas +1. Fiel: Ao custo de 2PD e apenas "
                    + "uma vez por Rodada, o tipo de dano de seu ataque muda para Elemental: Gelo (Físico ou Mágico, "
                    + "conforme tipo do ataque) e recebe a Corrente de Efeito – Explosão Cataclísmica. Fundamentalista: "
                    + "Você recebe Bônus Variável de +1 em Força ou Vigor, a sua escolha.",
            FeatRequirements.builder().requiredDeity(Deity.YMIR).build()) {
        @Override
        public List<FeatChoice<?>> resolveRungChoices(final DevotionTier rung, final Character holder) {
            return switch (rung) {
                case ADEPTO -> List.of(FeatChoice.ofOne(YmirianImpact.class, List.of(YmirianImpact.values())));
                case FIEL -> List.of();
                case FUNDAMENTALISTA -> List.of(FeatChoice.ofOne(AttributeDomain.class,
                        List.of(AttributeDomain.STRENGTH, AttributeDomain.VIGOR)));
            };
        }

        @Override
        public Optional<DamageBonus> resolveDamageBonus(final SkillType attackingSkillType,
                                                        final SceneContext sceneContext,
                                                        final CombatantSheet attackTarget, final Character actor) {
            return attackingSkillType.isAttackSkill()
                    && picks(actor, DevotionTier.ADEPTO, YmirianImpact.class).contains(YmirianImpact.DANOS)
                    ? Optional.of(new DamageBonus(1, DamageType.FISICO)) : Optional.empty();
        }

        @Override
        public int resolveDefenseBonus(final DefenseType defenseType, final Character character) {
            return picks(character, DevotionTier.ADEPTO, YmirianImpact.class).contains(YmirianImpact.DEFESAS) ? 1 : 0;
        }

        @Override
        public Optional<ActiveAbility> resolveActiveAbility() {
            return Optional.of(ImpactoYmirianoActiveAbility.INSTANCE);
        }

        @Override
        public DamageDescriptor resolveDamageRetype(final Character attacker, final SkillType attackSkill,
                                                    final AttackSource attackSource, final CombatantSheet holder) {
            if (!ymirianWindowOpen(attacker, holder)) {
                return null;
            }
            return new DamageDescriptor(attackSource instanceof Spell ? DamageType.ELEMENTAL
                    : DamageType.FISICO_ELEMENTAL, ElementalType.GELO);
        }

        @Override
        public List<EffectChain> resolveEffectChains(final Character attacker, final SkillType attackSkill,
                                                     final AttackSource attackSource, final CombatantSheet holder,
                                                     final SceneContext sceneContext,
                                                     final CriticalResult criticalResult) {
            return ymirianWindowOpen(attacker, holder) ? List.of(new ExplosaoCataclismica()) : List.of();
        }

        @Override
        public int resolveAttributeBonus(final AttributeDomain domain, final Character character) {
            return picks(character, DevotionTier.FUNDAMENTALISTA, AttributeDomain.class).contains(domain) ? 1 : 0;
        }
    },

    /**
     * Adepto: "uma nova Especialização de Conhecimentos". Fiel: "uma Habilidade de Competência de Conhecimentos". Both
     * real, rung picks. Fundamentalista: "O GD de suas rolagens de Conhecimentos é reduzido em -1 nível" (real).
     */
    MENTALIDADE_DE_TESLA(
            "Adepto: Você adquire uma nova Especialização de Conhecimentos. Fiel: Você adquire uma Habilidade de "
                    + "Competência de Conhecimentos. Fundamentalista: O GD de suas rolagens de Conhecimentos é reduzido "
                    + "em -1 nível.",
            FeatRequirements.builder().requiredDeity(Deity.TESLA).build()) {
        @Override
        public List<FeatChoice<?>> resolveRungChoices(final DevotionTier rung, final Character holder) {
            return switch (rung) {
                case ADEPTO -> List.of(FeatChoice.ofOne(SkillTrait.class,
                        List.copyOf(SkillTraitCatalog.specializationsOf(SkillType.CONHECIMENTOS))));
                case FIEL -> List.of(competency(SkillType.CONHECIMENTOS));
                case FUNDAMENTALISTA -> List.of();
            };
        }

        @Override
        public List<SkillTrait> getGrantedSkillTraits(final Character character) {
            return grantedTraits(character);
        }

        @Override
        public int resolveDifficultyReduction(final SkillType skillType, final Character character) {
            return reached(character, DevotionTier.FUNDAMENTALISTA) && skillType == SkillType.CONHECIMENTOS ? 1 : 0;
        }
    },

    /**
     * Adepto: "Efeitos de cura de Habilidades e Magias que sejam Ativos ou Conjurados por você são aumentados em +1"
     * (real, {@code Feat#resolveHealingDealtBonus}). Fiel: "+1 nas suas Defesas e Vantagem Rolagens de Perícia de
     * Ataque e Domínio do Mana enquanto em uma embarcação, rio ou mar" — ⚠️ read as a Cena whose terrain is {@link
     * TerrainType#AQUATIC}. Fundamentalista: "Após ser alvo de Magias ou Habilidades, conjurados ou ativados por outros
     * personagens, que te permitam recuperar PV o dano de seus ataques aumentam em +1 por 1 Rodada" (real, {@code
     * Feat#resolveHealingReceivedBlessings}).
     */
    TOCADO_POR_UNDINE_E_HALOI(
            "Adepto: Efeitos de cura de Habilidades e Magias que sejam Ativos ou Conjurados por você são aumentados em "
                    + "+1. Fiel: Recebe +1 nas suas Defesas e Vantagem Rolagens de Perícia de Ataque e Domínio do Mana "
                    + "enquanto em uma embarcação, rio ou mar. Fundamentalista: Após ser alvo de Magias ou Habilidades, "
                    + "conjurados ou ativados por outros personagens, que te permitam recuperar PV o dano de seus "
                    + "ataques aumentam em +1 por 1 Rodada.",
            FeatRequirements.builder().requiredDeity(Deity.UNDINE_E_HALOI).build()) {
        @Override
        public int resolveHealingDealtBonus(final Character healer, final HealingSource source) {
            return reached(healer, DevotionTier.ADEPTO) && isMagiaOrHabilidade(source) ? 1 : 0;
        }

        @Override
        public int resolveDefenseBonus(final DefenseType defenseType, final Character character,
                                       final SceneContext sceneContext) {
            return reached(character, DevotionTier.FIEL) && afloat(sceneContext) ? 1 : 0;
        }

        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character) {
            return reached(character, DevotionTier.FIEL) && afloat(sceneContext)
                    && (skillType.isAttackSkill() || skillType == SkillType.DOMINIO_DO_MANA) ? Skill.ADVANTAGE_BONUS : 0;
        }

        @Override
        public List<Blessing> resolveHealingReceivedBlessings(final Character holder, final HealingSource source,
                                                              final CombatantSheet holderSheet) {
            boolean byAnother = source != null && source.healer() != null && source.healer() != holderSheet;
            return reached(holder, DevotionTier.FUNDAMENTALISTA) && byAnother && isMagiaOrHabilidade(source)
                    ? List.of(new Blessing(ModifierType.DAMAGE_ROLL_BONUS, 1, 1, TargetScope.SELF, name()))
                    : List.of();
        }
    },

    /**
     * Adepto: "Vantagem em Furtividade para rolagens da especialização Maestria da Ocultação" (real). Fiel: "+1 em
     * Defesas para resistir a efeitos não-umbrais" — real: +1 everywhere, taken back against a Magia of an Árvore
     * Umbral ({@code Feat#resolveSpellDefenseBonus}). Fundamentalista — see the TODOs.
     */
    // TODO: Fiel's "sente a presença de personagens e criaturas umbrais em Distância Longa" — no senses in this core.
    // Fundamentalista's sombra conselheira is SubordinateService#summonShadowCounsel (core 0.0.98).
    ABRACADO_PELA_ESQUECIDA(
            "Adepto: Recebe Vantagem em Furtividade para rolagens da especialização Maestria da Ocultação. Fiel: "
                    + "Recebe Bônus de +1 em Defesas para resistir a efeitos não-umbrais e você sente a presença de "
                    + "personagens e criaturas umbrais em Distância Longa (não é possível saber o local exato, apenas "
                    + "sente a existência). Fundamentalista: Apenas uma vez por Cena, você pode fazer uma breve oração à "
                    + "Esquecida (Tempo de Ação 3PA) para invocar uma sombra conselheira, que te auxilia agindo como um "
                    + "Subordinado Peão, Cavaleiro ou Torre por Concentração +1 Rodada.",
            FeatRequirements.builder().requiredDeity(Deity.A_ESQUECIDA).build()) {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character) {
            return concealment(character, skillType, requestedAbility);
        }

        @Override
        public int resolveDefenseBonus(final DefenseType defenseType, final Character character) {
            return reached(character, DevotionTier.FIEL) ? 1 : 0;
        }

        @Override
        public int resolveSpellDefenseBonus(final DefenseType defenseType, final Spell spell,
                                            final CombatantSheet holder) {
            return holder != null && reached(holder.getCharacter(), DevotionTier.FIEL) && isUmbral(spell) ? -1 : 0;
        }
    },

    /**
     * Adepto: as Abraçado pela Esquecida's (real). Fiel: "A Margem Crítica Menor de suas Perícias de Ataque e Persuasão
     * roladas contra personagens não Umbrais aumentam em +1" (real, against {@code SceneContext#getOpposedCharacter()}
     * when it is not {@link #isUmbral}). Fundamentalista: "Dano Crítico contra alvos não Umbrais aumentam em +3"
     * (real, a flat {@code CriticalDamage}).
     */
    CULTISTA_UMBRAL(
            "Adepto: Recebe Vantagem em Furtividade para rolagens da especialização Maestria da Ocultação. Fiel: A "
                    + "Margem Crítica Menor de suas Perícias de Ataque e Persuasão roladas contra personagens não "
                    + "Umbrais aumentam em +1. Fundamentalista: Dano Crítico contra alvos não Umbrais aumentam em +3.",
            FeatRequirements.builder().requiredDeityCategory(DeityCategory.SENHOR_UMBRAL).build()) {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character) {
            return concealment(character, skillType, requestedAbility);
        }

        @Override
        public int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext,
                                                 final Character character) {
            return reached(character, DevotionTier.FIEL)
                    && (skillType.isAttackSkill() || skillType == SkillType.PERSUASAO)
                    && againstNonUmbral(sceneContext) ? 1 : 0;
        }

        @Override
        public CriticalDamage resolveCriticalDamage(final SkillType attackSkill, final SceneContext sceneContext,
                                                    final Character character, final AttackSource attackSource,
                                                    final CriticalResult criticalResult, final SkillRoll skillRoll) {
            return reached(character, DevotionTier.FUNDAMENTALISTA) && againstNonUmbral(sceneContext)
                    ? new CriticalDamage(0, 3) : null;
        }
    },

    /**
     * "Escolha uma segunda Divindade, você deve seguir as Obrigações e Restrições da divindade escolhida e é
     * considerado um Devoto de ambas as divindades" (core 0.0.87). Real: the pick ({@link ChosenDeityFeat}) joins
     * {@code Character#getGenuineDevotions()}, so its Talentos de Devoção are eligible and read the character's own
     * tier. Pré-requisito "Apenas Devotos de Deuses Elementais ou Senhores Umbrais". Tagged Geral/Devoto/Destino.
     */
    // TODO: "custa apenas 1EXP para Homens-Fera, mas apenas para culto a Gaea e um Senhor Umbral (ou A Esquecida)" —
    //  a Talento's EXP cost is the Race's (Race#getNewFeatCost), which sees no Talento's pick.
    SINCRETISMO_RELIGIOSO(
            "Escolha uma segunda Divindade, você deve seguir as Obrigações e Restrições da divindade escolhida e é "
                    + "considerado um Devoto de ambas as divindades. Este Talento custa apenas 1EXP para Homens-Fera, "
                    + "mas apenas para culto a Gaea e um Senhor Umbral (ou A Esquecida).",
            FeatRequirements.builder()
                    .alternative(FeatRequirements.builder().requiredDeityCategory(DeityCategory.DEUS_ELEMENTAL).build())
                    .alternative(FeatRequirements.builder().requiredDeityCategory(DeityCategory.SENHOR_UMBRAL).build())
                    .build()) {
        @Override
        public List<FeatChoice<?>> resolveRequiredChoices(final Character holder) {
            return List.of(FeatChoice.ofOne(Deity.class, otherDeities(holder)));
        }
    },

    /**
     * "Escolha uma Divindade, você vive falsamente sobre os dogmas da divindade escolhida e é beneficiado por isso.
     * Você é considerado Devoto Adepto da divindade escolhida para efeitos de Talentos e Habilidades mesmo que não
     * siga as Obrigações e Restrições" (core 0.0.87). Real: the pick joins {@code Character#getFeignedDevotions()} —
     * its Talentos de Devoção are eligible and read Adepto ({@link #tierFor}). Pré-requisito "Devotos de Sylph, da
     * Escuridão Profunda, de Senhores Umbrais ou dA Esquecida". Tagged Geral/Devoto/Especialista.
     */
    FALSA_DEVOCAO(
            "Escolha uma Divindade, você vive falsamente sobre os dogmas da divindade escolhida e é beneficiado por "
                    + "isso. Você é considerado Devoto Adepto da divindade escolhida para efeitos de Talentos e "
                    + "Habilidades mesmo que não siga as Obrigações e Restrições.",
            FeatRequirements.builder()
                    .alternative(FeatRequirements.builder().requiredDeity(Deity.SYLPH).build())
                    .alternative(FeatRequirements.builder().requiredDeity(Deity.ESCURIDAO_PROFUNDA).build())
                    .alternative(FeatRequirements.builder().requiredDeity(Deity.A_ESQUECIDA).build())
                    .alternative(FeatRequirements.builder().requiredDeityCategory(DeityCategory.SENHOR_UMBRAL).build())
                    .build()) {
        @Override
        public List<FeatChoice<?>> resolveRequiredChoices(final Character holder) {
            return List.of(FeatChoice.ofOne(Deity.class, otherDeities(holder)));
        }

        @Override
        public boolean isEspecialistaTagged() {
            return true;
        }
    };

    /** "Resistência às Correntes de Efeitos +2" — added to the margin a Corrente must clear, as Resoluto does. */
    public static final int EFFECT_CHAIN_RESISTANCE = 2;

    /** Caminhar de Epona's "+2UD". */
    static final int EPONA_LAND_MOVEMENT = 2;

    private final FeatCategory featCategory = FeatCategory.DEVOTO;
    private final String description;
    private final FeatRequirements featRequirements;

    DevotoFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    /**
     * What rung asks the player to pick for this Talento, in order — empty for a rung that asks nothing. Read by
     * {@code DevotionService#owedPicks} when the holder first reaches the rung.
     */
    public List<FeatChoice<?>> resolveRungChoices(final DevotionTier rung, final Character holder) {
        return List.of();
    }

    /**
     * Whether holder's devotion <b>to this Talento's Divindade</b> reaches rung — see {@link #tierFor}.
     */
    public boolean reached(final Character holder, final DevotionTier rung) {
        DevotionTier tier = tierFor(holder);
        return tier != null && tier.reaches(rung);
    }

    /**
     * The tier holder's rungs of this Talento read (core 0.0.87): the character's own tier when it is genuinely
     * devoted to this Talento's Divindade (its own, or a Sincretismo Religioso one); Adepto when it only follows it
     * falsely (Falsa Devoção — "considerado Devoto Adepto"); the higher of the two when both hold; {@code null} when
     * neither. A Talento that names no Divindade (Astúcia de Sylph) reads the character's tier as it stands.
     */
    public DevotionTier tierFor(final Character holder) {
        if (holder == null) {
            return null;
        }
        if (featRequirements.requiredDeity() == null && featRequirements.requiredDeityCategory() == null) {
            return holder.getDevotionTier();
        }
        DevotionTier genuine = holder.getGenuineDevotions().stream().anyMatch(this::namesDeity)
                ? holder.getDevotionTier() : null;
        DevotionTier feigned = holder.getFeignedDevotions().stream().anyMatch(this::namesDeity)
                ? DevotionTier.ADEPTO : null;
        if (genuine == null) {
            return feigned;
        }
        return feigned == null || genuine.reaches(feigned) ? genuine : feigned;
    }

    /** Whether deity is the Divindade this Talento's Pré-requisito names, or of the category it names. */
    private boolean namesDeity(final Deity deity) {
        return deity != null && (deity == featRequirements.requiredDeity()
                || featRequirements.requiredDeityCategory() != null
                        && deity.getCategory() == featRequirements.requiredDeityCategory());
    }

    /** The picks rung made for this Talento, of type, while holder's devotion reaches it — empty otherwise. */
    <T> List<T> picks(final Character holder, final DevotionTier rung, final Class<T> type) {
        if (!reached(holder, rung)) {
            return List.of();
        }
        return holder.getDevotionPicks(this, rung).stream().filter(type::isInstance).map(type::cast).toList();
    }

    /** Every Habilidade/Especialização picked at a rung holder's devotion reaches. */
    List<SkillTrait> grantedTraits(final Character holder) {
        List<SkillTrait> traits = new ArrayList<>();
        for (DevotionTier rung : DevotionTier.values()) {
            traits.addAll(picks(holder, rung, SkillTrait.class));
        }
        return List.copyOf(traits);
    }

    /** One Habilidade de Competência of skill. */
    private static FeatChoice<SkillTrait> competency(final SkillType skill) {
        return FeatChoice.ofOne(SkillTrait.class, List.copyOf(SkillTraitCatalog.competencyAbilitiesOf(skill)));
    }

    /** Escolhido de Gaea's Perícias — Conhecimentos only for its Natureza Especialização. */
    private static boolean gaeaScope(final SkillType skillType, final SkillTrait requestedAbility) {
        return skillType == SkillType.EMPATIA_SELVAGEM || skillType == SkillType.MEDICINA_E_CURA
                || skillType == SkillType.CONHECIMENTOS && requestedAbility == ConhecimentosSpecialization.NATUREZA;
    }

    /** The Adepto rung Abraçado pela Esquecida and Cultista Umbral share. */
    int concealment(final Character character, final SkillType skillType,
                                   final SkillTrait requestedAbility) {
        return reached(character, DevotionTier.ADEPTO) && skillType == SkillType.FURTIVIDADE
                && requestedAbility == FurtividadeSpecialization.MAESTRIA_DA_OCULTACAO ? Skill.ADVANTAGE_BONUS : 0;
    }

    /** ⚠️ On an embarcação, rio ou mar — read as an {@link TerrainType#AQUATIC} Cena. */
    private static boolean afloat(final SceneContext sceneContext) {
        return sceneContext != null && sceneContext.isTerrain(TerrainType.AQUATIC);
    }

    /** A heal from a Magia or a Habilidade — what Tocado por Undine e Haloi's two healing rungs name. */
    private static boolean isMagiaOrHabilidade(final HealingSource source) {
        return source != null && (source.spell() != null || source.titleAbility() != null);
    }

    /** Whether Impacto Ymiriano's 2PD window is open on holder, and its Fiel rung reached. */
    boolean ymirianWindowOpen(final Character attacker, final CombatantSheet holder) {
        return holder != null && reached(attacker, DevotionTier.FIEL)
                && holder.hasActivationWindow(ImpactoYmirianoActiveAbility.INSTANCE);
    }

    /**
     * Whether character is Umbral — ⚠️ read as devoted to a Senhor Umbral or to A Esquecida, the devotions that reach
     * the Força Umbral. {@code null} is not.
     */
    public static boolean isUmbral(final Character character) {
        return character != null && character.getDevotedDeities().stream()
                .anyMatch(deity -> deity.getCategory() == DeityCategory.SENHOR_UMBRAL || deity == Deity.A_ESQUECIDA);
    }

    /** Whether spell is an Umbral effect — a Magia of an Árvore de Magia Umbral (table ruling). */
    public static boolean isUmbral(final Spell spell) {
        return spell != null && spell.getTree() != null && spell.getTree().hasMagicType(MagicType.UMBRAL);
    }

    /** Whether the roll is against a named character who is not Umbral. */
    private static boolean againstNonUmbral(final SceneContext sceneContext) {
        CombatantSheet opposed = sceneContext == null ? null : sceneContext.getOpposedCharacter();
        return opposed != null && !isUmbral(opposed.getCharacter());
    }

    /**
     * Whether other is a character devoted to a Divindade other than holder's, Luz Primordial aside — Bênção de
     * Surt'Eldur's "personagens devotos de outras Divindades (exceto Luz Primordial)". A foe ({@code MonsterSheet}) is
     * judged by its own Divindade, like anyone.
     */
    static boolean devotedElsewhere(final Character holder, final CombatantSheet other) {
        if (other == null || other.getCharacter() == null) {
            return false;
        }
        Deity theirs = other.getCharacter().getDeity();
        return theirs != null && theirs != Deity.NENHUMA && theirs != Deity.LUZ_PRIMORDIAL
                && theirs != holder.getDeity();
    }

    /** Armamento Vulcano's Fiel pick — "Armaduras ou Escudos (à sua escolha)". */
    public enum VulcanArmament {
        ARMADURAS(ItemCategory.ARMOR),
        ESCUDOS(ItemCategory.SHIELD);

        private final ItemCategory category;

        VulcanArmament(final ItemCategory category) {
            this.category = category;
        }

        /** Whether character wears an item of this kind. */
        public boolean isWornBy(final Character character) {
            return character.getEquipment().stream().map(Item::getCategory).anyMatch(category::equals);
        }
    }

    /** Impacto Ymiriano's Adepto pick — "Danos físicos causados +1 ou Defesas +1". */
    public enum YmirianImpact {
        DANOS,
        DEFESAS
    }

    /**
     * Impacto Ymiriano's Fiel rung — "Ao custo de 2PD e apenas uma vez por Rodada". Activating pays the 2PD (an Ação
     * Livre, ⚠️ the clause states no Tempo) and opens a one-Rodada {@link ActivationWindow} keyed by this ability, while
     * which the holder's attacks are Gelo and carry Explosão Cataclísmica. Its Resfriamento of 1 Rodada is the "uma vez
     * por Rodada". Usable only while the holder's devotion reaches Fiel.
     */
    public static final class ImpactoYmirianoActiveAbility implements ActiveAbility {

        public static final ImpactoYmirianoActiveAbility INSTANCE = new ImpactoYmirianoActiveAbility();

        private ImpactoYmirianoActiveAbility() {
        }

        @Override
        public String getDescription() {
            return "Impacto Ymiriano (Fiel): o tipo de dano de seu ataque muda para Elemental: Gelo e recebe a Corrente "
                    + "de Efeito – Explosão Cataclísmica.";
        }

        @Override
        public ActionCost getActionPointCost() {
            return ActionCost.FREE_ACTION;
        }

        @Override
        public int getDeterminationPointCost() {
            return 2;
        }

        @Override
        public int getCooldownRounds() {
            return 1;
        }

        @Override
        public int getDurationInRounds() {
            return 1;
        }

        @Override
        public int getMagicPointCost() {
            return 0;
        }

        @Override
        public TemporaryEffect resolveEffect(final Character character) {
            return new ActivationWindow(this, 1);
        }

        @Override
        public boolean isUsableBy(final CombatantSheet activator) {
            return IMPACTO_YMIRIANO.reached(activator.getCharacter(), DevotionTier.FIEL);
        }
    }

    /** Every Divindade but "Nenhuma" and the ones holder is already devoted to — Sincretismo's and Falsa's options. */
    private static List<Deity> otherDeities(final Character holder) {
        return Arrays.stream(Deity.values())
                .filter(deity -> deity != Deity.NENHUMA)
                .filter(deity -> holder == null || !holder.getDevotedDeities().contains(deity))
                .toList();
    }

    /** Every Talento de Devoção. */
    public static List<DevotoFeat> all() {
        return Arrays.asList(values());
    }
}
