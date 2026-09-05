package org.aventyrs.core.magic.catalog;

import org.aventyrs.core.magic.ActivationTime;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.MagicType;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.SpellTree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the authored Magia catalog against {@code docs/rules/magias.txt}'s complete section — the
 * "Lista de Arcanismos e Preces Divinas", 145 Magias across 20 Árvores.
 *
 * <p><b>The per-tree counts are the point.</b> Wiring a tree's enum into its {@link MagicTree}
 * constant is a separate step from writing the enum, and forgetting it fails <em>silently</em>:
 * {@link SpellTree#getSpells()} just answers empty and the whole Árvore stops being offered. That
 * is the same failure mode {@code FeatCatalog} avoids by sealing {@code Feat}, and since this
 * catalog is enumerated through data rather than through the compiler, a test has to stand in for
 * the {@code permits} clause.
 */
class SpellCatalogTest {

    /**
     * Every Árvore's Magia count, transcribed from the source document by walking its identity
     * lines ({@code Semente – …}, {@code Broto – …}) between ALL-CAPS tree headings.
     */
    private static final Map<MagicTree, Integer> EXPECTED_COUNTS = new EnumMap<>(Map.ofEntries(
            Map.entry(MagicTree.ALIADOS_DA_NATUREZA, 7),
            Map.entry(MagicTree.ANULACAO, 5),
            Map.entry(MagicTree.ARSENAL_ELEMENTAL, 7),
            Map.entry(MagicTree.ARTESAO, 6),
            Map.entry(MagicTree.CORPO_ROCHOSO, 7),
            Map.entry(MagicTree.DOMINIO_PRIMORDIAL, 6),
            Map.entry(MagicTree.FURIA_DE_TESLA, 7),
            Map.entry(MagicTree.IRA_DE_VULCANO, 5),
            Map.entry(MagicTree.MORTE, 8),
            Map.entry(MagicTree.OCULTACAO, 8),
            Map.entry(MagicTree.PIROMANCIA, 9),
            Map.entry(MagicTree.POLIMORFISMO, 9),
            Map.entry(MagicTree.PROFANAR, 8),
            Map.entry(MagicTree.PROTECAO_INVERNAL, 7),
            Map.entry(MagicTree.REANIMAR, 8),
            Map.entry(MagicTree.REGENERACAO, 6),
            Map.entry(MagicTree.TEMPO, 7),
            Map.entry(MagicTree.TRANSPORTE, 9),
            Map.entry(MagicTree.VIDA, 9),
            Map.entry(MagicTree.VOO, 7)));

    /** The document's own total, and the number the coverage index quotes. */
    private static final int TOTAL_AUTHORED_SPELLS = 145;

    /** The two Árvores that run straight through; every other one diverges into exactly two. */
    private static final Set<MagicTree> LINEAR_TREES = Set.of(MagicTree.ANULACAO, MagicTree.IRA_DE_VULCANO);

    @Test
    void theCatalogHoldsEveryMagiaOfTheCompleteSection() {
        assertEquals(TOTAL_AUTHORED_SPELLS, SpellCatalog.all().size());
    }

    @ParameterizedTest
    @EnumSource(MagicTree.class)
    void eachTreeIsWiredToItsOwnMagias(final MagicTree tree) {
        assertEquals(EXPECTED_COUNTS.get(tree), tree.getSpells().size(),
                tree.getName() + " is not wired to its Magia enum, or its rungs have drifted");
    }

    @ParameterizedTest
    @EnumSource(MagicTree.class)
    void everyMagiaReportsTheTreeItIsDeclaredIn(final MagicTree tree) {
        tree.getSpells().forEach(spell -> assertSame(tree, spell.getTree(),
                spell.getName() + " reports a different Árvore than the one holding it"));
    }

    @ParameterizedTest
    @EnumSource(MagicTree.class)
    void aTreeDivergesIntoExactlyTwoRamificacoesOrNone(final MagicTree tree) {
        SpellTree.validateBranches(tree);
        assertEquals(LINEAR_TREES.contains(tree) ? 0 : SpellTree.BRANCH_COUNT, tree.getBranches().size(),
                tree.getName() + " has the wrong number of ramificações");
    }

    @ParameterizedTest
    @EnumSource(MagicTree.class)
    void everyBranchAMagiaNamesBelongsToItsOwnTree(final MagicTree tree) {
        tree.getSpells().stream()
                .flatMap(spell -> spell.getBranch().stream())
                .forEach(branch -> assertSame(tree, ((MagicBranch) branch).getTree(),
                        "a Magia of " + tree.getName() + " sits on another Árvore's ramificação"));
    }

    /**
     * A tree diverges once and stays diverged until it converges, so a rung is either wholly on
     * the trunk or wholly split — never one branched entry with no sibling.
     */
    @ParameterizedTest
    @EnumSource(MagicTree.class)
    void noRungCarriesASingleBranchedMagia(final MagicTree tree) {
        Map<BranchLevel, List<Spell>> byRung = tree.getSpells().stream()
                .filter(spell -> spell.getBranch().isPresent())
                .collect(Collectors.groupingBy(Spell::getBranchLevel));

        byRung.forEach((rung, spells) -> assertEquals(SpellTree.BRANCH_COUNT,
                spells.stream().map(Spell::getBranch).distinct().count(),
                tree.getName() + " has a lone ramificação at " + rung));
    }

    @Test
    void everyMagiaCarriesTheColumnsThatAreNeverBlankInTheSource() {
        SpellCatalog.all().forEach(spell -> {
            assertNotNull(spell.getName(), "a Magia has no name");
            assertNotNull(spell.getBranchLevel(), spell.getName() + " has no rung");
            assertNotNull(spell.getActivationTime(), spell.getName() + " has no Tempo de Ativação");
            assertNotNull(spell.getDescription(), spell.getName() + " has no Descrição");
            assertNotNull(spell.getPrimaryEffectDescription(), spell.getName() + " has no Efeito");
            assertNotNull(spell.getDuration(), spell.getName() + " has no Duração");
            assertNotNull(spell.getTargeting(), spell.getName() + " has no Alcance");
        });
    }

    /**
     * The nine descriptors the source document leaves blank in its otherwise-complete first
     * section — transcribed as {@code null} rather than guessed at. Pinned so that filling one in
     * later is a deliberate change rather than an accident, and so the count cannot quietly grow.
     */
    @Test
    void onlyTheDocumentsOwnBlankDescriptorsAreMissing() {
        assertEquals(Set.of("Invocar Traje de Batalha", "Estalo Primordial", "Proteção Primordial",
                        "Identificação", "Toque Curativo", "Remover Maldição", "Aumentar Passos"),
                namesWhere(spell -> spell.getCastingDifficultyLevel() == null
                        && !spell.isCastingDifficultyFlooredByTargetMagicDefense()),
                "the set of Magias with no fixed GD has changed");

        assertEquals(Set.of("Necropotência", "Reanimar", "Imbuir Fadiga"),
                namesWhere(spell -> spell.getCriticalEffectType() == null),
                "the set of Magias with no Efeito Crítico has changed");

        assertEquals(Set.of("Aumentar Passos", "Lentidão"),
                namesWhere(spell -> spell.getAttackSkillType() == null),
                "the set of Magias with no Perícia Chave has changed");
    }

    /**
     * The three Magias whose GD is a table over the rung of the effect they target, rather than a
     * fixed tier. {@code Spell#castingDifficultyAgainst} is that table, and it is shared rather
     * than restated three times.
     */
    @Test
    void theRungScaledGdsResolveThroughTheSharedTable() {
        Set<String> scaled = namesWhere(spell ->
                spell.getCastingDifficultyAgainst(BranchLevel.SEMENTE).isPresent());
        assertEquals(Set.of("Identificação", "Toque Curativo", "Remover Maldição"), scaled);

        Spell identificacao = AnulacaoSpell.IDENTIFICACAO;
        assertEquals(org.aventyrs.core.skill.DifficultyLevel.EASY,
                identificacao.getCastingDifficultyAgainst(BranchLevel.SEMENTE).orElseThrow());
        assertEquals(org.aventyrs.core.skill.DifficultyLevel.UNLIKELY,
                identificacao.getCastingDifficultyAgainst(BranchLevel.FLORESCENTE).orElseThrow());
    }

    /** Casting cost is purely a function of the rung, per the document's PM table (L35–45). */
    @ParameterizedTest
    @EnumSource(BranchLevel.class)
    void manaCostFollowsTheRung(final BranchLevel rung) {
        SpellCatalog.at(rung).forEach(spell -> assertEquals(rung.getManaCost(), spell.getManaCost()));
    }

    @Test
    void everyRungIsPopulatedAcrossTheCatalog() {
        for (BranchLevel rung : BranchLevel.values()) {
            assertFalse(SpellCatalog.at(rung).isEmpty(), "no Magia sits at " + rung);
        }
    }

    /** A tree's category tag reaches its Magias rather than being authored 145 times. */
    @Test
    void aMagiaTakesItsCategoryFromItsTree() {
        Spell aliado = AliadosDaNaturezaSpell.CATIVAR_ANIMAL;
        assertEquals(MagicType.NATURAL, aliado.getPrimaryType());
        assertEquals(MagicType.INVOCACAO, aliado.getSecondaryType());
        assertTrue(SpellCatalog.ofType(MagicType.INVOCACAO).contains(aliado));

        Spell single = AnulacaoSpell.IDENTIFICACAO;
        assertEquals(MagicType.PRIMORDIAL, single.getPrimaryType());
        assertNull(single.getSecondaryType(), "Anulação is single-typed");
    }

    /** Both Tipos de Magia the catalog forced into existence are carried by real trees. */
    @Test
    void theTwoNewMagicTypesAreUsed() {
        assertEquals(Set.of(MagicTree.TEMPO, MagicTree.TRANSPORTE), treesOfType(MagicType.TEMPORAL));
        assertTrue(treesOfType(MagicType.UMBRAL).isEmpty(),
                "the Umbral Árvores are a draft and are deliberately not authored");
    }

    /** The 19 dual-reach Magias — {@code Pessoal ou Toque} and its one inverted spelling. */
    @Test
    void theDualReachMagiasAuthorBothOfTheirReaches() {
        Set<String> dualReach = namesWhere(spell -> spell.getAlternateTargeting().isPresent());
        assertEquals(19, dualReach.size(), "the set of Pessoal ou Toque Magias has changed");
        assertTrue(dualReach.contains("Fogo Fátuo"), "the one Alvo Único – Toque ou Pessoal entry");
    }

    /** Casting a Magia is two rolls, and the second is always Domínio do Mana — no Magia authors it. */
    @Test
    void theConjurationRollIsAlwaysDominioDoMana() {
        SpellCatalog.all().forEach(spell ->
                assertEquals(org.aventyrs.core.skill.SkillType.DOMINIO_DO_MANA, spell.getConjurationSkillType()));
    }

    /** Non-PA activations are real and rare: five Reações and four Ações Livres across the catalog. */
    @Test
    void theNonActionPointActivationsAreTheDocumentsOwn() {
        assertEquals(5, countActivations(org.aventyrs.core.magic.ActivationType.REACAO));
        assertEquals(4, countActivations(org.aventyrs.core.magic.ActivationType.ACAO_LIVRE));
    }

    @Test
    void theStructuredPrimaryDamageIsAuthoredWhereItHasBeenModelled() {
        Map<String, org.aventyrs.core.magic.SpellDamage> byName = SpellCatalog.all().stream()
                .filter(spell -> spell.getPrimaryDamage().isPresent())
                .collect(Collectors.toMap(Spell::getName, spell -> spell.getPrimaryDamage().orElseThrow()));

        // Ira de Vulcano's four straightforward damage rungs, plus three Piromancia entries.
        assertEquals(Set.of("Sopro de Magma Menor", "Sopro de Magma Maior", "Torrente Vulcânica",
                        "Ira de Vulcano", "Hálito de Eldur", "Bola de Fogo Elduriana", "Olhar de Eldur"),
                byName.keySet());

        org.aventyrs.core.magic.SpellDamage menor = byName.get("Sopro de Magma Menor");
        assertEquals(0, menor.diceCount());
        assertEquals(org.aventyrs.core.magic.FocusScaling.HALF, menor.focusScaling());
        assertEquals(org.aventyrs.core.magic.ElementalType.MAGMA, menor.elementalType());

        byName.values().forEach(damage ->
                assertEquals(org.aventyrs.core.magic.FocusScaling.HALF, damage.focusScaling()));
    }

    private static long countActivations(final org.aventyrs.core.magic.ActivationType type) {
        return SpellCatalog.all().stream()
                .map(Spell::getActivationTime)
                .map(ActivationTime::type)
                .filter(type::equals)
                .count();
    }

    private static Set<MagicTree> treesOfType(final MagicType magicType) {
        return SpellCatalog.ofType(magicType).stream()
                .map(spell -> (MagicTree) spell.getTree())
                .collect(Collectors.toSet());
    }

    private static Set<String> namesWhere(final java.util.function.Predicate<Spell> predicate) {
        return SpellCatalog.all().stream().filter(predicate).map(Spell::getName).collect(Collectors.toSet());
    }
}
