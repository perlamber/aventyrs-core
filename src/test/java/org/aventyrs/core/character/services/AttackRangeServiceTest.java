package org.aventyrs.core.character.services;

import java.util.ArrayList;
import java.util.Optional;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.ArtilhariaFeat;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.SpellTargeting;
import org.aventyrs.core.magic.TestSpell;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AttackRangeServiceTest {

    private final AttackRangeService attackRangeService = new AttackRangeServiceImpl();

    private static final Weapon LONGBOW = AbstractWeapon.builder()
            .name("Arco Longo")
            .category(ItemCategory.BOW)
            .hardness(6)
            .damageBase(DamageBase.of(1, 3))
            .skillType(SkillType.ATAQUE_A_DISTANCIA)
            .range(Range.DISTANCIA_LONGA)
            .build();

    private static final Weapon DAGGER = AbstractWeapon.builder()
            .name("Adaga")
            .category(ItemCategory.LIGHT_BLADE)
            .damageBase(DamageBase.of(1, 2))
            .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
            .build();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private Character blankCharacter() {
        return CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>()).build();
    }

    private static Spell spellReaching(final SpellTargeting targeting) {
        return new TestSpell() {
            @Override
            public SpellTargeting getTargeting() {
                return targeting;
            }
        };
    }

    @Test
    void aWeaponWithNoFeatsReportsItsOwnAuthoredAlcance() {
        Character character = blankCharacter();

        assertEquals(Range.DISTANCIA_LONGA, attackRangeService.getEffectiveRange(character, LONGBOW));
        assertEquals(Range.ADJACENTE, attackRangeService.getEffectiveRange(character, DAGGER));
    }

    @Test
    void tiroLongoWidensARangedWeaponByOneBandAndLeavesAMeleeOneAlone() {
        Character character = blankCharacter();
        character.grantFeat(ArtilhariaFeat.TIRO_LONGO);

        assertEquals(Range.DISTANCIA_MUITO_LONGA, attackRangeService.getEffectiveRange(character, LONGBOW));
        assertEquals(Range.ADJACENTE, attackRangeService.getEffectiveRange(character, DAGGER));
    }

    @Test
    void aDestroyedWeaponReachesOnlyAdjacenteEvenWithTiroLongo() {
        Character character = blankCharacter();
        character.grantFeat(ArtilhariaFeat.TIRO_LONGO);
        Weapon wreck = AbstractWeapon.builder()
                .name("Arco Partido")
                .category(ItemCategory.BOW)
                .hardness(6)
                .damageBase(DamageBase.of(1, 3))
                .skillType(SkillType.ATAQUE_A_DISTANCIA)
                .range(Range.DISTANCIA_LONGA)
                .build();
        wreck.applyDamage(6);

        assertEquals(Range.DISTANCIA_MUITO_CURTA, attackRangeService.getEffectiveRange(character, wreck));
    }

    @Test
    void theWeaponOverloadRefusesANullWeapon() {
        Character character = blankCharacter();

        assertThrows(NullPointerException.class,
                () -> attackRangeService.getEffectiveRange(character, (Weapon) null));
    }

    @Test
    void aRangedMagiaReportsItsOwnReachAndTiroLongoWidensItToo() {
        Character character = blankCharacter();
        Spell fireball = spellReaching(SpellTargeting.distancia(Range.DISTANCIA_MEDIA));

        assertEquals(Optional.of(Range.DISTANCIA_MEDIA),
                attackRangeService.getEffectiveRange(character, fireball));

        character.grantFeat(ArtilhariaFeat.TIRO_LONGO);
        assertEquals(Optional.of(Range.DISTANCIA_LONGA),
                attackRangeService.getEffectiveRange(character, fireball));
    }

    @Test
    void aMagiaWithNoPlacedDistanceHasNoMaximumRangeToWiden() {
        Character character = blankCharacter();
        character.grantFeat(ArtilhariaFeat.TIRO_LONGO);

        assertEquals(Optional.empty(),
                attackRangeService.getEffectiveRange(character, spellReaching(SpellTargeting.PESSOAL)));
        assertEquals(Optional.empty(),
                attackRangeService.getEffectiveRange(character, spellReaching(SpellTargeting.TOQUE)));
    }
}
