package org.aventyrs.core.magic.catalog;

import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.magic.ActivationTime;
import org.aventyrs.core.magic.AuthoredSpell;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.SpellData;
import org.aventyrs.core.magic.SpellDuration;
import org.aventyrs.core.magic.SpellTargeting;
import org.aventyrs.core.magic.SpellTree;
import org.aventyrs.core.scene.AreaOfEffect;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;

/**
 * REANIMAR (Profana/Invocação) — eight Magias, diverging at Muda into a creation line and a
 * servant-management line, never converging.
 *
 * <h2>The one tree with a creature already built for it</h2>
 *
 * {@code org.aventyrs.core.monster.summon.Zumbi} <b>is</b> {@link #REANIMAR}'s stat block, and it
 * is a real {@code SummonedMonsterTemplate} parameterized by the Conjurador's Graduações em
 * Domínio do Mana — the tiered bonuses at 4, 7 and 10 Graduações are live on it. What is still
 * missing is only the link: {@code Spell} carries no column pointing at a template, so nothing
 * connects the Magia to the creature it raises. Criar Carniçal and Invocar Abantesma would each
 * want the same treatment.
 *
 * <p>Both of the catalog's <b>blank {@code Efeito Crítico:} lines</b> are in this tree, on its two
 * shallowest rungs.
 */
public enum ReanimarSpell implements AuthoredSpell {

