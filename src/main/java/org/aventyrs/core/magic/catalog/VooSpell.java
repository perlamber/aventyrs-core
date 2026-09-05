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
 * VOO (Encantamento/Elemental: Ar) — seven Magias, diverging at Muda into a granting line and a
 * denying line, converging at Florescente.
 *
 * <p><b>The most Concentração-heavy tree in the catalog</b>: four of its seven, including the only
 * bare {@code Duração: Concentração} in the game (Queda Lenta), which is the limiting case with no
 * trailing count at all — it ends the instant focus breaks.
 *
 * <p>Flight and levitation are a movement sub-stat this core does not have. {@code
 * ModifierType#MOVEMENT} is ground movement per Ponto de Ação; vertical and flight movement are
 * deliberately <em>not</em> wired into it (the same distinction {@code
 * AtletismoCompetencyAbility#ALPINISTA_VELOZ}/{@code ANFIBIO} document), so none of these seven can
 * be applied — not the grants, and not the denials either, since there is no capability to remove.
 */
public enum VooSpell implements AuthoredSpell {

    /**
     * <b>Its {@code GD da Conjuração:} line prints the descriptor label twice</b> — "GD da
     * Conjuração: GD da Conjuração: Fácil (13|14) ou DM do Alvo (Maior)". A transcription slip in
     * the source, not a second field; the value is read once.
     *
     * <p>The catalog's only bare {@code Duração: Concentração}, which is {@code
     * SpellDuration#CONCENTRACAO} — phase one with a trailing count of zero.
     */
    QUEDA_LENTA(SpellData.builder()
            .name("Queda Lenta")
            .branchLevel(BranchLevel.SEMENTE)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.EASY)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Reduz a velocidade de um objeto ou personagem em queda livre.")
            .primaryEffectDescription("O alvo tem sua velocidade em queda livre reduzida, nunca se movimentando mais "
                    + "que Distância Muito Curta em direção ao solo.")
            .secondaryEffectDescription("Queda Sincronizada: Conjurada sobre si mesmo, o conjurador pode fazer com "
                    + "que outros personagens e objetos em queda caiam na mesma velocidade que você. Apenas "
                    + "personagens com 5 ou mais Graduações em Conhecimentos podem Conjurar este efeito.")
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.CONCENTRACAO)
            .targeting(SpellTargeting.distancia(Range.DISTANCIA_CURTA))
            .build()),

    /** The last Magia before the divergence; its principal effect is what {@link MagicBranch#VOO_PRINCIPAL} is traced to. */
    LEVITAR(SpellData.builder()
            .name("Levitar")
            .branchLevel(BranchLevel.BROTO)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.MEDIUM)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("O conjurador cede a um personagem o poder da levitação.")
            .primaryEffectDescription("Personagem tocado adquire a capacidade de levitar verticalmente, ou de ficar "
                    + "parado livremente no ar. "
                    + "Enquanto sob efeito dessa magia, o movimento vertical de cada personagem é igual à metade do "
                    + "seu Movimento Base, movimentos horizontais no ar são limitados ao espaço adjacentes.")
            .secondaryEffectDescription("Poltergeist: Você pode fazer com que um objeto em Distância Curta levite "
                    + "verticalmente, até uma altura máxima igual a metade de seu Foco. Apenas objetos que não "
                    + "estejam sob posse ou guarda de outros personagens podem ser afetada por esta magia.")
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.concentracaoMais(1))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /** "mesmo que receba esta habilidade de novas fontes" is a suppression that outlasts a re-grant — nothing lets one effect veto another that has not been applied yet. */
    PURGAR_ASAS(SpellData.builder()
            .name("Purgar Asas")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.VOO_ALTERNATIVO)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.ATAQUE_A_DISTANCIA)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Você pode negar a capacidade de levitar ou voar de outras criaturas.")
            .primaryEffectDescription("O alvo desta magia perde a capacidade de voar ou levitar, independente desta "
                    + "habilidade ser de origem natural ou mágica. "
                    + "Enquanto sob este efeito o alvo não consegue voar ou levitar, mesmo que receba esta "
                    + "habilidade de novas fontes. Quando perdem suas habilidades o alvo passa a ser lentamente "
                    + "puxado para o chão, como se estivesse sob efeito de Queda Lenta.")
            .secondaryEffectDescription("Prisão de Ar: Ao invés de cair, o alvo fica preso no ar, imóvel, incapaz de "
                    + "se mover ou de pousar. Indefeso, o alvo também sofre penalidade de -2 em DF e DM. "
                    + "Este efeito alternativo reduz a duração desta magia pela metade.")
            .criticalEffectType(CriticalEffectType.PREVENIR)
            .duration(SpellDuration.concentracaoMais(1))
            .targeting(SpellTargeting.distancia(Range.DISTANCIA_MEDIA))
            .build()),

    /** Its Duração line reads "Concentração +2 Rodada", singular — one of the document's four spellings of that descriptor. */
    VOO_LIVRE(SpellData.builder()
            .name("Voo Livre")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.VOO_PRINCIPAL)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Concede a um personagem a capacidade de voar.")
            .primaryEffectDescription("O alvo desta magia Movimento Base de Voo. "
                    + "A velocidade do voo é igual ao Movimento Base do alvo somado à metade do seu Foco.")
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.concentracaoMais(2))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /** Takes a capability from one combatant and gives it to another — the only transfer of its kind in the catalog, and there is no capability to move. */
    FURTAR_ASAS(SpellData.builder()
            .name("Furtar Asas")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.VOO_ALTERNATIVO)
            .activationTime(ActivationTime.pa(4))
            .attackSkillType(SkillType.ATAQUE_A_DISTANCIA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .description("O conjurador rouba a capacidade de levitar ou voar de outras criaturas.")
            .primaryEffectDescription("O alvo desta magia perde a capacidade de voar e levitar, como se fosse "
                    + "afetada por ‘Purgar Asas’, o conjurador adquire a habilidade perdida por ela. O conjurador "
                    + "pode voar ou levitar, conforme habilidade do alvo.")
            .secondaryEffectDescription("Transferir o Dom: Você pode transferir uma habilidade de voo ou levitação "
                    + "adquirida com esta magia para outros personagens ao toque, esta ação exige o uso de 2PA e "
                    + "dispensa o uso de PM.")
            .criticalEffectType(CriticalEffectType.PREVENIR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.distancia(Range.DISTANCIA_MEDIA))
            .build()),

    /**
     * <b>The catalog's only {@code AreaShape#EXPLOSAO}</b>, and it is authored with a size the
     * document does not give: its {@code Alcance:} reads "Pessoal e Área de Efeito – Explosão" with
     * no band at all. The Efeito supplies the missing figure — "todos os aliados adjacentes, no
     * momento da conjuração" — so it is authored as an Explosão of {@link Range#ADJACENTE}.
     *
     * <p>The "Pessoal e" half needs no {@code alternateTargeting}: an area centred on the
     * Conjurador already covers them, and the two are conjunctive here rather than the free choice
     * a dual reach describes.
     */
    VOO_EM_MASSA(SpellData.builder()
            .name("Voo em Massa")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.VOO_PRINCIPAL)
            .activationTime(ActivationTime.pa(4))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .description("O conjurador e seus aliados próximos adquirem a capacidade de voar.")
            .primaryEffectDescription("Você emite uma poderosa aura mágica, que permite que ele e todos os aliados "
                    + "adjacentes, no momento da conjuração, adquiram a habilidade de voar, como se afetados pela "
                    + "Magia Voo Livre. "
                    + "Após a conjuração os aliados afetados podem se distanciar de você, até Distância Longa, sem "
                    + "perder os benefícios da magia.")
            .effectChainDescription("Pouso Seguro: Aliados que se afastem do conjurador além dos limites da magia "
                    + "são afetados automaticamente por Queda Lenta.")
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.areaDeEfeito(AreaOfEffect.explosion(Range.ADJACENTE)))
            .build()),

    /**
     * The convergence rung, and it names both ramificações in one sentence — everyone who can fly
     * is stripped "como se afetadas por Purgar Asas" while everyone who cannot is granted flight
     * "como se fossem afetadas por Voo Livre". Sitting on the trunk is what makes it reachable
     * from either path.
     *
     * <p>Its {@code GD da Conjuração:} line ends "Improvável (32|36)<b>e</b>" — a stray trailing
     * character in the source, not part of the value.
     */
    ESCARNIO_DE_SYLPH(SpellData.builder()
            .name("Escarnio de Sylph")
            .branchLevel(BranchLevel.FLORESCENTE)
            .activationTime(ActivationTime.pa(5))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.UNLIKELY)
            .description("Uma magia capaz de dar capacidade de voar para quem não voa enquanto remove esta "
                    + "habilidade daqueles que antes a detinham.")
            .primaryEffectDescription("Na área de efeito desta magia todos os personagens que possuam a habilidade "
                    + "de voar perdem esta habilidade e não podem recuperá-las, como se afetadas por Purgar Asas. "
                    + "Simultaneamente todos os personagens que antes não podiam voar recebem esta habilidade, como "
                    + "se fossem afetadas por Voo Livre.")
            .secondaryEffectDescription("Inverter Gravidade: Você pode inverter a gravidade da área, fazendo "
                    + "criaturas e objetos \"caírem para cima\", até a altura máxima de Metade do Foco UD.")
            .criticalEffectType(CriticalEffectType.GUILHOTINA)
            .duration(SpellDuration.rodadas(2))
            .targeting(SpellTargeting.areaDeEfeito(AreaOfEffect.circle(Range.DISTANCIA_LONGA)))
            .build());

    private final SpellData data;

    VooSpell(final SpellData data) {
        this.data = data;
    }

    @Override
    public SpellData getData() {
        return data;
    }

    @Override
    public SpellTree getTree() {
        return MagicTree.VOO;
    }
}
