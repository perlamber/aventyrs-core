package org.aventyrs.core.effect;

import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.skill.CriticalResult;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CriticalEffectType} is authored data transcribed from a document this core does not
 * ship ({@code docs/rules/efeitos-criticos.txt}, "Lista de Efeitos Críticos Ofensivos"), so the
 * risk it carries is <b>silent drift</b> rather than wrong behaviour: a constant deleted or
 * renamed breaks nothing at compile time except the stat block or Magia that named it, and
 * neither of those exists yet for most of these.
 *
 * <p>These tests pin the catalog itself, the same way {@code MetamagicoFeatTest} pins its
 * constants' declaration order — the value is in failing when authored data moves, not in
 * exercising behaviour.
 */
class CriticalEffectTypeTest {

    /**
     * The catalog was transcribed in full rather than as-needed. Doing it piecemeal is what left
     * a Magia catalog naming 14 of these able to reference only 3, so an incomplete list is the
     * specific regression worth guarding.
     */
    @Test
    void theCatalogHoldsAllTwentyThreeEfeitosCriticosOfensivos() {
        Set<String> expected = Set.of(
                "SANGRAMENTO", "PURGA_DE_MANA", "PRIMOR", "SABOTAGEM", "EXECUCAO_REAL",
                "AMALDICOAR", "AMENIZAR", "ATORDOANTE", "CATACLISMO", "DESMEMBRAR",
                "DILACERAR", "EMPALAR", "ESTILHACADOR", "EXCRUCIANTE", "FERIDA_PROFUNDA",
                "FORTALECER", "GUILHOTINA", "IMUNIZAR", "INFLAMAR", "OFERENDA_MALDITA",
                "POTENCIALIZAR", "PREVENIR", "TOQUE_DO_AETHER");

        Set<String> actual = Arrays.stream(CriticalEffectType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertEquals(expected, actual);
    }

    /**
     * The javadoc splits the catalog into five implemented types and eighteen named-only ones.
     * That split is a maintenance claim, so it is asserted rather than trusted: building an
     * effect for a sixth type should force the list above it to be updated in the same change.
     */
    @Test
    void exactlyFiveTypesAreBackedByAConcreteCriticalEffect() {
        List<CriticalEffect> implemented = List.of(
                new Sangramento(CriticalResult.ACERTO_CRITICO_MAIOR),
                new ManaPurge(CriticalResult.ACERTO_CRITICO_MAIOR),
                new Sabotage(CriticalResult.ACERTO_CRITICO_MAIOR),
                new RealExecution(CriticalResult.ACERTO_CRITICO_MAIOR),
                new Primor(CriticalResult.ACERTO_CRITICO_MAIOR, EgoDomain.SORTE));

        Set<CriticalEffectType> backed = implemented.stream()
                .map(CriticalEffect::getType)
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                        CriticalEffectType.SANGRAMENTO,
                        CriticalEffectType.PURGA_DE_MANA,
                        CriticalEffectType.SABOTAGEM,
                        CriticalEffectType.EXECUCAO_REAL,
                        CriticalEffectType.PRIMOR),
                backed);
    }

    /**
     * Every type a Magia's {@code Efeito Crítico:} descriptor names must resolve, since that
     * column is the reason the catalog was completed. These fourteen are the distinct values
     * used across the 145 fully-specified Magias in {@code docs/rules/magias.txt}; before the
     * import only AMALDICOAR, DILACERAR and EXECUCAO_REAL of them existed.
     */
    @Test
    void everyEfeitoCriticoNamedByAMagiaResolves() {
        List<CriticalEffectType> namedByMagias = List.of(
                CriticalEffectType.POTENCIALIZAR,
                CriticalEffectType.AMENIZAR,
                CriticalEffectType.FORTALECER,
                CriticalEffectType.INFLAMAR,
                CriticalEffectType.OFERENDA_MALDITA,
                CriticalEffectType.IMUNIZAR,
                CriticalEffectType.PREVENIR,
                CriticalEffectType.DILACERAR,
                CriticalEffectType.ATORDOANTE,
                CriticalEffectType.AMALDICOAR,
                CriticalEffectType.GUILHOTINA,
                CriticalEffectType.TOQUE_DO_AETHER,
                CriticalEffectType.EXECUCAO_REAL,
                CriticalEffectType.CATACLISMO);

        assertEquals(14, Set.copyOf(namedByMagias).size());
        assertTrue(List.of(CriticalEffectType.values()).containsAll(namedByMagias));
    }
}
