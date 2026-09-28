package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.item.ArmorItem;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.esquivaeaparar.EsquivaEAparar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Sobrevivência constants a stale TODO had kept inert: VITALIDADE's low-PV RD, the
 * ESPECIALISTA_EM_ARMADURAS / DOMINAR_ARMADURAS pair, and PERMANECER_CONSCIENTE's disjunctive
 * Pré-requisito.
 */
class SobrevivenciaFeatTest {

    private final DamageService damageService = new DamageServiceImpl();
    private final DefenseService defenseService = new DefenseServiceImpl();
    private final HitPointsService hitPointsService = new HitPointsServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet sheet(final List<Feat> feats, final int dodgeGraduation, final Item... equipment) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.of(Map.of(AttributeDomain.VIGOR, 4)))
                .skill(SkillType.ESQUIVA_E_APARAR, CharacterSkill.builder()
                        .skill(new EsquivaEAparar())
                        .graduation(SkillGraduation.builder().graduationValue(dodgeGraduation).build())
                        .build())
                .equipment(new ArrayList<>(List.of(equipment)))
                .feats(new ArrayList<>(feats))
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private int rd(final CharacterSheet sheet) {
        return damageService.getTotalDamageReduction(sheet, DamageType.FISICO, null);
    }

    // ---------- VITALIDADE ----------

    @Test
    void vitalidadeGrantsNoRdAtFullHealth() {
        CharacterSheet holder = sheet(List.of(SobrevivenciaFeat.VITALIDADE), 0);

        assertEquals(rd(sheet(List.of(), 0)), rd(holder));
    }

    /** "Enquanto sua quantidade de PV atuais for igual ou inferior ao seu valor de Multiplicador de PV". */
    @Test
    void vitalidadeGrantsRdOnceCurrentPvFallToTheMultiplier() {
        CharacterSheet holder = sheet(List.of(SobrevivenciaFeat.VITALIDADE), 0);
        int before = rd(holder);
        Character character = holder.getCharacter();
        holder.applyDamage(hitPointsService.getCurrentHitPoints(character, holder)
                - hitPointsService.getLifeMultiplier(character, holder));

        assertEquals(before + DamageService.DEFAULT_DAMAGE_REDUCTION, rd(holder));
    }

    @Test
    void oneAboveTheMultiplierIsNotEnough() {
        CharacterSheet holder = sheet(List.of(SobrevivenciaFeat.VITALIDADE), 0);
        int before = rd(holder);
        Character character = holder.getCharacter();
        holder.applyDamage(hitPointsService.getCurrentHitPoints(character, holder)
                - hitPointsService.getLifeMultiplier(character, holder) - 1);

        assertEquals(before, rd(holder));
    }

    // ---------- ESPECIALISTA_EM_ARMADURAS / DOMINAR_ARMADURAS ----------

    private int gain(final DefenseType type, final List<Feat> feats, final int graduation, final Item... equipment) {
        return defenseService.getTotalDefense(sheet(feats, graduation, equipment), type)
                - defenseService.getTotalDefense(sheet(List.of(), graduation, equipment), type);
    }

    @Test
    void theBareConstantDeclaresTheWeightChoice() {
        List<FeatChoice<?>> choices = SobrevivenciaFeat.ESPECIALISTA_EM_ARMADURAS.resolveRequiredChoices(
                CharacterFixture.blank(CharacterFixture.BLANK).build());

        assertEquals(ItemWeightClass.class, choices.get(0).type());
        assertEquals(List.of(ItemWeightClass.values()), choices.get(0).options());
    }

    @Test
    void theChosenTierOfArmourGrantsTheLadder() {
        List<Feat> heavy = List.of(EspecialistaEmArmadurasFeat.of(ItemWeightClass.HEAVY));

        assertEquals(1, gain(DefenseType.PHYSICAL, heavy, 2, ArmorItem.ARMADURA_COMPLETA));
        assertEquals(1, gain(DefenseType.MAGIC, heavy, 2, ArmorItem.ARMADURA_COMPLETA));
        assertEquals(2, gain(DefenseType.PHYSICAL, heavy, 4, ArmorItem.ARMADURA_COMPLETA));
        assertEquals(3, gain(DefenseType.PHYSICAL, heavy, 7, ArmorItem.ARMADURA_COMPLETA));
        assertEquals(5, gain(DefenseType.PHYSICAL, heavy, 10, ArmorItem.ARMADURA_COMPLETA));
    }

    @Test
    void anotherTierOfArmourGrantsNothing() {
        assertEquals(0, gain(DefenseType.PHYSICAL, List.of(EspecialistaEmArmadurasFeat.of(ItemWeightClass.HEAVY)), 7,
                ArmorItem.ROBE_CERIMONIAL));
    }

    /** "se aplicam a qualquer armadura que vestir" — the ladder alone, no +2 outside the chosen tier. */
    @Test
    void dominarArmadurasWidensTheLadderToAnyArmour() {
        List<Feat> both = List.of(EspecialistaEmArmadurasFeat.of(ItemWeightClass.HEAVY),
                SobrevivenciaFeat.DOMINAR_ARMADURAS);

        assertEquals(2, gain(DefenseType.PHYSICAL, both, 4, ArmorItem.ROBE_CERIMONIAL));
    }

    /** "+2 em suas Defesas enquanto estiver vestindo uma armadura da categoria que você se especializou". */
    @Test
    void dominarArmadurasAddsTwoInTheChosenTier() {
        List<Feat> both = List.of(EspecialistaEmArmadurasFeat.of(ItemWeightClass.HEAVY),
                SobrevivenciaFeat.DOMINAR_ARMADURAS);

        assertEquals(4, gain(DefenseType.PHYSICAL, both, 4, ArmorItem.ARMADURA_COMPLETA));
    }

    @Test
    void noArmourGrantsNothingEvenWithDominar() {
        assertEquals(0, gain(DefenseType.PHYSICAL, List.of(EspecialistaEmArmadurasFeat.of(ItemWeightClass.HEAVY),
                SobrevivenciaFeat.DOMINAR_ARMADURAS), 10));
    }

    // ---------- PERMANECER_CONSCIENTE ----------

    private static Character candidate(final AttributeDomain domain) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.of(Map.of(domain, 5)))
                .feats(new ArrayList<>(List.of(SobrevivenciaFeat.DURO_DE_MATAR)))
                .build();
        character.grantTitle(new org.aventyrs.core.title.santo.Santo(List.of(), List.of()),
                org.aventyrs.core.character.TitleSlot.PRIMARY);
        return character;
    }

    @Test
    void vigorOrInstintoFiveBothQualify() {
        assertTrue(SobrevivenciaFeat.PERMANECER_CONSCIENTE.isEligible(candidate(AttributeDomain.VIGOR)));
        assertTrue(SobrevivenciaFeat.PERMANECER_CONSCIENTE.isEligible(candidate(AttributeDomain.INSTINCT)));
        assertFalse(SobrevivenciaFeat.PERMANECER_CONSCIENTE.isEligible(candidate(AttributeDomain.GNOSE)));
    }
}
