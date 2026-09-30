package org.aventyrs.core.feat;

import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.defect.VulnerabilityKind;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.defect.DefectSeverity;
import org.aventyrs.core.defect.HeldDefect;
import org.aventyrs.core.defect.Limb;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.ResourceType;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.TitleIdentity;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * The effect of every Defeito at every gravidade ({@code docs/rules/defeitos-e-qualidades.txt}) —
 * {@code COMPORTAMENTO_EXCENTRICO_LEVE} is "Chato". Not Talentos in the rules text; they take a
 * {@link Feat}'s shape, as {@link AntecedenteFeat} does, so each rides the Talento hook it needs:
 * {@code Character#getFeats()} folds in the constant of every held {@code HeldDefect}. A level's
 * own choice (the limb, the sense…) is read back off that {@code HeldDefect} — {@link #heldBy}.
 *
 * <p>{@link FeatCategory#DEFEITO}: absent from {@link FeatCatalog}, never bought.
 *
 * <p>Plan Phases 2–3 ({@code docs/plans/defeitos-e-qualidades-plan.md}) are wired; what stays TODO on a
 * constant names the mechanism still missing. Desapego Material, Fobia, Memória Fraca and
 * Restrição Moral are narrative by design — the Narrador applies Fobia's Condição.
 */
@Getter
public enum DefeitoFeat implements Feat {

    COMPORTAMENTO_EXCENTRICO_LEVE(DefectSeverity.LEVE, "Chato",
            "Desvantagem em rolagens de Perícias baseadas em Carisma") {
        @Override
        public int resolveGoverningAttributeRollBonus(final AttributeDomain domain, final CombatantSheet holder,
                                                      final SceneContext sceneContext) {
            return domain == AttributeDomain.CHARISMA ? Skill.DISADVANTAGE_MALUS : 0;
        }
    },
    COMPORTAMENTO_EXCENTRICO_MODERADO(DefectSeverity.MODERADO, "Esquisitão",
            "GD das Perícias baseadas em Carisma aumenta em +1 Nível.") {
        @Override
        public int resolveGoverningAttributeDifficultyReduction(final AttributeDomain domain, final Character character,
                                                                final SkillRoll skillRoll) {
            return domain == AttributeDomain.CHARISMA ? -1 : 0;
        }
    },
    COMPORTAMENTO_EXCENTRICO_GRAVE(DefectSeverity.GRAVE, "O Louco",
            "Você é incapaz de expressar ideias de maneira clara ou objetiva, para a maioria das"
                    + " pessoas suas falas não fazem sentido, por isso você falha automaticamente em rolagens de"
                    + " perícias baseadas em Carisma.") {
        @Override
        public boolean resolveAutomaticFailure(final SkillType skillType, final AttributeDomain domain,
                                               final Character character) {
            return domain == AttributeDomain.CHARISMA;
        }
    },
    CORPO_FRAGIL_LEVE(DefectSeverity.LEVE, "Saúde Fraca",
            "Multiplicador de PV reduzido em -1.") {
        @Override
        public int resolveLifeMultiplierIncrease(final Character character) {
            return -1;
        }
    },
    /** "Doença" and "Veneno" are the Condições Doente and Envenenado. */
    CORPO_FRAGIL_MODERADO(DefectSeverity.MODERADO, "Doença Persistente",
            "Multiplicador de PV reduzido em -2. Os Malefícios “Doença” e “Veneno” tem as Durações"
                    + " dobradas em você.") {
        @Override
        public int resolveLifeMultiplierIncrease(final Character character) {
            return -2;
        }

        @Override
        public int resolveConditionDurationMultiplier(final ConditionType type, final Character character) {
            return isDiseaseOrPoison(type) ? 2 : 1;
        }
    },
    // TODO: "maximizadas" — a rolled Duração arrives already rolled (this core never rolls), so maximizing
    //  it is the caller's; only the doubling is applied here.
    CORPO_FRAGIL_GRAVE(DefectSeverity.GRAVE, "Coração de Vidro",
            "Seu Multiplicador de PV é sempre igual à 1, não é possível aumentar esta quantidade. Os"
                    + " Malefícios “Doença” e “Veneno” tem as Durações maximizadas e dobradas em você.") {
        @Override
        public Integer resolveFixedMultiplier(final ResourceType resource, final Character character) {
            return resource == ResourceType.HIT_POINTS ? 1 : null;
        }

        @Override
        public int resolveConditionDurationMultiplier(final ConditionType type, final Character character) {
            return isDiseaseOrPoison(type) ? 2 : 1;
        }
    },
    DESAPEGO_MATERIAL_LEVE(DefectSeverity.LEVE, "Minimalista",
            "Seus bens se restringem ao que você puder carregar"),
    DESAPEGO_MATERIAL_MODERADO(DefectSeverity.MODERADO, "Voto de Pobreza",
            "Você não aceita recompensas ou novos equipamentos, mantendo seus itens iniciais enquanto"
                    + " estiverem em condições de uso, substituindo por novos apenas quando eles forem"
                    + " destruídos. Você recusa novos itens mesmo se forem melhores que os seus, enquanto eles"
                    + " estiverem em um estado aceitável."),
    DESAPEGO_MATERIAL_GRAVE(DefectSeverity.GRAVE, "Recusa Material",
            "Você não possui e nem carrega nenhum item que não seja essencial para a vida, possuindo"
                    + " no máximo um Equipamento Ofensivo e um Equipam amento Defensivo (não tecnológicos) e"
                    + " roupas simples. Você recusa novos itens, mesmo que melhores que os seus, enquanto os"
                    + " atuais estiverem em condições de uso."),
    /**
     * The limb's Perícias are {@link Limb#getDependentSkills()} (a table ruling). "Danos Físicos" is read
     * as any dano roll not delivered by a Magia.
     */
    DEFICIENCIA_FISICA_LEVE(DefectSeverity.LEVE, "Dano Permanente",
            "Você sofre Desvantagem em rolagens de Perícias que dependam do membro escolhido. Caso a"
                    + " escolha tenha sido Braços, adicionalmente você sofre Desvantagem em suas rolagens de"
                    + " Danos Físicos. Se a escolha for Pernas, além do efeito comuns, Seu Movimento Base é"
                    + " reduzido em -2UD.") {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                         final SkillTrait requestedAbility, final Character character) {
            return limbOf(character).filter(limb -> limb.getDependentSkills().contains(skillType)).isPresent()
                    ? Skill.DISADVANTAGE_MALUS : 0;
        }

        @Override
        public Optional<DamageBonus> resolveDamageBonus(final SkillType attackingSkillType, final SceneContext sceneContext,
                                                        final CombatantSheet attackTarget, final Character actor,
                                                        final AttackSource attackSource) {
            return limbOf(actor).filter(limb -> limb == Limb.BRACOS).isPresent() && !(attackSource instanceof Spell)
                    ? Optional.of(new DamageBonus(Skill.DISADVANTAGE_MALUS, DamageType.FISICO))
                    : Optional.empty();
        }

        @Override
        public int resolveMovementIncrease(final Character character) {
            return limbOf(character).filter(limb -> limb == Limb.PERNAS).isPresent() ? -2 : 0;
        }
    },
    // TODO: the prosthesis toggle ("substituir os efeitos … com uso de aparatos de auxílio") back to Dano
    //  Permanente's effects — nothing records that a character is using one.
    DEFICIENCIA_FISICA_MODERADO(DefectSeverity.MODERADO, "Membro Ausente",
            "Você é incapaz de realizar rolagens de Perícias que dependam do membro ausente ou não"
                    + " funcional, caso o membro escolhido seja a pernas, adicionalmente seu Movimento Base é"
                    + " reduzido à metade. Você pode substituir os efeitos deste Defeito pelos efeitos de “Dano"
                    + " Permanente” com uso de aparatos de auxílio, como próteses ou muletas.") {
        @Override
        public boolean preventsSkillUse(final SkillType skillType, final Character character) {
            return membroAusentePrevents(skillType, character);
        }

        @Override
        public boolean halvesMovementBase(final Character character) {
            return limbOf(character).filter(limb -> limb == Limb.PERNAS).isPresent();
        }
    },
    DEFICIENCIA_FISICA_GRAVE(DefectSeverity.GRAVE, "Dependência",
            "Cumulativamente aos efeitos de Membro Ausente, você não pode adquirir Habilidades de"
                    + " Atributo de Força ou de Destreza (escolhido aleatoriamente).") {
        @Override
        public boolean preventsSkillUse(final SkillType skillType, final Character character) {
            return membroAusentePrevents(skillType, character);
        }

        @Override
        public boolean halvesMovementBase(final Character character) {
            return limbOf(character).filter(limb -> limb == Limb.PERNAS).isPresent();
        }

        @Override
        public boolean forbidsAttributeAbility(final AttributeDomain domain, final Character character) {
            return heldBy(character).flatMap(held -> held.choice(AttributeDomain.class)).filter(domain::equals).isPresent();
        }
    },
    /** "Relacionadas ao sentido escolhido" is every Atenção roll, and no other Perícia — a table ruling. */
    DEFICIENCIA_SENSORIAL_LEVE(DefectSeverity.LEVE, "Percepção nublada",
            "Seu sentido é pouco mais fraco que o comum, por isso você sofre Desvantagens em rolagens"
                    + " de Atenção - e outras Perícias quando aplicável -, mas apenas enquanto relacionadas ao"
                    + " sentido escolhido.") {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                         final SkillTrait requestedAbility, final Character character) {
            return skillType == SkillType.ATTENTION ? Skill.DISADVANTAGE_MALUS : 0;
        }
    },
    DEFICIENCIA_SENSORIAL_MODERADO(DefectSeverity.MODERADO, "Sentido ineficiente",
            "Um de seus sentidos é muito inferior ao padrão de sua raça, o GD para rolagens de Atenção"
                    + " - ou outras Perícias situacionais -, é aumentada em +1 Nível.") {
        @Override
        public int resolveDifficultyReduction(final SkillType skillType, final Character character) {
            return skillType == SkillType.ATTENTION ? -1 : 0;
        }
    },
    DEFICIENCIA_SENSORIAL_GRAVE(DefectSeverity.GRAVE, "Ausência Sensorial",
            "Você não possui o sentido escolhido, por isso falha automaticamente em rolagens de"
                    + " Atenção que envolva este sentido. Este efeito pode ser aplicado a outras perícias quando"
                    + " pertinente, conforme o Narrador.") {
        @Override
        public boolean resolveAutomaticFailure(final SkillType skillType, final AttributeDomain domain,
                                               final Character character) {
            return skillType == SkillType.ATTENTION;
        }
    },
    DESCONEXAO_COM_O_AETHER_LEVE(DefectSeverity.LEVE, "Inapto para Magias",
            "Seu Multiplicador de PM é reduzido em -2.") {
        @Override
        public int resolveManaMultiplierIncrease(final Character character) {
            return -2;
        }
    },
    DESCONEXAO_COM_O_AETHER_MODERADO(DefectSeverity.MODERADO, "Incompetência Arcana",
            "Seu Multiplicador de PM é igual à 1 e não é possível aumentar esta quantidade.") {
        @Override
        public Integer resolveFixedMultiplier(final ResourceType resource, final Character character) {
            return resource == ResourceType.MAGIC_POINTS ? 1 : null;
        }
    },
    /** "Efeitos similares" to a Descanso are read as the Descanso bonuses; other PM recovery is untouched. */
    DESCONEXAO_COM_O_AETHER_GRAVE(DefectSeverity.GRAVE, "Nulificador",
            "Seu Multiplicador de PM é igual à 1, não é possível aumentar esta quantidade e você nunca"
                    + " recupera PM com Descansos e efeitos similares.") {
        @Override
        public Integer resolveFixedMultiplier(final ResourceType resource, final Character character) {
            return resource == ResourceType.MAGIC_POINTS ? 1 : null;
        }

        @Override
        public boolean preventsRestRecovery(final ResourceType resource, final Character character) {
            return resource == ResourceType.MAGIC_POINTS;
        }
    },
    /** Desvantagem on Iniciativa is −2 on the roll made on entering a Scene — a table ruling. */
    DISTURBIO_DE_ATENCAO_LEVE(DefectSeverity.LEVE, "Desatento",
            "Desvantagem nas rolagens de Iniciativa e Atenção.") {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                         final SkillTrait requestedAbility, final Character character) {
            return desatentoRollBonus(skillType);
        }

        @Override
        public int resolveInitiativeBonus(final Character character) {
            return Skill.DISADVANTAGE_MALUS;
        }
    },
    DISTURBIO_DE_ATENCAO_MODERADO(DefectSeverity.MODERADO, "Sem Foco",
            "O mesmo que Desatento, adicionalmente você sofre Redutor de -1PA em Rodadas Ímpares das"
                    + " Cenas de Combate.") {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                         final SkillTrait requestedAbility, final Character character) {
            return desatentoRollBonus(skillType);
        }

        @Override
        public int resolveInitiativeBonus(final Character character) {
            return Skill.DISADVANTAGE_MALUS;
        }

        @Override
        public int resolveRoundActionPointsIncrease(final Character character, final SceneContext sceneContext) {
            return isOddCombatRound(sceneContext) ? -1 : 0;
        }
    },
    /** "Rodadas Ímpares" are read as a Cena de Combate's, like Sem Foco's which it builds on. */
    DISTURBIO_DE_ATENCAO_GRAVE(DefectSeverity.GRAVE, "Preso a Imaginação",
            "Similar a Sem Foco, adicionalmente você não pode efetuar Ações Livres ou Reações nas"
                    + " Rodadas Ímpares Rodada.") {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                         final SkillTrait requestedAbility, final Character character) {
            return desatentoRollBonus(skillType);
        }

        @Override
        public int resolveInitiativeBonus(final Character character) {
            return Skill.DISADVANTAGE_MALUS;
        }

        @Override
        public int resolveRoundActionPointsIncrease(final Character character, final SceneContext sceneContext) {
            return isOddCombatRound(sceneContext) ? -1 : 0;
        }

        @Override
        public boolean preventsReactions(final Character character, final SceneContext sceneContext) {
            return isOddCombatRound(sceneContext);
        }

        @Override
        public boolean preventsFreeActions(final Character character, final SceneContext sceneContext) {
            return isOddCombatRound(sceneContext);
        }
    },
    FOBIA_LEVE(DefectSeverity.LEVE, "Medo",
            "Enquanto na presença do objeto de sua fobia você recebe a Condição Abalado."),
    FOBIA_MODERADO(DefectSeverity.MODERADO, "Repulsa",
            "Como Medo, mas a Condição recebida é Assustado."),
    FOBIA_GRAVE(DefectSeverity.GRAVE, "Pavor Absoluto",
            "Como Medo, mas a Condição recebida é Apavorado."),
    HERANCA_DE_GILGAMESH_LEVE(DefectSeverity.LEVE, "Vontade Fraca",
            "Seu Multiplicador de PD é reduzido em -1 e é incapaz de Despertar seu Título Secundário.") {
        @Override
        public int resolveDeterminationMultiplierIncrease(final Character character) {
            return -1;
        }

        @Override
        public boolean permitsTitleSlot(final TitleSlot slot, final Character character) {
            return slot != TitleSlot.SECONDARY;
        }
    },
    /** "Apenas com 25 EXP" is read as the sheet's total EXP ever earned; no sheet in hand refuses the Primário. */
    HERANCA_DE_GILGAMESH_MODERADO(DefectSeverity.MODERADO, "Centelha Dormente",
            "Seu Multiplicador de PD é reduzido em -2 e é incapaz de Despertar seu Título Secundário,"
                    + " seu Título Primário (e único) será Desperto em atraso, apenas com 25 EXP.") {
        @Override
        public int resolveDeterminationMultiplierIncrease(final Character character) {
            return -2;
        }

        @Override
        public boolean permitsTitleSlot(final TitleSlot slot, final Character character) {
            return slot != TitleSlot.SECONDARY;
        }

        @Override
        public boolean permitsTitleSlot(final TitleSlot slot, final Character character, final CharacterSheet sheet) {
            return slot == TitleSlot.PRIMARY && sheet != null
                    && sheet.getTotalExperience().compareTo(DELAYED_AWAKENING_EXPERIENCE) >= 0;
        }
    },
    HERANCA_DE_GILGAMESH_GRAVE(DefectSeverity.GRAVE, "Filho de Gilgamesh",
            "Seu Multiplicador de PD é igual à 1 e não é possível aumentar esta quantidade. Você não é"
                    + " capaz de Despertar Títulos Aventyr.") {
        @Override
        public Integer resolveFixedMultiplier(final ResourceType resource, final Character character) {
            return resource == ResourceType.DETERMINATION_POINTS ? 1 : null;
        }

        /** Every slot closed, rather than a PROHIBIT opinion another Talento's ALLOW could outvote. */
        @Override
        public boolean permitsTitleSlot(final TitleSlot slot, final Character character) {
            return false;
        }

        @Override
        public TitleAcquisitionPermission resolveTitleAcquisitionPermission(final Optional<TitleIdentity> title,
                                                                            final Character character) {
            return TitleAcquisitionPermission.PROHIBIT;
        }
    },
    MEMORIA_FRACA_LEVE(DefectSeverity.LEVE, "Lapsos de Memória",
            "Você tem dificuldades para se lembrar de eventos passados e esquece as coisas com"
                    + " frequência."),
    MEMORIA_FRACA_MODERADO(DefectSeverity.MODERADO, "Amnésia",
            "Você não se recorda de nenhum evento da sua vida anterior ao início da campanha."),
    MEMORIA_FRACA_GRAVE(DefectSeverity.GRAVE, "Amnésia Recorrente",
            "Após passar por um Descanso Verdadeiro você pode escolher apenas um evento ocorrido, você"
                    + " esquece todos os outros acontecimentos do dia exceto pelo evento escolhido, ainda assim"
                    + " você não se recordará de todos os detalhes. Você também só é capaz de se recordar das"
                    + " pessoas que você com diariamente."),
    /**
     * "Danos Físicos não-Elementais" is plain {@code FISICO}; "Mágicos" {@code MAGICO}; the two Elementos
     * match an elemental hit's own Elemento. The extra damage lands on the raw hit before mitigation (⚠️ a
     * reading — "sofrerá N pontos de danos adicionais" names no stage), and every defence clause needs the
     * attack's {@code DamageDescriptor} in hand.
     */
    VULNERABILIDADE_LEVE(DefectSeverity.LEVE, "Dificuldade para Reagir",
            "Você sofre Desvantagem em rolagens de Esquiva e Aparar para evitar ataques do tipo"
                    + " escolhido, caso não consiga evitar o ataque sofrerá 2 pontos de danos adicionais.") {
        @Override
        public int resolveDefenseBonusAgainst(final DamageDescriptor descriptor, final Character character) {
            return vulnerableTo(null, descriptor, character) ? Skill.DISADVANTAGE_MALUS : 0;
        }

        @Override
        public int resolveDamageTakenIncrease(final DamageType damageType, final DamageDescriptor descriptor,
                                              final Character character) {
            return vulnerableTo(damageType, descriptor, character) ? 2 : 0;
        }
    },
    VULNERABILIDADE_MODERADO(DefectSeverity.MODERADO, "Ponto Fraco",
            "A GD de Esquiva e Aparar para evitar ataques do tipo escolhido é aumentada em +1 Nível,"
                    + " caso não consiga evitar o ataque sofrerá 3 pontos de danos adicionais.") {
        @Override
        public int resolveDefenseDifficultyReductionAgainst(final DamageDescriptor descriptor, final Character character) {
            return vulnerableTo(null, descriptor, character) ? -1 : 0;
        }

        @Override
        public int resolveDamageTakenIncrease(final DamageType damageType, final DamageDescriptor descriptor,
                                              final Character character) {
            return vulnerableTo(damageType, descriptor, character) ? 3 : 0;
        }
    },
    // TODO: "+1d6 pontos de danos destes ataques" — this core never rolls dice, and no damage-taken path
    //  reports an extra die for the caller to throw.
    VULNERABILIDADE_GRAVE(DefectSeverity.GRAVE, "Trava Mental",
            "Você não é capaz de se defender de ataques do tipo escolhido, adicionalmente você sofre"
                    + " +1d6 pontos de danos destes ataques.") {
        @Override
        public boolean preventsDefenseAgainst(final DamageDescriptor descriptor, final Character character) {
            return vulnerableTo(null, descriptor, character);
        }
    },
    // TODO: Restrição Moral stays narrative (table ruling); Moderado/Grave could later refuse attacks on
    //  Caído/Desprevenido targets.
    RESTRICAO_MORAL_LEVE(DefectSeverity.LEVE, "Seguidor da Lei",
            "Você sempre cumpre com as leis locais e nunca mente."),
    RESTRICAO_MORAL_MODERADO(DefectSeverity.MODERADO, "Código dos Cavaleiro",
            "Como Seguidor da Lei, mas seu código moral também o impede de se aproveitar de fraquezas"
                    + " alheias, você nunca se beneficia ou ataca personagens claramente mais fracos que você,"
                    + " que estejam rendidos, caídos ou desprevenidos."),
    RESTRICAO_MORAL_GRAVE(DefectSeverity.GRAVE, "Herói Interior",
            "Como Código dos Cavaleiros, porém você também sempre ajuda e protege os mais fracos mesmo"
                    + " que isso o prejudique, nunca ataca alvos em menor número (exceto quando claramente mais"
                    + " poderosos) e não permite que outros quebrem o Código dos Cavaleiro em sua presença.");

    /** Centelha Dormente's "Desperto em atraso, apenas com 25 EXP". */
    static final BigDecimal DELAYED_AWAKENING_EXPERIENCE = BigDecimal.valueOf(25);

    private final DefectSeverity severity;
    /** The level's own name — "Chato", "Coração de Vidro"… */
    private final String levelName;
    private final String description;

    DefeitoFeat(final DefectSeverity severity, final String levelName, final String description) {
        this.severity = severity;
        this.levelName = levelName;
        this.description = description;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.DEFEITO;
    }

    @Override
    public FeatRequirements getFeatRequirements() {
        return FeatRequirements.builder().build();
    }

    /** Held only through a Defeito — never bought. */
    @Override
    public boolean isAcquirableOnlyAtCreation() {
        return true;
    }

    /** The {@code HeldDefect} this constant is the effect of, on character — where its choices live. */
    Optional<HeldDefect> heldBy(final Character character) {
        return character == null ? Optional.empty()
                : character.getDefects().stream().filter(held -> held.isActive() && held.effect() == this).findFirst();
    }

    Optional<Limb> limbOf(final Character character) {
        return heldBy(character).flatMap(held -> held.choice(Limb.class));
    }

    /** Membro Ausente's "incapaz de realizar rolagens de Perícias que dependam do membro" — Dependência repeats it. */
    boolean membroAusentePrevents(final SkillType skillType, final Character character) {
        return limbOf(character).filter(limb -> limb.getDependentSkills().contains(skillType)).isPresent();
    }

    static boolean isDiseaseOrPoison(final ConditionType type) {
        return type == ConditionType.DOENTE || type == ConditionType.ENVENENADO;
    }

    /** Whether a hit of this type — descriptor first, damageType when only that is known — is the kind chosen. */
    boolean vulnerableTo(final DamageType damageType, final DamageDescriptor descriptor, final Character character) {
        DamageType effective = descriptor != null ? descriptor.damageType() : damageType;
        return heldBy(character).map(held -> held.choice(VulnerabilityKind.class).map(kind -> switch (kind) {
            case FISICO_NAO_ELEMENTAL -> effective == DamageType.FISICO;
            case MAGICO -> effective == DamageType.MAGICO;
            case ELEMENTAL -> descriptor != null && descriptor.elementalType() != null
                    && held.choicesOf(ElementalType.class).contains(descriptor.elementalType());
        }).orElse(false)).orElse(false);
    }

    /** An odd Rodada of a Cena de Combate — Rodadas count from 1; 0 is "no one has acted yet". */
    static boolean isOddCombatRound(final SceneContext sceneContext) {
        return sceneContext != null && sceneContext.isCombatScene() && sceneContext.getCurrentRound() % 2 == 1;
    }

    /** Desatento's "Desvantagem nas rolagens de … Atenção", which Sem Foco and Preso a Imaginação repeat. */
    private static int desatentoRollBonus(final SkillType skillType) {
        return skillType == SkillType.ATTENTION ? Skill.DISADVANTAGE_MALUS : 0;
    }
}
