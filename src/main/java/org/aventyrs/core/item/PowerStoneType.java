package org.aventyrs.core.item;

import java.util.List;

import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.Skill;

import lombok.Getter;

/**
 * The Tipo of a Pedra do Poder — the catalog of what a socketed stone actually does, one constant
 * per entry of the source "Tipos de Pedras do Poder" list (L1254–1321). The other axis, its Preço
 * and charge economy, is {@link PowerStoneQuality}; a fitted {@link PowerStone} pairs the two.
 *
 * <p><b>Every stone has a tri-modal effect:</b> an <em>Efeito Base</em> that is always active,
 * plus <em>either</em> an <em>Equipamento Defensivo</em> or an <em>Equipamento Ofensivo</em>
 * effect, selected by the host {@link Item#getType()}. {@link #resolveBonus(ModifierType,
 * ItemType)} sums the base contribution and whichever mode-specific one the host calls for.
 *
 * <p><b>Most mode effects have no {@link ModifierType} to express them yet</b> and live only in
 * the {@code *Effect} rules text, each blocked on a specific missing system — no
 * Resistência/Vulnerabilidade Elemental, no first-instance-per-Rodada damage tracking, no Área de
 * Efeito, no attribute grant from equipment (the sole racial-bonus hook is {@code
 * Race#getFixedAttributeBonuses()}), no multiplicative/halving stage, no Corrupção/immunity
 * mechanism, no Roubo de Vida from equipment, no PV-regen tick. The handful that <em>are</em>
 * expressible carry a typed {@link ItemBonus} and reach real machinery
 * ({@code DefenseService}/{@code DamageService}/{@code MovementService}/{@code DamageBaseService})
 * through {@link Item}'s enhancement aggregation, exactly like a {@link DefensiveImprovement}.
 *
 * <p>A "Vantagem" clause is a flat {@code +Skill.ADVANTAGE_BONUS} on the named Perícia's own
 * {@code <SKILL>_ROLL_BONUS}, the same convention {@link DefensiveMasterpiece#BANHADA_EM_OURO}
 * and {@link DefensiveImprovement#OCULTA} follow. Damage-type simplification (a "de Corte" /
 * "Físicos" / "Elemental: X" scoped RD modeled as plain {@link ModifierType#DAMAGE_REDUCTION})
 * is the same treatment {@code ArmorItem#ARMADURA_COMPLETA} documents.
 */
@Getter
public enum PowerStoneType {

    /**
     * Adamante Bruto (Sintética). Base "Vigor +1" has no equipment→Atributo hook and the Impacto
     * Meteórico Corrente needs an item-granted Corrente de Efeitos path; the offensive "ignora RD
     * e Meio-Dano" needs an attack-side mitigation-bypass flag. The defensive clause is a real
     * {@link ModifierType#DAMAGE_REDUCTION} 1, its Corte/Perfuração scoping simplified away since
     * no damage-type-scoped mitigation exists.
     */
    ADAMANTE_BRUTO("Adamante Bruto", "Sintética",
            "Vigor +1, ataques recebem a Corrente de Efeitos – Impacto Meteórico: Ataque se torna "
                    + "Área de Efeito - Explosão.",
            "RD, Danos cortantes e perfurantes reduzidos em -1.",
            "Ataques Ignoram RD e efeitos de Meio-Dano.",
            List.of(), List.of(new ItemBonus(ModifierType.DAMAGE_REDUCTION, 1)), List.of()),

    /**
     * Aqua Marina (Elemental: Água). Nothing expressible: Movimento de Natação is a sub-stat
     * deliberately not wired to {@link ModifierType#MOVEMENT}, the defensive clause needs RE and
     * first-instance tracking, the offensive one needs geometry and elemental damage typing.
     */
    AQUA_MARINA("Aqua Marina", "Elemental: Água",
            "Permite respirar na água, concede Movimento Base de Natação.",
            "Ignora o primeiro dano Elemental: Água sofrido, então concede RE: Água.",
            "Distância do Ataque aumenta em +2UD, Dano causado é Elemental: Água em adição aos seus tipos."),

    /**
     * Calcita Vulcânica (Elemental: Magma). "Dano +1" has no flat damage-roll {@link
     * ModifierType}; the offensive Dano Crítico +1d6 and elemental typing, and the defensive
     * RE/first-instance clause, are all blocked. The base "Danos Físicos sofridos -1" is a real
     * {@link ModifierType#DAMAGE_REDUCTION} 1, type-simplified.
     */
    CALCITA_VULCANICA("Calcita Vulcânica", "Elemental: Magma",
            "Dano +1, Danos Físicos sofridos reduzidos em -1.",
            "Ignora o primeiro dano Elemental: Magma sofrido, então concede RE: Magma.",
            "Dano Crítico +1d6, Dano causado é Elemental: Magma em adição aos seus tipos.",
            List.of(new ItemBonus(ModifierType.DAMAGE_REDUCTION, 1)), List.of(), List.of()),

