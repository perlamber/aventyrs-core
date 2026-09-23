package org.aventyrs.core.skill;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CriticalDamage;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.ArtilhariaFeat;
import org.aventyrs.core.feat.AssassinoFeat;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.CrossbowItem;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.item.OffensiveImprovement;
import org.aventyrs.core.item.OffensiveMasterpiece;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.ataqueadistancia.AtaqueADistanciaInteraction;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The Margem Crítica Menor an attack is judged against, and the Dano Crítico a critical hit then
 * adds to its own dano roll — the two halves added in 0.0.41, exercised end-to-end through a real
 * Interaction rather than against the hooks in isolation.
 */
class CriticalDamageResolutionTest {

    /** A total of 17: a critical at the default Margem Crítica Menor, with nothing held. */
    private static final List<Integer> SEVENTEEN = List.of(6, 6, 5);

    /** A total of 16: one número short of the default margin. */
    private static final List<Integer> SIXTEEN = List.of(6, 6, 4);

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    // ---------- the margin the roll is judged against ----------

    /** A weapon with no authored number is on {@link Weapon#DEFAULT_LESSER_CRITICAL_MARGIN}. */
    @Test
    void anAttackIsJudgedAgainstItsWeaponsOwnMargin() {
        Weapon plain = weapon(ItemCategory.HEAVY_BLADE);
        CharacterSheet sheet = sheetWielding(plain);

        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, meleeCritical(sheet, SEVENTEEN, plain));
        assertEquals(CriticalResult.NONE, meleeCritical(sheet, SIXTEEN, plain));
    }

    /**
     * A Besta de Mão's own column reads 16, so it crits on a 16 that no default-margin weapon
     * would — the authored number that {@code Weapon#getLesserCriticalMargin()} carried unread
     * until 0.0.41.
     */
    @Test
    void aWeaponPrintedSixteenCritsOneNumeroMoreReadily() {
        Weapon besta = CrossbowItem.BESTA_DE_MAO;
        CharacterSheet sheet = sheetWielding(besta);

        assertEquals(16, besta.getLesserCriticalMargin());
        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR,
                new AtaqueADistanciaInteraction()
                        .applyTo(sheet, null, new SkillRoll(SIXTEEN), null, besta).getCriticalResult());
    }

    /** Decisiva's Característica Adicional, "Margem Crítica Menor +1" — ungated, so a wielder who
     * meets none of its Requisitos still gets it. */
    @Test
    void decisivaWidensItsOwnWeaponsMargin() {
        AbstractWeapon decisiva = weapon(ItemCategory.HEAVY_BLADE);
        decisiva.setMasterpiece(OffensiveMasterpiece.DECISIVA);
        CharacterSheet sheet = sheetWielding(decisiva);

        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, meleeCritical(sheet, SIXTEEN, decisiva));
    }

    /** Decisiva's Favor, "Margem Crítica Maior +1" — gated on Destreza 5, so 17 becomes a Maior. */
    @Test
    void decisivasFavorWidensTheMaiorMarginForAWielderMeetingItsRequisitos() {
        AbstractWeapon decisiva = weapon(ItemCategory.HEAVY_BLADE);
        decisiva.setMasterpiece(OffensiveMasterpiece.DECISIVA);

        assertEquals(CriticalResult.ACERTO_CRITICO_MAIOR,
                meleeCritical(sheetWielding(Map.of(AttributeDomain.DEXTERITY, 5), decisiva), List.of(6, 6, 5), decisiva));
        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR,
                meleeCritical(sheetWielding(decisiva), List.of(6, 6, 5), decisiva));
    }

    /** Resistência Absoluta: "A Margem Crítica Menor/Maior dos ataques sofridos é reduzida em -1". */
    @Test
    void resistenciaAbsolutaPushesBothMarginsBackByOne() {
        CharacterSheet target = sheetWielding();
        target.applyEffect(new org.aventyrs.core.sheet.TemporaryBonus(
                org.aventyrs.core.modifier.ModifierType.ABSOLUTE_DAMAGE_REDUCTION, 2, 3));
        org.aventyrs.core.character.services.CriticalService service =
                new org.aventyrs.core.character.services.CriticalServiceImpl();

        assertEquals(1, service.getLesserCriticalResistance(target, null));
        assertEquals(1, service.getMajorCriticalResistance(target, null));
    }

    /** …and only its own: the other weapon in the same loadout keeps the default margin. */
    @Test
    void decisivaWidensNoOtherWeaponInTheSameLoadout() {
        AbstractWeapon decisiva = weapon(ItemCategory.HEAVY_BLADE);
        decisiva.setMasterpiece(OffensiveMasterpiece.DECISIVA);
        AbstractWeapon plain = weapon(ItemCategory.LIGHT_BLADE);
        CharacterSheet sheet = sheetWielding(decisiva, plain);

        assertEquals(CriticalResult.NONE, meleeCritical(sheet, SIXTEEN, plain));
    }

    // ---------- what the critical adds to the dano roll ----------

    /** The baseline: Vantagem em Danos, a flat +2 and no die. */
    @Test
    void aCriticalHitGrantsVantagemEmDanosAndNoDie() {
        Weapon plain = weapon(ItemCategory.HEAVY_BLADE);
        CharacterSheet sheet = sheetWielding(plain);

        assertEquals(new CriticalDamage(0, Skill.ADVANTAGE_BONUS),
                meleeResult(sheet, SEVENTEEN, plain).getCriticalDamage());
    }

    /** A roll that wasn't a critical reports none at all, rather than a pair of zeroes. */
    @Test
    void anOrdinaryHitReportsNoCriticalDamage() {
        Weapon plain = weapon(ItemCategory.HEAVY_BLADE);
        CharacterSheet sheet = sheetWielding(plain);

        assertNull(meleeResult(sheet, SIXTEEN, plain).getCriticalDamage());
    }

    /** Cruel's Favor "muda para +3" its own Característica Adicional rather than stacking with it,
     * and lands on top of the baseline Vantagem. */
    @Test
    void cruelAddsItsThreeToTheBaseline() {
        AbstractWeapon cruel = weapon(ItemCategory.HEAVY_BLADE);
        cruel.addImprovement(OffensiveImprovement.CRUEL);
        CharacterSheet sheet = sheetWielding(cruel);

        assertEquals(new CriticalDamage(0, Skill.ADVANTAGE_BONUS + 3),
                meleeResult(sheet, SEVENTEEN, cruel).getCriticalDamage());
    }

    /** Mitral's "+3" is its Favor, so unlike its margin clause it is gated on Destreza 3. */
    @Test
    void mitralAddsItsThreeOnlyOnceItsRequisitosAreMet() {
        AbstractWeapon mitral = weapon(ItemCategory.HEAVY_BLADE);
        mitral.setMasterpiece(OffensiveMasterpiece.MITRAL);

        CharacterSheet unqualified = sheetWielding(Map.of(), mitral);
        CharacterSheet deft = sheetWielding(Map.of(AttributeDomain.DEXTERITY, 3), mitral);

        // A 16 crits either way — Mitral's Margem Crítica Menor +1 is a Característica Adicional.
        assertEquals(new CriticalDamage(0, Skill.ADVANTAGE_BONUS),
                meleeResult(unqualified, SIXTEEN, mitral).getCriticalDamage());
        assertEquals(new CriticalDamage(0, Skill.ADVANTAGE_BONUS + 3),
                meleeResult(deft, SIXTEEN, mitral).getCriticalDamage());
    }

    /** A Cruel Mitral stacks both — they are different clauses on different layers. */
    @Test
    void anEnhancedWeaponSumsEveryClauseFittedToIt() {
        AbstractWeapon both = weapon(ItemCategory.HEAVY_BLADE);
        both.setMasterpiece(OffensiveMasterpiece.MITRAL);
        both.addImprovement(OffensiveImprovement.CRUEL);
        CharacterSheet sheet = sheetWielding(Map.of(AttributeDomain.DEXTERITY, 3), both);

        assertEquals(new CriticalDamage(0, Skill.ADVANTAGE_BONUS + 3 + 3),
                meleeResult(sheet, SIXTEEN, both).getCriticalDamage());
    }

    /** …and a Cruel weapon left in the pack sharpens nothing swung with the other hand. */
    @Test
    void anEnhancementOnAnotherWeaponAddsNothing() {
        AbstractWeapon cruel = weapon(ItemCategory.LIGHT_BLADE);
        cruel.addImprovement(OffensiveImprovement.CRUEL);
        AbstractWeapon plain = weapon(ItemCategory.HEAVY_BLADE);
        CharacterSheet sheet = sheetWielding(cruel, plain);

        assertEquals(new CriticalDamage(0, Skill.ADVANTAGE_BONUS),
                meleeResult(sheet, SEVENTEEN, plain).getCriticalDamage());
    }

    // ---------- Mira Mortal: the die an activated Talento buys ----------

    /**
     * "Sempre que tiver um Acerto Crítico usando o talento ‘Mira Impecável’ você causa +1d6 de dano
     * adicional" — so a 1d6 weapon rolls 2d6, plus the baseline +2, but only on a roll the attacker
     * actually spent Mira Impecável on.
     */
    @Test
    void miraMortalGrantsItsDieOnlyWhenMiraImpecavelWasActivated() {
        Weapon bow = rangedWeapon();
        CharacterSheet sheet = sheetWielding(Map.of(), List.of(ArtilhariaFeat.MIRA_IMPECAVEL,
                AssassinoFeat.ACERTO_CRITICO_APRIMORADO, ArtilhariaFeat.MIRA_MORTAL), bow);

        assertEquals(new CriticalDamage(1, Skill.ADVANTAGE_BONUS),
                rangedResult(sheet, activating(ArtilhariaFeat.MIRA_IMPECAVEL), bow).getCriticalDamage());
        assertEquals(new CriticalDamage(0, Skill.ADVANTAGE_BONUS),
                rangedResult(sheet, new SkillRoll(SEVENTEEN), bow).getCriticalDamage());
    }

    /** Holding Mira Impecável without Mira Mortal buys nothing, however the roll was made. */
    @Test
    void miraImpecavelAloneGrantsNoDie() {
        Weapon bow = rangedWeapon();
        CharacterSheet sheet = sheetWielding(Map.of(), List.of(ArtilhariaFeat.MIRA_IMPECAVEL), bow);

        assertEquals(new CriticalDamage(0, Skill.ADVANTAGE_BONUS),
                rangedResult(sheet, activating(ArtilhariaFeat.MIRA_IMPECAVEL), bow).getCriticalDamage());
    }

    // ---------- Violência Descomunal: the die that replaces the baseline ----------

    /** "Você não recebe Vantagem em Danos em seus Acertos Críticos, ao invés disso recebe Bônus de
     * +1d6" — the +2 goes, the die arrives. */
    @Test
    void violenciaDescomunalSwapsTheBaselineForADie() {
        Weapon plain = weapon(ItemCategory.HEAVY_BLADE);
        CharacterSheet sheet = sheetWielding(Map.of(), List.of(AssassinoFeat.VIOLENCIA_DESCOMUNAL), plain);

        assertEquals(new CriticalDamage(1, 0), meleeResult(sheet, SEVENTEEN, plain).getCriticalDamage());
    }

    /** A weapon clause still adds to what is left after the swap — the replacement drops the
     * baseline, not every other source. */
    @Test
    void violenciaDescomunalStillLetsAWeaponClauseAdd() {
        AbstractWeapon cruel = weapon(ItemCategory.HEAVY_BLADE);
        cruel.addImprovement(OffensiveImprovement.CRUEL);
        CharacterSheet sheet = sheetWielding(Map.of(), List.of(AssassinoFeat.VIOLENCIA_DESCOMUNAL), cruel);

        assertEquals(new CriticalDamage(1, 3), meleeResult(sheet, SEVENTEEN, cruel).getCriticalDamage());
    }

    // ---------- fixtures ----------

    private static SkillRoll activating(final Feat feat) {
        return new SkillRoll(SEVENTEEN, null, null, null, null, Set.of(feat));
    }

    private static AbstractWeapon weapon(final ItemCategory category) {
        return AbstractWeapon.builder()
                .name("Arma de teste")
                .category(category)
                .weightClass(ItemWeightClass.LIGHT)
                .damageBase(DamageBase.of(1, 0))
                .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
                .build();
    }

    private static AbstractWeapon rangedWeapon() {
        return AbstractWeapon.builder()
                .name("Arco de teste")
                .category(ItemCategory.BOW)
                .weightClass(ItemWeightClass.MEDIUM)
                .damageBase(DamageBase.of(1, 0))
                .skillType(SkillType.ATAQUE_A_DISTANCIA)
                .build();
    }

    private static CharacterSheet sheetWielding(final Weapon... equipment) {
        return sheetWielding(Map.of(), equipment);
    }

    private static CharacterSheet sheetWielding(final Map<AttributeDomain, Integer> attributes,
                                                final Weapon... equipment) {
        return sheetWielding(attributes, List.of(), equipment);
    }

    private static CharacterSheet sheetWielding(final Map<AttributeDomain, Integer> attributes,
                                                final List<Feat> feats, final Weapon... equipment) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.of(attributes))
                .equipment(new ArrayList<>(List.of(equipment)))
                .feats(new ArrayList<>(feats))
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private static CriticalResult meleeCritical(final CharacterSheet sheet, final List<Integer> dice,
                                                final Weapon weapon) {
        return meleeResult(sheet, dice, weapon).getCriticalResult();
    }

    private static InteractionResult meleeResult(final CharacterSheet sheet, final List<Integer> dice,
                                                 final Weapon weapon) {
        return new AtaqueCorpoACorpoInteraction().applyTo(sheet, null, new SkillRoll(dice), null, weapon);
    }

    private static InteractionResult rangedResult(final CharacterSheet sheet, final SkillRoll roll,
                                                  final Weapon weapon) {
        return new AtaqueADistanciaInteraction().applyTo(sheet, null, roll, null, weapon);
    }
}
