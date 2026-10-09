package org.aventyrs.core.feat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.EgoValue;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.EgoPointsServiceImpl;
import org.aventyrs.core.ego.AutocontroleAdvantage;
import org.aventyrs.core.ego.EgoAdvantage;
import org.aventyrs.core.ego.MoralHerdadaAbility;
import org.aventyrs.core.ego.SorteAdvantage;
import org.aventyrs.core.effect.EffectChainServiceImpl;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DestinoFeat#AUTOCONHECIMENTO} — "Escolha um Ego que você possua valor 2 ou superior. Você adquire uma
 * Vantagem do Ego escolhido." Table rulings (2026-10-08): the Ego's total value counts, and the Vantagem stacks
 * beside the one chosen at creation for the same Ego.
 */
class AutoconhecimentoTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character.CharacterBuilder character(final int autocontroleBase, final int autocontroleVariable) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>())
                .egos(CharacterEgos.builder()
                        .autocontrole(EgoValue.builder().base(autocontroleBase).variable(autocontroleVariable).build())
                        .build());
    }

    @Test
    void theBareTalentoOwesAChoiceOfVantagem() {
        List<FeatChoice<?>> owed = DestinoFeat.AUTOCONHECIMENTO.resolveRequiredChoices(character(2, 0).build());

        assertEquals(1, owed.size());
        assertEquals(EgoAdvantage.class, owed.get(0).type());
        assertTrue(owed.get(0).options().contains(AutocontroleAdvantage.RESOLUTO));
    }

    @Test
    void anEgoBelowTwoOffersNothing() {
        Character holder = character(1, 0).build();

        assertFalse(VantagemDeEgoEscolhidaFeat.optionsFor(holder).contains(AutocontroleAdvantage.RESOLUTO));
        assertThrows(IllegalOperationException.class,
                () -> VantagemDeEgoEscolhidaFeat.of(holder, AutocontroleAdvantage.RESOLUTO));
    }

    /** "valor 2" is the Ego's total: a point a Talento or Título added counts. */
    @Test
    void theEgosTotalValueCounts() {
        assertTrue(VantagemDeEgoEscolhidaFeat.optionsFor(character(1, 1).build())
                .contains(AutocontroleAdvantage.RESOLUTO));
    }

    /** Another Vantagem of an Ego already holding one is offered; the one already held is not. */
    @Test
    void aSecondVantagemOfTheSameEgoIsOfferedButNotTheSameOne() {
        Character holder = character(3, 0)
                .egoAdvantage(EgoDomain.AUTOCONTROLE, AutocontroleAdvantage.MOTIVACAO_DE_MOSES)
                .build();

        List<EgoAdvantage> options = VantagemDeEgoEscolhidaFeat.optionsFor(holder);

        assertTrue(options.contains(AutocontroleAdvantage.RESOLUTO));
        assertFalse(options.contains(AutocontroleAdvantage.MOTIVACAO_DE_MOSES));
    }

    /** Moral Herdada carries its Fama, so it is offered once per Fama. */
    @Test
    void moralHerdadaIsOfferedPerFama() {
        long moral = VantagemDeEgoEscolhidaFeat.optionsFor(character(2, 0).build()).stream()
                .filter(MoralHerdadaAbility.class::isInstance)
                .count();

        assertEquals(MoralHerdadaAbility.FamaChoice.values().length, moral);
    }

    /** Stacked with the creation one, and resolved like it: Resoluto raises the Corrente margin to 7. */
    @Test
    void theGrantedVantagemStacksAndApplies() {
        Character holder = character(3, 0)
                .egoAdvantage(EgoDomain.AUTOCONTROLE, AutocontroleAdvantage.MOTIVACAO_DE_MOSES)
                .build();
        int before = new EffectChainServiceImpl().getRequiredMargin(holder);

        holder.grantFeat(VantagemDeEgoEscolhidaFeat.of(holder, AutocontroleAdvantage.RESOLUTO));

        assertEquals(List.of(AutocontroleAdvantage.MOTIVACAO_DE_MOSES, AutocontroleAdvantage.RESOLUTO),
                holder.getEgoAdvantages(EgoDomain.AUTOCONTROLE));
        assertEquals(7, new EffectChainServiceImpl().getRequiredMargin(holder));
        assertTrue(before < 7);
        assertEquals(AutocontroleAdvantage.RESOLUTO, VantagemDeEgoEscolhidaFeat.chosenBy(holder).orElseThrow());
    }

    /** A granted Vantagem's Ego recovery joins the creation one's: Dileto de Tykhe's extra Sorte point. */
    @Test
    void aGrantedVantagemsSessionRecoveryCounts() {
        Character holder = character(2, 0).build();
        holder.grantFeat(VantagemDeEgoEscolhidaFeat.of(holder, SorteAdvantage.DILETO_DE_TYKHE));

        assertEquals(1, new EgoPointsServiceImpl().getExtraSessionRecovery(holder, EgoDomain.SORTE));
    }
}
