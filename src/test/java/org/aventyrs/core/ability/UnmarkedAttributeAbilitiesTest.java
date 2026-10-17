package org.aventyrs.core.ability;

import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.item.HandBudget;
import org.aventyrs.core.item.ItemCatalog;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.magic.catalog.VidaSpell;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The four Habilidades wired without a TODO having marked them (core 0.1.7.14): Besta de Carga, Agressão Anunciada,
 * Floreio Arcano and Voz de Ouro.
 */
class UnmarkedAttributeAbilitiesTest {

    private final ActionPointsService actionPoints = new ActionPointsServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet sheet(final AttributeAbility... abilities) {
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK);
        for (AttributeAbility ability : abilities) {
            builder.attributeAbility(ability);
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    private static CombatantAction rolled(final SkillType skill, final AttributeDomain domain,
                                          final org.aventyrs.core.skill.AttackSource source) {
        return new CombatantAction(skill, domain, source, null, 1, null, null, Set.of());
    }

    private static SkillType charismaSkill() {
        return Arrays.stream(SkillType.values())
                .filter(type -> type.newSkillInstance().getAttributeDomain() == AttributeDomain.CHARISMA)
                .findFirst().orElseThrow();
    }

    @Test
    void bestaDeCargaCarriesEverythingOneClassLighterNeverBelowLeve() {
        Character strong = sheet(StrengthAbility.BESTA_DE_CARGA).getCharacter();
        Character plain = sheet().getCharacter();

        assertEquals(ItemWeightClass.MEDIUM, strong.carriedWeightClassOf(ItemWeightClass.HEAVY));
        assertEquals(ItemWeightClass.LIGHT, strong.carriedWeightClassOf(ItemWeightClass.MEDIUM));
        assertEquals(ItemWeightClass.LIGHT, strong.carriedWeightClassOf(ItemWeightClass.LIGHT));
        assertEquals(ItemWeightClass.HEAVY, plain.carriedWeightClassOf(ItemWeightClass.HEAVY));
    }

    @Test
    void bestaDeCargaWieldsAMedioMeleeWeaponInOneHand() {
        Weapon medium = ItemCatalog.all().stream()
                .filter(item -> item instanceof Weapon weapon && weapon.getWeightClass() == ItemWeightClass.MEDIUM
                        && weapon.getSkillType() == SkillType.ATAQUE_CORPO_A_CORPO
                        && item.getCategory() != ItemCategory.BOW && item.getCategory() != ItemCategory.CROSSBOW)
                .map(Weapon.class::cast)
                .findFirst().orElseThrow();

        assertTrue(HandBudget.isTwoHanded(medium, sheet().getCharacter()));
        assertFalse(HandBudget.isTwoHanded(medium, sheet(StrengthAbility.BESTA_DE_CARGA).getCharacter()));
    }

    @Test
    void agressaoAnunciadaFavoursAForcaOrDestrezaRollRightAfterACarismaOne() {
        CharacterSheet holder = sheet(CharismaAbility.AGRESSAO_ANUNCIADA);
        AttributeAbility ability = CharismaAbility.AGRESSAO_ANUNCIADA;
        holder.startTurn(1);
        assertEquals(0, ability.resolveAttributeDomainRollBonus(AttributeDomain.STRENGTH, holder.getCharacter(), holder));

        holder.recordAction(rolled(charismaSkill(), AttributeDomain.CHARISMA, null));
        assertEquals(Skill.ADVANTAGE_BONUS,
                ability.resolveAttributeDomainRollBonus(AttributeDomain.STRENGTH, holder.getCharacter(), holder));
        assertEquals(Skill.ADVANTAGE_BONUS,
                ability.resolveAttributeDomainRollBonus(AttributeDomain.DEXTERITY, holder.getCharacter(), holder));
        assertEquals(0, ability.resolveAttributeDomainRollBonus(AttributeDomain.FOCUS, holder.getCharacter(), holder));

        holder.recordAction(rolled(SkillType.ATAQUE_CORPO_A_CORPO, AttributeDomain.STRENGTH, null));
        assertEquals(0, ability.resolveAttributeDomainRollBonus(AttributeDomain.STRENGTH, holder.getCharacter(), holder),
                "no longer immediately after");
    }

    @Test
    void floreioArcanoFavoursACarismaRollRightAfterAMagia() {
        CharacterSheet holder = sheet(CharismaAbility.FLOREIO_ARCANO);
        AttributeAbility ability = CharismaAbility.FLOREIO_ARCANO;
        holder.startTurn(1);
        holder.recordAction(rolled(SkillType.ATAQUE_CORPO_A_CORPO, AttributeDomain.STRENGTH, null));
        assertEquals(0, ability.resolveAttributeDomainRollBonus(AttributeDomain.CHARISMA, holder.getCharacter(), holder));

        holder.recordAction(rolled(VidaSpell.REVIGORAR.getAttackSkillType(), AttributeDomain.FOCUS, VidaSpell.REVIGORAR));
        assertEquals(Skill.ADVANTAGE_BONUS,
                ability.resolveAttributeDomainRollBonus(AttributeDomain.CHARISMA, holder.getCharacter(), holder));
        assertEquals(0, ability.resolveAttributeDomainRollBonus(AttributeDomain.STRENGTH, holder.getCharacter(), holder));
    }

    @Test
    void vozDeOuroBuysOnePaOffACarismaRollForTwoPd() {
        CharacterSheet holder = sheet(CharismaAbility.VOZ_DE_OURO);
        SkillType charisma = charismaSkill();
        int plain = actionPoints.getSkillRollCost(holder, charisma, Set.of(), 1, null).spentActionPoints();

        assertEquals(2, actionPoints.getBoughtReductionCost(holder, charisma));
        assertEquals(plain - 1, actionPoints.getSkillRollCost(holder, charisma, Set.of(), 1, null, true)
                .spentActionPoints());
        assertEquals(0, actionPoints.getBoughtReductionCost(holder, SkillType.ATAQUE_CORPO_A_CORPO));
        assertEquals(0, actionPoints.getBoughtReductionCost(sheet(), charisma));
    }
}
