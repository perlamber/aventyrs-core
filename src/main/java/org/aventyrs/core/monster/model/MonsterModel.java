package org.aventyrs.core.monster.model;

import org.aventyrs.core.monster.model.almaelemental.AlmaElementalAbility;
import org.aventyrs.core.monster.model.aspectohumanoide.AspectoHumanoideAbility;
import org.aventyrs.core.monster.model.bestaalada.BestaAladaAbility;
import org.aventyrs.core.monster.model.brotosdemapinguari.BrotosDeMapinguariAbility;
import org.aventyrs.core.monster.model.cireneia.CireneiaAbility;
import org.aventyrs.core.monster.model.mutantemonstruoso.MutanteMonstruosoAbility;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A monster's Modelo — {@code criacao-de-monstros.txt}, "As características do Monstro são
 * determinadas pelo seu Modelo". A monster may hold up to its Categoria's {@code maximumModels}
 * of them, and every Habilidade Monstruosa it takes must come from one it holds.
 *
 * <p>Each Modelo names the enum of its own {@link MonstrousAbility} constants. The link is a
 * {@link Supplier} of that enum's {@code values()} rather than the array itself because each
 * ability's {@link MonstrousAbility#getModel()} points back here — two enums whose constructors
 * read each other would see {@code null} depending on which class loaded first.
 *
 * <p>Only {@link #ABENCOADO_DE_CIRENEIA}'s Habilidades are implemented; see {@link
 * #isImplemented()}.
 */
@Getter
public enum MonsterModel {

    ABENCOADO_DE_CIRENEIA("Abençoado de Cireneia",
            "Cireneia, um Apóstolo conhecido por sua agilidade e mobilidade, as vezes concede seus dons a certos mortais. Quando a benção de Cireneia recai sobre um monstro, o que temos é uma criatura sorrateira, notada por sua grande velocidade, discrição, precisão e frequentemente alta letalidade.",
            CireneiaAbility::values),

    ALMA_ELEMENTAL("Alma Elemental",
            "Almas Elementais são um grupo de criaturas que abrangem desde os Elementais verdadeiros aos descendentes desta raça, em seus sangues flui grande quantidade de poder e são dotados da capacidade de evocar e controlar os Elementos.",
            AlmaElementalAbility::values),

    ASPECTO_HUMANOIDE("Aspecto Humanoide",
            "Este monstro possui corpo um de aparência humanoide forte e treinado, contando com membros que lhe permite utilizar Equipamentos normalmente.",
            AspectoHumanoideAbility::values),

    MUTANTE_MONSTRUOSO("Mutante Monstruoso",
            "Mutantes Monstruosos são versões de outros monstros que sofreram mutações, evoluções, involuções ou corrupções, mudanças estas que garantem uma série de habilidades únicas a estas criaturas.",
            MutanteMonstruosoAbility::values),

    BESTA_ALADA("Besta Alada",
            "",
            BestaAladaAbility::values),

    BROTOS_DE_MAPINGUARI("Brotos de Mapinguari",
            "“Anatomia Vegetal – Bônus Racial de +2 em Vigor, recebem Resistência à Críticos, além disso são imunes aos efeitos críticos Atordoante, Ferida Profunda e Sangramento. Como fraquezas, são também vulneráveis à dano Elemental: Fogo, adicionalmente são vulneráveis à danos Elementais Gelo ou Natural. Também são imunes a efeitos de Roubo de Vida de personagens que não tenham Anatomia Vegetal.”",
            BrotosDeMapinguariAbility::values);

    private final String displayName;

    /** The Modelo's own introductory rules text — for Brotos de Mapinguari, its Anatomia Vegetal. */
    private final String description;

    private final Supplier<MonstrousAbility[]> abilitySource;

    MonsterModel(final String displayName, final String description, final Supplier<MonstrousAbility[]> abilitySource) {
        this.displayName = displayName;
        this.description = description;
        this.abilitySource = abilitySource;
    }

    /** Its Habilidades Monstruosas, in rules order. */
    public List<MonstrousAbility> getAbilities() {
        return Arrays.asList(abilitySource.get());
    }

    /** Whether every one of its Habilidades has its effects applied by this core. */
    public boolean isImplemented() {
        return getAbilities().stream().allMatch(MonstrousAbility::isImplemented);
    }

    /**
     * The Habilidade named {@code name} — its constant name, the wire key — across every
     * Modelo. Constant names are unique within a Modelo, but not across them, so a lookup naming
     * no Modelo returns the first match in declaration order; prefer {@link #findAbility(String)}
     * on a known Modelo.
     */
    public static Optional<MonstrousAbility> anyAbility(final String name) {
        return Arrays.stream(values())
                .map(model -> model.findAbility(name))
                .flatMap(Optional::stream)
                .findFirst();
    }

    /** This Modelo's Habilidade named {@code name}, if it has one. */
    public Optional<MonstrousAbility> findAbility(final String name) {
        return getAbilities().stream().filter(ability -> ability.name().equals(name)).findFirst();
    }
}
