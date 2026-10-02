package org.aventyrs.core.feat;

import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageScope;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.defect.EnergyKind;
import org.aventyrs.core.defect.HeldQuality;
import org.aventyrs.core.defect.QualityClass;
import org.aventyrs.core.item.RegaliaGrade;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.magic.MagicType;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

/**
 * The effect of every Qualidade at each class ({@code docs/rules/defeitos-e-qualidades.txt}) —
 * {@code RADIANTE_MENOR} is "Amigável". Talento-shaped like {@link DefeitoFeat}; a held Qualidade
 * Maior folds in <b>both</b> its constants, since "todas as Qualidades Maiores possuem implicitamente,
 * de forma cumulativa, os efeitos da Qualidade Menor de seu tipo". A level's own choice is read back
 * off its {@code HeldQuality} — {@link #heldBy}.
 *
 * <p>{@link FeatCategory#QUALIDADE}: absent from {@link FeatCatalog}, never bought.
 *
 * <p>Plan Phases 2–3 are wired; a constant's TODO names the mechanism still missing. Three
 * Maiores are <b>materialized at creation</b> instead ({@code
 * CharacterCreationServiceImpl#applyQualityGrant} and {@code Character#getBonusAttributeAbilitySlots}):
 * Privilegiado's Recursos, Precognição's Iniciativa and Atenção trait, and Divinal's Habilidade.
 *
 * <p>TODO: "podem ser desativadas por Malefícios temporários, como efeitos de Maldições" — blocked until
 * a Maldição carries the suppression (table ruling: out of scope for now).
 */
@Getter
public enum QualidadeFeat implements Feat {

