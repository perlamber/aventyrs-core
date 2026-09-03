package org.aventyrs.core.magic.catalog;

import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.magic.ActivationTime;
import org.aventyrs.core.magic.AuthoredSpell;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.DurationUnit;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.magic.SpellDamage;
import org.aventyrs.core.magic.SpellData;
import org.aventyrs.core.magic.SpellDuration;
import org.aventyrs.core.magic.SpellTargeting;
import org.aventyrs.core.magic.SpellTree;
import org.aventyrs.core.scene.AreaOfEffect;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;

/**
 * PIROMANCIA (Divina/Elemental: Fogo) — nine Magias, the catalog's joint-largest tree, diverging
 * as early as Broto and never converging.
 *
 * <p><b>The only tree whose ramificações the document effectively names.</b> Every Magia on one
 * path carries Eldur's name and every Magia on the other Boros' — two deities — so {@link
 * MagicBranch#PIROMANCIA_PRINCIPAL} and {@link MagicBranch#PIROMANCIA_ALTERNATIVO} are the two
 * that override {@code getName()} with something other than their {@code BranchRole}'s label.
 *
 * <p>Its two Florescentes are also the catalog's only Magias with an <b>acquisition prerequisite
 * outside the three gates</b>: "Apenas devotos de Eldur podem aprender esta magia". Devotion has
 * no representation in this core — there is no deity, no faith and no Devoto — so {@code
 * Spell#isEligible} cannot enforce it and the clause stays in the prose, the same restraint every
 * "Requer N Graduações" comment gets.
 */
public enum PiromanciaSpell implements AuthoredSpell {

    /**
     * The catalog's only {@code Concentração + até 1 minuto} — a trailing count in a unit other
     * than Rodadas, which is what {@code SpellDuration#concentracaoMais(int, DurationUnit)} is
     * for. One of only two self-only Concentração Magias (the other is Corpo Fechado); the
     * remaining seventeen sustain an effect on somebody else's sheet.
     */
    LUZ_DE_VELA(SpellData.builder()
            .name("Luz de Vela")
            .branchLevel(BranchLevel.SEMENTE)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.EASY)
            .description("Cria uma pequena chama, pouco maior que o de uma vela, nas mãos de seu conjurador.")
            .primaryEffectDescription("Cria uma pequena chama capaz de iluminar uma área circular que permite a "
                    + "visão em Distância Curta. "
                    + "Estas chamas são muito pequenas e não podem ser utilizadas ofensivamente, mas podem ser "
                    + "utilizadas para acender fogueiras e incendiar materiais inflamáveis.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.concentracaoMais(1, DurationUnit.MINUTO))
            .targeting(SpellTargeting.PESSOAL)
            .build()),

    /**
     * TODO its whole effect turns on classifying an attack as Desarmado or made with an Arma
     * Natural, which is one of this core's two named missing markers: {@code AttackSource} is
     * implemented only by {@code Weapon} and {@code Spell}, so an Ataque Desarmado is
     * indistinguishable from a caller who simply did not say, and nothing marks a weapon as
     * natural either.
     */
    GOLPE_DE_FOGO(SpellData.builder()
            .name("Golpe de Fogo")
            .branchLevel(BranchLevel.BROTO)
            .branch(MagicBranch.PIROMANCIA_ALTERNATIVO)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.MEDIUM)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("As mãos do conjurador, e suas armas naturais, são envoltas por uma grande chama, "
                    + "lembrando as armas de um Elemental do Fogo.")
            .primaryEffectDescription("Conforme a descrição, as mãos e as armas naturais do conjurador são tomadas "
                    + "por chamas. "
                    + "Seus ataques desarmados, ou realizados com suas armas naturais, passam a ser rolados contra a "
                    + "DM do alvo, se bem-sucedido o dano causado muda para 2d6+Metade do Foco (a Força não é "
                    + "aplicada ao dano dos ataques feitos com essa magia). Seus ataques adquirem o Efeito Crítico "
                    + "Inflamar, como um Efeito Crítico adicional.")
            .secondaryEffectDescription("Armamento Elduriano: O alcance desta magia muda para Toque e deve ser "
                    + "conjurada sobre uma arma. Ataques com arma tocada devem ser realizados contra DM, recebem "
                    + "vantagem na rolagem de dano e tipo de dano causado muda para Dano Mágico Elemental: Fogo, "
                    + "recebe Efeito Crítico: inflamar.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.PESSOAL)
            .build()),

