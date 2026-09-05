package org.aventyrs.core.magic.catalog;

import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.magic.ActivationTime;
import org.aventyrs.core.magic.AuthoredSpell;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.SpellData;
import org.aventyrs.core.magic.SpellDuration;
import org.aventyrs.core.magic.SpellTargeting;
import org.aventyrs.core.magic.SpellTree;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;

/**
 * TRANSPORTE (Temporal/Invocação) — nine Magias, the catalog's joint-largest tree, diverging at
 * Broto into a teleportation line and a summoned-vehicle line that never converge. The second tree
 * requiring {@link org.aventyrs.core.magic.MagicType#TEMPORAL}.
 *
 * <p><b>The two Magias that needed {@code SpellReach#PLANAR}</b> are both here: Portal's {@code
 * Alcance: Mesmo Plano} and Portal Planar's {@code Alcance: Planos Elementais}. No plane exists as
 * a value in this core, so the reach is a classification and the destination stays prose.
 *
 * <p>Every Magia in it is blocked on geometry. Teleportation is movement to a chosen point,
 * distances here run to thousands of UD ("‘Domínio do Mana’*1000 UD"), and the vehicles have
 * Movimento Base figures of their own that accelerate per Rodada — none of which this core has any
 * notion of.
 */
public enum TransporteSpell implements AuthoredSpell {

    /** Its identity line spells the Magia "Magica de Rua" and its own Efeito Alternativo "Magia de Rua"; the identity line is the authored name. */
    MAGICA_DE_RUA(SpellData.builder()
            .name("Magica de Rua")
            .branchLevel(BranchLevel.SEMENTE)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.EASY)
            .description("Permite ao conjurador transportar um objeto para outro local, e então recebê-lo de volta.")
            .primaryEffectDescription("Um objeto tocado é teletransportado pra um local próximo, em Distância "
                    + "Curta, retornando as suas mãos ao fim de sua Duração. "
                    + "Esta magia afeta apenas objetos que não estejam em alcance visual de outros personagens.")
            .secondaryEffectDescription("Desaparecimento Caótico: A Duração da magia muda para Instantânea, de "
                    + "funcionamento similar à Magia de Rua, ao conjurar esta magia você não escolhe o destino "
                    + "temporário do objeto, ele aparecerá em um local aleatório em Distância Média e não retornará "
                    + "para suas mãos.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(2))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    PISCAR(SpellData.builder()
            .name("Piscar")
            .branchLevel(BranchLevel.BROTO)
            .branch(MagicBranch.TRANSPORTE_PRINCIPAL)
            .activationTime(ActivationTime.ACAO_LIVRE)
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.MEDIUM)
            .description("Permite se teletransportar para um local próximo.")
            .primaryEffectDescription("Esta magia permite a você, ou um objeto em sua posse, se teletransportar para "
                    + "um local à até 1d6+Metade do Foco UD. "
                    + "A Distância aumenta para 2d6+Metade do Foco UD se você tiver 5 o mais Graduações em ‘Domínio "
                    + "do Mana’, então para 3d6+Metade do Foco UD se tiver 10 Graduações. "
                    + "O local de destino é limitado à locais que o conjurador possa ver.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.PESSOAL)
            .build()),

