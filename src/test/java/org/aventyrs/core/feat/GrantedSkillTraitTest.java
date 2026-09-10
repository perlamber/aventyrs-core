package org.aventyrs.core.feat;

import org.aventyrs.core.ability.StrengthAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.race.Bestial;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.atletismo.Atletismo;
import org.aventyrs.core.skill.atletismo.AtletismoCompetencyAbility;
import org.aventyrs.core.skill.atletismo.AtletismoSpecialization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code Feat#getGrantedSkillTraits} — the "você recebe uma Habilidade de Competência /
 * Especialização de &lt;Perícia&gt;" grant, which was the racial catalog's most-cited blocker.
 *
 * <p>Tested by the effect, per the {@code testing-a-feat} convention: a granted trait has to
 * reach the same two views an acquired one does — {@code SkillCompetencyAbility#allFor} and
 * {@code Character#getSpecializations(SkillType)} — and, through them, the roll path, which is
 * what refuses a {@code SkillRoll} naming a trait the character doesn't hold.
 */
class GrantedSkillTraitTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character.CharacterBuilder character() {
        return CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>());
    }

    /** A Bestial trained in Atletismo — the Perícia both Heranças below hand a trait from. */
    private static Character bestial() {
        return character().race(new Bestial())
                .skill(SkillType.ATLETISMO, CharacterSkill.builder()
                        .skill(new Atletismo())
                        .graduation(SkillGraduation.builder().graduationValue(2).build())
                        .build())
                .build();
    }

    @Test
    void aCharacterWithoutTheGrantHoldsNeitherTrait() {
        Character plain = bestial();

        assertFalse(SkillCompetencyAbility.allFor(plain).contains(AtletismoCompetencyAbility.PASSO_LARGO));
        assertTrue(plain.getSpecializations(SkillType.ATLETISMO).isEmpty());
    }

    /**
     * A granted {@code SkillCompetencyAbility} joins {@code allFor}, which is the single view
     * every three-source scan and the roll path already read — so it needs no service change.
     */
    @Test
    void aGrantedCompetencyAbilityJoinsAllFor() {
        Character bestial = bestial();
        bestial.grantFeat(HerancaBestialFeat.of(BestialFeat.HERANCA_BOVIDEA,
                AtletismoCompetencyAbility.PASSO_LARGO));

        assertTrue(SkillCompetencyAbility.allFor(bestial).contains(AtletismoCompetencyAbility.PASSO_LARGO));
    }

    /** And a granted Especialização joins the matching aggregate on {@code Character}. */
    @Test
    void aGrantedSpecializationJoinsTheCharactersOwn() {
        Character bestial = bestial();
        bestial.grantFeat(HerancaBestialFeat.of(BestialFeat.HERANCA_REPTILIANA,
                AtletismoSpecialization.TRI_ATLETA));

        assertEquals(List.of(AtletismoSpecialization.TRI_ATLETA),
                bestial.getSpecializations(SkillType.ATLETISMO));
        // Scoped to its own Perícia, not spilled across every one.
        assertTrue(bestial.getSpecializations(SkillType.FURTIVIDADE).isEmpty());
    }

    /**
     * The point of routing both views through the aggregate: a roll may <em>name</em> a granted
     * trait. {@code AbstractSkillInteraction} refuses a {@code SkillRoll} whose requested trait
     * the character doesn't hold, and a granted one now counts as held.
     */
    @Test
    void aRollMayNameAGrantedTrait() {
        Character bestial = bestial();
        CharacterSheet sheet = CharacterSheet.of(bestial, new Player());
        SkillRoll namingTheSpecialization =
                new SkillRoll(List.of(3, 3, 3), AtletismoSpecialization.TRI_ATLETA);

        assertThrows(IllegalOperationException.class, () -> SkillType.ATLETISMO.newInteraction()
                .applyTo(sheet, null, namingTheSpecialization));

        bestial.grantFeat(HerancaBestialFeat.of(BestialFeat.HERANCA_REPTILIANA,
                AtletismoSpecialization.TRI_ATLETA));

        assertDoesNotThrow(() -> SkillType.ATLETISMO.newInteraction()
                .applyTo(sheet, null, namingTheSpecialization));
    }

    /**
     * The Herança's own clauses survive being replaced by its acquired form — the delegation
     * {@code HerancaBestialFeat} does by hand. Bovídea grants +1 Força and Chifres Poderosos.
     */
    @Test
    void theAcquiredFormKeepsTheHerancasOwnEffects() {
        Character bestial = bestial();
        int forcaBefore = bestial.getEffectiveAttributeTotal(AttributeDomain.STRENGTH);

        bestial.grantFeat(HerancaBestialFeat.of(BestialFeat.HERANCA_BOVIDEA,
                AtletismoCompetencyAbility.PASSO_LARGO));

        assertEquals(forcaBefore + 1, bestial.getEffectiveAttributeTotal(AttributeDomain.STRENGTH));
        assertEquals(BestialFeat.HERANCA_BOVIDEA.getGrantedNaturalWeapons(bestial),
                bestial.getNaturalWeapons());
        assertEquals(BestialFeat.HERANCA_BOVIDEA,
                bestial.getFeats().get(0).catalogEntry(), "still the catalog constant for gating");
    }

    /** Several Heranças at once, each with its own pick — which is why {@code chosenBy} takes one. */
    @Test
    void eachHerancaKeepsItsOwnChoice() {
        Character bestial = bestial();
        bestial.grantFeat(HerancaBestialFeat.of(BestialFeat.HERANCA_BOVIDEA,
                AtletismoCompetencyAbility.PASSO_LARGO));
        bestial.grantFeat(HerancaBestialFeat.of(BestialFeat.HERANCA_REPTILIANA,
                AtletismoSpecialization.TRI_ATLETA));

        assertEquals(AtletismoCompetencyAbility.PASSO_LARGO,
                HerancaBestialFeat.chosenBy(bestial, BestialFeat.HERANCA_BOVIDEA).orElseThrow());
        assertEquals(AtletismoSpecialization.TRI_ATLETA,
                HerancaBestialFeat.chosenBy(bestial, BestialFeat.HERANCA_REPTILIANA).orElseThrow());
        assertTrue(HerancaBestialFeat.chosenBy(bestial, BestialFeat.HERANCA_CANINA).isEmpty());
    }

    /** A grant duplicating an already-acquired ability must not make it count twice. */
    @Test
    void aGrantDuplicatingAnAcquiredAbilityIsNotCountedTwice() {
        Character bestial = character().race(new Bestial())
                .skillCompetencyAbility(AtletismoCompetencyAbility.PASSO_LARGO)
                .build();
        bestial.grantFeat(HerancaBestialFeat.of(BestialFeat.HERANCA_BOVIDEA,
                AtletismoCompetencyAbility.PASSO_LARGO));

        assertEquals(1, SkillCompetencyAbility.allFor(bestial).stream()
                .filter(AtletismoCompetencyAbility.PASSO_LARGO::equals).count());
    }

    // ---------- Habilidade de Atributo, of a chosen Atributo ----------

    /**
     * {@code DestinoFeat#GENIALIDADE}'s "uma das Habilidades do Atributo escolhido" — folded into
     * {@code Character#getAttributeAbilities()} without consuming a slot, the same mechanism
     * {@code ConselheiroDeGuerraYmirianoFeat} uses for its fixed-Atributo twin.
     */
    @Test
    void aChosenAttributeAbilityIsGrantedWithoutASlot() {
        Character character = withStrength(3);

        character.grantFeat(new HabilidadeDeAtributoEscolhidaFeat(
                DestinoFeat.GENIALIDADE, StrengthAbility.DESTRUIDOR_DE_MUROS));

        assertTrue(character.getAttributeAbilities().contains(StrengthAbility.DESTRUIDOR_DE_MUROS));
        assertFalse(character.getAcquiredAttributeAbilities().contains(StrengthAbility.DESTRUIDOR_DE_MUROS),
                "granted, not acquired — no paid slot consumed");
    }

    /** Only Genialidade Desperta of the three adds the "+1 de Bônus Racial no Atributo escolhido". */
    @Test
    void onlyGenialidadeDespertaAddsTheRacialBonus() {
        Character plain = withStrength(3);
        Character desperta = withStrength(3);
        int before = plain.getEffectiveAttributeTotal(AttributeDomain.STRENGTH);

        plain.grantFeat(new HabilidadeDeAtributoEscolhidaFeat(
                DestinoFeat.GENIALIDADE, StrengthAbility.DESTRUIDOR_DE_MUROS));
        desperta.grantFeat(new HabilidadeDeAtributoEscolhidaFeat(
                DestinoFeat.GENIALIDADE_DESPERTA, StrengthAbility.DESTRUIDOR_DE_MUROS));

        assertEquals(before, plain.getEffectiveAttributeTotal(AttributeDomain.STRENGTH));
        assertEquals(before + 1, desperta.getEffectiveAttributeTotal(AttributeDomain.STRENGTH));
    }

    /** The validated factory enforces "você precisa cumprir com Requisitos" against the chosen Atributo. */
    @Test
    void theFactoryRefusesAnAbilityInAnAttributeWithNoSlot() {
        Character tooWeak = withStrength(1);

        assertThrows(IllegalOperationException.class, () -> HabilidadeDeAtributoEscolhidaFeat.of(
                tooWeak, DestinoFeat.PRODIGIO, StrengthAbility.DESTRUIDOR_DE_MUROS));
        assertDoesNotThrow(() -> HabilidadeDeAtributoEscolhidaFeat.of(
                withStrength(3), DestinoFeat.PRODIGIO, StrengthAbility.DESTRUIDOR_DE_MUROS));
    }

    // ---------- The no-other-effects form ----------

    /**
     * {@code ChosenSkillTraitsFeat} serves the constants whose whole payload is the grant, across
     * trees — three traits for {@code PeritoFeat#TREINADO_EM_PERICIAS}, one for {@code
     * GnomoFeat#SABICHAO}. Mixed kinds in one pick, which is what "uma Especialização <i>ou</i>
     * Habilidade de Competência de cada uma destas Perícias" asks for.
     */
    @Test
    void theNoOtherEffectsFormGrantsEveryChosenTraitAcrossKinds() {
        Character character = bestial();

        character.grantFeat(ChosenSkillTraitsFeat.of(PeritoFeat.TREINADO_EM_PERICIAS,
                AtletismoCompetencyAbility.PASSO_LARGO, AtletismoSpecialization.TRI_ATLETA));

        assertTrue(SkillCompetencyAbility.allFor(character).contains(AtletismoCompetencyAbility.PASSO_LARGO));
        assertEquals(List.of(AtletismoSpecialization.TRI_ATLETA),
                character.getSpecializations(SkillType.ATLETISMO));
        assertEquals(PeritoFeat.TREINADO_EM_PERICIAS, character.getFeats().get(0).catalogEntry());
    }

    /** Two such Talentos held at once each keep their own pick. */
    @Test
    void chosenByIsScopedToTheTalentoAsked() {
        Character character = bestial();
        character.grantFeat(ChosenSkillTraitsFeat.of(GnomoFeat.SABICHAO,
                AtletismoCompetencyAbility.PASSO_LARGO));

        assertEquals(java.util.Set.of(AtletismoCompetencyAbility.PASSO_LARGO),
                ChosenSkillTraitsFeat.chosenBy(character, GnomoFeat.SABICHAO));
        assertTrue(ChosenSkillTraitsFeat.chosenBy(character, PeritoFeat.TREINADO_EM_PERICIAS).isEmpty());
    }

    private static Character withStrength(final int base) {
        return character()
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder()
                                .domain(AttributeDomain.STRENGTH).base(base).build())
                        .build())
                .build();
    }
}
