package org.aventyrs.core.monster;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.character.services.CharacterAttributeService;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.item.ArmorItem;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.race.CreatureType;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoSpecialization;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonsterTemplateTest {

    private final HitPointsService hitPointsService = new HitPointsServiceImpl();
    private final DefenseService defenseService = new DefenseServiceImpl();

    private AbstractMonsterTemplate.AbstractMonsterTemplateBuilder troll() {
        return AbstractMonsterTemplate.builder()
                .name("Troll da Ponte Velha")
                .attributeBase(AttributeDomain.VIGOR, 9)
                .attributeBase(AttributeDomain.STRENGTH, 8)
                .skillGraduation(SkillType.ATAQUE_CORPO_A_CORPO, 12)
                .sizeCategory(SizeCategory.PLUS_TWO)
                .physicalDefense(19)
                .magicDefense(13)
                .attackDifficulty(DifficultyLevel.HARD)
                .attackBonus(3);
    }

    @Test
    void spawnCarriesTheStatBlockOntoTheSheet() {
        MonsterSheet troll = troll().build().spawn(new Player());

        assertEquals(19, troll.getPhysicalDefense());
        assertEquals(13, troll.getMagicDefense());
        assertEquals(DifficultyLevel.HARD, troll.getAttackDifficulty());
        assertEquals(3, troll.getAttackBonus());
        assertEquals(19, troll.getDefense(DefenseType.PHYSICAL));
        assertEquals(13, troll.getDefense(DefenseType.MAGIC));
    }

    @Test
    void spawnBuildsACharacterWithTheMonsterRaceAndNoPlayer() {
        Character troll = troll().build().spawn(new Player()).getCharacter();

        assertEquals(CreatureType.MONSTRUOSO, troll.getRace().getCreatureType());
        assertNull(troll.getPlayer());
        assertEquals("Troll da Ponte Velha", troll.getName());
        assertEquals(SizeCategory.PLUS_TWO, troll.getSizeCategory());
    }

    /**
     * The point of the whole exercise: a monster's Attributes and Graduações go far past what a
     * player could ever reach, and that needs no special mechanism — the caps only ever lived on
     * the XP-spending services, which a monster cannot reach.
     */
    @Test
    void aSpawnedMonstersAttributesAndGraduacoesAreUncapped() {
        Character troll = troll().build().spawn(new Player()).getCharacter();

        assertTrue(9 > CharacterAttributeService.MAX_ATTRIBUTE_BASE);
        assertEquals(9, troll.getAttributes().getAttribute(AttributeDomain.VIGOR).getBase());
        assertEquals(12, troll.getSkills().get(SkillType.ATAQUE_CORPO_A_CORPO)
                .getGraduation().getGraduationValue());
    }

    @Test
    void twoSpawnsFromOneTemplateAreFullyIndependent() {
        MonsterTemplate template = troll().build();
        MonsterSheet first = template.spawn(new Player());
        MonsterSheet second = template.spawn(new Player());

        assertNotSame(first.getId(), second.getId());

        // SkillGraduation is mutable and increaseGraduation mutates in place, so a shared
        // instance would raise both monsters at once.
        first.getCharacter().getSkills().get(SkillType.ATAQUE_CORPO_A_CORPO).increaseGraduation(5);
        assertEquals(17, first.getCharacter().getSkills().get(SkillType.ATAQUE_CORPO_A_CORPO)
                .getGraduation().getGraduationValue());
        assertEquals(12, second.getCharacter().getSkills().get(SkillType.ATAQUE_CORPO_A_CORPO)
                .getGraduation().getGraduationValue());

        first.applyDamage(10);
        assertEquals(10, first.getDamageTaken());
        assertEquals(0, second.getDamageTaken());
    }

    /**
     * The reason {@code lifeMultiplier} became a field: bulk you can tune without inflating
     * Vigor, which would also inflate every Vigor-governed roll.
     */
    @Test
    void theLifeMultiplierDecouplesPvFromVigor() {
        Character ordinary = troll().build().spawn(new Player()).getCharacter();
        Character tanky = troll().lifeMultiplier(8).build().spawn(new Player()).getCharacter();

        assertEquals(HitPointsService.BASE_HIT_POINTS + 9 * HitPointsService.DEFAULT_LIFE_MULTIPLIER,
                hitPointsService.getMaxHitPoints(ordinary));
        assertEquals(HitPointsService.BASE_HIT_POINTS + 9 * 8, hitPointsService.getMaxHitPoints(tanky));
        // Vigor itself is untouched, so nothing else about the monster shifted.
        assertEquals(ordinary.getAttributes().getAttribute(AttributeDomain.VIGOR).getTotal(),
                tanky.getAttributes().getAttribute(AttributeDomain.VIGOR).getTotal());
    }

    @Test
    void anEquippedItemReachesTheSharedScanningServices() {
        MonsterSheet armoured = troll()
                .attributeBase(AttributeDomain.STRENGTH, 8)
                .equipmentItem(ArmorItem.ARMADURA_COMPLETA)
                .build()
                .spawn(new Player());

        // The armour's Favor needs Força 3; the troll has 8. Same DefenseService a player uses.
        assertEquals(ArmorItem.ARMADURA_COMPLETA.getPhysicalDefenseBonus(),
                defenseService.getTotalDefense(armoured, DefenseType.PHYSICAL));
        assertEquals(2, ArmorItem.ARMADURA_COMPLETA.resolveFavorBonus(
                ModifierType.DAMAGE_REDUCTION, armoured.getCharacter()));
    }

    @Test
    void aGenericMonsterSpawnsReadyToUse() {
        MonsterSheet aberracao = GenericMonster.ABERRACAO.spawn(new Player());

        assertEquals("Aberração", aberracao.getCharacter().getName());
        assertEquals(24, aberracao.getPhysicalDefense());
        assertEquals(DifficultyLevel.VERY_HARD, aberracao.getAttackDifficulty());
        assertEquals(14, aberracao.getCharacter().getSkills()
                .get(SkillType.ATAQUE_CORPO_A_CORPO).getGraduation().getGraduationValue());
        assertEquals(HitPointsService.BASE_HIT_POINTS + 12 * 8,
                hitPointsService.getMaxHitPoints(aberracao.getCharacter()));
    }

    @Test
    void everyGenericMonsterSpawnsWithoutError() {
        for (GenericMonster monster : GenericMonster.values()) {
            MonsterSheet sheet = monster.spawn(new Player());
            assertEquals(monster.getName(), sheet.getCharacter().getName());
            assertTrue(sheet.getInventory().isEmpty());
        }
    }

    @Test
    void aTemplateWithoutTheOptionalHooksKeepsEveryDefault() {
        MonsterSheet troll = troll().build().spawn(new Player());

        // Every hook added for stat blocks that need it leaves every other foe exactly as it was.
        assertEquals(ActionPointsService.DEFAULT_ACTION_POINTS, troll.getCharacter().getActionPoints());
        assertEquals(List.of(), troll.getCharacter().getSkills()
                .get(SkillType.ATAQUE_CORPO_A_CORPO).getSpecializations());
        assertFalse(troll.isUndead());
        assertEquals(Set.of(), troll.getCriticalEffectImmunities());
    }

    @Test
    void aStatBlockCanAuthorItsOwnActionPoints() {
        MonsterSheet troll = troll().actionPoints(2).build().spawn(new Player());

        assertEquals(2, troll.getCharacter().getActionPoints());
    }

    @Test
    void aStatBlockCanGiveAPericiaAnEspecializacao() {
        MonsterSheet troll = troll()
                .skillSpecialization(SkillType.ATAQUE_CORPO_A_CORPO,
                        List.of(AtaqueCorpoACorpoSpecialization.INFANTARIA_PESADA))
                .build().spawn(new Player());

        assertEquals(List.of(AtaqueCorpoACorpoSpecialization.INFANTARIA_PESADA),
                troll.getCharacter().getSkills().get(SkillType.ATAQUE_CORPO_A_CORPO).getSpecializations());
        // The Graduação it was authored with is untouched by carrying one.
        assertEquals(12, troll.getCharacter().getSkills()
                .get(SkillType.ATAQUE_CORPO_A_CORPO).getGraduation().getGraduationValue());
    }

    @Test
    void anEspecializacaoNamingAnUntrainedPericiaIsIgnored() {
        MonsterSheet troll = troll()
                .skillSpecialization(SkillType.ARTES, List.of(AtaqueCorpoACorpoSpecialization.PRIMAL))
                .build().spawn(new Player());

        assertNull(troll.getCharacter().getSkills().get(SkillType.ARTES));
    }

    @Test
    void aStatBlockCanAuthorItsAnatomy() {
        MonsterSheet troll = troll()
                .undead(true)
                .criticalEffectImmunity(CriticalEffectType.SANGRAMENTO)
                .build().spawn(new Player());

        assertTrue(troll.isUndead());
        assertEquals(Set.of(CriticalEffectType.SANGRAMENTO), troll.getCriticalEffectImmunities());
    }

    @Test
    void anAttributeOmittedFromTheTemplateKeepsItsDefault() {
        Character troll = troll().build().spawn(new Player()).getCharacter();

        assertEquals(1, troll.getAttributes().getAttribute(AttributeDomain.CHARISMA).getBase());
    }
}
