package org.aventyrs.core.title.bruxo;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.MimetizedSpell;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.catalog.MagicTree;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.aventyrs.core.title.bruxo.BruxoCatalogTest.bruxo;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_CHOICE_LOCKED;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_PREREQUISITE_NOT_MET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BruxoMisticismoTest {

    private static final List<BruxoSpecialization> BOTH =
            List.of(BruxoSpecialization.ILUMINADO, BruxoSpecialization.ORACULO_ABISSAL);

    /** Seven Habilidades de Bruxo, Pacto de Conjuração among them, in acquisition order. */
    private static final List<AventyrTitleAbility> SEVEN = List.of(BruxoAbility.FAMILIAR_MAIOR,
            IluminadoAbility.BENCAO_DO_MAR_DO_SUL, IluminadoAbility.BENCAO_DO_VENTO_DO_LESTE,
            OraculoAbissalAbility.MALDICAO_DO_DESERTO_DO_OESTE, OraculoAbissalAbility.MALDICAO_DAS_CHAMAS_DO_NORTE,
            IluminadoAbility.MALDICAO_DO_PANTANO_DO_SUDESTE, BruxoAbility.PACTO_DE_CONJURACAO);

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void grantSpecializationRefusesAnotherTitulosEspecializacao() {
        Bruxo bruxo = bruxo(List.of());
        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> bruxo.grantSpecialization(org.aventyrs.core.title.curandeiro.CurandeiroSpecialization.MEDICO_DE_GUERRA));
        assertEquals(TITLE_ABILITY_PREREQUISITE_NOT_MET, refused.getMessage());
    }

    @Test
    void theDespertarTeachesTheFirstMisticismo() {
        Bruxo bruxo = bruxo(List.of());
        assertEquals(List.of(BruxoDespertar.CONTRATO_COM_ALEM), bruxo.getPendingMisticismoTeachers());

        bruxo.chooseMisticismo(BruxoDespertar.CONTRATO_COM_ALEM, MagicTree.ALIADOS_DA_NATUREZA);

        assertEquals(List.of(MagicTree.ALIADOS_DA_NATUREZA), bruxo.getMisticismos());
        assertEquals(List.of(), bruxo.getPendingMisticismoTeachers());
    }

    @Test
    void aPickIsLockedAndATreeIsTaughtOnce() {
        Bruxo bruxo = bruxo(List.of(BruxoSpecialization.ILUMINADO));
        bruxo.chooseMisticismo(BruxoDespertar.CONTRATO_COM_ALEM, MagicTree.VIDA);
        bruxo.chooseMisticismo(BruxoDespertar.CONTRATO_COM_ALEM, MagicTree.VIDA);

        assertLocked(() -> bruxo.chooseMisticismo(BruxoDespertar.CONTRATO_COM_ALEM, MagicTree.VOO));
        assertLocked(() -> bruxo.chooseMisticismo(BruxoSpecialization.ILUMINADO, MagicTree.VIDA));
        assertTrue(!bruxo.getMisticismoOptions(BruxoSpecialization.ILUMINADO).contains(MagicTree.VIDA));
    }

    @Test
    void aTypeRestrictedTeacherRefusesATreeItsClauseDoesNotAdmit() {
        Bruxo bruxo = bruxo(List.of(BruxoSpecialization.ILUMINADO), IluminadoAbility.BENCAO_DO_VENTO_DO_LESTE);

        assertLocked(() -> bruxo.chooseMisticismo(IluminadoAbility.BENCAO_DO_VENTO_DO_LESTE, MagicTree.TEMPO));
        bruxo.chooseMisticismo(IluminadoAbility.BENCAO_DO_VENTO_DO_LESTE, MagicTree.PIROMANCIA);
        assertEquals(List.of(MagicTree.PIROMANCIA), bruxo.getMisticismos());
    }

    @Test
    void aPickWhoseTeacherIsNotHeldGrantsNothing() {
        Bruxo bruxo = bruxo(List.of());
        bruxo.chooseMisticismo(BruxoSpecialization.ORACULO_ABISSAL, MagicTree.MORTE);

        assertEquals(List.of(), bruxo.getMisticismos());
        assertEquals(Map.of("ORACULO_ABISSAL", MagicTree.MORTE), bruxo.getMisticismoPicks());
    }

    @Test
    void theCeilingFollowsTheHabilidadesDeBruxo() {
        assertEquals(BranchLevel.SEMENTE, ceilingWith(0));
        assertEquals(BranchLevel.SEMENTE, ceilingWith(1));
        assertEquals(BranchLevel.BROTO, ceilingWith(2));
        assertEquals(BranchLevel.BROTO, ceilingWith(4));
        assertEquals(BranchLevel.MUDA, ceilingWith(5));
        assertEquals(BranchLevel.EMERGENTE, ceilingWith(7));
        assertEquals(Optional.empty(), bruxo(List.of()).getMisticismoCeiling(MagicTree.VIDA));
    }

    @Test
    void misticismosAreMimetizedAtTheirRungsPrice() {
        Bruxo bruxo = withAliados(5);

        List<MimetizedSpell> granted = bruxo.getGrantedMimetizedSpells(null);
        // Semente, Broto, and both Mudas — both ramificações (table ruling).
        assertEquals(4, granted.size());
        granted.forEach(mimetized -> assertEquals(Bruxo.getMisticismoCost(mimetized.getSpell().getBranchLevel()),
                mimetized.getDeterminationPointCost()));
        assertEquals(List.of(0, 1, 2, 2), granted.stream().map(MimetizedSpell::getDeterminationPointCost).toList());
    }

    @Test
    void pactoOpensFlorescenteAtFivePdOnlyOnItsTwoTrees() {
        Bruxo bruxo = new Bruxo(BOTH, SEVEN);
        bruxo.chooseMisticismo(BruxoDespertar.CONTRATO_COM_ALEM, MagicTree.ALIADOS_DA_NATUREZA);
        bruxo.chooseMisticismo(BruxoSpecialization.ILUMINADO, MagicTree.VIDA);
        bruxo.chooseMisticismo(BruxoSpecialization.ORACULO_ABISSAL, MagicTree.MORTE);

        assertLocked(() -> bruxo.choosePactoTrees(Set.of(MagicTree.VIDA)));
        assertLocked(() -> bruxo.choosePactoTrees(Set.of(MagicTree.VIDA, MagicTree.TEMPO)));
        bruxo.choosePactoTrees(Set.of(MagicTree.ALIADOS_DA_NATUREZA, MagicTree.VIDA));
        assertLocked(() -> bruxo.choosePactoTrees(Set.of(MagicTree.VIDA, MagicTree.MORTE)));

        assertEquals(Optional.of(BranchLevel.FLORESCENTE), bruxo.getMisticismoCeiling(MagicTree.ALIADOS_DA_NATUREZA));
        assertEquals(Optional.of(BranchLevel.EMERGENTE), bruxo.getMisticismoCeiling(MagicTree.MORTE));
        MimetizedSpell florescente = bruxo.getGrantedMimetizedSpells(null).stream()
                .filter(mimetized -> mimetized.getSpell().getBranchLevel() == BranchLevel.FLORESCENTE)
                .filter(mimetized -> mimetized.getSpell().getTree() == MagicTree.ALIADOS_DA_NATUREZA)
                .findFirst().orElseThrow();
        assertEquals(5, florescente.getDeterminationPointCost());
    }

    @Test
    void theCharactersMimetizedSpellsIncludeTheMisticismosAndGrowWithAHabilidade() {
        Bruxo bruxo = withAliados(1);
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).mimetizedSpells(new ArrayList<>()).build();
        character.grantTitle(bruxo, TitleSlot.SECONDARY);
        assertEquals(1, character.getMimetizedSpells().size());

        bruxo.grantAbility(IluminadoAbility.BENCAO_DO_MAR_DO_SUL);

        Spell broto = character.getMimetizedSpells().get(1).getSpell();
        assertEquals(BranchLevel.BROTO, broto.getBranchLevel());
        assertEquals(2, character.getMimetizedSpells().size());
    }

    @Test
    void restoringRebuildsThePicks() {
        Bruxo restored = new Bruxo(BOTH, SEVEN,
                Map.of("CONTRATO_COM_ALEM", MagicTree.ALIADOS_DA_NATUREZA, "ILUMINADO", MagicTree.VIDA),
                Set.of(MagicTree.ALIADOS_DA_NATUREZA, MagicTree.VIDA));

        assertEquals(List.of(MagicTree.ALIADOS_DA_NATUREZA, MagicTree.VIDA), restored.getMisticismos());
        assertEquals(Set.of(MagicTree.ALIADOS_DA_NATUREZA, MagicTree.VIDA), restored.getPactoTrees());
    }

    private static BranchLevel ceilingWith(final int habilidades) {
        return withAliados(habilidades).getMisticismoCeiling(MagicTree.ALIADOS_DA_NATUREZA).orElseThrow();
    }

    /** A Bruxo knowing Aliados da Natureza, holding the first habilidades of {@link #SEVEN} less Pacto. */
    private static Bruxo withAliados(final int habilidades) {
        List<AventyrTitleAbility> held = new ArrayList<>(SEVEN.subList(0, 6));
        held.add(OraculoAbissalAbility.BENCAO_DOS_VULCOES_DO_NOROESTE);
        Bruxo bruxo = new Bruxo(BOTH, held.subList(0, habilidades));
        bruxo.chooseMisticismo(BruxoDespertar.CONTRATO_COM_ALEM, MagicTree.ALIADOS_DA_NATUREZA);
        return bruxo;
    }

    private static void assertLocked(final org.junit.jupiter.api.function.Executable executable) {
        assertEquals(TITLE_ABILITY_CHOICE_LOCKED,
                assertThrows(IllegalOperationException.class, executable).getMessage());
    }
}
