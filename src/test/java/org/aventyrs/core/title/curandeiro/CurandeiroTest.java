package org.aventyrs.core.title.curandeiro;

import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.title.TitleArchetype;
import org.aventyrs.core.title.TitleCatalog;
import org.aventyrs.core.title.santo.SantoSpecialization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.aventyrs.core.title.curandeiro.CurandeiroFixtures.curandeiro;
import static org.aventyrs.core.title.curandeiro.CurandeiroFixtures.holder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Título's identity, catalog shape, costs and enforced prerequisites. */
class CurandeiroTest {

    private static final CurandeiroSpecialization MEDICO = CurandeiroSpecialization.MEDICO_DE_GUERRA;
    private static final CurandeiroSpecialization MARTIR = CurandeiroSpecialization.MARTIR_ALTRUISTA;

    @BeforeEach
    void setup() {
        CurandeiroFixtures.loadTemplates();
    }

    @Test
    void isRegisteredAsAnAbencoadoTitulo() {
        assertTrue(TitleCatalog.all().stream().anyMatch(Curandeiro.class::isInstance));
        assertEquals(TitleArchetype.ABENCOADO, curandeiro(List.of()).getArchetype());
        assertEquals("Curandeiro", curandeiro(List.of()).getName());
        assertNotNull(curandeiro(List.of()).getPrimaryTitleBonusDescription());
    }

    @Test
    void refusesNullLists() {
        assertThrows(NullPointerException.class, () -> new Curandeiro(null, List.of()));
        assertThrows(NullPointerException.class, () -> new Curandeiro(List.of(), null));
    }

    @Test
    void grantsIntoTheirOwnListsAndRefusesAnotherTitulosEspecializacao() {
        Curandeiro title = curandeiro(List.of());

        title.grantSpecialization(MARTIR);
        title.grantAbility(CurandeiroAbility.CURANDEIRO_VELOZ);

        assertEquals(List.of(MARTIR), title.getSpecializations());
        assertEquals(List.of(CurandeiroAbility.CURANDEIRO_VELOZ), title.getAbilities());
        assertThrows(IllegalOperationException.class,
                () -> title.grantSpecialization(SantoSpecialization.ABENCOADO_PELA_LUZ));
    }

    @Test
    void costsAreAsTheRulesStateThem() {
        assertTrue(MEDICO.isPassive());
        assertTrue(MARTIR.isPassive());
        assertEquals(2, CurandeiroAbility.CURANDEIRO_VELOZ.getPDCost().minimum());
        assertEquals(ActionCost.FREE_ACTION, CurandeiroAbility.CURANDEIRO_VELOZ.getActionPointCost());
        assertTrue(CurandeiroAbility.LEVANTAR_OS_CAIDOS.isPassive());
        assertTrue(CurandeiroAbility.CURAR_OS_MORTOS.isSupreme());
        assertEquals(2, CurandeiroAbility.CURAR_OS_MORTOS.getPDCost().minimum());
        assertEquals(ActionCost.ofActionPoints(1), CurandeiroAbility.CURAR_OS_MORTOS.getActionPointCost());
        assertTrue(CurandeiroAbility.CURAR_OS_MORTOS.getInteractionClass().isPresent());
        assertTrue(CurandeiroAbility.BENCAO_DE_BOROS.isSupreme());
        assertTrue(MedicoDeGuerraAbility.DOUTOR_DE_ELDUR.isSupreme());
        assertEquals(3, MartirAltruistaAbility.TRANSFERIR_ESSENCIA.getActionPointCost().minimum());
    }

    @Test
    void levantarOsCaidosNeedsAnEspecializacaoAndTwoOtherHabilidades() {
        assertFalse(CurandeiroAbility.LEVANTAR_OS_CAIDOS.isEligible(curandeiro(List.of(MEDICO),
                CurandeiroAbility.CURANDEIRO_VELOZ)));
        assertFalse(CurandeiroAbility.LEVANTAR_OS_CAIDOS.isEligible(curandeiro(List.of(),
                CurandeiroAbility.CURANDEIRO_VELOZ, MedicoDeGuerraAbility.TRATAMENTO_FURTIVO)));
        assertTrue(CurandeiroAbility.LEVANTAR_OS_CAIDOS.isEligible(curandeiro(List.of(MEDICO),
                CurandeiroAbility.CURANDEIRO_VELOZ, MedicoDeGuerraAbility.TRATAMENTO_FURTIVO)));
    }

    @Test
    void bothSupremasNeedLevantarOsCaidosByName() {
        Curandeiro without = curandeiro(List.of(MEDICO), CurandeiroAbility.CURANDEIRO_VELOZ);
        Curandeiro with = curandeiro(List.of(MEDICO), CurandeiroAbility.LEVANTAR_OS_CAIDOS);

        assertFalse(CurandeiroAbility.CURAR_OS_MORTOS.isEligible(without));
        assertFalse(CurandeiroAbility.BENCAO_DE_BOROS.isEligible(without));
        assertTrue(CurandeiroAbility.CURAR_OS_MORTOS.isEligible(with));
        assertTrue(CurandeiroAbility.BENCAO_DE_BOROS.isEligible(with));
    }

    @Test
    void especializacaoGatedTraitsNeedTheirOwnEspecializacao() {
        assertFalse(MedicoDeGuerraAbility.CURA_PROTETORA.isEligible(curandeiro(List.of(MARTIR))));
        assertTrue(MedicoDeGuerraAbility.CURA_PROTETORA.isEligible(curandeiro(List.of(MEDICO))));
        assertFalse(MartirAltruistaAbility.TRANSFERIR_RANCOR.isEligible(curandeiro(List.of(MARTIR),
                MartirAltruistaAbility.TRANSFERIR_VITALIDADE)));
        assertTrue(MartirAltruistaAbility.TRANSFERIR_RANCOR.isEligible(curandeiro(List.of(MARTIR),
                MartirAltruistaAbility.TRANSFERIR_VITALIDADE, MartirAltruistaAbility.TRANSFERIR_ESSENCIA)));
    }

    @Test
    void theRevivalWindowIsHalfTheHoldersMedicinaECura() {
        CharacterSheet sheet = holder();
        Curandeiro title = Curandeiro.heldBy(sheet).orElseThrow();

        assertEquals(CurandeiroFixtures.MEDICINA_E_CURA / 2, title.resolveRevivalWindowRounds(sheet.getCharacter()));
        assertEquals(0, title.resolveRevivalWindowRounds(CurandeiroFixtures.bystander().getCharacter()));
    }

    @Test
    void curandeiroTraitsAreEveryConstantOfTheFourCatalogs() {
        assertTrue(Curandeiro.isCurandeiroTrait(CurandeiroAbility.CURAR_OS_MORTOS));
        assertTrue(Curandeiro.isCurandeiroTrait(MEDICO));
        assertTrue(Curandeiro.isCurandeiroTrait(MedicoDeGuerraAbility.CURA_PROTETORA));
        assertTrue(Curandeiro.isCurandeiroTrait(MartirAltruistaAbility.TRANSFERIR_VITALIDADE));
        assertFalse(Curandeiro.isCurandeiroTrait(SantoSpecialization.ABENCOADO_PELA_LUZ));
        assertFalse(Curandeiro.isCurandeiroTrait(null));
    }
}
