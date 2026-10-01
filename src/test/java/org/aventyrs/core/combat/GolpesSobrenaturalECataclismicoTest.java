package org.aventyrs.core.combat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.character.services.ActiveAbilityServiceImpl;
import org.aventyrs.core.character.services.DamageBaseServiceImpl;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.effect.EscancararDefesas;
import org.aventyrs.core.effect.ExplosaoCataclismica;
import org.aventyrs.core.feat.AssassinoFeat;
import org.aventyrs.core.feat.ElementalFeat;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.monster.GenericMonster;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.race.Invernal;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.TerrainType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Golpe Sobrenatural, Gana Elemental and Golpe Cataclísmico (core 0.0.88) — the attacks a Talento sends against the
 * DM instead of the DF, with their Correntes.
 */
class GolpesSobrenaturalECataclismicoTest {

    private final AttackDelivery attackDelivery = new AttackDelivery();

    private static final Weapon SWORD = AbstractWeapon.builder().name("Espada").category(ItemCategory.HEAVY_BLADE)
            .damageBase(DamageBase.of(2, 0)).skillType(SkillType.ATAQUE_CORPO_A_CORPO).build();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private static Character.CharacterBuilder fighter() {
        CharacterSkill skill = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1).build();
        skill.increaseGraduation(6);
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>())
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(6).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, skill);
    }

    private static SceneContext opposing(final CombatantSheet opponent) {
        return new SceneContext(List.of(), List.of(opponent), Map.of(opponent, Range.ADJACENTE),
                TerrainType.values()[0], true, 1, false, opponent);
    }

    private static List<Interaction<CombatantSheet>> stages(final DeliveredAttackResult result) {
        List<Interaction<CombatantSheet>> stages = new ArrayList<>();
        Interaction<CombatantSheet> next = result.getAttackResult().getNextInteraction().getNextInteraction();
        while (next != null) {
            stages.add(next);
            next = next.getNextInteraction();
        }
        return stages;
    }

    // ---------- Golpe Sobrenatural ----------

    private CharacterSheet assassin() {
        Character character = fighter().build();
        character.grantFeat(AssassinoFeat.GOLPE_DE_FINALIZACAO);
        character.grantFeat(AssassinoFeat.GOLPE_SOBRENATURAL);
        return CharacterSheet.of(character, new Player());
    }

    private static MonsterSheet woundedCapanga() {
        MonsterSheet capanga = GenericMonster.CAPANGA.spawn(new Player());
        capanga.applyDamage(new HitPointsServiceImpl().getMaxHitPoints(capanga.getCharacter(), capanga) / 2 + 1);
        return capanga;
    }

    private static SkillRoll supernatural(final List<Integer> dice) {
        return new SkillRoll(dice, null, null, null, null, Set.of(AssassinoFeat.GOLPE_SOBRENATURAL));
    }

    /** "contra a DM do alvo, ao invés da DF; este ataque causa Danos Mágicos" — and its 1PM is reported. */
    @Test
    void golpeSobrenaturalStrikesTheDmForMagicalDamage() {
        CharacterSheet hero = assassin();
        MonsterSheet capanga = woundedCapanga();

        DeliveredAttackResult result = attackDelivery.resolve(DeliveredAttack.from(capanga, DefenseType.PHYSICAL)
                .attacker(hero)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .sceneContext(opposing(capanga))
                .attackRoll(supernatural(List.of(3, 3, 3)))
                .build());

        assertEquals(capanga.getDefense(DefenseType.MAGIC), result.getRequiredTotal());
        assertEquals(DamageType.MAGICO, result.getAttackResult().getRetypedDamage().damageType());
        assertEquals(1, result.getAttackResult().getActivationManaCost());
    }

    @Test
    void golpeSobrenaturalsCriticalOpensTheDefenses() {
        CharacterSheet hero = assassin();
        MonsterSheet capanga = woundedCapanga();

        DeliveredAttackResult result = attackDelivery.resolve(DeliveredAttack.from(capanga, DefenseType.PHYSICAL)
                .attacker(hero)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .sceneContext(opposing(capanga))
                .attackRoll(supernatural(List.of(6, 6, 6)))
                .build());

        assertTrue(stages(result).stream().anyMatch(EscancararDefesas.class::isInstance));
    }

    /** Only on a Golpe de Finalização, and "apenas 1 vez a cada Rodada". */
    @Test
    void golpeSobrenaturalNeedsAFinishingBlowAndOncePerRodada() {
        CharacterSheet hero = assassin();
        MonsterSheet fresh = GenericMonster.CAPANGA.spawn(new Player());
        assertThrows(IllegalOperationException.class, () -> attackDelivery.resolve(
                DeliveredAttack.from(fresh, DefenseType.PHYSICAL).attacker(hero)
                        .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO).sceneContext(opposing(fresh))
                        .attackRoll(supernatural(List.of(3, 3, 3))).build()));

        MonsterSheet wounded = woundedCapanga();
        hero.recordAction(new CombatantAction(SkillType.ATAQUE_CORPO_A_CORPO, AttributeDomain.STRENGTH, null, null,
                1, null, wounded.getId(), Set.of(AssassinoFeat.GOLPE_SOBRENATURAL)));
        assertThrows(IllegalOperationException.class, () -> attackDelivery.resolve(
                DeliveredAttack.from(wounded, DefenseType.PHYSICAL).attacker(hero)
                        .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO).sceneContext(opposing(wounded))
                        .attackRoll(supernatural(List.of(3, 3, 3))).build()));
    }

    // ---------- Gana Elemental / Golpe Cataclísmico ----------

    private CharacterSheet elemental(final boolean cataclysmic) {
        Character character = fighter().race(new Invernal(new Human())).equipment(new ArrayList<>(List.of(SWORD)))
                .build();
        character.grantFeat(ElementalFeat.GANA_ELEMENTAL);
        if (cataclysmic) {
            character.grantFeat(ElementalFeat.GOLPE_CATACLISMICO);
        }
        return CharacterSheet.of(character, new Player());
    }

    /** "o Dano Base da Arma escolhida aumenta em +2 e o tipo de dano causado muda para Físico Elemental por 2 Rodadas". */
    @Test
    void ganaElementalEnchantsTheWeaponForTwoRodadas() {
        CharacterSheet invernal = elemental(false);
        DamageBase before = new DamageBaseServiceImpl().getDamageBase(invernal, SWORD);

        new ActiveAbilityServiceImpl().activate(invernal.getCharacter(), invernal,
                ElementalFeat.GanaElementalActiveAbility.INSTANCE, 1);

        assertEquals(before.scaledUp(2), new DamageBaseServiceImpl().getDamageBase(invernal, SWORD));
        assertEquals(2, invernal.getDeterminationSpent());
        var retype = SkillType.ATAQUE_CORPO_A_CORPO.newInteraction()
                .applyTo(invernal, null, new SkillRoll(List.of(3, 3, 3)), null, SWORD).getRetypedDamage();
        assertEquals(DamageType.FISICO_ELEMENTAL, retype.damageType());
        assertEquals(ElementalType.GELO, retype.elementalType());

        invernal.tickTemporaryEffects();
        invernal.tickTemporaryEffects();
        assertEquals(before, new DamageBaseServiceImpl().getDamageBase(invernal, SWORD), "the window closed");
    }

    /** The first attack of the Rodada with Gana active: against the DM, mágico, and Explosão on a critical. */
    @Test
    void golpeCataclismicoIsTheFirstAttackOfTheRodadaWhileGanaHolds() {
        CharacterSheet invernal = elemental(true);
        MonsterSheet capanga = GenericMonster.CAPANGA.spawn(new Player());
        DeliveredAttack plain = DeliveredAttack.from(capanga, DefenseType.PHYSICAL).attacker(invernal)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO).attackSource(SWORD)
                .attackRoll(new SkillRoll(List.of(6, 6, 6))).build();
        assertEquals(capanga.getDefense(DefenseType.PHYSICAL), attackDelivery.resolve(plain).getRequiredTotal(),
                "no Gana, no Golpe");

        new ActiveAbilityServiceImpl().activate(invernal.getCharacter(), invernal,
                ElementalFeat.GanaElementalActiveAbility.INSTANCE, 1);
        DeliveredAttackResult first = attackDelivery.resolve(plain);

        assertEquals(capanga.getDefense(DefenseType.MAGIC), first.getRequiredTotal());
        assertEquals(DamageType.ELEMENTAL, first.getAttackResult().getRetypedDamage().damageType());
        assertTrue(stages(first).stream().anyMatch(ExplosaoCataclismica.class::isInstance));

        invernal.recordAction(new CombatantAction(SkillType.ATAQUE_CORPO_A_CORPO, AttributeDomain.STRENGTH, SWORD,
                null, 1, null, capanga.getId(), Set.of()));
        DeliveredAttackResult second = attackDelivery.resolve(plain);
        assertEquals(capanga.getDefense(DefenseType.PHYSICAL), second.getRequiredTotal());
        assertEquals(DamageType.FISICO_ELEMENTAL, second.getAttackResult().getRetypedDamage().damageType());
        assertFalse(stages(second).stream().anyMatch(ExplosaoCataclismica.class::isInstance));
    }
}
