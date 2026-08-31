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
 * ARTESÃO (Encantamento/Elemental: Magma) — six Magias, diverging at Muda for exactly one rung
 * and converging again at Emergente. The shallowest convergence in the catalog.
 *
 * <p><b>Almost every effect in this tree is blocked on the same missing system</b>: the
 * owned/produced item copy. Obra-Prima tiers, Aprimoramentos, an item's remaining PV, Regalia
 * status and "which of my equipment is affected" are all per-copy state, and this core's {@code
 * Item} is a catalog entry shared by every copy. Inventory itself is real ({@code
 * Character#equipment}), so a Magia here can identify <em>which</em> items are in hand; what it
 * cannot do is change one of them.
 */
public enum ArtesaoSpell implements AuthoredSpell {

    /**
     * Its Efeito Alternativo widens a weapon's Margem Crítica Menor by +2 números, which is
     * mechanically {@code AbstractSkillInteraction#sumCriticalMarginIncrease} feeding {@code
     * SkillRoll#getCriticalResult(int)} — the one half of the roll-resolution engine that is
     * real. TODO it still cannot be granted from here: every {@code resolveCriticalMarginIncrease}
     * hook lives on a trait its holder <em>has</em>, and this is a temporary enchantment on a
     * weapon, which is per-copy item state.
     *
     * <p>Its Corrente reduces this Magia's own {@code Tempo de Conjuração} by 1PA — the only
     * self-referential activation discount in the catalog, and {@code ActivationTime} holds one
     * cost.
     */
    ATAQUE_CERTEIRO(SpellData.builder()
            .name("Ataque Certeiro")
            .branchLevel(BranchLevel.SEMENTE)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.EASY)
            .description("Aumenta temporariamente a letalidade de uma arma.")
            .primaryEffectDescription("Arma tocada tem seu Dano aumentado em +1 e concede Vantagem na rolagem da "
                    + "Perícia de Ataque.")
            .effectChainDescription("Graça da Guerra: O Tempo de Conjuração desta magia é reduzido em -1PA.")
            .secondaryEffectDescription("Ataque Letal: Em substituição aos efeitos padrões a arma tocada tem a "
                    + "Margem Crítica Menor aumentada em +2 números. Assim como Ataque Certeiro também recebe a "
                    + "Corrente de Efeitos – Graça da Guerra.")
            .criticalEffectType(CriticalEffectType.FORTALECER)
            .duration(SpellDuration.rodadas(1))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * The last Magia before the divergence, so its Efeito Alternativo <i>Reforjar</i> is what the
     * two ramificações are traced against — see {@link MagicBranch#ARTESAO_PRINCIPAL}.
     *
     * <p>Its Corrente is headed {@code Corrente de Efeito –} (singular), one of the document's
     * spelling variants of {@code Corrente de Efeitos –}.
     */
    APRIMORAR(SpellData.builder()
            .name("Aprimorar")
            .branchLevel(BranchLevel.BROTO)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.MEDIUM)
            .description("Transforma um equipamento mundano em uma versão obra-prima.")
            .primaryEffectDescription("Item tocado se transforma em uma Obra-Prima comum ou incomum, de tipo "
                    + "escolhido aleatoriamente.")
            .effectChainDescription("Favor Vulcano: o tipo de Obra-Prima é escolhido aleatoriamente entre incomum ou "
                    + "raro.")
            .secondaryEffectDescription("Reforjar: Você pode mudar o tipo de Obra-Prima de um equipamento por outro "
                    + "de usa escolha, o novo tipo deverá pertencer a uma raridade um nível superior. Apenas "
                    + "Obras-Primas naturais podem ser alvo deste Efeito.")
            .criticalEffectType(CriticalEffectType.FORTALECER)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /** TODO an object's PV is real and spendable now ({@code Item#applyDamage}), but damage is one-way: nothing repairs a copy, and a destroyed one is deliberately unrecoverable. */
    RESTAURAR_OBJETOS(SpellData.builder()
            .name("Restaurar Objetos")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.ARTESAO_PRINCIPAL)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .description("Faz com que um objeto se recupere dos danos sofridos, podendo voltar até mesmo ao seu "
                    + "estado original.")
            .primaryEffectDescription("Ao tocar um objeto o conjurador pode fazer com que ele recupere 2d6+Metade do "
                    + "Foco PV.")
            .effectChainDescription("Forja Branca: O objeto alvo desta magia recebe um Aprimoramento de Obra-Prima "
                    + "por 2 Rodadas, se aplicável.")
            .criticalEffectType(CriticalEffectType.FORTALECER)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * <b>Its {@code GD da Conjuração:} line is blank in the source document</b> — one of four such
     * blanks in the otherwise-complete first section, so {@code getCastingDifficultyLevel()} is
     * {@code null} rather than guessed at. Its neighbours at Muda are Difícil (20|23), which is
     * what it would most likely be; that is not a reason to author it.
     *
     * <p>TODO its Efeito Alternativo lets invoked technological items spend PM instead of Carga.
     * Neither a Carga economy nor technological items exist.
     */
    INVOCAR_TRAJE_DE_BATALHA(SpellData.builder()
            .name("Invocar Traje de Batalha")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.ARTESAO_ALTERNATIVO)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .description("Invoca uma Arma, uma Armadura ou um item do tipo Escudo.")
            .primaryEffectDescription("Você deve mentalizar o tipo de item durante a conjuração, então o item "
                    + "imaginado é criado em sua posse, armas e escudos em mãos, armaduras e braçadeiras lhe "
                    + "vestindo. "
                    + "Não é possível criar itens tecnológicos ou obras-primas desta forma.")
            .effectChainDescription("Dádiva do Deus da Guerra: O item criado é uma Obra-Prima de sua preferência, "
                    + "escolhida entre comuns, incomuns ou raras. Caso possua 5 ou mais Graduações em Domínio do "
                    + "Mana, o item recebe também um Aprimoramento aleatório de qualquer raridade.")
            .secondaryEffectDescription("Forja do Progresso: Você pode criar itens tecnológicos com esta magia, este "
                    + "efeito também pode receber os benefícios de Dádiva do Deus da Guerra. Itens invocados desta "
                    + "forma utilizam PM ao invés de Carga. Conjurar este efeito exige 10 graduações em Domínio do "
                    + "Mana.")
            .criticalEffectType(CriticalEffectType.FORTALECER)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.PESSOAL)
            .build()),

    /**
     * The convergence rung. Its Efeito carries a four-by-two table (Divino/Elemental/Primordial/
     * Profano × Armas/Armaduras) transcribed inline below, since {@code Spell} holds prose and not
     * a chosen-at-cast effect matrix.
     */
    REGALIA_PROVISORIA(SpellData.builder()
            .name("Regalia Provisória")
            .branchLevel(BranchLevel.EMERGENTE)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .description("Transforma um item Obra-Prima em uma falsa Regalia por um breve período.")
            .primaryEffectDescription("A tocar uma Obra-Prima ela receberá uma quantidade de Aprimoramentos, a sua "
                    + "escolha e de qualquer raridade, igual Metade do seu Foco. "
                    + "Adicionalmente você deve escolher entre efeitos Elementais, Divinos, Profanos ou Primordiais. "
                    + "Divino — Armas: Dano causado é sagrado, não reduz PV para valores negativos, mas ignora RD e "
                    + "RA; Armaduras: Cura seu usuário em 3PV por Rodada. "
                    + "Elemental — Armas: Vantagem do Dano, dano causado se torna de um elemento escolhido; "
                    + "Armaduras: DF+2 e imunidade ao elemento escolhido. "
                    + "Primordial — Armas: Vantagem no ataque, dano se torna mágico e ignora RM; Armaduras: DM+2, "
                    + "reduz a metade dano mágico não primordial ou de outras regalias. "
                    + "Profano — Armas: Dano causado é profano, recebe Roubo de Vida 2; Armaduras: Imunidade a "
                    + "magias profanas e roubo de vida.")
            .effectChainDescription("Musa de Gilgamesh: Adicionalmente aos efeitos anteriores, armas podem ser "
                    + "ativadas utilizando 2PM em uma Ação Livre, aumentando o dano causado em +1d6 por 1 Rodada. "
                    + "Armaduras e Escudos podem ser ativados utilizando 2PM em uma Reação, concedendo ao usuário RA "
                    + "e causando 1d6 pontos de dano aos atacantes corpo-a-corpo, estes benefícios também dura por 1 "
                    + "Rodada.")
            .criticalEffectType(CriticalEffectType.FORTALECER)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    GLORIA_DE_GILGAMESH(SpellData.builder()
            .name("Glória de Gilgamesh")
            .branchLevel(BranchLevel.FLORESCENTE)
            .activationTime(ActivationTime.pa(4))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.UNLIKELY)
            .description("Diversos dentre os seus equipamentos são transformados em Regalias Provisórias.")
            .primaryEffectDescription("Até Metade do Foco equipamentos em sua posse são afetados simultaneamente por "
                    + "Regalia Provisória.")
            .effectChainDescription("Campeão de Vulcano: Todos os seus equipamentos Obra-Prima são afetados por "
                    + "Regalia Provisória.")
            .secondaryEffectDescription("Arsenal de Gilgamesh: Um equipamento seu e de cada aliado adjacente se "
                    + "transforma em uma Regalia Provisória, você pode escolher os benefícios de cada item "
                    + "individualmente.")
            .criticalEffectType(CriticalEffectType.FORTALECER)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.TOQUE)
            .build());

    private final SpellData data;

    ArtesaoSpell(final SpellData data) {
        this.data = data;
    }

    @Override
    public SpellData getData() {
        return data;
    }

    @Override
    public SpellTree getTree() {
        return MagicTree.ARTESAO;
    }
}
