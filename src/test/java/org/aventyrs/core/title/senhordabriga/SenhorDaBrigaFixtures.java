package org.aventyrs.core.title.senhordabriga;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.item.AbstractItem;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shared builders for the Senhor da Briga tests — call {@link #loadTemplates()} in a {@code @BeforeEach}. */
final class SenhorDaBrigaFixtures {

    private SenhorDaBrigaFixtures() {
    }

    static void loadTemplates() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    /** A combatant with Força 4, Destreza 4, Ataque Corpo-a-Corpo 3, mutable equipment, and title in slot. */
    static CharacterSheet holder(final SenhorDaBriga title, final TitleSlot slot) {
        CharacterSheet sheet = combatant();
        if (title != null) {
            sheet.getCharacter().grantTitle(title, slot);
        }
        return sheet;
    }

    static CharacterSheet holder(final SenhorDaBriga title) {
        return holder(title, TitleSlot.PRIMARY);
    }

    /** A plain combatant with no Título. */
    static CharacterSheet combatant() {
        CharacterSkill skill = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1).build();
        skill.increaseGraduation(3);
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>())
                .equipment(new ArrayList<>())
                .drawnWeapons(new ArrayList<>())
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(4).build())
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(4).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, skill)
                .build();
        return CharacterSheet.of(character, new Player());
    }

    static Weapon sword() {
        return AbstractWeapon.builder().name("Espada").category(ItemCategory.HEAVY_BLADE)
                .damageBase(DamageBase.of(2, 0)).skillType(SkillType.ATAQUE_CORPO_A_CORPO).build();
    }

    /** Equips and draws a sword on sheet, returning it. */
    static Weapon drawSword(final CombatantSheet sheet) {
        Weapon sword = sword();
        sheet.getCharacter().equip(sword);
        sheet.getCharacter().drawWeapon(sword);
        return sword;
    }

    static Item armor() {
        return AbstractItem.builder().name("Armadura de teste").category(ItemCategory.ARMOR).build();
    }

    /** holder's snapshot: every given enemy at the given distance, in the given 0-based Rodada. */
    static SceneContext context(final int round, final Map<CombatantSheet, Range> enemies) {
        return new SceneContext(List.of(), new ArrayList<>(enemies.keySet()), new HashMap<>(enemies), null, true,
                round, false);
    }
}
