package org.aventyrs.core.magic.catalog;

import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.magic.ActivationTime;
import org.aventyrs.core.magic.AuthoredSpell;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.SpellData;
import org.aventyrs.core.magic.SpellDuration;
import org.aventyrs.core.magic.SpellTargeting;
import org.aventyrs.core.magic.SpellTree;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;

/**
 * CORPO ROCHOSO (Encantamento/Elemental: Terra) — seven Magias, diverging at Muda and converging
 * at Florescente.
 *
 * <h2>Five of the seven inherit their Duração from Rigidez Térrea</h2>
 *
 * {@code Duração: A mesma da Rigidez Térrea} is authored as {@code SpellDuration.sameAs}, a live
 * reference rather than a copied {@code 3}, so raising the Broto's own Duração raises the whole
 * tree's — which is exactly what its Corrente <i>Dádiva de Epona</i> ("A Duração da Rigidez Térrea
 * aumenta em +2 Rodadas") does. Copying the number would have silently broken that.
 *
 * <h2>The tree has no Efeito Alternativo anywhere, so L30's trace is unavailable</h2>
 *
 * Not one of these seven carries an {@code Efeito Alternativo}, so which ramificação "foca na
 * evolução dos Efeitos Alternativos" cannot be read off the text the usual way. What separates
 * them instead is written on the GD line: the debuff path's Magias all read "ou DM do Alvo
 * (Maior)", the floor clause that only means anything against an unwilling target, and the buff
 * path's do not. See {@link MagicBranch#CORPO_ROCHOSO_PRINCIPAL}.
 */
public enum CorpoRochosoSpell implements AuthoredSpell {