    /**
     * Cianita Dracônica (Dracônica). "Instinto +1" (no attribute hook), an added Efeito Crítico,
     * an elemental half-damage stage and an RE/half-damage bypass — none expressible.
     */
    CIANITA_DRACONICA("Cianita Dracônica", "Dracônica",
            "Instinto +1, Danos Elementais recebem Cataclismo como Efeito Crítico adicional.",
            "O primeiro Dano Elemental sofrido em cada Rodada é reduzido à metade (efeito de meio-dano).",
            "Ignora RE e Efeitos de Meio-Dano utilizados para reduzir danos Elementais."),

    /**
     * Citrino Incandescente (Elemental: Fogo). A per-Rodada damage aura, an RE/first-instance
     * clause and elemental damage typing / base Efeito Crítico change — none expressible.
     */
    CITRINO_INCANDESCENTE("Citrino Incandescente", "Elemental: Fogo",
            "Personagens em Distância Muito Curta sofrem 3 pontos de Danos Físicos Elementais: Fogo a cada Rodada.",
            "Ignora o primeiro dano Elemental: Fogo sofrido, então concede RE: Fogo.",
            "Dano causado é Elemental: Fogo em adição aos seus tipos, Efeito Crítico base muda para Inflamar."),

    /**
     * Coração Vazio (Planar). Corrupção immunity, cumulative retaliation damage on both sides —
     * no immunity mechanism, no reactive-damage path, no within-Scene attack counter.
     */
    CORACAO_VAZIO("Coração Vazio", "Planar",
            "Você se torna imune a Efeitos de Corrupção.",
            "Atacantes sofrem 1 ponto de dano, cumulativo, para cada ataque bem-sucedido efetuado anteriormente. "
                    + "Efeito de Corrupção.",
            "Alvo sofre 1 ponto de dano adicional, cumulativo, para cada ataque sofrido antes. Efeito de Corrupção."),

    /**
     * Esfera de AEther (Primordial). "Foco +1" (no attribute hook), magic-damage and
     * Encantamento-Duração halving, and an offensive clause gated on "anexo apenas à Artefatos de
     * Conjuração" — which need the offensive Encaixe that does not exist yet. Blocked whole.
     */
    ESFERA_DE_AETHER("Esfera de AEther", "Primordial",
            "Foco +1",
            "Danos mágicos sofridos reduzidos à metade, Duração de Encantamentos e Maldições sofridas reduzidas à metade.",
            "Pode ser anexo apenas à Artefatos de Conjuração. Dano e Cura mágica +3, Duração de seus "
                    + "Encantamentos e Maldições +2 Rodadas."),

    /**
     * Fluorita Prismática (Elemental). "Carisma +1", element-scoped Conjuração and Defesas
     * bonuses, and full elemental damage retyping — none expressible (a {@link
     * ModifierType#DEFESAS} bonus cannot be scoped to a damage element).
     */
    FLUORITA_PRISMATICA("Fluorita Prismática", "Elemental",
            "Carisma +1, Conjuração de Magias Elementais +1",
            "Defesas +3 para resistir a efeitos Elementais.",
            "Danos causados são Elementais: Todos em substituição aos seus outros tipos e o Efeito "
                    + "Crítico base muda para Cataclismo."),

    /**
     * Hematita do Vendaval (Elemental: Ar). "Destreza +1" and the RE/first-instance defensive
     * clause are blocked; the base "Movimento Base +2UD" is a real {@link ModifierType#MOVEMENT}
     * 2. The offensive "Vantagem no Ataque" is authored as a flat advantage on both Ataque
     * Perícias — inert until offensive-item sockets exist, then live with no rewiring.
     */
    HEMATITA_DO_VENDAVAL("Hematita do Vendaval", "Elemental: Ar",
            "Destreza +1, Movimento Base +2UD",
            "Ignora o primeiro dano Elemental: Ar sofrido, então concede RE: Ar.",
            "Vantagem no Ataque e Corrente de Efeitos – Vendaval: Alvo é empurrado 2UD para trás.",
            List.of(new ItemBonus(ModifierType.MOVEMENT, 2)),
            List.of(),
            List.of(new ItemBonus(ModifierType.ATAQUE_CORPO_A_CORPO_ROLL_BONUS, Skill.ADVANTAGE_BONUS),
                    new ItemBonus(ModifierType.ATAQUE_A_DISTANCIA_ROLL_BONUS, Skill.ADVANTAGE_BONUS))),