    /**
     * Its Corrente <i>Duração Expandida</i> <b>sets</b> the Duração to 1 hour rather than adding to
     * it — the one Corrente in the catalog that replaces a duration outright. Worth knowing
     * alongside {@code CriticalEffectType#POTENCIALIZAR}, which adds "+2d6 unidades" in the Magia's
     * own unit: this Magia is authored in Rodadas, so a Potencializar here is +2d6 Rodadas, and
     * after its Corrente fires it would be +2d6 <em>hours</em>. Nothing resolves either.
     */
    MONTARIA_NA_MANGA(SpellData.builder()
            .name("Montaria na Manga")
            .branchLevel(BranchLevel.BROTO)
            .branch(MagicBranch.TRANSPORTE_ALTERNATIVO)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.MEDIUM)
            .description("Cria magicamente um veículo ou animal de montaria.")
            .primaryEffectDescription("Permite a você invocar um animal ou veículo leve (para apenas 1 pessoa) feito "
                    + "de energia primordial para que possa ser usado como transporte. "
                    + "Por motivos desconhecidos esta magia não funciona se outros personagens estiverem vendo sua "
                    + "invocação, por isso normalmente os animais são invocados de trás de objetos, capaz, casas, "
                    + "árvores etc. "
                    + "O veículo ou mantaria invocada possui 10PV e ignoram todo o dano sofrido de fontes não "
                    + "Primordiais ou Regalias, o movimento base da invocação é de 11UD.")
            .effectChainDescription("Duração Expandida: A duração desta magia aumenta para 1 hora.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.distancia(Range.ADJACENTE))
            .build()),

    TELETRANSPORTE_VERDADEIRO(SpellData.builder()
            .name("Teletransporte Verdadeiro")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.TRANSPORTE_PRINCIPAL)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .description("Você pode se teletransportar para um local conhecido.")
            .primaryEffectDescription("Permite se teletransportar para um local conhecido à até ‘Domínio do "
                    + "Mana’*1000 UD.")
            .secondaryEffectDescription("Piscar Longínquo: O tempo de Conjuração muda para 1PA, você pode se "
                    + "teletransportar para um local aleatório, em 2d6*100 UD, na direção escolhida. Você sempre "
                    + "surge em solo firme e nunca em espaços ocupados por objetos ou outros personagens.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.PESSOAL)
            .build()),

    CARRUAGEM_OCULTA(SpellData.builder()
            .name("Carruagem Oculta")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.TRANSPORTE_ALTERNATIVO)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .description("Cria um veículo veloz capaz de voar e transportar diversos personagens.")
            .primaryEffectDescription("Você pode invocar um animal ou veículo existente ou imaginário, conforme sua "
                    + "vontade, capaz de transportar o conjurador e até outros 3 personagens. "
                    + "O Movimento Base da Carruagem Oculta é de 9UD e possui 18PV, também ignoram todo o dano "
                    + "sofrido por fontes não Primordiais ou Regalias.")
            .secondaryEffectDescription("Transporte Alado: O número máximo de pessoas no veículo é reduzido para 2, "
                    + "o meio de transporte invocado recebe Movimento Base de Voo.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.concentracaoMais(1))
            .targeting(SpellTargeting.distancia(Range.ADJACENTE))
            .build()),

    /** One of the catalog's two {@code SpellReach#PLANAR} entries — {@code Alcance: Mesmo Plano}. */
    PORTAL(SpellData.builder()
            .name("Portal")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.TRANSPORTE_PRINCIPAL)
            .activationTime(ActivationTime.pa(4))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .description("Abre um portal para um local conhecido.")
            .primaryEffectDescription("Ao conjurar esta magia, seu conjurador deve escolher um local conhecido "
                    + "dentro do mesmo plano. Um portal é então aberto a sua frente e permite que qualquer "
                    + "personagem que passe por ele viaje instantaneamente para o local escolhido.")
            .secondaryEffectDescription("Portal de Fuga: O Tempo de Conjuração desta magia muda para 2PA e a Duração "
                    + "para 1 Rodada. Ao invés do efeito padrão, o destino do portal passa a ser um local aleatório "
                    + "à até 3d6*1000 UD. Portal da Fuga não recebe os benefícios de Efeitos Críticos.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.PLANAR)
            .build()),

