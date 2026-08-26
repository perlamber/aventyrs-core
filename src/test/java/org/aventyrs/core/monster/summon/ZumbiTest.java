package org.aventyrs.core.monster.summon;

import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.character.services.LifeStealService;
import org.aventyrs.core.character.services.LifeStealServiceImpl;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.monster.GenericMonster;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.race.CreatureType;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoInteraction;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoSpecialization;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZumbiTest {

    private final HitPointsService hitPointsService = new HitPointsServiceImpl();
    private final ActionPointsService actionPointsService = new ActionPointsServiceImpl();
    private final LifeStealService lifeStealService = new LifeStealServiceImpl();
    private final AtaqueCorpoACorpoInteraction ataque = new AtaqueCorpoACorpoInteraction();

    @BeforeEach
    void setUp() {
        CharacterFixture.loadTemplates();
    }

    /** A living player character, for the against-the-living clause and as a roll subject. */
    private CharacterSheet livingHero() {
        Character hero = CharacterFixture.blank(CharacterFixture.BLANK).build();
        return CharacterSheet.of(hero, new Player());
    }

    private Character conjuradorWithGraduation(final int graduation) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.DOMINIO_DO_MANA, CharacterSkill.builder()
                        .skill(SkillType.DOMINIO_DO_MANA.newSkillInstance())
                        .graduation(SkillGraduation.builder().graduationValue(graduation).build())
                        .build())
                .build();
    }

    @Test
    void aNarrativeZumbiCarriesTheStatBlockOntoTheSheet() {
        MonsterSheet zumbi = Zumbi.builder().build().spawn(new Player());
        Character corpse = zumbi.getCharacter();

        assertEquals("Zumbi", corpse.getName());
        assertEquals(CreatureType.MONSTRUOSO, corpse.getRace().getCreatureType());
        assertEquals(Zumbi.DEFENSES, zumbi.getDefense(DefenseType.PHYSICAL));
        assertEquals(Zumbi.DEFENSES, zumbi.getDefense(DefenseType.MAGIC));

        // Every Attribute the block states, including the five that equal AttributeValue's default.
        assertEquals(Zumbi.BASE_STRENGTH, corpse.getAttributes().getStrength().getTotal());
        assertEquals(1, corpse.getAttributes().getDexterity().getTotal());
        assertEquals(2, corpse.getAttributes().getVigor().getTotal());
        assertEquals(1, corpse.getAttributes().getCharisma().getTotal());

        // "20PV (Multiplicador de PV x5)" — 10 base + Vigor 2 x 5, with no Conjurador bonus.
        assertEquals(20, hitPointsService.getMaxHitPoints(corpse));

        // "Possuem 2 Pontos de Ação (PA)" — not the default 3 every other combatant starts with.
        assertEquals(Zumbi.ACTION_POINTS, corpse.getActionPoints());
        assertEquals(Zumbi.ACTION_POINTS, actionPointsService.getMaxActionPoints(corpse, 0));
    }

    @Test
    void theAtaqueCorpoACorpoPericiaCarriesThePrimalEspecializacao() {
        Character corpse = Zumbi.builder().build().spawn(new Player()).getCharacter();
        CharacterSkill ataqueSkill = corpse.getSkills().get(SkillType.ATAQUE_CORPO_A_CORPO);

        assertEquals(2, ataqueSkill.getGraduation().getGraduationValue());
        assertEquals(java.util.List.of(AtaqueCorpoACorpoSpecialization.PRIMAL),
                ataqueSkill.getSpecializations());
    }

    @Test
    void theConjuradorsManaGraduationBecomesAnAttackBonus() {
        // The threshold a defender's Esquiva e Aparar roll must clear: AttackReceiver's direction.
        assertEquals(0, Zumbi.summonedBy(0).spawn(new Player()).getAttackBonus());
        assertEquals(3, Zumbi.summonedBy(3).spawn(new Player()).getAttackBonus());

        // And the Zumbi's own Ataque roll: the ability's @Modifier, resolved off the instance.
        MonsterSheet plain = Zumbi.summonedBy(0).spawn(new Player());
        MonsterSheet raised = Zumbi.summonedBy(3).spawn(new Player());
        assertEquals(rollBonusOf(plain) + 3, rollBonusOf(raised));
    }

    private int rollBonusOf(final MonsterSheet zumbi) {
        InteractionResult result = ataque.applyTo(zumbi);
        return result.getSkillRollBonus();
    }

    @Test
    void fourGraduationsGrantTenExtraHitPoints() {
        assertEquals(20, hitPointsService.getMaxHitPoints(Zumbi.summonedBy(3).spawn(new Player()).getCharacter()));
        assertEquals(20 + ZumbiAbility.ENCANTAMENTO_HIT_POINTS,
                hitPointsService.getMaxHitPoints(Zumbi.summonedBy(4).spawn(new Player()).getCharacter()));
    }

    @Test
    void theTenExtraHitPointsStayFlatRatherThanScalingWithVigor() {
        // The point of ModifierType.HIT_POINTS over a lifeMultiplier uplift: had this been
        // expressed as a multiplier it would only ever have landed on +10 at Vigor exactly 2.
        Character corpse = Zumbi.summonedBy(4).spawn(new Player()).getCharacter();
        assertEquals(Zumbi.LIFE_MULTIPLIER, hitPointsService.getLifeMultiplier(corpse));
        assertEquals(ZumbiAbility.ENCANTAMENTO_HIT_POINTS, hitPointsService.getHitPointsBonus(corpse));
    }

    @Test
    void sevenGraduationsGrantStrengthAndLifeSteal() {
        MonsterSheet six = Zumbi.summonedBy(6).spawn(new Player());
        MonsterSheet seven = Zumbi.summonedBy(7).spawn(new Player());

        assertEquals(Zumbi.BASE_STRENGTH, six.getCharacter().getAttributes().getStrength().getTotal());
        assertEquals(Zumbi.BASE_STRENGTH + ZumbiAbility.ENCANTAMENTO_STRENGTH,
                seven.getCharacter().getAttributes().getStrength().getTotal());

        assertEquals(0, lifeStealService.getTotalLifeSteal(six.getCharacter(), six));
        assertEquals(ZumbiAbility.LIFE_STEAL,
                lifeStealService.getTotalLifeSteal(seven.getCharacter(), seven));
    }

    @Test
    void theLifeStealOutlivesEveryRodada() {
        // Open-ended: nothing about the encantamento counts down, so ticking must not expire it.
        MonsterSheet zumbi = Zumbi.summonedBy(7).spawn(new Player());
        zumbi.finishTurn();
        zumbi.finishTurn();
        zumbi.finishTurn();

        assertEquals(ZumbiAbility.LIFE_STEAL, zumbi.getTotalLifeSteal());
    }

    @Test
    void tenGraduationsReduceTheDifficultyByOneLevel() {
        assertEquals(0, ataque.applyTo(Zumbi.summonedBy(9).spawn(new Player())).getDifficultyReduction());
        assertEquals(1, ataque.applyTo(Zumbi.summonedBy(10).spawn(new Player())).getDifficultyReduction());
    }

    @Test
    void aZumbiHasAdvantageAgainstTheLiving() {
        MonsterSheet zumbi = Zumbi.builder().build().spawn(new Player());
        CharacterSheet hero = livingHero();

        int againstNobody = ataque.applyTo(zumbi, null, null, null).getSkillRollBonus();
        int againstHero = ataque.applyTo(zumbi, null, null, hero).getSkillRollBonus();

        assertEquals(againstNobody + Skill.ADVANTAGE_BONUS, againstHero);
    }

    @Test
    void butNotAgainstAnotherUndead() {
        MonsterSheet zumbi = Zumbi.builder().build().spawn(new Player());
        MonsterSheet otherZumbi = Zumbi.builder().build().spawn(new Player());
        MonsterSheet livingBrute = GenericMonster.BRUTAMONTES.spawn(new Player());

        int againstNobody = ataque.applyTo(zumbi, null, null, null).getSkillRollBonus();

        assertEquals(againstNobody, ataque.applyTo(zumbi, null, null, otherZumbi).getSkillRollBonus());
        // A foe that isn't a Morto-Vivo still counts as living — the flag, not the sheet type.
        assertEquals(againstNobody + Skill.ADVANTAGE_BONUS,
                ataque.applyTo(zumbi, null, null, livingBrute).getSkillRollBonus());
    }

    @Test
    void theAnatomyMakesItUndeadAndImmuneToFiveEfeitosCriticos() {
        MonsterSheet zumbi = Zumbi.builder().build().spawn(new Player());

        assertTrue(zumbi.isUndead());
        assertEquals(Zumbi.ANATOMIA_DE_MORTO_VIVO_MENOR, zumbi.getCriticalEffectImmunities());
        assertTrue(zumbi.getCriticalEffectImmunities().contains(CriticalEffectType.SANGRAMENTO));
        // Named but unimplemented effects are immunities all the same — see CriticalEffectType.
        assertTrue(zumbi.getCriticalEffectImmunities().contains(CriticalEffectType.DILACERAR));
        // Not every Efeito Crítico: Purga de Mana is not on its anatomy's list.
        assertFalse(zumbi.getCriticalEffectImmunities().contains(CriticalEffectType.PURGA_DE_MANA));
    }

    @Test
    void theSizeCategoryIsClampedToTheBodysRange() {
        assertEquals(SizeCategory.ZERO, Zumbi.builder().build().spawn(new Player()).getCharacter().getSizeCategory());
        assertEquals(SizeCategory.PLUS_ONE, sizeOf(SizeCategory.PLUS_ONE));
        assertEquals(Zumbi.MIN_SIZE_CATEGORY, sizeOf(SizeCategory.MINUS_TWO));
        assertEquals(Zumbi.MAX_SIZE_CATEGORY, sizeOf(SizeCategory.PLUS_TWO));
        // A corpse outside the range is described, not rejected — it just animates at the limit.
        assertEquals(Zumbi.MIN_SIZE_CATEGORY, sizeOf(SizeCategory.MINUS_FOUR));
        assertEquals(Zumbi.MAX_SIZE_CATEGORY, sizeOf(SizeCategory.PLUS_FIVE));
    }

    private SizeCategory sizeOf(final SizeCategory body) {
        return Zumbi.builder().bodySizeCategory(body).build().spawn(new Player()).getCharacter().getSizeCategory();
    }

    @Test
    void spawnFromASummonerReadsTheirDominioDoManaGraduation() {
        Zumbi template = Zumbi.builder().build();

        assertEquals(7, template.spawn(conjuradorWithGraduation(7), new Player()).getAttackBonus());
        assertEquals(20 + ZumbiAbility.ENCANTAMENTO_HIT_POINTS,
                hitPointsService.getMaxHitPoints(template.spawn(conjuradorWithGraduation(4), new Player()).getCharacter()));
    }

    @Test
    void aConjuradorUntrainedInDominioDoManaSummonsTheUntieredBaseline() {
        Zumbi template = Zumbi.builder().build();
        Character untrained = CharacterFixture.blank(CharacterFixture.BLANK).build();

        assertEquals(0, template.spawn(untrained, new Player()).getAttackBonus());
        assertEquals(20, hitPointsService.getMaxHitPoints(template.spawn(untrained, new Player()).getCharacter()));
        // Identical to the no-Conjurador case, which is the point of 0 being a real value.
        assertEquals(template.spawn(new Player()).getAttackBonus(), template.spawn(untrained, new Player()).getAttackBonus());
    }

    @Test
    void withConjuradorLeavesTheOriginalTemplateUntouched() {
        Zumbi narrative = Zumbi.builder().build();
        Zumbi raised = narrative.withConjurador(10);

        assertEquals(0, narrative.getConjuradorManaGraduation());
        assertEquals(10, raised.getConjuradorManaGraduation());
        // The body choice survives re-parameterization.
        assertEquals(SizeCategory.PLUS_TWO,
                Zumbi.builder().bodySizeCategory(SizeCategory.PLUS_TWO).build()
                        .withConjurador(4).getSizeCategory());
    }

    @Test
    void twoSpawnsFromOneZumbiAreFullyIndependent() {
        Zumbi template = Zumbi.summonedBy(7);
        MonsterSheet first = template.spawn(new Player());
        MonsterSheet second = template.spawn(new Player());

        first.applyDamage(6);
        first.getCharacter().getSkills().get(SkillType.ATAQUE_CORPO_A_CORPO).increaseGraduation(5);

        assertEquals(0, second.getDamageTaken());
        assertEquals(2, second.getCharacter().getSkills()
                .get(SkillType.ATAQUE_CORPO_A_CORPO).getGraduation().getGraduationValue());
        // The LifeSteal is per-sheet too, not one shared effect instance.
        assertEquals(ZumbiAbility.LIFE_STEAL, second.getTotalLifeSteal());
        assertFalse(first.getId().equals(second.getId()));
    }

    @Test
    void theAttackDifficultyItPresentsIsAuthored() {
        assertEquals(DifficultyLevel.EASY, Zumbi.builder().build().spawn(new Player()).getAttackDifficulty());
    }

    @Test
    void aZumbiCannotReachTheExperienceSpendingServices() {
        // Same guarantee MonsterSheetTest pins for every foe, asserted reflectively because
        // `zumbi instanceof CharacterSheet` doesn't compile — which is the point.
        assertFalse(CharacterSheet.class.isAssignableFrom(Zumbi.builder().build().spawn(new Player()).getClass()));
    }

    @Test
    void everyStatBlockConstantMatchesTheRulesText() {
        // Pins the literal numbers against the source text, so a later edit to a constant that
        // several other tests only compare against itself can't quietly redefine the creature.
        assertEquals(Map.of(SkillType.ATAQUE_CORPO_A_CORPO, 2), Zumbi.builder().build().getSkillGraduations());
        assertEquals(4, Zumbi.BASE_STRENGTH);
        assertEquals(11, Zumbi.DEFENSES);
        assertEquals(5, Zumbi.LIFE_MULTIPLIER);
        assertEquals(2, Zumbi.ACTION_POINTS);
        assertEquals(10, ZumbiAbility.ENCANTAMENTO_HIT_POINTS);
        assertEquals(2, ZumbiAbility.ENCANTAMENTO_STRENGTH);
        assertEquals(1, ZumbiAbility.LIFE_STEAL);
        assertEquals(5, Zumbi.ANATOMIA_DE_MORTO_VIVO_MENOR.size());
    }
}