    /**
     * Mitral Puro (Sintética). Base "Movimento Base +2UD" ({@link ModifierType#MOVEMENT} 2) and
     * "Vantagem nas rolagens de Perícia de Ataque" (flat advantage on both Ataque Perícias) are
     * real. Both Margem Crítica clauses — reducing an attacker's margin (defensive) and raising
     * one's own (offensive) — need a hook no {@code Item} has; deferred.
     */
    MITRAL_PURO("Mitral Puro", "Sintética",
            "Vantagem nas rolagens de Perícia de Ataque, Movimento Base +2UD",
            "Margem Crítica Menor dos inimigos atacantes reduzida em -2 números.",
            "Margem Crítica Menor aumentada em +2 números.",
            List.of(new ItemBonus(ModifierType.MOVEMENT, 2),
                    new ItemBonus(ModifierType.ATAQUE_CORPO_A_CORPO_ROLL_BONUS, Skill.ADVANTAGE_BONUS),
                    new ItemBonus(ModifierType.ATAQUE_A_DISTANCIA_ROLL_BONUS, Skill.ADVANTAGE_BONUS)),
            List.of(), List.of()),

    /**
     * Moldavita Selvagem (Natural). Healing prevention on dealt damage, Natural immunity /
     * first-instance, and elemental damage typing — none expressible.
     */
    MOLDAVITA_SELVAGEM("Moldavita Selvagem", "Natural",
            "Danos que causar não podem ser curados, exceto com Descansos Verdadeiros",
            "Ignora o primeiro dano Natural sofrido, você se torna imune a efeitos Naturais.",
            "Dano causado é Elemental: Natural em adição aos seus tipos."),

    /**
     * Opala Purificadora (Sagrada). A per-Rodada PV-regen tick, a damage-type-scoped ("profanos")
     * RD and effect-Duração reduction, and Sagrado damage typing / anti-undead dice — none
     * expressible.
     */
    OPALA_PURIFICADORA("Opala Purificadora", "Sagrada",
            "Você recupera 2PV a cada Rodada.",
            "Danos profanos reduzidos em -3, Duração de efeitos Profanos reduzido em -2 Rodadas.",
            "Dano causado é Sagrado em adição aos seus tipos e aumenta em +1d6 contra Mortos-Vivos e Abissais."),

    /**
     * Relâmpago Dourado (Temporal). Base "Movimento Base +2UD" is a real {@link
     * ModifierType#MOVEMENT} 2; the paired "movimentos não permitem Reações", the
     * moved-5UD-grants-PA defensive clause and the once-per-Rodada action-cost reduction are all
     * blocked (no movement-triggered-Reação suppression, no distance tracking, no per-Rodada
     * action-cost hook).
     */
    RELAMPAGO_DOURADO("Relâmpago Dourado", "Temporal",
            "Movimento Base aumenta em +2UD e seus movimentos não permitem Reações.",
            "Após se mover por ao menos 5UD você recebe +2PA (não cumulativo).",
            "Uma vez por Rodada seu Tempo de Ação de rolagens de Perícia de Ataque e Domínio do Mana "
                    + "é reduzido em -1PA.",
            List.of(new ItemBonus(ModifierType.MOVEMENT, 2)), List.of(), List.of()),

    /**
     * Rútilo Subterrâneo (Elemental: Terra). Base "Defesas +2" ({@link ModifierType#DEFESAS} 2)
     * and "Vantagem nas Rolagens de Atletismo" (flat advantage) are real. Ignoring Terreno
     * Difícil, the defensive weapon-damage / RE clause, and the object-scoped damage advantage /
     * elemental typing are blocked.
     */
    RUTILO_SUBTERRANEO("Rútilo Subterrâneo", "Elemental: Terra",
            "Defesas +2, Vantagem nas Rolagens de Atletismo e seus movimentos ignoram Terreno Difícil.",
            "Armas inimigas corpo-a-corpo e projéteis sofrem 2 pontos de danos. Ignora o primeiro "
                    + "dano Elemental: Terra sofrido, então concede RE: Terra.",
            "Vantagem nas Rolagens de Danos contra objetos, os danos causados são Elemental: Terra "
                    + "em adição aos seus tipos.",
            List.of(new ItemBonus(ModifierType.DEFESAS, 2),
                    new ItemBonus(ModifierType.ATLETISMO_ROLL_BONUS, Skill.ADVANTAGE_BONUS)),
            List.of(), List.of()),