    VIAJANTE_DO_AETHER(SpellData.builder()
            .name("Viajante do Aether")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.TRANSPORTE_ALTERNATIVO)
            .activationTime(ActivationTime.pa(4))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .description("Um veículo extremamente veloz e capaz e resistir aos efeitos do Aether.")
            .primaryEffectDescription("Invoca um veículo extremamente veloz e capaz de voar, podemos até mesmo "
                    + "ultrapassar o domo que protege Tellus e navegar no Aether. "
                    + "O Viajante do Aether tem o Movimento Base de 20UD, possui 30PV e é imune a danos, exceto "
                    + "aqueles causados por Regalias. Personagens em seu interior são protegidos de quais efeitos "
                    + "externos, seja ele Aether, Magias ou ataques. "
                    + "A velocidade deste veículo aumenta a cada Rodada em operação, acelerando em +10UD por Rodada, "
                    + "até o máximo de 100UD. Este veículo pode carregar até 5 personagens.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.concentracaoMais(1))
            .targeting(SpellTargeting.distancia(Range.ADJACENTE))
            .build()),

    /** The second {@code SpellReach#PLANAR} entry — {@code Alcance: Planos Elementais}. */
    PORTAL_PLANAR(SpellData.builder()
            .name("Portal Planar")
            .branchLevel(BranchLevel.FLORESCENTE)
            .branch(MagicBranch.TRANSPORTE_PRINCIPAL)
            .activationTime(ActivationTime.pa(4))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.UNLIKELY)
            .description("Permite ao conjurador abrir um portal para um plano Elemental.")
            .primaryEffectDescription("Ao conjurar esta magia deve se escolher um Plano Elemental, e então um portal "
                    + "será aberto para o plano escolhido. "
                    + "Caso você conheça algum local do plano de destino você pode abrir um portal diretamente para "
                    + "este local, caso contrário o portal levará à um local aleatório do Plano especificado.")
            .secondaryEffectDescription("Retorno à Tellus: Permite abrir um portal de volta para o plano material, "
                    + "este efeito pode ser gerado apenas quando estiver em um Plano Elemental.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(2))
            .targeting(SpellTargeting.PLANAR)
            .build()),

    /** Its identity line reads "Perfuratriz de Sonhos" and its Efeito "Perfuratriz dos Sonhos"; the identity line is the authored name. */
    PERFURATRIZ_DE_SONHOS(SpellData.builder()
            .name("Perfuratriz de Sonhos")
            .branchLevel(BranchLevel.FLORESCENTE)
            .branch(MagicBranch.TRANSPORTE_ALTERNATIVO)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.UNLIKELY)
            .description("Um veículo capaz de cruzar o véu do tempo, da realidade e ir para o mundo dos sonhos.")
            .primaryEffectDescription("Você pode invocar o mais veloz e poderoso dos veículos. "
                    + "Completamente imune a efeitos externos a Perfuratriz dos Sonhos é indestrutível, tem "
                    + "Movimento Base de 50UD e sua velocidade aumenta em +10UD a cada Rodada, até o máximo de "
                    + "500UD. "
                    + "Ao atingir sua velocidade máxima a Perfuratriz dos sonhos pode viajar para o passado, futuro "
                    + "ou ir para o Mundo dos Sonhos, o Plano dos Deuses Morpheus e Tykhé. "
                    + "Enquanto no passado os personagens não podem deixar a perfuratriz, a pressão do fluxo do "
                    + "tempo mata imediatamente quaisquer que o fizer, mas podem observar seus imutáveis eventos. "
                    + "Caso viagem para o futuro sofrem menos dos efeitos do tempo, mas ainda assim podem morrer "
                    + "caso permaneçam muito tempo sujeito a ele, nestas condições os personagens perdem 1 "
                    + "Multiplicador de PV a cada hora fora da perfuratriz. "
                    + "Até o máximo de 5 personagens podem viajar desta forma.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.concentracaoMais(1))
            .targeting(SpellTargeting.distancia(Range.ADJACENTE))
            .build());

    private final SpellData data;

    TransporteSpell(final SpellData data) {
        this.data = data;
    }

    @Override
    public SpellData getData() {
        return data;
    }

    @Override
    public SpellTree getTree() {
        return MagicTree.TRANSPORTE;
    }
}
