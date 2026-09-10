package org.aventyrs.core.character.services;

import java.util.ArrayList;
import java.util.Optional;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.SizeCategory;
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

    /** A Lança — one of the eight melee weapons equipamentos.txt authors at Muito Curta or Curta.
     * The case the old melee-only-replace rule had no coverage for, and got wrong. */
    private static final Weapon SPEAR = AbstractWeapon.builder()
            .name("Lança")
            .category(ItemCategory.SPEAR)
            .damageBase(DamageBase.of(1, 3))
            .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
            .range(Range.DISTANCIA_MUITO_CURTA)
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

    private Character sizedCharacter(final SizeCategory sizeCategory) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>())
                .sizeCategory(sizeCategory)
                .build();
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
    void aLargerSizeCategoryWidensAMeleeWeaponsAdjacenteReach() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>())
                .sizeCategory(SizeCategory.PLUS_TWO)
                .build();

        assertEquals(Range.DISTANCIA_MUITO_CURTA, attackRangeService.getEffectiveRange(character, DAGGER));
    }

    /**
     * Size widens a weapon that states its own reach by adding {@code getRangeModifier()} to it —
     * so a Gigante's lança reaches 3UD where a human's reaches 2, and, crucially, further than that
     * same Gigante's bare fist (2UD). Under the previous melee-only-replace rule the fist
     * out-reached the polearm.
     */
    @Test
    void sizeAddsToAWeaponThatStatesItsOwnReach() {
        Character human = blankCharacter();
        Character giant = sizedCharacter(SizeCategory.PLUS_TWO);

        assertEquals(2, attackRangeService.getEffectiveRangeInUnidadesDeDistancia(human, SPEAR));
        assertEquals(3, attackRangeService.getEffectiveRangeInUnidadesDeDistancia(giant, SPEAR));
        assertEquals(2, attackRangeService.getEffectiveRangeInUnidadesDeDistancia(giant, DAGGER),
                "the fist/adaga case substitutes the Alcance column instead");
    }

    /** Ranged weapons are widened too — a giant draws a longbow further than a goblin does. The
     * band form cannot express 17UD and rounds up to MUITO_LONGA's 24, which is exactly why the UD
     * form exists. */
    @Test
    void sizeAddsToARangedWeaponToo() {
        Character giant = sizedCharacter(SizeCategory.PLUS_TWO);

        assertEquals(17, attackRangeService.getEffectiveRangeInUnidadesDeDistancia(giant, LONGBOW));
        assertEquals(Range.DISTANCIA_LONGA,
                attackRangeService.getEffectiveRange(blankCharacter(), LONGBOW));
    }

    /**
     * The band is the smallest one that <em>covers</em> the reach, so it rounds up and the two
     * forms genuinely disagree: an adaga at Categoria +4 reaches 3UD — Muito Curta plus one — and
     * there is no band worth 3. Pinned in one test so the lossiness reads as deliberate, and so
     * anyone measuring reaches for the UD form.
     */
    @Test
    void theBandRoundsUpWhereTheReachHasNoBandOfItsOwn() {
        Character huge = sizedCharacter(SizeCategory.PLUS_FOUR);

        assertEquals(3, attackRangeService.getEffectiveRangeInUnidadesDeDistancia(huge, DAGGER));
        assertEquals(Range.DISTANCIA_CURTA, attackRangeService.getEffectiveRange(huge, DAGGER));
        assertEquals(4, Range.DISTANCIA_CURTA.getMaxUnidadesDeDistancia(),
                "the band a caller would wrongly measure with is worth one UD more than the reach");
    }

    /** A small creature still reaches the hex in front of it — the MINIMUM_MELEE_RANGE floor, on
     * both branches. */
    @Test
    void noCreatureEverReachesLessThanOneUnidadeDeDistancia() {
        Character tiny = sizedCharacter(SizeCategory.MINUS_FOUR);

        assertEquals(1, attackRangeService.getEffectiveRangeInUnidadesDeDistancia(tiny, DAGGER));
        assertEquals(1, attackRangeService.getEffectiveRangeInUnidadesDeDistancia(tiny, SPEAR));
        assertEquals(Range.ADJACENTE, attackRangeService.getEffectiveRange(tiny, DAGGER));
    }

    /** A reach limited only by sight names no distance to add a modifier to. */
    @Test
    void aReachLimitedOnlyBySightStaysUnbounded() {
        Weapon gaze = AbstractWeapon.builder()
                .name("Olhar")
                .category(ItemCategory.PROJECTILE)
                .damageBase(DamageBase.of(1, 0))
                .skillType(SkillType.ATAQUE_A_DISTANCIA)
                .range(Range.AO_ALCANCE_DOS_OLHOS)
                .build();
        Character giant = sizedCharacter(SizeCategory.PLUS_TWO);

        assertEquals(AttackRangeService.UNBOUNDED_RANGE,
                attackRangeService.getEffectiveRangeInUnidadesDeDistancia(giant, gaze));
        assertEquals(Range.AO_ALCANCE_DOS_OLHOS, attackRangeService.getEffectiveRange(giant, gaze));
    }

    /**
     * Size and a Talento genuinely compose, in that order: the UD reach is resolved to a band
     * first, then the band step is applied. A PLUS_TWO Gigante with a longbow reaches 17UD, which
     * rounds to MUITO_LONGA, and TIRO_LONGO's "+1 nível" then takes it to AO_ALCANCE_DOS_OLHOS.
     *
     * <p>The ordering is forced rather than chosen: {@code Feat#resolveAttackRangeIncrease} returns
     * whole band steps, never a UD count, so a step cannot be applied before the rounding.
     */
    @Test
    void sizeAndTiroLongoCompose() {
        Character giant = sizedCharacter(SizeCategory.PLUS_TWO);
        giant.grantFeat(ArtilhariaFeat.TIRO_LONGO);

        assertEquals(Range.AO_ALCANCE_DOS_OLHOS, attackRangeService.getEffectiveRange(giant, LONGBOW));
        // The UD form is size-only, so the Talento leaves it untouched.
        assertEquals(17, attackRangeService.getEffectiveRangeInUnidadesDeDistancia(giant, LONGBOW));
    }

    /** TIRO_LONGO is scoped to Ataque à Distância, so a melee weapon sees only the size widening —
     * the two sources stay unconflated. */
    @Test
    void tiroLongoLeavesAMeleeWeaponToItsSizeWideningAlone() {
        Character giant = sizedCharacter(SizeCategory.PLUS_TWO);
        giant.grantFeat(ArtilhariaFeat.TIRO_LONGO);

        assertEquals(Range.DISTANCIA_MUITO_CURTA, attackRangeService.getEffectiveRange(giant, DAGGER));
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