    /**
     * Sodalita Gélida (Elemental: Gelo). "Força +1", an RE/first-instance clause, and an
     * offensive "Vantagem nas Rolagens de Danos" (no damage-roll {@link ModifierType}) plus
     * elemental typing — none expressible.
     */
    SODALITA_GELIDA("Sodalita Gélida", "Elemental: Gelo",
            "Força +1.",
            "Ignora o primeiro dano Elemental: Gelo sofrido, então concede RE: Gelo.",
            "Vantagem nas Rolagens de Danos, os danos causados são Elemental: Gelo em adição aos seus tipos."),

    /**
     * Sombra Solidificada (Umbral) — the cleanest of the catalog. The base "item conta como um
     * Subordinado" needs a subordinate model, but the defensive "Danos sofridos reduzidos em 1"
     * is an <em>unscoped</em> {@link ModifierType#DAMAGE_REDUCTION} 1 (no simplification needed),
     * and the offensive "Dano Base da Arma aumentado em +1" is a real Dano Base scale-up via
     * {@link #resolveDamageBaseIncrease(Weapon, ItemType)}.
     */
    SOMBRA_SOLIDIFICADA("Sombra Solidificada", "Umbral",
            "Item conta como um subordinado (definido na criação, baseado no personagem da Umbra derrotado).",
            "Danos sofridos reduzidos em 1.",
            "Dano Base da Arma aumentado em +1.",
            List.of(), List.of(new ItemBonus(ModifierType.DAMAGE_REDUCTION, 1)), List.of()) {
        @Override
        public int resolveDamageBaseIncrease(final Weapon weapon, final ItemType hostType) {
            return hostType == ItemType.OFFENSIVE ? 1 : 0;
        }
    },

    /**
     * Turmalina Obscura (Profana). Damage-type-scoped ("Sagradas") RD and effect-Duração
     * reduction, a Roubo de Vida grant plus immunity (no equipment→Roubo de Vida path, and the
     * immunity half is unbuildable regardless), and Profano damage typing / an added Efeito
     * Crítico — none expressible.
     */
    TURMALINA_OBSCURA("Turmalina Obscura", "Profana",
            "Danos e Curas Sagradas reduzidos em -3, Duração de efeitos Sagrados reduzido em -2 Rodadas.",
            "Roubo de Vida 2 e imunidade a efeitos de Roubo de Vida.",
            "Dano causado é Profano em adição aos seus tipos e recebe Oferenda Maldita como Efeito Crítico adicional.");

    private final String name;
    private final String originLabel;
    private final String baseEffect;
    private final String defensiveEffect;
    private final String offensiveEffect;
    private final List<ItemBonus> baseBonuses;
    private final List<ItemBonus> defensiveBonuses;
    private final List<ItemBonus> offensiveBonuses;

    PowerStoneType(final String name, final String originLabel, final String baseEffect,
                   final String defensiveEffect, final String offensiveEffect) {
        this(name, originLabel, baseEffect, defensiveEffect, offensiveEffect,
                List.of(), List.of(), List.of());
    }

    PowerStoneType(final String name, final String originLabel, final String baseEffect,
                   final String defensiveEffect, final String offensiveEffect,
                   final List<ItemBonus> baseBonuses, final List<ItemBonus> defensiveBonuses,
                   final List<ItemBonus> offensiveBonuses) {
        this.name = name;
        this.originLabel = originLabel;
        this.baseEffect = baseEffect;
        this.defensiveEffect = defensiveEffect;
        this.offensiveEffect = offensiveEffect;
        this.baseBonuses = List.copyOf(baseBonuses);
        this.defensiveBonuses = List.copyOf(defensiveBonuses);
        this.offensiveBonuses = List.copyOf(offensiveBonuses);
    }

    /**
     * How much of modifierType this stone currently grants a host item of hostType — its Efeito
     * Base contribution plus, if the host is Ofensivo/Defensivo, that mode's contribution too.
     * Additive, the same convention every other bonus source in this core follows.
     */
    public int resolveBonus(final ModifierType modifierType, final ItemType hostType) {
        int total = sumMatching(baseBonuses, modifierType);
        if (hostType == ItemType.DEFENSIVE) {
            total += sumMatching(defensiveBonuses, modifierType);
        } else if (hostType == ItemType.OFFENSIVE) {
            total += sumMatching(offensiveBonuses, modifierType);
        }
        return total;
    }

    /**
     * Dano Base scale-ups this stone grants when its host is a weapon. 0 for every stone but
     * {@link #SOMBRA_SOLIDIFICADA}, whose Efeito Ofensivo is exactly "+1 Dano Base".
     */
    public int resolveDamageBaseIncrease(final Weapon weapon, final ItemType hostType) {
        return 0;
    }

    private static int sumMatching(final List<ItemBonus> bonuses, final ModifierType modifierType) {
        return bonuses.stream()
                .filter(bonus -> bonus.modifierType() == modifierType)
                .mapToInt(ItemBonus::value)
                .sum();
    }
}
