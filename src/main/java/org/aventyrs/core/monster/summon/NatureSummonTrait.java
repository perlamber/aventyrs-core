package org.aventyrs.core.monster.summon;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;

import java.util.List;

/**
 * The two {@link NatureSummon} clauses only an {@link AttributeAbility} hook reaches:
 * <ul>
 *   <li>Membros Múltiplos' "em Rodadas pares recebem +1PA" — the Aliado's own Característica, and a
 *       {@link LacertoPower#MEMBROS_MULTIPLOS} roll. Even Rodadas read as {@code DexterityAbility#APRESSADO} reads
 *       them;</li>
 *   <li>the Anciente's "Imunidade a Efeitos Críticos Menores" — {@code CriticalEffect#applicableTo} drops a Menor
 *       critical's whole chain.</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class NatureSummonTrait implements AttributeAbility {

    /** "em Rodadas pares recebem +1PA". */
    public static final int EVEN_ROUND_ACTION_POINTS = 1;

    private final NatureSummonKind kind;
    private final List<LacertoPower> powers;

    /** Whether it has Membros Múltiplos — the Aliado (and its Predador) always, a Lacerto creature by roll. */
    public boolean hasMultipleLimbs() {
        return kind == NatureSummonKind.ALIADO_DA_NATUREZA || kind == NatureSummonKind.PREDADOR_REGIONAL
                || powers.contains(LacertoPower.MEMBROS_MULTIPLOS);
    }

    @Override
    public int resolveActionPointsBonus(final int turnNumber) {
        return hasMultipleLimbs() && turnNumber % 2 == 1 ? EVEN_ROUND_ACTION_POINTS : 0;
    }

    @Override
    public boolean ignoresMinorCriticalEffects() {
        return kind == NatureSummonKind.ANCIENTE;
    }

    @Override
    public AttributeDomain getAttributeDomain() {
        return AttributeDomain.DEXTERITY;
    }

    @Override
    public String getDescription() {
        return kind == NatureSummonKind.ANCIENTE
                ? "Imunidade a Efeitos Críticos Menores e Encantamentos nocivos."
                : "Membros Múltiplos: Podem usar 3PA para realizar 2 ataques com Desvantagem na Rolagem de Perícia de "
                + "Ataque, em Rodadas pares recebem +1PA.";
    }
}