    /**
     * The Magia that settles this tree's branch roles: it names Luz de Vela outright ("como se
     * este fosse afetado pela magia Luz de Vela"), which is the Semente's own principal effect.
     *
     * <p>One of the catalog's <b>19 dual-reach entries</b>, and the only one written {@code Alvo
     * Único – Toque ou Pessoal} rather than plain {@code Pessoal ou Toque} — so it authors both a
     * {@code targeting} and an {@code alternateTargeting}, and the caster picks per cast.
     */
    FOGO_FATUO(SpellData.builder()
            .name("Fogo Fátuo")
            .branchLevel(BranchLevel.BROTO)
            .branch(MagicBranch.PIROMANCIA_PRINCIPAL)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.MEDIUM)
            .description("Cria uma chama flutuante que ilumina o caminho e protege o alvo de magias do próprio "
                    + "conjurador.")
            .primaryEffectDescription("Cria uma chama flutuante que paira sobre a cabeça do alvo e o persegue, "
                    + "iluminando seu caminho, como se este fosse afetado pela magia Luz de Vela. "
                    + "Esta magia também fornece imunidade a magias do tipo Elemental: Fogo lançadas por você, e "
                    + "reduz à metade o dano que as suas outras magias causariam ao alvo.")
            .effectChainDescription("Amor Boros: Enquanto Fogo Fátuo estiver ativo, sempre que o Alvo for "
                    + "beneficiado por outras magias, do tipo Broto ou superior, lançadas por você ele "
                    + "adicionalmente recupera 1PV.")
            .secondaryEffectDescription("Maldição Ignis: A Perícia chave muda para Ataque à Distância, O GD para "
                    + "Médio ou DM do Alvo (Maior) e o alcance para Alvo Único – Distância Curta. Sempre que o alvo "
                    + "da Maldição Ignis for alvo de outras magias, Broto ou Superiores, ele adicionalmente perderá "
                    + "1PV. O alvo também recebe a Malefício Amaldiçoado.")
            .criticalEffectType(CriticalEffectType.INFLAMAR)
            .duration(SpellDuration.concentracaoMais(2))
            .targeting(SpellTargeting.TOQUE)
            .alternateTargeting(SpellTargeting.PESSOAL)
            .build()),

    /** Its Efeito Alternativo line is headed {@code Efeito Alternativa –}, one of the document's spelling variants. */
    HALITO_DE_ELDUR(SpellData.builder()
            .name("Hálito de Eldur")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.PIROMANCIA_ALTERNATIVO)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.ATAQUE_A_DISTANCIA)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Permite ao conjurador soprar um cone de fogo a sua frente.")
            .primaryEffectDescription("Ao conjurar essa magia o conjurador pode soprar a sua frente um cone de fogo, "
                    + "infligindo 1d6+Metade do Foco de Dano Mágico Elemental: Fogo a todos os alvos em sua Área de "
                    + "Efeito. Este dano é reduzido em 1 para cada UD percorrida.")
            // The "-1 por UD percorrida" falloff stays prose — this core does no geometry.
            .primaryDamage(SpellDamage.halfFocusElemental(1, ElementalType.FOGO))
            .secondaryEffectDescription("Cuspe de Salamandra: O Alcance desta magia muda para Alvo Único Distante – "
                    + "Distância Média e o dano muda para 2d6+Metade do Foco, este dano é reduzido em 1 para cada "
                    + "2UD entre você e o alvo.")
            .criticalEffectType(CriticalEffectType.INFLAMAR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.areaDeEfeito(AreaOfEffect.cone(Range.DISTANCIA_CURTA)))
            .build()),

    /**
     * Its {@code Alcance:} reads "Área de Efeito – Personagens à até Distância Curta" — a band
     * rather than a shape, which is a Círculo of that radius centred on the fire. TODO "Devotos de
     * Eldur … recuperam 2PD. Este efeito não beneficia o mesmo personagem 2 vezes" needs both a
     * devotion concept and per-target effect history, and this core has neither.
     */
    FOGUEIRA_BOROS(SpellData.builder()
            .name("Fogueira Boros")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.PIROMANCIA_PRINCIPAL)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .description("Uma fogueira que recupera as forças daqueles ao seu redor, recebendo seu calor.")
            .primaryEffectDescription("Cria uma fogueira no chão, imóvel, adjacente a você. A Fogueira Boros aquece "
                    + "todas as criaturas em seu Alcance. "
                    + "Os personagens ao redor da Fogueira recuperam 2PV por rodada, personagens em Distância Muito "
                    + "Curta adicionalmente recebem Resistência Elemental: Frio. "
                    + "Devotos de Eldur, na primeira Rodada de contato com essa magia, recuperam 2PD. Este efeito "
                    + "não beneficia o mesmo personagem 2 vezes, voltando a beneficiá-lo apenas após passar por um "
                    + "Descanso Longo.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(5))
            .targeting(SpellTargeting.areaDeEfeito(AreaOfEffect.circle(Range.DISTANCIA_CURTA)))
            .build()),

    BOLA_DE_FOGO_ELDURIANA(SpellData.builder()
            .name("Bola de Fogo Elduriana")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.PIROMANCIA_ALTERNATIVO)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.ATAQUE_A_DISTANCIA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Invoca uma bola de fogo que quando lançada explode causando grandes danos.")
            .primaryEffectDescription("Você cria uma bola de fogo em suas mãos e lança contra um alvo ou área. Ao "
                    + "impacto a Bola de Fogo Elduriana explode, causando 2d6+Metade do Foco pontos de Dano Mágico "
                    + "Elemental: Fogo no alvo e em todos os outros personagens em Distância Curta. Objetos em posse "
                    + "dos personagens afetados sofrem metade deste dano.")
            // The half-damage-to-carried-objects rider stays prose — no per-copy item damage from a spell yet.
            .primaryDamage(SpellDamage.halfFocusElemental(2, ElementalType.FOGO))
            .secondaryEffectDescription("Chuva de Meteoros: Este efeito pode ser ativado apenas em locais com céu "
                    + "aberto. Dos céus caem diversas pedras incandescentes. Um alvo Ao Alcance dos Olhos e todos os "
                    + "personagens à Distância Longa dele sofrem 1d6+Metade do Foco pontos de Dano. Objetos em posse "
                    + "dos personagens afetados sofrem metade deste dano.")
            .criticalEffectType(CriticalEffectType.INFLAMAR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.distancia(Range.DISTANCIA_MEDIA))
            .build()),

    /**
     * Its identity line reads "Bolo de Fogo Boros" and its own Efeito "A Bola de Fogo Boros" — a
     * typo in one of the two. The identity line is kept as the authored name, the same treatment
     * Experimento de Larcerto gets.
     */
    BOLO_DE_FOGO_BOROS(SpellData.builder()
            .name("Bolo de Fogo Boros")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.PIROMANCIA_PRINCIPAL)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.ATAQUE_A_DISTANCIA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("O conjurador cria uma esfera flamejante, que quando lançada cura seus aliados e fere seus "
                    + "inimigos.")
            .primaryEffectDescription("A Bola de Fogo Boros deve ser lançada em um personagem aliado. "
                    + "Cura 2d6PV+Metade do Foco do alvo e metade deste valor de todos os aliados adjacentes a ele. "
                    + "Inimigos adjacentes ao alvo sofrem uma quantidade de pontos de danos igual à metade dos "
                    + "pontos de vida curados - pelo alvo principal - com esta magia.")
            .secondaryEffectDescription("Chuva Boros: Com alcance pessoal e apenas em local aberto, uma chuva de "
                    + "fogo cai no local, recuperando 3d6+Metade do Foco PV e de todos os personagens adjacentes.")
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.distancia(Range.DISTANCIA_CURTA))
            .build()),

    /**
     * Its Corrente <i>Julgamento Elduriano</i> — "ignora RA e RM, assim como toda e quaisquer
     * formas de Redução ou Imunidade à danos Elemental: Fogo" — is the strongest mitigation
     * bypass in the catalog. {@code DamageService#calculateFinalDamage}'s {@code
     * ignoreDamageReduction} skips RD and deliberately never RA, so even the half of this that
     * names an existing stat cannot be expressed.
     */
    OLHAR_DE_ELDUR(SpellData.builder()
            .name("Olhar de Eldur")
            .branchLevel(BranchLevel.FLORESCENTE)
            .branch(MagicBranch.PIROMANCIA_ALTERNATIVO)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.ATAQUE_A_DISTANCIA)
            .castingDifficultyLevel(DifficultyLevel.UNLIKELY)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("O conjurador dispara dos olhos um raio de energia Elemental: Fogo de seus olhos.")
            .primaryEffectDescription("Apenas devotos de Eldur podem aprender esta magia. "
                    + "Esta magia tem por alvo principal apenas personagens que você possa ver. "
                    + "O Alvo, e todos os personagens entre você e o alvo, sofrem 2d6+Metade do Foco pontos de Dano "
                    + "Elemental: Fogo. Sempre que um personagem é atingido por esta magia ela enfraquece os efeitos "
                    + "dela, reduzindo em 2 o dano em alvos posteriores. "
                    + "Todos os Equipamentos utilizados por personagens afetados por esta magia sofrem metade do "
                    + "dano sofrido por seus usuários. "
                    + "Personagens e Objetos que tenham seus PV reduzidos à zero ou menos são destruídos "
                    + "imediatamente e não podem ser revividos ou reparados.")
            // "Dano Elemental: Fogo" -> ELEMENTAL/FOGO. The "-2 em alvos posteriores" falloff and the
            // half-damage-to-equipment rider stay prose (no target ordering / spell-to-item damage yet).
            .primaryDamage(SpellDamage.halfFocusElemental(2, ElementalType.FOGO))
            .effectChainDescription("Julgamento Elduriano: Esta magia ignora RA e RM, assim como toda e quaisquer "
                    + "formas de Redução ou Imunidade à danos Elemental: Fogo que os alvos possuam.")
            .criticalEffectType(CriticalEffectType.INFLAMAR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.areaDeEfeito(AreaOfEffect.line(Range.DISTANCIA_MEDIA)))
            .build()),

    /** Its Corrente line is headed {@code Corrente de Efeito –} (singular), as Aprimorar's is. */
    VALQUIRIA_BOROS(SpellData.builder()
            .name("Valquíria Boros")
            .branchLevel(BranchLevel.FLORESCENTE)
            .branch(MagicBranch.PIROMANCIA_PRINCIPAL)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.UNLIKELY)
            .description("A sombra de Celestial Flamejante cobre seu corpo, curando a você e seus aliados, "
                    + "fortalece suas defesas e pune seus inimigos.")
            .primaryEffectDescription("Apenas devotos de Eldur podem aprender esta magia. "
                    + "Você adquire Imunidade a Dano Elemental: Fogo, e emite uma aura flamejante, que afeta os "
                    + "personagens adjacentes. "
                    + "Você e seus Aliados afetados pela aura flamejante recuperam 1d6+Metade do Foco PV a cada "
                    + "Rodada, inimigos sofrem esta mesma quantidade em pontos de Dano Mágico Elemental: Fogo. "
                    + "Enquanto nessa forma você recebe Bônus de +5 em suas Defesas e RM para resistir aos efeitos "
                    + "de magia que não sejam Divinas ou Primordiais.")
            .effectChainDescription("Aspecto Boros: Na Rodada de conjuração desta magia você e seus aliados "
                    + "recuperam 3d6PV (Ao invés de 1d6+Metade do Foco).")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.PESSOAL)
            .build());

    private final SpellData data;

    PiromanciaSpell(final SpellData data) {
        this.data = data;
    }

    @Override
    public SpellData getData() {
        return data;
    }

    @Override
    public SpellTree getTree() {
        return MagicTree.PIROMANCIA;
    }
}
