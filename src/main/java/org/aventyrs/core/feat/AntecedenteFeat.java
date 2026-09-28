package org.aventyrs.core.feat;

import lombok.Getter;
import org.aventyrs.core.background.AcquiredBackground;
import org.aventyrs.core.background.CareerBackground;
import org.aventyrs.core.background.OriginBackground;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.MovementMode;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.catalog.MagicTree;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.race.CreatureType;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.TerrainType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ResourceType;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.conhecimentos.ConhecimentosSpecialization;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The named Benefício of every Antecedente ({@code org.aventyrs.core.background}), one constant per
 * {@code OriginBackground}/{@code CareerBackground} and named after it. The rules text does not
 * call these Talentos; they take a {@link Feat}'s shape so each rides the Talento hook it needs
 * with no new plumbing — {@code Character#getFeats()} folds in the Benefício of every held
 * Antecedente, which also makes the activated ones reach the roll-activation check and a client's
 * activation menu as they are.
 *
 * <p>{@link FeatCategory#ANTECEDENTE}: absent from {@link FeatCatalog}, refused by {@code
 * FeatService#grantFeat} ({@link #isAcquirableOnlyAtCreation()}, and no starting slot takes the
 * category). "A Perícia escolhida" and every other pick is read back off the holder's {@link
 * AcquiredBackground}, the {@code FocoEmPericiaFeat#chosenBy} way — no acquired-form class.
 *
 * <p>Frequency readings (table rulings): "uma vez por dia" renews on a Descanso Longo, "uma vez
 * por semana" is once per game session — both claimed by {@link #onActivationRecorded}.
 */
@Getter
public enum AntecedenteFeat implements Feat {

    // ---- Naturalidade -------------------------------------------------------------------------

    DECIEMBRANO("Benefício Regional", "Movimento Base de Natação +2UD.") {
        @Override
        public int resolveModeMovementIncrease(final MovementMode mode, final Character character) {
            return mode == MovementMode.SWIM ? 2 : 0;
        }
    },

    /**
     * TODO: "Equipamentos Tecnológicos (Vapor) são considerados de uma Raridade Inferior e custam
     * -2PE" — blocked: no item carries a Tecnológico/Vapor classification (the Armas/Equipamentos
     * tables print none), so nothing can tell which items it reaches.
     */
    ELDURIANO("A Todo Vapor",
            "Equipamentos Tecnológicos (Vapor) são considerados de uma Raridade Inferior e custam -2PE."),

    /** Narrative only — nothing in this core finds water or food. */
    HARENAI("Nômades do Deserto",
            "São sempre bem-sucedidos em encontrar água e alimentos (se houver) em regiões inóspitas ou selvagens."),

    /**
     * "+1 de cada Bônus Base" per Descanso Verdadeiro is real (PV, PM and PD). TODO: "esse benefício
     * aumenta em +1 enquanto estão nos territórios de Jully" — blocked: no location/region concept.
     */
    JULLYANO("Curados pelo AEther",
            "A cada Descanso Verdadeiro recuperam +1 de cada Bônus Base, esse benefício aumenta em +1 enquanto "
                    + "estão nos territórios de Jully.") {
        @Override
        public int resolveTrueRestBonus(final ResourceType resource, final RestType restType, final Character character) {
            return 1;
        }
    },

    /** "O Dano Base de suas Armas aumenta em +1" — every Arma, natural ones included. */
    NORTENHO("Guerreiros Caçadores",
            "A vida de conflitos e caça ensinou os Nortenhos a extraírem o máximo de suas armas, por isso o Dano "
                    + "Base de suas Armas aumenta em +1.") {
        @Override
        public int resolveDamageBaseIncrease(final Character character, final Weapon weapon) {
            return weapon != null ? 1 : 0;
        }
    },

    OFI("Nascido para Aventureiras",
            "Treinados para viverem os mais diversos perigos, recebem bônus de +1 em suas Defesas.") {
        @Override
        public int resolveDefenseBonus(final DefenseType defenseType, final Character character) {
            return 1;
        }
    },

    /**
     * "Vantagem em rolagens de Conhecimentos: Natureza, mas apenas em Florestas" is real — a roll
     * naming the Especialização Natureza in a {@link TerrainType#FOREST} Cena. The labyrinth
     * clause is narrative (no maze or orientation concept).
     */
    SUDITO_DO_DRAGAO("Senso de Orientação Aprimorado",
            "Nunca se perdem em labirintos, mágicos ou mundanos, sendo sempre capaz de reconhecer onde já estiveram "
                    + "e como retornar a locais anteriores. Também recebem vantagem em rolagens de Conhecimentos: "
                    + "Natureza, mas apenas em Florestas.") {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                         final SkillTrait requestedAbility, final Character character,
                                         final AttackSource attackSource, final CombatantSheet holder,
                                         final SkillRoll skillRoll) {
            boolean inForest = sceneContext != null && sceneContext.getTerrainType() == TerrainType.FOREST;
            return skillType == SkillType.CONHECIMENTOS && requestedAbility == ConhecimentosSpecialization.NATUREZA
                    && inForest ? Skill.ADVANTAGE_BONUS : 0;
        }
    },

    /** "+2PD ou +2PM a cada Descanso" — the player picks at each Descanso (a table ruling). */
    VASTARE("Conexão com o Mana",
            "Seus hábitos de meditação os permitem recuperar Bônus Base mais rapidamente, recuperando +2PD ou +2PM "
                    + "a cada Descanso.") {
        @Override
        public Set<ResourceType> resolveRestBonusChoices(final Character character) {
            return Set.of(ResourceType.DETERMINATION_POINTS, ResourceType.MAGIC_POINTS);
        }

        @Override
        public int resolveChosenRestBonus(final ResourceType chosen, final RestType restType, final Character character) {
            return chosen == ResourceType.DETERMINATION_POINTS || chosen == ResourceType.MAGIC_POINTS ? 2 : 0;
        }
    },

    NATUREZA_LONGINQUA("Adaptação do Viajante",
            "Adquirir novas Graduações na Perícia escolhida até a 5ª Graduação custa -0.5EXP.") {
        @Override
        public BigDecimal resolveGraduationCostReduction(final Character character, final SkillType skillType,
                                                         final int targetGraduation) {
            return discountUpToFifth(targetGraduation,
                    chosenSkill(character, OriginBackground.NATUREZA_LONGINQUA).filter(skillType::equals).isPresent());
        }
    },

    SEM_PATRIA_RECONHECIDA("Vontade dos Excluídos", "Seu Multiplicador de PD aumenta em +1.") {
        @Override
        public int resolveDeterminationMultiplierIncrease(final Character character) {
            return 1;
        }
    },

    // ---- Carreira -----------------------------------------------------------------------------

    /**
     * "Iniciativa +1", and Prontidão as two combat-start Blessings: +1PA for two Rodadas and +1PA
     * for one, which is +2PA in the first Turn and +1PA in the second.
     */
    ACADEMICO_AVENTYR("Prontidão",
            "Iniciativa +1. Nos primeiros Turnos de cada combate você tem PA adicionais, +2PA no primeiro turno, "
                    + "+1PA no segundo turno.") {
        @Override
        public int resolveInitiativeBonus(final Character character) {
            return 1;
        }

        @Override
        public List<Blessing> resolveCombatStartBlessings(final Character character) {
            return List.of(new Blessing(ModifierType.ACTION_POINTS, 1, 2, TargetScope.SELF, name()),
                    new Blessing(ModifierType.ACTION_POINTS, 1, 1, TargetScope.SELF, name()));
        }
    },

    /** Any Perícia, chosen as it is used: activate it on the roll it should ease. */
    ALDEAO("Ação Rotineira",
            "Apenas uma vez por dia, renovado a cada Descanso Longo Verdadeiro, você pode reduzir a GD de uma "
                    + "rolagem de Perícia escolhida em -1 nível.") {
        @Override
        public boolean permitsActivation(final SkillType skillType, final SkillRoll skillRoll,
                                         final AttackSource attackSource, final CombatantSheet holder) {
            return holder != null && !holder.isAffectedUntilRest(this);
        }

        @Override
        public int resolveDifficultyReduction(final SkillType skillType, final Character character,
                                              final SceneContext sceneContext, final SkillRoll skillRoll) {
            return skillRoll != null && skillRoll.activated(this) ? 1 : 0;
        }

        @Override
        public void onActivationRecorded(final CombatantSheet holder) {
            holder.markAffectedUntilRest(this, RestType.LONGO);
        }
    },

    APOSTADOR("Sortudo",
            "A primeira vez em cada sessão de jogo que sua sorte se tornar zero você recebe 1 ponto temporário de Sorte.") {
        @Override
        public int resolveEgoDepletionGrant(final EgoDomain domain) {
            return domain == EgoDomain.SORTE ? 1 : 0;
        }
    },

    APRENDIZ("Teoria de Tudo",
            "A cada Rodada você pode receber Vantagem em uma de suas rolagens da Perícia escolhida.") {
        @Override
        public boolean permitsActivation(final SkillType skillType, final SkillRoll skillRoll,
                                         final AttackSource attackSource, final CombatantSheet holder) {
            return onceThisRoundOn(this, holder, skillType, chosenSkill(holder, CareerBackground.APRENDIZ));
        }

        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                         final SkillTrait requestedAbility, final Character character,
                                         final AttackSource attackSource, final CombatantSheet holder,
                                         final SkillRoll skillRoll) {
            return skillRoll != null && skillRoll.activated(this) ? Skill.ADVANTAGE_BONUS : 0;
        }
    },

    /**
     * "Escolher ser bem-sucedido … esse é um Sucesso Crítico Maior": the player chooses the result,
     * so the roll it is activated on must carry that result — dice of 6-6-6, which every critical
     * reader already takes as a Crítico Maior. Once per session (a table ruling for "por semana").
     */
    ARTISTA("Criar Obra-Prima",
            "Apenas uma vez por semana você pode escolher ser bem-sucedido em uma rolagem de Artes, esse é um "
                    + "Sucesso Crítico Maior.") {
        @Override
        public boolean permitsActivation(final SkillType skillType, final SkillRoll skillRoll,
                                         final AttackSource attackSource, final CombatantSheet holder) {
            return skillType == SkillType.ARTES && holder != null && !holder.hasConsumedOncePerSession(this)
                    && skillRoll != null && skillRoll.getTotal() == MAJOR_CRITICAL_TOTAL;
        }

        @Override
        public void onActivationRecorded(final CombatantSheet holder) {
            holder.consumeOncePerSession(this);
        }
    },

    /**
     * "+2UD", doubled in the opening Rodada when first to act — read as winning initiative, an
     * initiative-win Blessing of another +2UD for one Rodada.
     */
    ATLETA("Treinamento Atlético",
            "Seu Movimento Base aumenta em +2UD, se você for o primeiro a agir em uma Cena este Benefício é "
                    + "dobrado durante a primeira Rodada.") {
        @Override
        public int resolveMovementIncrease(final Character character) {
            return 2;
        }

        @Override
        public List<Blessing> resolveInitiativeBlessings() {
            return List.of(new Blessing(ModifierType.MOVEMENT, 2, 1, TargetScope.SELF, name()));
        }
    },

    BATEDOR("Sombra", "Adquirir novas Graduações em Atenção, até a 5ª Graduação, custa -0.5EXP.") {
        @Override
        public BigDecimal resolveGraduationCostReduction(final Character character, final SkillType skillType,
                                                         final int targetGraduation) {
            return discountUpToFifth(targetGraduation, skillType == SkillType.ATTENTION);
        }
    },

    /** "Animais e monstros" = a target whose creature type is ANIMAL or MONSTRUOSO (a table ruling). */
    CACADOR("Conhecimento de Caçador",
            "A Margem Crítica Menor dos seus ataques contra animais e monstros aumenta em +2 números.") {
        @Override
        public int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext,
                                                 final Character character, final AttackSource attackSource,
                                                 final CombatantSheet holder, final CombatantSheet attackTarget) {
            if (!skillType.isAttackSkill() || attackTarget == null) {
                return 0;
            }
            CreatureType type = attackTarget.getCreatureType();
            return type == CreatureType.ANIMAL || type == CreatureType.MONSTRUOSO ? 2 : 0;
        }
    },

    /** TODO: "Você aprende 2 novos idiomas" — blocked: this core has no Idiomas. */
    COMERCIANTE("Linguista", "Você aprende 2 novos idiomas."),

    /**
     * Magical healing is real — +1 on every Magia de cura the holder casts. TODO: the physical half
     * — blocked: a Medicina e Cura roll's healing amount is not computed by this core.
     */
    CURANDEIRO("Toque de Hipócrates", "Seus Efeitos de Cura, físicos ou mágicos, aumentam em +1.") {
        @Override
        public int resolveSpellHealingBonus(final Spell spell, final Character character, final Set<Feat> activatedFeats) {
            return 1;
        }
    },

    /** Narrative only — nothing in this core finds water, food or shelter. */
    EREMITA("Uno com Gaea",
            "Você é sempre bem-sucedido em encontrar água, comida e abrigo em locais inóspitos e selvagens. Se você "
                    + "tiver outras fontes de benefícios similares a este, a qualidade e segurança dos alimentos e "
                    + "locais encontrada é maior e mais duradoura."),

    ESCUDEIRO("Aprendiz de Cavaleiro",
            "Você recebeu instruções militares, escolha entre Esquiva e Aparar ou uma Perícia de Ataque. Adquirir "
                    + "Graduações na perícia escolhida custa -0.5EXP até a 5ª Graduação.") {
        @Override
        public BigDecimal resolveGraduationCostReduction(final Character character, final SkillType skillType,
                                                         final int targetGraduation) {
            boolean chosen = AcquiredBackground.heldBy(character, CareerBackground.ESCUDEIRO)
                    .flatMap(held -> held.benefitChoice(SkillType.class))
                    .filter(skillType::equals)
                    .isPresent();
            return discountUpToFifth(targetGraduation, chosen);
        }
    },

    /** TODO: "Você inicia o jogo com 2 Poções" — blocked: no Poção is authored in the item catalog. */
    ESPIAO("Kit de Disfarces",
            "Você inicia o jogo com 2 Poções, escolhidas entre Poções do Silêncio e Sussurros de Sylph."),

    /**
     * The chosen Árvore's Semente and Broto, cast as if learned — paying PM — without opening the
     * Árvore (a table ruling): see {@link Feat#getGrantedCastableSpells}.
     */
    ESTUDIOSO_ARCANO("Aprendizado Arcano",
            "Escolha uma Árvore de Magias, você é capaz de conjurar as Magias Semente e Broto da árvore escolhida.") {
        @Override
        public List<Spell> getGrantedCastableSpells(final Character character) {
            return AcquiredBackground.heldBy(character, CareerBackground.ESTUDIOSO_ARCANO)
                    .flatMap(held -> held.benefitChoice(MagicTree.class))
                    .map(tree -> tree.getSpells().stream()
                            .filter(spell -> spell.getBranchLevel() == BranchLevel.SEMENTE
                                    || spell.getBranchLevel() == BranchLevel.BROTO)
                            .toList())
                    .orElse(List.of());
        }
    },

    MARINHEIRO("Homem ao Mar", "Você recebe Movimento Base de Natação.") {
        @Override
        public boolean grantsMovementMode(final MovementMode mode, final Character character, final CombatantSheet holder) {
            return mode == MovementMode.SWIM;
        }
    },

    NEGOCIADOR("Dom da Negociação",
            "Apenas uma vez por dia, você pode reduzir a GD de uma rolagem da Perícia escolhida em -1 Nível.") {
        @Override
        public boolean permitsActivation(final SkillType skillType, final SkillRoll skillRoll,
                                         final AttackSource attackSource, final CombatantSheet holder) {
            return holder != null && !holder.isAffectedUntilRest(this)
                    && chosenSkill(holder, CareerBackground.NEGOCIADOR).filter(skillType::equals).isPresent();
        }

        @Override
        public int resolveDifficultyReduction(final SkillType skillType, final Character character,
                                              final SceneContext sceneContext, final SkillRoll skillRoll) {
            return skillRoll != null && skillRoll.activated(this) ? 1 : 0;
        }

        @Override
        public void onActivationRecorded(final CombatantSheet holder) {
            holder.markAffectedUntilRest(this, RestType.LONGO);
        }
    },

    NOVICO("Dominar Impulsos",
            "A primeira vez em cada sessão de jogo que seu Autocontrole se tornar zero você recebe 1 ponto "
                    + "temporário de Autocontrole.") {
        @Override
        public int resolveEgoDepletionGrant(final EgoDomain domain) {
            return domain == EgoDomain.AUTOCONTROLE ? 1 : 0;
        }
    },

    NOBRE("Carteirada", "Apenas uma vez por Rodada, você pode receber Vantagem em uma rolagem de Persuasão.") {
        @Override
        public boolean permitsActivation(final SkillType skillType, final SkillRoll skillRoll,
                                         final AttackSource attackSource, final CombatantSheet holder) {
            return onceThisRoundOn(this, holder, skillType, Optional.of(SkillType.PERSUASAO));
        }

        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                         final SkillTrait requestedAbility, final Character character,
                                         final AttackSource attackSource, final CombatantSheet holder,
                                         final SkillRoll skillRoll) {
            return skillRoll != null && skillRoll.activated(this) ? Skill.ADVANTAGE_BONUS : 0;
        }
    },

    RITUALISTA("Poder da Fé",
            "Os Deuses lhe concedem Mana adicional para atuar em nome deles e em prol dos fiéis, seu Multiplicador "
                    + "de PM aumenta em +1.") {
        @Override
        public int resolveManaMultiplierIncrease(final Character character) {
            return 1;
        }
    },

    /**
     * "Multiplicador de PV +1", and Retribuição Atroz as an activation: an attack, while below
     * {@link #RETRIBUICAO_HIT_POINTS} PV, against a target that has damaged the holder this Cena
     * ({@code CombatantSheet#wasDamagedByThisCena} — the Cena is how far back this core remembers),
     * gains Dano equal to the Multiplicador de PV; then it is spent until a Descanso Longo.
     */
    SELVAGEM("Retribuição Atroz",
            "Multiplicador de PV +1. Enquanto seus PV forem inferiores à 10 você recebe Bônus de Danos igual ao "
                    + "seu Multiplicador de PV, este benefício afeta apenas um ataque e só pode ser direcionado a um "
                    + "alvo que tenha lhe infligido danos. Após utilizar deste benefício você o perde até passar por "
                    + "um Descanso Longo.") {
        @Override
        public int resolveLifeMultiplierIncrease(final Character character) {
            return 1;
        }

        @Override
        public boolean permitsActivation(final SkillType skillType, final SkillRoll skillRoll,
                                         final AttackSource attackSource, final CombatantSheet holder,
                                         final SceneContext sceneContext) {
            if (!skillType.isAttackSkill() || holder == null || holder.isAffectedUntilRest(this)) {
                return false;
            }
            if (new HitPointsServiceImpl().getCurrentHitPoints(holder.getCharacter(), holder) >= RETRIBUICAO_HIT_POINTS) {
                return false;
            }
            CombatantSheet target = sceneContext == null ? null : sceneContext.getOpposedCharacter();
            return target == null || holder.wasDamagedByThisCena(target.getId());
        }

        @Override
        public Optional<DamageBonus> resolveDamageBonus(final SkillType attackingSkillType, final SceneContext sceneContext,
                                                        final CombatantSheet attackTarget, final Character actor,
                                                        final AttackSource attackSource, final int targetCount,
                                                        final CombatantSheet holder, final SkillRoll skillRoll) {
            if (skillRoll == null || !skillRoll.activated(this) || holder == null || attackTarget == null
                    || !holder.wasDamagedByThisCena(attackTarget.getId())) {
                return Optional.empty();
            }
            return Optional.of(new DamageBonus(new HitPointsServiceImpl().getLifeMultiplier(actor, holder), DamageType.FISICO));
        }

        @Override
        public void onActivationRecorded(final CombatantSheet holder) {
            holder.markAffectedUntilRest(this, RestType.LONGO);
        }
    },

    /**
     * "Redução de Danos Sofridos 1 para resistir ao primeiro ataque de cada Cena de Combate" — RDS 1
     * while the holder has taken no hit yet this Cena ({@code CombatantSheet#getHitsReceivedThisCena}).
     */
    SOLDADO("Tolerância Marcial",
            "Redução de Danos Sofridos 1 para resistir ao primeiro ataque de cada Cena de Combate.") {
        @Override
        public int resolveDamageTakenReduction(final Character character, final CombatantSheet holder) {
            return holder != null && holder.getHitsReceivedThisCena() == 0 ? 1 : 0;
        }
    },

    TROMBADINHA("Dar Fuga", "Uma vez por Rodada você pode receber Vantagem nas rolagens da Perícia escolhida.") {
        @Override
        public boolean permitsActivation(final SkillType skillType, final SkillRoll skillRoll,
                                         final AttackSource attackSource, final CombatantSheet holder) {
            return onceThisRoundOn(this, holder, skillType, chosenSkill(holder, CareerBackground.TROMBADINHA));
        }

        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                         final SkillTrait requestedAbility, final Character character,
                                         final AttackSource attackSource, final CombatantSheet holder,
                                         final SkillRoll skillRoll) {
            return skillRoll != null && skillRoll.activated(this) ? Skill.ADVANTAGE_BONUS : 0;
        }
    };

    /**
     * Whether this Benefício is spent on a roll — named in {@code SkillRoll#getActivatedFeats()},
     * gated by {@link #permitsActivation} — rather than always on. What a client lists as a per-roll
     * opt-in: Ação Rotineira, Teoria de Tudo, Criar Obra-Prima, Dom da Negociação, Carteirada,
     * Retribuição Atroz (on an attack) and Dar Fuga.
     */
    public boolean isRollActivated() {
        return switch (this) {
            case ALDEAO, APRENDIZ, ARTISTA, NEGOCIADOR, NOBRE, SELVAGEM, TROMBADINHA -> true;
            default -> false;
        };
    }

    /** A 3d6 of 6-6-6 — what "escolher ser bem-sucedido … um Sucesso Crítico Maior" sets the dice to. */
    public static final int MAJOR_CRITICAL_TOTAL = 18;

    /** Retribuição Atroz's "enquanto seus PV forem inferiores à 10". */
    public static final int RETRIBUICAO_HIT_POINTS = 10;

    /** "-0.5EXP … até a 5ª Graduação". */
    static final BigDecimal GRADUATION_DISCOUNT = new BigDecimal("0.5");
    static final int GRADUATION_DISCOUNT_MAX = 5;

    /** The Benefício's own name — "Prontidão", "Sortudo", "Benefício Regional"… */
    private final String benefitName;
    private final String description;

    AntecedenteFeat(final String benefitName, final String description) {
        this.benefitName = benefitName;
        this.description = description;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.ANTECEDENTE;
    }

    @Override
    public FeatRequirements getFeatRequirements() {
        return FeatRequirements.builder().build();
    }

    /** Held only through an Antecedente, chosen at creation — never bought. */
    @Override
    public boolean isAcquirableOnlyAtCreation() {
        return true;
    }

    private static Optional<SkillType> chosenSkill(final Character character,
                                                   final org.aventyrs.core.background.Background background) {
        return AcquiredBackground.heldBy(character, background).flatMap(AcquiredBackground::chosenSkill);
    }

    private static Optional<SkillType> chosenSkill(final CombatantSheet holder,
                                                   final org.aventyrs.core.background.Background background) {
        return holder == null ? Optional.empty() : chosenSkill(holder.getCharacter(), background);
    }

    /** "Uma vez por Rodada … da Perícia escolhida". */
    private static boolean onceThisRoundOn(final Feat feat, final CombatantSheet holder, final SkillType rolled,
                                           final Optional<SkillType> scope) {
        return holder != null && scope.filter(rolled::equals).isPresent()
                && holder.countFeatActivationsThisRound(feat) == 0;
    }

    private static BigDecimal discountUpToFifth(final int targetGraduation, final boolean applies) {
        return applies && targetGraduation <= GRADUATION_DISCOUNT_MAX ? GRADUATION_DISCOUNT : BigDecimal.ZERO;
    }
}