    /**
     * <b>Its {@code Efeito Crítico:} line is blank in the source document</b> — the descriptor is
     * printed with nothing after it, so {@code getCriticalEffectType()} is {@code null}.
     *
     * <p>Also the catalog's only {@code Duração: Até o final do turno}, which is what {@code
     * DurationKind#UNTIL_END_OF_TURN} exists for — a turn boundary rather than a count, so {@code
     * inRodadas()} is empty for it.
     */
    NECROPOTENCIA(SpellData.builder()
            .name("Necropotência")
            .branchLevel(BranchLevel.SEMENTE)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.EASY)
            .description("Cria uma fenda escura que fortalece a primeira magia Profana que passar por ela.")
            .primaryEffectDescription("O conjurador desta magia cria uma espécie de portal a sua frente, que "
                    + "desaparece no final do mesmo turno que é conjurado. Este efeito segue o conjurador, "
                    + "permanecendo sempre a sua frente. A primeira magia Profana que que tocar o portal, ou passar "
                    + "por ela, será Necropotêncializada, garantindo Vantagem em sua rolagem de Ataque e de Efeito.")
            .secondaryEffectDescription("Sacrifício Mortis: O conjurador pode pegar a essência de um servo "
                    + "Morto-Vivo voluntário, o destruindo no processo. Se o fizer criará um portal maior e mais "
                    + "denso, capaz de maximizar os dados de dano da magia que passar por ela.")
            .duration(SpellDuration.UNTIL_END_OF_TURN)
            .targeting(SpellTargeting.distancia(Range.ADJACENTE))
            .build()),

    /**
     * <b>Its {@code Efeito Crítico:} line is blank too</b> — the second and last of the catalog's
     * two blanks.
     *
     * <p>Its Zumbi is {@code org.aventyrs.core.monster.summon.Zumbi}, already built; see the class
     * javadoc. The one clause of that stat block still unbuildable there is the Divine-magic
     * inversion ("sofrem Danos de Magias Divinas que recuperam PV ao invés de se curarem"), since
     * {@code CombatantSheet#heal} has no hook to redirect a recovery into damage.
     */
    REANIMAR(SpellData.builder()
            .name("Reanimar")
            .branchLevel(BranchLevel.BROTO)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.MEDIUM)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Transforma um cadáver em um Zumbi ou Esqueleto.")
            .primaryEffectDescription("O conjurador pode transformar um cadáver em um morto-vivo do tipo Zumbi. "
                    + "Mortos-Vivos destes tipos não possuem inteligência e perdem quaisquer conhecimentos de sua "
                    + "vida anterior. "
                    + "Criaturas criadas com esta magia não recuperam PV com Descansos e Magias Divinas, não "
                    + "precisam dormir, comer ou respirar, e não sofrem Desvantagem em rolagens de Perícias baseadas "
                    + "em Força, Destreza e Instinto, devido à falta de Treinamento.")
            .secondaryEffectDescription("Servo Cadavérico: O morto-vivo gerado com esta magia não consegue executar "
                    + "ações de formas independentes, mas são prestativos e podem ser usados como ajudantes. O corpo "
                    + "é reanimado como um Subordinado do tipo Cavaleiro ou Torre. Conjurar a magia desta forma lhe "
                    + "concede a Corrente de Efeitos – Escudeiro Cadavérico: O morto-vivo gerado conta como um "
                    + "Subordinado Prodigioso nas 2 primeiras Rodadas de Efeito desta magia.")
            .duration(SpellDuration.concentracaoMais(2))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * TODO healing a target is real ({@code CombatantSheet#heal}), but "Como parte dessa magia é
     * preciso tocar um Personagem Morto-Vivo" restricts it to the undead, and this core has no
     * vitality classification on a {@code Character} — {@code MonsterTemplate#isUndead()} is a
     * deliberately narrow stand-in that a Magia cannot reach.
     */
    CURA_MORTIS(SpellData.builder()
            .name("Cura Mortis")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.REANIMAR_ALTERNATIVO)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Esta magia permite ao seu conjurador curar mortos-vivos.")
            .primaryEffectDescription("Como parte dessa magia é preciso tocar um Personagem Morto-Vivo. "
                    + "O alvo recupera 2d6 + Metade do Foco PV, você perde uma quantidade de pontos de vida igual a "
                    + "metade deste valor. "
                    + "Não é possível curar a si mesmo com esta Magia.")
            .effectChainDescription("Regeneração Mortis: Adicionalmente aos efeitos anteriores o alvo recupera 2PV "
                    + "por Metade do Foco Rodadas.")
            .secondaryEffectDescription("Necrofagia: A Perícia Chave para conjurar esta magia muda para Ataque "
                    + "Corpo-a-Corpo e a GD para DM do Alvo. O Morto-Vivo tocado sofre 2d6+Metade do Foco pontos de "
                    + "Dano, você recupera uma quantidade de PV igual a metade do dano causado.")
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * TODO its Carniçal is a stat block (Força 6, Destreza 4, Vigor 4, Gnose 1, Instinto 4, Foco 1,
     * Carisma 1; Tamanho from the corpse used, 3PA, 26PV at Multiplicador de PV x4, Defesas +11,
     * Danos 2d6+3) sharing the Zumbi's Anatomia de Morto-Vivo Menor — including the five Efeito
     * Crítico immunities, which {@code CriticalEffectType} can already express exactly.
     */
    CRIAR_CARNICAL(SpellData.builder()
            .name("Criar Carniçal")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.REANIMAR_PRINCIPAL)
            .activationTime(ActivationTime.pa(4))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .description("Carniçais são criaturas similares aos zumbis, mas são ágeis e inteligentes. Diferente dos "
                    + "Zumbis, Carniçais possuem maior variedade de Atributos, habilidades e graduações em Perícias.")
            .primaryEffectDescription("Cria um Carniçal para lutar ao lado do conjurador.")
            .secondaryEffectDescription("Açougueiro: Você pode aumentar o custo de Conjuração desta magia em +5PM ao "
                    + "para criar seu Carniçal. Se o fizer não poderá controlar mais nenhum outro Carniçal com esta "
                    + "magia. Seres criados com este método não possuem Duração, permanecendo ativos até terem seus "
                    + "PV reduzidos à 0. Recebem +1PA, Vantagem nas rolagens de Perícias de Ataque e Danos, possuem "
                    + "Roubo de Vida 1 e recuperam 1d6PV sempre que reduzem os PV de um personagem vivo à zero ou "
                    + "menos. "
                    + "Os PM adicionais utilizados para conjurar essa magia são recuperados apenas se este "
                    + "Morto-Vivo for destruído e somente após você passar por um Descanso Longo.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.concentracaoMais(2))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * "Este é um efeito de Meio-Dano" names {@code ModifierType#HALF_DAMAGE} outright, and {@code
     * DamageService} applies halving for real as its last mitigation stage. TODO what is missing is
     * the redirection: half the damage lands on a <em>different</em> combatant, and nothing moves
     * damage from one sheet to another. Its Corrente sends it back at the attacker instead, which
     * is the retaliation gap.
     */
    ESCUDO_DE_CADAVERES(SpellData.builder()
            .name("Escudo de Cadáveres")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.REANIMAR_ALTERNATIVO)
            .activationTime(ActivationTime.REACAO)
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .description("O conjurador pode transferir parte dos danos que sofreria para um Morto-Vivo sob seu "
                    + "controle.")
            .primaryEffectDescription("Esta magia pode ser conjurada apenas enquanto você for o alvo primário de um "
                    + "ataque, metade do dano que o ataque causaria a você ao invés disso é causado a um morto-vivo "
                    + "adjacente que você controle, ou que te veja como um aliado. "
                    + "Este é um efeito de Meio-Dano.")
            .effectChainDescription("Necro Sadismo: Esta Corrente de Efeitos é ativada apenas quando o atacante for "
                    + "um personagem morto-vivo. Metade dos Danos que seriam causados a você ao invés disso é "
                    + "causado ao próprio atacante.")
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.PESSOAL)
            .build()),

    /**
     * TODO its Abantesma has Força and Vigor "Nula" and no PV at all — losing Duração instead of
     * Hit Points when damaged. {@code AttributeValue} has no null state and {@code
     * HitPointsService} derives a maximum from Vigor, so a creature outside both is not
     * expressible; nor is damage that shortens an effect rather than reducing a pool.
     */
    INVOCAR_ABANTESMA(SpellData.builder()
            .name("Invocar Abantesma")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.REANIMAR_PRINCIPAL)
            .activationTime(ActivationTime.pa(5))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .description("Ao tocar um cadáver você Invoca um Fantasma para lhe servir.")
            .primaryEffectDescription("Um breve ritual que permite invocar uma criatura inteligente que tenha "
                    + "morrido naquele local ou nas proximidades. O conjurador não escolhe quem será invocado, uma "
                    + "alma aleatória receberá o chamado. "
                    + "Fantasmas atacam à Distância, são incorpóreos e imunes à danos Físicos (exceto danos "
                    + "Elementais), seus ataques possuem a Corrente de Efeitos – Possessão Furiosa: O alvo é "
                    + "possuído (efeito de possessão) por 2 Rodadas e sempre ataca o aliado mais próximo. "
                    + "Personagens possuídos desta forma não podem Conjurar Magias que não sejam Raciais ou ativar "
                    + "Habilidades Aventyr, mas são beneficiados por efeitos ativos ou passivos. "
                    + "Fantasmas não possuem PV, quando sofrem danos a Duração da Magia é reduzida em -1 Rodada.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(5))
            .targeting(SpellTargeting.distancia(Range.ADJACENTE))
            .build()),

    /** TODO its effect repeats on an odd 1d6 each Rodada and resolves on an even one — this core never rolls dice, and nothing re-evaluates an effect per Rodada against a roll. */
    CADAVER_INSTAVEL(SpellData.builder()
            .name("Cadáver Instável")
            .branchLevel(BranchLevel.FLORESCENTE)
            .branch(MagicBranch.REANIMAR_ALTERNATIVO)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.UNLIKELY)
            .description("Um Morto-Vivo sob controle do conjurador explode com efeitos de dano e cura.")
            .primaryEffectDescription("Seu toque faz com que Morto-Vivo controlado por você, que não seja um "
                    + "subordinado, cresça por algumas Rodadas e em seguida exploda, afetando todos os personagens "
                    + "ao redor. "
                    + "O alvo tocado aumenta em 1 sua Categoria de Tamanho e recebe Vantagem em suas rolagens de "
                    + "Ataque e Dano. A cada nova Rodada você deve rolar 1d6, resultados ímpares repetem este efeito "
                    + "de forma cumulativa. "
                    + "Ao fim da Duração desta magia, ou quando você obtiver um resultado par na rolagem de 1d6, o "
                    + "cadáver explode causando 1d6+Metade do Foco pontos de dano em todos os personagens vivos em "
                    + "Distância Curta. O dano causado aumenta em +1d6 para cada Categoria de Tamanho que o alvo "
                    + "ganhou com esta magia. "
                    + "Se você estiver dentro da área da explosão você recupera uma quantidade de PV igual à metade "
                    + "do dano rolado.")
            .effectChainDescription("Graça da Escuridão: Seus aliados não sofrem danos decorrente da explosão do "
                    + "Cadáver Instável.")
            .criticalEffectType(CriticalEffectType.IMUNIZAR)
            .duration(SpellDuration.rodadas(2))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * One of the four Magias attaching its own clause to Concentração, and the only one to
     * <em>opt out</em>: its summons "não podem ter a Duração estendida por efeitos ou
     * Concentração". Whatever mechanism eventually sustains a Concentração effect has to honour an
     * exemption, not only a break.
     *
     * <p>Its {@code Alcance:} places the area's centre at a point adjacent to the caster rather
     * than on the caster, which is the {@code Range}-carrying form of {@code
     * SpellTargeting#areaDeEfeito} — the only Magia in the catalog authored that way.
     */
    FESTIM_DOS_MORTOS(SpellData.builder()
            .name("Festim dos Mortos")
            .branchLevel(BranchLevel.FLORESCENTE)
            .branch(MagicBranch.REANIMAR_PRINCIPAL)
            .activationTime(ActivationTime.pa(5))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.UNLIKELY)
            .description("O conjurador profana uma área escolhida, fazendo surgir mortos vivos no local.")
            .primaryEffectDescription("Ao lançar esta magia você cria a sua frente um Sarcófago Negro, um sarcófago "
                    + "feito de energia profana materializada. "
                    + "A cada Rodada você deve rolar 1d6, resultados de 1 a 3 indicam que um Zumbi se levantará "
                    + "dele, resultados 4 e 5 indicam a presença de Carniçal, resultados 6 indicam a presença de um "
                    + "Abantesma. Mortos vivos criados desta forma são consideradas criaturas Invocadas para todos "
                    + "os efeitos, não podem ter a Duração estendida por efeitos ou Concentração e só podem agir na "
                    + "Rodada seguinte a sua aparição. "
                    + "O grande número de mortos criados com essa magia torna ela instável, e você pode não "
                    + "conseguir controlar todos os seres invocados. Se seu número máximo de invocações for superado "
                    + "mortos-vivos continuaram sendo invocados sem obedecer a nenhum mestre e com uma fome "
                    + "insaciável, sempre atacam o personagem vivo mais próximo. "
                    + "Se você possuir 10 Graduações em Domínio do Mana, todos os Mortos-Vivos aliados (mas não você "
                    + "mesmo) que estejam em Distância Curta do Sarcófago Negro ou Jazigo Profano efetuam rolagens "
                    + "de Perícia com a GD reduzida em -2 Níveis.")
            .secondaryEffectDescription("Horda Faminta: Ao invés de um sarcófago você pode conjurar um jazigo "
                    + "profano. A cada Rodada, adicionalmente à criatura invocada aleatoriamente, um zumbi será "
                    + "invocado. Você não possui controle de nenhum morto-vivo invocado desta forma e eles sempre "
                    + "atacar o personagem vivo mais próximo do jazigo.")
            .criticalEffectType(CriticalEffectType.PREVENIR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.areaDeEfeito(Range.ADJACENTE,
                    AreaOfEffect.circle(Range.DISTANCIA_MUITO_CURTA)))
            .build());

    private final SpellData data;

    ReanimarSpell(final SpellData data) {
        this.data = data;
    }

    @Override
    public SpellData getData() {
        return data;
    }

    @Override
    public SpellTree getTree() {
        return MagicTree.REANIMAR;
    }
}
