package org.aventyrs.core.title.giganteenfurecido;

import org.aventyrs.core.action.ReactionTrigger;
import org.aventyrs.core.feat.MetamagicoFeat;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.title.EgoCost;
import org.aventyrs.core.title.TitleArchetype;
import org.aventyrs.core.title.TitleCatalog;
import org.aventyrs.core.title.santo.SantoSpecialization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.aventyrs.core.title.giganteenfurecido.GiganteEnfurecidoFixtures.gigante;
import static org.aventyrs.core.title.giganteenfurecido.GiganteEnfurecidoFixtures.holder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The catalog's shape, costs and enforced prerequisites. */
class GiganteEnfurecidoCatalogTest {

    private static final GiganteEnfurecidoSpecialization TITA = GiganteEnfurecidoSpecialization.TITA_ENLOUQUECIDO;
    private static final GiganteEnfurecidoSpecialization BERSERKER = GiganteEnfurecidoSpecialization.BERSERKER;

    @BeforeEach
    void setup() {
        GiganteEnfurecidoFixtures.loadTemplates();
    }

    @Test
    void isRegisteredAsABrutoTitulo() {
        assertTrue(TitleCatalog.all().stream().anyMatch(GiganteEnfurecido.class::isInstance));
        assertEquals(TitleArchetype.BRUTO, gigante(List.of()).getArchetype());
        assertEquals("Gigante Enfurecido", gigante(List.of()).getName());
    }

    @Test
    void theDespertarIsAlwaysHeldButCountsTowardNoPrerequisite() {
        GiganteEnfurecido title = gigante(List.of());

        assertEquals(GiganteEnfurecidoDespertar.FRENESI, title.getAllAbilities().get(0));
        assertTrue(title.getAbilities().isEmpty());
        assertFalse(GiganteEnfurecidoAbility.FRENESI_REATIVO.isEligible(title));
    }

    @Test
    void refusesAnotherTitulosEspecializacao() {
        assertThrows(IllegalOperationException.class,
                () -> gigante(List.of()).grantSpecialization(SantoSpecialization.ABENCOADO_PELA_LUZ));
    }

    @Test
    void costsAreEgoAndPdAsTheRulesStateThem() {
        assertEquals(EgoCost.autocontrole(1), GiganteEnfurecidoDespertar.FRENESI.getEgoCost());
        assertEquals(ActionCost.ofActionPoints(2), GiganteEnfurecidoDespertar.FRENESI.getActionPointCost());
        assertEquals(EgoCost.autocontrole(1), TITA.getEgoCost());
        assertTrue(TITA.isPassive());
        assertEquals(2, GiganteEnfurecidoAbility.PROLONGAR_DESCONTROLE.getPDCost().minimum());
        assertEquals(EgoCost.autocontrole(2), GiganteEnfurecidoAbility.FRENESI_REATIVO.getEgoCost());
        assertEquals(3, TitaEnlouquecidoAbility.GRITOS_DE_GUERRA.getPDCost().minimum());
        assertEquals(2, BerserkerAbility.FRENESI_ASSUSTADOR.getPDCost().minimum());
        assertTrue(GiganteEnfurecidoAbility.UNO_COM_A_IRA.isPassive());
        assertFalse(GiganteEnfurecidoAbility.FRENESI_ESMERALDA.isPassive());
    }

    @Test
    void reacoesAreOfferedOnTheirOwnTriggers() {
        assertEquals(ReactionTrigger.SELF_HIT_BY_SUCCESSFUL_ATTACK, GiganteEnfurecidoAbility.FRENESI_REATIVO.getReactionTrigger());
        assertEquals(ReactionTrigger.ADJACENT_ENEMY_ATTACKS_OTHER, BerserkerAbility.RETALIACAO_FURIOSA.getReactionTrigger());
        assertEquals(ReactionTrigger.SELF_WOULD_DROP_TO_ZERO_HP, BerserkerAbility.FANATICO_DE_CYT.getReactionTrigger());
    }

    @Test
    void unoComAIraNeedsProlongarDescontroleByName() {
        assertFalse(GiganteEnfurecidoAbility.UNO_COM_A_IRA.isEligible(gigante(List.of(TITA),
                TitaEnlouquecidoAbility.GRITOS_DE_GUERRA, TitaEnlouquecidoAbility.COLOSSO_ENFURECIDO,
                BerserkerAbility.DESPREZAR_DANOS)));
        assertTrue(GiganteEnfurecidoAbility.UNO_COM_A_IRA.isEligible(gigante(List.of(TITA),
                GiganteEnfurecidoAbility.PROLONGAR_DESCONTROLE, TitaEnlouquecidoAbility.GRITOS_DE_GUERRA,
                TitaEnlouquecidoAbility.COLOSSO_ENFURECIDO)));
    }

    @Test
    void frenesiEsmeraldaNeedsUnoComAIra() {
        GiganteEnfurecido without = gigante(List.of(TITA), GiganteEnfurecidoAbility.PROLONGAR_DESCONTROLE,
                TitaEnlouquecidoAbility.GRITOS_DE_GUERRA, TitaEnlouquecidoAbility.COLOSSO_ENFURECIDO);
        GiganteEnfurecido with = gigante(List.of(TITA), GiganteEnfurecidoAbility.PROLONGAR_DESCONTROLE,
                TitaEnlouquecidoAbility.GRITOS_DE_GUERRA, TitaEnlouquecidoAbility.COLOSSO_ENFURECIDO,
                GiganteEnfurecidoAbility.UNO_COM_A_IRA);

        assertFalse(GiganteEnfurecidoAbility.FRENESI_ESMERALDA.isEligible(without));
        assertTrue(GiganteEnfurecidoAbility.FRENESI_ESMERALDA.isEligible(with));
    }

    @Test
    void especializacaoGatedTraitsNeedTheirOwnEspecializacao() {
        assertFalse(TitaEnlouquecidoAbility.GRITOS_DE_GUERRA.isEligible(gigante(List.of(BERSERKER))));
        assertTrue(TitaEnlouquecidoAbility.GRITOS_DE_GUERRA.isEligible(gigante(List.of(TITA))));
        assertFalse(BerserkerAbility.RETALIACAO_FURIOSA.isEligible(gigante(List.of(TITA))));
        assertFalse(TitaEnlouquecidoAbility.CATACLISMO_ELEMENTAL.isEligible(gigante(List.of(TITA),
                TitaEnlouquecidoAbility.GRITOS_DE_GUERRA, BerserkerAbility.DESPREZAR_DANOS)));
        assertTrue(TitaEnlouquecidoAbility.CATACLISMO_ELEMENTAL.isEligible(gigante(List.of(TITA),
                TitaEnlouquecidoAbility.GRITOS_DE_GUERRA, TitaEnlouquecidoAbility.COLOSSO_ENFURECIDO)));
    }

    @Test
    void frenesiArcanoAlsoNeedsTheArcanistaTalento() {
        GiganteEnfurecido title = gigante(List.of(TITA));
        CharacterSheet sheet = holder(title);

        assertFalse(TitaEnlouquecidoAbility.FRENESI_ARCANO.isEligible(title, sheet.getCharacter()));
        sheet.getCharacter().grantFeat(MetamagicoFeat.ARCANISTA);
        assertTrue(TitaEnlouquecidoAbility.FRENESI_ARCANO.isEligible(title, sheet.getCharacter()));
    }
}