    /**
     * Real for a Habilidade de Título, whose activation is one priced transaction: the first PD-priced one
     * of each combat is free, the first Ego-priced one costs a point — each claimed per combat, cleared by
     * {@code CombatantSheet#endCombat()}. TODO: a Talento's PD-priced activation ({@code ActiveAbility}, the
     * Formas) takes no relief yet.
     */
    CENTELHA_MAIOR_MENOR(QualityClass.MENOR, "Pronto para Ação",
            "Em cada Cena de Combate, o Primeiro Efeito de Habilidade de Título ou Talento que você"
                    + " ativar e que possua um custo em PD tem este custo reduzido à zero. Em cada Cena de"
                    + " Combate, o Primeiro Efeito de Habilidade de Título ou Talento que você ativar e que"
                    + " possua um custo em Pontos de Ego tem este custo reduzido à uma unidade.") {
        @Override
        public boolean waivesTitleActivationDeterminationCost(final CombatantSheet activator,
                                                              final SceneContext sceneContext) {
            return sceneContext != null && sceneContext.isCombatScene()
                    && !activator.isAffectedThisCombat(PRONTO_PARA_ACAO_PD);
        }

        @Override
        public boolean capsTitleActivationEgoCost(final CombatantSheet activator, final SceneContext sceneContext) {
            return sceneContext != null && sceneContext.isCombatScene()
                    && !activator.isAffectedThisCombat(PRONTO_PARA_ACAO_EGO);
        }

        @Override
        public void onTitleActivationCostRelief(final CombatantSheet activator, final boolean determinationWaived,
                                                final boolean egoCapped) {
            if (determinationWaived) {
                activator.markAffectedThisCombat(PRONTO_PARA_ACAO_PD);
            }
            if (egoCapped) {
                activator.markAffectedThisCombat(PRONTO_PARA_ACAO_EGO);
            }
        }
    },
    /**
     * "Regalia Verdadeira" is any Regalia, whatever its grade (a table ruling); ⚠️ "empunhando" is read
     * as equipped ({@code Character#possessesRegalia}), the scan every equipment clause here makes.
     */
    CENTELHA_MAIOR_MAIOR(QualityClass.MAIOR, "Sonho de Gilgamesh",
            "Seu Multiplicador de PD aumenta em +1. Enquanto estiver empunhando pelo menos uma Regalia"
                    + " Verdadeira seu Multiplicador de PD, ao invés disso, aumenta em +2.") {
        @Override
        public int resolveDeterminationMultiplierIncrease(final Character character) {
            return character.possessesRegalia(RegaliaGrade.MENOR) ? 2 : 1;
        }
    },
    /**
     * The "+2PE cada" half is real, through {@code EgoPointsService#spendResourcesForEquipmentPoints}. The
     * arc-completion point is the Narrador's to hand over ({@code EgoPointsService#grantTemporaryByNarrator}).
     */
    DESTINADO_A_FORTUNA_MENOR(QualityClass.MENOR, "Saber Investir",
            "Seus pontos temporários e permanentes de Recursos valem +2PE cada, , sempre que recuperar"
                    + " Recursos por conclusão de arco de história você adquire 1 ponto temporário de Recurso"
                    + " adicional.") {
        @Override
        public int resolveResourcesPointValueBonus(final Character character) {
            return 2;
        }
    },
    /** Materialized at creation as +1 Recursos base. */
    DESTINADO_A_FORTUNA_MAIOR(QualityClass.MAIOR, "Privilegiado",
            "Você adquire 1 Ponto permanente de Recursos."),
    ESCOLHIDO_DA_MAGIA_MENOR(QualityClass.MENOR, "Linhagem Arcana",
            "Você recebe RM e Vantagem em suas rolagens de Domínio do Mana para Conjurar Magias.") {
        /** One instance of RM. */
        @Override
        public int resolveMagicReduction(final Character character) {
            return DamageService.DEFAULT_DAMAGE_REDUCTION;
        }

        @Override
        public int resolveCastingRollBonus(final Spell spell, final Character character, final Set<Feat> activatedFeats) {
            return Skill.ADVANTAGE_BONUS;
        }
    },
    // TODO: "pode escolher negar o Efeito" of the first Magia each Cena de Combate, recovering its PM — a
    //  choice made at the moment of being affected, which no Magia path offers the target (the closest,
    //  Feat#isImmuneToSpell, is a standing immunity, not a once-per-combat opt-in).
    ESCOLHIDO_DA_MAGIA_MAIOR(QualityClass.MAIOR, "Alma de AEther",
            "Seu multiplicador de PM é aumentado em +1. A primeira vez que você for afetado por uma"
                    + " Magia em cada Cena de Combate você pode escolher negar o Efeito, se o fizer você"
                    + " recupera PM igual a quantidade de PM utilizado na conjuração.") {
        @Override
        public int resolveManaMultiplierIncrease(final Character character) {
            return 1;
        }
    },
    // TODO: the Especialização owed at the end of the first session — no in-play path grants a Perícia
    //  Especialização at all (only character creation does), and a PendingAcquisition carries no pick.
    INTELECTO_SUPERIOR_MENOR(QualityClass.MENOR, "Memória Eidética",
            "Você raramente se esquece de algo que tenha presenciado e tem facilidade em aprender"
                    + " novas coisas. Ao fim da primeira sessão de jogo você adquire uma nova Especialização de"
                    + " uma Perícia treinada que possuir."),
    // TODO: the Habilidade de Competência owed at the end of the third session — needs a session count
    //  (CharacterSheet#applySessionEndAcquisitions never learns which Sessão ended) and a pick.
    INTELECTO_SUPERIOR_MAIOR(QualityClass.MAIOR, "Dominar Padrões",
            "Você nunca se esquece nada e rapidamente aprende novas coisas. Adquirir graduações em"
                    + " Perícias, até a terceira graduação, custa 0.5 EXP a menos. Ao fim da terceira sessão de"
                    + " jogo, você adquire uma Habilidade de Competência de uma Perícia qualquer (mesmo que não"
                    + " seja treinado nela).") {
        @Override
        public BigDecimal resolveGraduationCostReduction(final Character character, final SkillType skillType,
                                                         final int targetGraduation) {
            return targetGraduation <= 3 ? GRADUATION_DISCOUNT : BigDecimal.ZERO;
        }
    },
    // TODO: Vantagem on the first roll against each Desprevenido target per Cena — the action log
    //  names a target only for attacks, so a non-attack roll can't be counted per target yet.
    OPORTUNISTA_NATO_MENOR(QualityClass.MENOR, "Sorrateiro",
            "Você adquire Vantagem na primeira Rolagem de Perícia que efetuar contra cada personagem"
                    + " Desprevenido em uma Cena."),
    /** Two separate Vantagens, so a Persuasão roll against a Desprevenido target gets both (+4). */
    OPORTUNISTA_NATO_MAIOR(QualityClass.MAIOR, "Essência Malandra",
            "Você recebe Vantagem em rolagens de Atenção e Persuasão, também recebe Vantagem em"
                    + " qualquer rolagem de Perícia efetuada contra um alvo Desprevenido (não cumulativo com"
                    + " Sorrateiro).") {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                         final SkillTrait requestedAbility, final Character character) {
            int bonus = skillType == SkillType.ATTENTION || skillType == SkillType.PERSUASAO ? Skill.ADVANTAGE_BONUS : 0;
            CombatantSheet opposed = sceneContext == null ? null : sceneContext.getOpposedCharacter();
            if (opposed != null && opposed.hasCondition(ConditionType.DESPREVENIDO, sceneContext)) {
                bonus += Skill.ADVANTAGE_BONUS;
            }
            return bonus;
        }
    },
    RADIANTE_MENOR(QualityClass.MENOR, "Amigável",
            "Vantagem em rolagem de Perícias baseadas em Carisma.") {
        @Override
        public int resolveGoverningAttributeRollBonus(final AttributeDomain domain, final CombatantSheet holder,
                                                      final SceneContext sceneContext) {
            return domain == AttributeDomain.CHARISMA ? Skill.ADVANTAGE_BONUS : 0;
        }
    },
    /**
     * An opt-in activation — named in {@code SkillRoll#getActivatedFeats()} — once per Cena, on a Perícia
     * whose own Atributo is Carisma. ⚠️ The gate reads the Perícia's natural Atributo (the sheet-level
     * permission never sees a substitution); the GD reduction itself also checks the Atributo the roll was
     * really made with.
     */
    RADIANTE_MAIOR(QualityClass.MAIOR, "Aura de Confiança",
            "Você pode reduzir o GD de uma rolagem de Perícias a cada Cena. Aura de Confiança afeta"
                    + " apenas Perícias baseada em Carisma e seu uso é restrito a apenas uma vez por Cena.") {
        @Override
        public boolean permitsActivation(final SkillType skillType, final SkillRoll skillRoll,
                                         final AttackSource attackSource, final CombatantSheet holder) {
            if (holder == null) {
                return false;
            }
            CharacterSkill skill = holder.getCharacter().getSkills().get(skillType);
            return skill != null && skill.getSkill().getAttributeDomain() == AttributeDomain.CHARISMA
                    && holder.getActionsThisCena().stream().noneMatch(action -> action.activatedFeats().contains(this));
        }

        @Override
        public int resolveGoverningAttributeDifficultyReduction(final AttributeDomain domain, final Character character,
                                                                final SkillRoll skillRoll) {
            return domain == AttributeDomain.CHARISMA && skillRoll != null && skillRoll.activated(this) ? 1 : 0;
        }
    },
    // TODO: refusing Abalado/Assustado/Apavorado while an ally nearby holds one — needs a
    //  Condição-application veto that can see the Scene.
    RESILIENCIA_HEROICA_MENOR(QualityClass.MENOR, "Motivado pelo Desafio",
            "Enquanto um de seus aliados próximos estiver Abalado, Assustado ou Apavorado você não"
                    + " pode receber estas Condições."),
    /** An Assustado or Apavorado lands as Abalado, keeping its Duração and origin. */
    RESILIENCIA_HEROICA_MAIOR(QualityClass.MAIOR, "Inabalável",
            "Você não pode receber os malefícios Assustado e Apavorado, se limitando a Abalado.") {
        @Override
        public ConditionType resolveReceivedCondition(final ConditionType applied, final Character character) {
            return applied == ConditionType.ASSUSTADO || applied == ConditionType.APAVORADO ? ConditionType.ABALADO : applied;
        }
    },
    /**
     * An Elemento halves that Elemento's damage; Energia Profana/Divina halves every hit from a Magia of that
     * {@code MagicType}. TODO: the "redução de Duração" half — nothing classifies a timed effect by energy.
     */
    RESISTENCIA_ATIPICA_MENOR(QualityClass.MENOR, "Herança Dracônica",
            "Você adquire Resistência ao tipo de Energia escolhido (redução de Duração e Meio-Dano).") {
        @Override
        public boolean halvesDamage(final DamageType damageType, final DamageDescriptor descriptor,
                                    final Character character, final CombatantSheet holder) {
            return chosenElement(character).map(element -> DamageScope.element(element).matches(damageType, descriptor))
                    .orElse(false);
        }

        @Override
        public boolean halvesSpellDamage(final Spell spell, final CombatantSheet holder) {
            return ofChosenEnergy(spell, holder);
        }
    },
    /**
     * Immune to the chosen Elemento's damage, or to a Magia of the chosen Profana/Divina {@code MagicType}
     * outright (damage and effects, as {@code Feat#isImmuneToSpell} reads). TODO: immunity to an Elemento's
     * non-damage Efeitos — nothing classifies a Condição by Elemento.
     */
    RESISTENCIA_ATIPICA_MAIOR(QualityClass.MAIOR, "Herança Divina",
            "Você é imune aos Efeitos e danos do tipo de energia escolhido.") {
        @Override
        public boolean isImmuneToDamage(final DamageType damageType, final DamageDescriptor descriptor,
                                        final Character character, final CombatantSheet holder) {
            return chosenElement(character).map(element -> DamageScope.element(element).matches(damageType, descriptor))
                    .orElse(false);
        }

        @Override
        public boolean isImmuneToSpell(final Spell spell, final CombatantSheet holder) {
            return ofChosenEnergy(spell, holder);
        }
    },
    /**
     * ⚠️ Read as +1PV, plus one more for each Título Aventyr held, "para o máximo de +3PV" — the only
     * reading under which the stated maximum means anything with the two Título slots.
     */
    SAUDE_DE_FERRO_MENOR(QualityClass.MENOR, "Vigoroso",
            "A cada Descanso Longo ou Superior você recupera +1PV adicionais, +2PV se possuir um"
                    + " Título Aventyr (para o máximo de +3PV).") {
        @Override
        public int resolveRestHitPointsBonus(final RestType restType, final Character character) {
            return restType.isAtLeast(RestType.LONGO) ? Math.min(3, 1 + character.getAllTitles().size()) : 0;
        }
    },
    /** Cumulative with Vigoroso, which a Maior also holds. */
    SAUDE_DE_FERRO_MAIOR(QualityClass.MAIOR, "Constituição Inabalável",
            "Seu Multiplicador de PV aumenta em +1, a cada Descanso Longo ou superior você recupera"
                    + " +2PV.") {
        @Override
        public int resolveLifeMultiplierIncrease(final Character character) {
            return 1;
        }

        @Override
        public int resolveRestHitPointsBonus(final RestType restType, final Character character) {
            return restType.isAtLeast(RestType.LONGO) ? 2 : 0;
        }
    },
    /** "Relacionada ao sentido escolhido" is every Atenção roll — a table ruling. */
    SENTIDO_SUPERIOR_MENOR(QualityClass.MENOR, "Sempre Alerta",
            "Vantagem em Rolagens de Atenção, ou em qualquer outra Perícia que possa se beneficiar"
                    + " deste sentido, conforme o Narrador.") {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                         final SkillTrait requestedAbility, final Character character) {
            return skillType == SkillType.ATTENTION ? Skill.ADVANTAGE_BONUS : 0;
        }
    },
    /**
     * Activated on an Atenção roll, once per game session, like {@code AntecedenteFeat#ARTISTA}; the roll
     * then succeeds whatever it totals. Its Vantagem on every other Atenção roll is Sempre Alerta's.
     */
    SENTIDO_SUPERIOR_MAIOR(QualityClass.MAIOR, "Sentir o Todo",
            "Uma vez por sessão de jogo você pode escolher ser bem-sucedido em uma Rolagem de Atenção"
                    + " (ou outra Perícia, se aplicável), em Rolagens que não utilizar este benefício você"
                    + " recebe Vantagem. Esta Vantagem é aplicável apenas quando a rolagem está relacionada ao"
                    + " sentido escolhido.") {
        @Override
        public boolean permitsActivation(final SkillType skillType, final SkillRoll skillRoll,
                                         final AttackSource attackSource, final CombatantSheet holder) {
            return skillType == SkillType.ATTENTION && holder != null && !holder.hasConsumedOncePerSession(this);
        }

        @Override
        public boolean resolveAutomaticSuccess(final SkillType skillType, final int targetValue,
                                               final SceneContext sceneContext, final Character character,
                                               final SkillRoll skillRoll) {
            return skillType == SkillType.ATTENTION && skillRoll != null && skillRoll.activated(this);
        }

        @Override
        public void onActivationRecorded(final CombatantSheet holder) {
            holder.consumeOncePerSession(this);
        }
    },
    /** Vantagem on Iniciativa is +2 on the roll made on entering a Scene — a table ruling. */
    SEXTO_SENTIDO_MENOR(QualityClass.MENOR, "Atenção Sobrenatural",
            "Você recebe Vantagem em suas rolagens de Atenção e Iniciativa.") {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                         final SkillTrait requestedAbility, final Character character) {
            return skillType == SkillType.ATTENTION ? Skill.ADVANTAGE_BONUS : 0;
        }

        @Override
        public int resolveInitiativeBonus(final Character character) {
            return Skill.ADVANTAGE_BONUS;
        }
    },
    /** Materialized at creation: +1 Iniciativa base and the chosen Atenção Especialização/Competência. */
    SEXTO_SENTIDO_MAIOR(QualityClass.MAIOR, "Precognição",
            "Você recebe 1 ponto permanente de Inciativa + uma Especialização ou Habilidade de"
                    + " Competência de Atenção."),
    /**
     * "Rodada par" is a Cena de Combate's even Rodada; "primeira" is read off the holder's own Rodada log
     * — no earlier roll this Rodada governed by Força or Destreza.
     */
    TENDENCIA_ATLETICA_MENOR(QualityClass.MENOR, "Corpo Maleável",
            "Vantagem na primeira rolagem de Perícias baseadas em Força ou Destreza efetuada a cada"
                    + " Rodada par.") {
        @Override
        public int resolveGoverningAttributeRollBonus(final AttributeDomain domain, final CombatantSheet holder,
                                                      final SceneContext sceneContext) {
            boolean athletic = domain == AttributeDomain.STRENGTH || domain == AttributeDomain.DEXTERITY;
            boolean evenRound = sceneContext != null && sceneContext.isCombatScene()
                    && sceneContext.getCurrentRound() > 0 && sceneContext.getCurrentRound() % 2 == 0;
            boolean first = holder != null && holder.getActionsThisRound().stream()
                    .noneMatch(action -> action.governingDomain() == AttributeDomain.STRENGTH
                            || action.governingDomain() == AttributeDomain.DEXTERITY);
            return athletic && evenRound && first ? Skill.ADVANTAGE_BONUS : 0;
        }
    },
    /** Materialized as a bonus Habilidade de Atributo slot of the chosen Atributo. */
    TENDENCIA_ATLETICA_MAIOR(QualityClass.MAIOR, "Divinal",
            "Escolha entre Força ou Destreza, você adquire uma Habilidade do Atributo escolhido.");

    /** Pronto para Ação's two per-combat claims — {@code CombatantSheet#markAffectedThisCombat} keys. */
    private static final String PRONTO_PARA_ACAO_PD = "QUALIDADE_PRONTO_PARA_ACAO_PD";
    private static final String PRONTO_PARA_ACAO_EGO = "QUALIDADE_PRONTO_PARA_ACAO_EGO";

    /** Dominar Padrões' "custa 0.5 EXP a menos". */
    private static final BigDecimal GRADUATION_DISCOUNT = new BigDecimal("0.5");

    private final QualityClass qualityClass;
    /** The level's own name — "Amigável", "Aura de Confiança"… */
    private final String levelName;
    private final String description;

    QualidadeFeat(final QualityClass qualityClass, final String levelName, final String description) {
        this.qualityClass = qualityClass;
        this.levelName = levelName;
        this.description = description;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.QUALIDADE;
    }

    @Override
    public FeatRequirements getFeatRequirements() {
        return FeatRequirements.builder().build();
    }

    /** Held only through a Qualidade — never bought. */
    @Override
    public boolean isAcquirableOnlyAtCreation() {
        return true;
    }

    /** The {@code HeldQuality} this constant is (one of) the effects of — where its choices live. */
    Optional<HeldQuality> heldBy(final Character character) {
        return character == null ? Optional.empty()
                : character.getQualities().stream().filter(held -> held.effects().contains(this)).findFirst();
    }

    /** Whether spell is of the Energia Profana/Divina this Resistência Atípica chose. */
    boolean ofChosenEnergy(final Spell spell, final CombatantSheet holder) {
        if (spell == null || holder == null) {
            return false;
        }
        EnergyKind energy = heldBy(holder.getCharacter()).flatMap(held -> held.choice(EnergyKind.class)).orElse(null);
        return (energy == EnergyKind.PROFANA && spell.getTree().hasMagicType(MagicType.PROFANA))
                || (energy == EnergyKind.DIVINA && spell.getTree().hasMagicType(MagicType.DIVINA));
    }

    /** Resistência Atípica's Elemento, when the energy chosen was Elemental. */
    Optional<ElementalType> chosenElement(final Character character) {
        return heldBy(character)
                .filter(held -> held.choice(EnergyKind.class).orElse(null) == EnergyKind.ELEMENTAL)
                .flatMap(held -> held.choice(ElementalType.class));
    }
}