    DURABILIDADE(SpellData.builder()
            .name("Durabilidade")
            .branchLevel(BranchLevel.SEMENTE)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.EASY)
            .description("Você lança uma benção de proteção contra um alvo, endurecendo sua pele.")
            .primaryEffectDescription("Alvo tocado recebe Bônus de +1 em DF.")
            .effectChainDescription("Durabilidade Maior: Ao invés dos efeitos anteriores, alvo recebe Bônus de +1 em "
                    + "ambas as Defesas.")
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.rodadas(2))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * The Duração every deeper rung of this tree refers back to, and the gate all four of them
     * name as a prerequisite target state.
     *
     * <p>TODO "Sempre que o alvo sofrer danos a arma que lhe atingiu sofre 2 pontos de danos" is
     * reactive damage aimed at the attacker's <em>weapon</em>: {@code DamageService} only computes
     * damage to a target from an attacker, and an item has no PV to lose.
     */
    RIGIDEZ_TERREA(SpellData.builder()
            .name("Rigidez Térrea")
            .branchLevel(BranchLevel.BROTO)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.MEDIUM)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("O corpo do Alvo é endurecido com uma fina camada de terra, que o protege de ataques "
                    + "externos com sua energia Elemental.")
            .primaryEffectDescription("Alvo tocado recebe RD. "
                    + "Sempre que o alvo sofrer danos a arma que lhe atingiu sofre 2 pontos de danos, atacantes "
                    + "desarmados sofrem o dano, ao invés disso.")
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * "Todo o dano físico sofrido pelo alvo desta magia é reduzido à metade" is exactly {@code
     * ModifierType#HALF_DAMAGE}, which {@code DamageService} applies for real as its last
     * mitigation stage — but only for the physical half, and RD/RA are resolved with no notion of
     * damage type, so the scoping cannot be expressed. TODO its Corrente's "imune à Efeitos
     * Críticos Menores" is severity-scoped immunity; {@code CriticalEffectType} identifies which
     * effect, never its Maior/Menor tier.
     */
    CORPO_ROCHOSO(SpellData.builder()
            .name("Corpo Rochoso")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.CORPO_ROCHOSO_PRINCIPAL)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .description("Transforma o corpo do alvo em pedra, tornando o extremamente resistente à danos físicos.")
            .primaryEffectDescription("Apenas personagens sob efeito de Rigidez Térrea podem ser alvos dessa magia. "
                    + "Todo o dano físico sofrido pelo alvo desta magia é reduzido à metade.")
            .effectChainDescription("Órgãos Rochosos: Alvo adicionalmente se torna imune à Efeitos Críticos Menores.")
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.sameAs(() -> RIGIDEZ_TERREA.getDuration()))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * "O Movimento Base do alvo é reduzido em -2UD" is a real {@code ModifierType#MOVEMENT} of -2,
     * and the shape lands exactly — see the "Movimento Base" distinction between a per-Ponto-de-Ação
     * change and a one-shot step. TODO "Desvantagem em rolagens de Perícias baseadas em Força e
     * Destreza" is scoped by <em>governing Attribute</em> rather than by named Perícia, and
     * nothing resolves a bonus that way.
     */
    ENRIJECER_ARTICULACOES(SpellData.builder()
            .name("Enrijecer Articulações")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.CORPO_ROCHOSO_ALTERNATIVO)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Os músculos e articulações do alvo são transformados em pedra, dificultando seus "
                    + "movimentos.")
            .primaryEffectDescription("O Movimento Base do alvo é reduzido em -2UD e ele sofre Desvantagem em "
                    + "rolagens de Perícias baseadas em Força e Destreza.")
            .effectChainDescription("Corpo Pesado: Sempre que o alvo for afetado por outras magias, como efeito "
                    + "adicionalmente sofre Redutor de -1PA por 1 Rodada, este efeito não é cumulativo.")
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.sameAs(() -> RIGIDEZ_TERREA.getDuration()))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /** TODO "a Duração de encantamentos de outras Árvores de Magia e Habilidades são reduzidas à metade" halves someone else's active effects; nothing reaches into another effect's countdown. */
    CORPO_DIAMANTINO(SpellData.builder()
            .name("Corpo Diamantino")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.CORPO_ROCHOSO_PRINCIPAL)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .description("Cobre o corpo do alvo com cristais de AEther, tornando o capaz de absorver magia.")
            .primaryEffectDescription("Apenas personagens sobe feito de Corpo Rochoso podem ser alvos dessa magia. "
                    + "Todo o dano mágico sofrido pelo Alvo é reduzido à metade, a Duração de encantamentos de "
                    + "outras Árvores de Magia e Habilidades são reduzidas à metade.")
            .effectChainDescription("Dádiva de Epona: A Duração da Rigidez Térrea aumenta em +2 Rodadas.")
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.sameAs(() -> RIGIDEZ_TERREA.getDuration()))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * "O Alvo perde uma quantidade de PA iguais a metade do seu Foco (nunca reduz para menos de
     * 1PA)" is a negative {@code ModifierType#ACTION_POINTS} with a floor of 1 rather than the 0
     * every {@code <Stat>Service} clamps at, and "se torna incapaz de realizar Reações ou Ações
     * Livres" is a denial rather than a reduction — {@code ActionProfile} is where a denial lives,
     * and a Magia cannot grant one.
     */
    CAIXAO_ROCHOSO(SpellData.builder()
            .name("Caixão Rochoso")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.CORPO_ROCHOSO_ALTERNATIVO)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Aumenta magicamente a quantidade de pedras ao corpo do alvo, tornando-o irreconhecível e "
                    + "extremamente pesado.")
            .primaryEffectDescription("Apenas personagens sob efeito de Enrijecer Articulações podem ser alvos dessa "
                    + "magia. "
                    + "O Alvo perde uma quantidade de PA iguais a metade do seu Foco (nunca reduz para menos de "
                    + "1PA), na primeira Rodada de efeito também se torna incapaz de realizar Reações ou Ações "
                    + "Livres. "
                    + "Enquanto sob efeito do Caixão Rochoso o alvo sofre 1d6 pontos de Dano Mágico Elemental: Terra "
                    + "em função do enorme peso o esmagando.")
            .effectChainDescription("Dádiva de Epona: A Duração da Rigidez Térrea aumenta em +2 Rodadas.")
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.sameAs(() -> RIGIDEZ_TERREA.getDuration()))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * The convergence rung, reachable from either Emergente — its own text says so, naming Corpo
     * Diamantino <em>or</em> Caixão Rochoso as the state it builds on. Being branchless is what
     * makes both paths legal.
     *
     * <p>Its GD line reads "ou DM dos Alvos (Maior)", plural, unlike every other floored entry.
     * TODO "este dano não pode ser reduzido ou evitado" is unmitigable damage, which {@code
     * DamageService} cannot express — {@code ignoreDamageReduction} skips RD but never RA.
     */
    CORPO_ADAMANTINO(SpellData.builder()
            .name("Corpo Adamantino")
            .branchLevel(BranchLevel.FLORESCENTE)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.UNLIKELY)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Cobre o alvo com uma expeça camada de Adamante, um material extremamente resistente e "
                    + "pesado.")
            .primaryEffectDescription("Apenas personagens sob efeito de Corpo Diamantino ou Caixão Rochoso pode ser "
                    + "afetado por esta Magia. "
                    + "Se uma fonte puder causar danos ao alvo e fazê-lo perder mais do que 2PV, ao invés disso o "
                    + "alvo perde apenas 2PV. Se o alvo for um personagem inimigo o dano mágico máximo sofrido muda "
                    + "para 2+Metade do Foco. "
                    + "Enquanto na Forma Adamantina o alvo não pode Conjurar e Mimetizar Magias, ou Ativar "
                    + "Habilidades de Título. "
                    + "O peso e pressão do Corpo Adamantino fere o alvo, fazendo com que ele perca 1d6PV por Rodada, "
                    + "este dano não pode ser reduzido ou evitado.")
            .effectChainDescription("Réquiem de Epona: A vida perdida aumenta para 2d6PV por Rodada, personagens "
                    + "cujos PV sejam reduzidos à zero ou menos com esta magia se tornam estátuas eternamente. "
                    + "Apenas personagens inimigos são afetados por esta Corrente de Efeitos.")
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.sameAs(() -> RIGIDEZ_TERREA.getDuration()))
            .targeting(SpellTargeting.TOQUE)
            .build());

    private final SpellData data;

    CorpoRochosoSpell(final SpellData data) {
        this.data = data;
    }

    @Override
    public SpellData getData() {
        return data;
    }

    @Override
    public SpellTree getTree() {
        return MagicTree.CORPO_ROCHOSO;
    }
}
