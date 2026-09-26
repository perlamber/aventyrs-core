package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.CriticalDamage;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpo;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The Duelista constants whose effects were blocked only by a stale TODO: EXPLORAR_PONTOS_FRACOS'
 * critical dano and DEFESA_COM_2_ARMAS' DF ladder. Judged through the services a player's numbers
 * come from, never the hooks alone.
 */
class DuelistaFeatTest {

    /** A total of 17: a critical at the default Margem Crítica Menor. */
    private static final List<Integer> SEVENTEEN = List.of(6, 6, 5);

    /** A total of 16: not a critical. */
    private static final List<Integer> SIXTEEN = List.of(6, 6, 4);

    private final DefenseService defenseService = new DefenseServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static AbstractWeapon blade(final String name) {
        return AbstractWeapon.builder()
                .name(name)
                .category(ItemCategory.LIGHT_BLADE)
                .weightClass(ItemWeightClass.LIGHT)
                .damageBase(DamageBase.of(1, 0))
                .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
                .build();
    }

    private static CharacterSkill meleeAt(final int graduation) {
        return CharacterSkill.builder()
                .skill(new AtaqueCorpoACorpo())
                .graduation(SkillGraduation.builder().graduationValue(graduation).build())
                .build();
    }

    private static CharacterSheet sheet(final List<Feat> feats, final int meleeGraduation,
                                        final List<Weapon> drawn) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.of(Map.of(AttributeDomain.GNOSE, 5)))
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, meleeAt(meleeGraduation))
                .equipment(new ArrayList<>(drawn))
                .drawnWeapons(new ArrayList<>(drawn))
                .feats(new ArrayList<>(feats))
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private static void attackWith(final CharacterSheet sheet, final Weapon weapon) {
        sheet.recordAction(new CombatantAction(SkillType.ATAQUE_CORPO_A_CORPO, AttributeDomain.STRENGTH,
                weapon, null, 0, null));
    }

    private int physicalDefenseGain(final CharacterSheet with, final CharacterSheet without) {
        return defenseService.getTotalDefense(with, DefenseType.PHYSICAL)
                - defenseService.getTotalDefense(without, DefenseType.PHYSICAL);
    }

    // ---------- EXPLORAR_PONTOS_FRACOS ----------

    /** "Seus Acertos Críticos causam Metade da Gnose como dano adicional" — Gnose 5 adds 2, on top of the baseline Vantagem. */
    @Test
    void aCriticalAddsHalfTheGnose() {
        Weapon blade = blade("Adaga");
        CharacterSheet sheet = sheet(List.of(DuelistaFeat.EXPLORAR_PONTOS_FRACOS), 4, List.of(blade));

        assertEquals(new CriticalDamage(0, Skill.ADVANTAGE_BONUS + 2),
                new AtaqueCorpoACorpoInteraction().applyTo(sheet, null, new SkillRoll(SEVENTEEN), null, blade)
                        .getCriticalDamage());
    }

    @Test
    void anOrdinaryHitGainsNothing() {
        Weapon blade = blade("Adaga");
        CharacterSheet sheet = sheet(List.of(DuelistaFeat.EXPLORAR_PONTOS_FRACOS), 4, List.of(blade));

        assertNull(new AtaqueCorpoACorpoInteraction().applyTo(sheet, null, new SkillRoll(SIXTEEN), null, blade)
                .getCriticalDamage());
    }

    // ---------- DEFESA_COM_2_ARMAS ----------

    /** Two drawn blades, neither swung yet: +1 and the cumulative +2. */
    @Test
    void twoIdleWeaponsGrantTheFullLadder() {
        List<Weapon> pair = List.of(blade("Adaga"), blade("Sabre"));
        CharacterSheet with = sheet(List.of(DuelistaFeat.DEFESA_COM_2_ARMAS), 3, pair);
        CharacterSheet without = sheet(List.of(), 3, pair);

        assertEquals(3, physicalDefenseGain(with, without));
    }

    @Test
    void theLadderRisesWithTheMeleeGraduation() {
        List<Weapon> pair = List.of(blade("Adaga"), blade("Sabre"));

        assertEquals(4, physicalDefenseGain(sheet(List.of(DuelistaFeat.DEFESA_COM_2_ARMAS), 4, pair),
                sheet(List.of(), 4, pair)));
        assertEquals(5, physicalDefenseGain(sheet(List.of(DuelistaFeat.DEFESA_COM_2_ARMAS), 7, pair),
                sheet(List.of(), 7, pair)));
    }

    @Test
    void attackingWithOneWeaponLeavesOnlyTheWieldingBonus() {
        Weapon adaga = blade("Adaga");
        List<Weapon> pair = List.of(adaga, blade("Sabre"));
        CharacterSheet with = sheet(List.of(DuelistaFeat.DEFESA_COM_2_ARMAS), 3, pair);
        attackWith(with, adaga);

        assertEquals(1, physicalDefenseGain(with, sheet(List.of(), 3, pair)));
    }

    @Test
    void attackingWithBothWeaponsZeroesIt() {
        Weapon adaga = blade("Adaga");
        Weapon sabre = blade("Sabre");
        CharacterSheet with = sheet(List.of(DuelistaFeat.DEFESA_COM_2_ARMAS), 3, List.of(adaga, sabre));
        attackWith(with, adaga);
        attackWith(with, sabre);

        assertEquals(0, physicalDefenseGain(with, sheet(List.of(), 3, List.of(adaga, sabre))));
    }

    /** "a menos que você tenha 10 Graduações". */
    @Test
    void tenGraduationsKeepTheWieldingBonusAfterBothAttack() {
        Weapon adaga = blade("Adaga");
        Weapon sabre = blade("Sabre");
        CharacterSheet with = sheet(List.of(DuelistaFeat.DEFESA_COM_2_ARMAS), 10, List.of(adaga, sabre));
        attackWith(with, adaga);
        attackWith(with, sabre);

        assertEquals(3, physicalDefenseGain(with, sheet(List.of(), 10, List.of(adaga, sabre))));
    }

    @Test
    void oneDrawnWeaponGrantsNothing() {
        List<Weapon> one = List.of(blade("Adaga"));

        assertEquals(0, physicalDefenseGain(sheet(List.of(DuelistaFeat.DEFESA_COM_2_ARMAS), 3, one),
                sheet(List.of(), 3, one)));
    }

    @Test
    void theMagicDefenseIsUntouched() {
        List<Weapon> pair = List.of(blade("Adaga"), blade("Sabre"));

        assertEquals(defenseService.getTotalDefense(sheet(List.of(), 3, pair), DefenseType.MAGIC),
                defenseService.getTotalDefense(sheet(List.of(DuelistaFeat.DEFESA_COM_2_ARMAS), 3, pair),
                        DefenseType.MAGIC));
    }
}
