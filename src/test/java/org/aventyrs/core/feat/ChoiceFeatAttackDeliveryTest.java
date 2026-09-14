package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.FeatService;
import org.aventyrs.core.character.services.FeatServiceImpl;
import org.aventyrs.core.combat.AttackDelivery;
import org.aventyrs.core.combat.DeliveredAttack;
import org.aventyrs.core.combat.DeliveredAttackResult;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.AttackMethod;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.monster.GenericMonster;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.race.NascidoDoDragao;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.ataqueadistancia.AtaqueADistancia;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The attack-affecting acquisition choices, resolved through {@link AttackDelivery} — a real
 * attack against a real foe, made by a character who <b>legally acquired</b> the Talento and named
 * a choice while doing so.
 *
 * <p><b>Why this layer specifically.</b> Each of these Talentos scopes its effect to <em>what the
 * attack was delivered with</em>, which reaches the roll as {@code DeliveredAttack#attackSource}.
 * Their own unit tests assert the choice is recorded and {@code GeneralFeatEffectIntegrationTest}
 * takes several as far as the {@code Interaction} — but neither proves the {@code AttackSource}
 * survives the trip through {@code AttackDelivery} to the hook. A Talento whose choice was silently
 * dropped there would pass every other test in the repo and still do nothing in play.
 *
 * <p>Each case is written as a <b>discrimination</b>: the same roll, the same foe, the same
 * everything except which weapon the attack was made with — so what is asserted is that the choice
 * decided the outcome, rather than that some bonus happened to exist.
 */
class ChoiceFeatAttackDeliveryTest {

    private final AttackDelivery attackDelivery = new AttackDelivery();
    private final FeatService featService = new FeatServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Weapon weapon(final ItemCategory category) {
        return AbstractWeapon.builder().name(category.name()).category(category)
                .damageBase(DamageBase.of(1, 1)).skillType(SkillType.ATAQUE_CORPO_A_CORPO).build();
    }

    private static final Weapon LIGHT_BLADE = weapon(ItemCategory.LIGHT_BLADE);
    private static final Weapon HEAVY_BLADE = weapon(ItemCategory.HEAVY_BLADE);

    private static Character.CharacterBuilder character() {
        return CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>());
    }

    /** Ataque Corpo-a-Corpo 2 — what {@code ESPECIALISTA_EM_ARMA} asks for. */
    private static Character duelist() {
        return character()
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(3).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, CharacterSkill.builder()
                        .skill(new AtaqueCorpoACorpo())
                        .graduation(SkillGraduation.builder().graduationValue(2).build())
                        .build())
                .build();
    }

    private static CharacterSheet fundedSheet(final Character character) {
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.accumulateExperience(BigDecimal.valueOf(100));
        return sheet;
    }

    /** The real acquisition path — the Pré-requisito checked, the XP paid, the choice named. */
    private CharacterSheet acquire(final Character character, final Feat feat) throws IllegalOperationException {
        CharacterSheet sheet = fundedSheet(character);
        featService.grantFeat(character, sheet, feat);
        return sheet;
    }

    /** One attack, resolved end to end against a foe. */
    private DeliveredAttackResult deliver(final CharacterSheet attacker, final Weapon with, final SkillRoll roll) {
        MonsterSheet foe = GenericMonster.CAPANGA.spawn(new Player());
        return attackDelivery.resolve(DeliveredAttack.from(foe, DefenseType.PHYSICAL)
                .attacker(attacker)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .attackSource(with)
                .attackRoll(roll)
                .build());
    }

    // ---------- EspecialistaEmArmaFeat: Vantagem with the chosen weapon type ----------

    /**
     * "Receba vantagem nas rolagens de ataque com a arma escolhida." Delivered as an attack, the
     * Vantagem has to move the attack total — which is what the Defesa is compared against.
     */
    @Test
    void especialistaEmArmaLiftsTheAttackTotalOnlyWithTheChosenWeapon() throws IllegalOperationException {
        Character character = duelist();
        CharacterSheet attacker = acquire(character, EspecialistaEmArmaFeat.of(AttackMethod.LIGHT_BLADE));
        SkillRoll roll = new SkillRoll(List.of(3, 3, 2));

        int chosen = deliver(attacker, LIGHT_BLADE, roll).getAttackTotal();
        int other = deliver(attacker, HEAVY_BLADE, roll).getAttackTotal();

        assertEquals(other + 2, chosen, "Vantagem reaches AttackDelivery only for the chosen type");
    }

    /** And the discrimination belongs to the choice, not to the weapon: pick the other, get the mirror. */
    @Test
    void theOppositeChoiceGivesTheOppositeResult() throws IllegalOperationException {
        Character character = duelist();
        CharacterSheet attacker = acquire(character, EspecialistaEmArmaFeat.of(AttackMethod.HEAVY_BLADE));
        SkillRoll roll = new SkillRoll(List.of(3, 3, 2));

        int heavy = deliver(attacker, HEAVY_BLADE, roll).getAttackTotal();
        int light = deliver(attacker, LIGHT_BLADE, roll).getAttackTotal();

        assertEquals(light + 2, heavy);
    }

    /** A higher total is the point of the Vantagem: it is what turns a miss into a hit. */
    @Test
    void theChosenWeaponsVantagemCanBeWhatLandsTheAttack() throws IllegalOperationException {
        Character character = duelist();
        CharacterSheet attacker = acquire(character, EspecialistaEmArmaFeat.of(AttackMethod.LIGHT_BLADE));
        SkillRoll roll = new SkillRoll(List.of(3, 3, 2));

        DeliveredAttackResult chosen = deliver(attacker, LIGHT_BLADE, roll);
        DeliveredAttackResult other = deliver(attacker, HEAVY_BLADE, roll);

        assertEquals(chosen.getRequiredTotal(), other.getRequiredTotal(), "same foe, same Defesa");
        assertEquals(other.getMargin() + 2, chosen.getMargin(),
                "the whole Vantagem lands on the margin against the Defesa");
    }

    // ---------- AcertoCriticoAprimoradoFeat: Margem Crítica with the chosen weapon type ----------

    /**
     * "Sua Margem Crítica Menor com o tipo de arma escolhida … é aumentada em +1." 5+5+1 is not a
     * crit at the baseline margin; widened by one, the pair of 5s qualifies. Asserted on the
     * delivered attack's own {@code CriticalResult}, which is what drives the Efeito Crítico chain.
     */
    @Test
    void acertoCriticoAprimoradoWidensTheMarginOnlyForTheChosenWeapon() throws IllegalOperationException {
        Character character = duelist();
        CharacterSheet attacker = acquire(character, AcertoCriticoAprimoradoFeat.of(AttackMethod.LIGHT_BLADE));
        SkillRoll fives = new SkillRoll(List.of(5, 5, 1));

        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR,
                deliver(attacker, LIGHT_BLADE, fives).getCriticalResult());
        assertEquals(CriticalResult.NONE,
                deliver(attacker, HEAVY_BLADE, fives).getCriticalResult());
    }

    /** Without the Talento at all, neither weapon crits on that roll — the margin is the Talento's. */
    @Test
    void withoutTheTalentoNeitherWeaponCrits() {
        CharacterSheet attacker = fundedSheet(duelist());
        SkillRoll fives = new SkillRoll(List.of(5, 5, 1));

        assertEquals(CriticalResult.NONE, deliver(attacker, LIGHT_BLADE, fives).getCriticalResult());
        assertEquals(CriticalResult.NONE, deliver(attacker, HEAVY_BLADE, fives).getCriticalResult());
    }

    // ---------- AtiradorPerfeitoFeat: the chosen method, at the required distance ----------

    /**
     * "Vantagem … contra alvos a Distância Média ou maior" with the chosen {@code AttackMethod}.
     * Both halves of the condition have to survive the delivery — the {@code AttackSource} and the
     * {@code SceneContext} that says how far away the defender is.
     */
    @Test
    void atiradorPerfeitoNeedsBothTheChosenMethodAndTheDistanceOnADeliveredAttack()
            throws IllegalOperationException {
        Character character = character()
                .skill(SkillType.ATAQUE_A_DISTANCIA, CharacterSkill.builder()
                        .skill(new AtaqueADistancia())
                        .graduation(SkillGraduation.builder().graduationValue(2).build())
                        .build())
                .build();
        CharacterSheet attacker = acquire(character, AtiradorPerfeitoFeat.of(AttackMethod.BOW));
        Weapon bow = weapon(ItemCategory.BOW);
        Weapon crossbow = weapon(ItemCategory.CROSSBOW);
        SkillRoll roll = new SkillRoll(List.of(3, 3, 2));

        assertEquals(2, shoot(attacker, bow, roll, Range.DISTANCIA_MEDIA).getAttackTotal()
                        - shoot(attacker, bow, roll, Range.DISTANCIA_CURTA).getAttackTotal(),
                "the chosen method, far enough away");
        assertEquals(0, shoot(attacker, crossbow, roll, Range.DISTANCIA_MEDIA).getAttackTotal()
                        - shoot(attacker, crossbow, roll, Range.DISTANCIA_CURTA).getAttackTotal(),
                "the distance alone buys nothing — the choice was spent on a bow");
    }

    /** A ranged delivery that tells the roll how far off the defender stands. */
    private DeliveredAttackResult shoot(final CharacterSheet attacker, final Weapon with,
                                        final SkillRoll roll, final Range distance) {
        MonsterSheet foe = GenericMonster.CAPANGA.spawn(new Player());
        SceneContext context = new SceneContext(List.of(), List.of(foe), Map.of(foe, distance),
                null, true, 1, false, foe);
        return attackDelivery.resolve(DeliveredAttack.from(foe, DefenseType.PHYSICAL)
                .attacker(attacker)
                .attackSkill(SkillType.ATAQUE_A_DISTANCIA)
                .attackSource(with)
                .attackRoll(roll)
                .sceneContext(context)
                .build());
    }

    // ---------- FocoEmPericiaFeat: a chosen Perícia that happens to be an attack one ----------

    /**
     * The chosen Perícia is any Perícia — including a Perícia de Ataque, which is the case that
     * has to reach a delivered attack rather than only a bare roll. Chosen elsewhere, the same
     * attack gets nothing.
     */
    @Test
    void focoEmPericiaReachesADeliveredAttackWhenAnAttackPericiaWasChosen()
            throws IllegalOperationException {
        Character focused = duelist();
        CharacterSheet withMelee = acquire(focused, FocoEmPericiaFeat.of(SkillType.ATAQUE_CORPO_A_CORPO));
        Character elsewhere = duelist();
        CharacterSheet withAtletismo = acquire(elsewhere, FocoEmPericiaFeat.of(SkillType.ATLETISMO));
        SkillRoll roll = new SkillRoll(List.of(3, 3, 2));

        assertEquals(deliver(withAtletismo, LIGHT_BLADE, roll).getAttackTotal() + 2,
                deliver(withMelee, LIGHT_BLADE, roll).getAttackTotal());
    }

    // ---------- ArmamentoDraconicoFeat: the chosen Armas Naturais reach a delivered attack ----------

    /**
     * The two Armas Naturais a Nascido do Dragão picked are real weapons to attack with — and the
     * two they did <em>not</em> pick are not theirs. Delivered rather than read off {@code
     * getNaturalWeapons()}, so a chosen weapon is exercised as an actual {@code AttackSource}.
     */
    @Test
    void theChosenArmasNaturaisAreWhatTheHolderFightsWith() throws IllegalOperationException {
        Character character = character()
                .race(new NascidoDoDragao(new Human(), ElementalType.FOGO))
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, CharacterSkill.builder()
                        .skill(new AtaqueCorpoACorpo())
                        .graduation(SkillGraduation.builder().graduationValue(2).build())
                        .build())
                .build();
        CharacterSheet attacker = acquire(character, ArmamentoDraconicoFeat.of(
                NaturalWeapon.GARRAS_AFIADAS, NaturalWeapon.PRESAS_LONGAS));

        assertTrue(character.getNaturalWeapons().contains(NaturalWeapon.GARRAS_AFIADAS));
        assertFalse(character.getNaturalWeapons().contains(NaturalWeapon.CAUDA_CHICOTE),
                "not picked, not theirs");

        DeliveredAttackResult result = deliver(attacker, NaturalWeapon.GARRAS_AFIADAS,
                new SkillRoll(List.of(4, 4, 3)));

        assertTrue(character.treatsAsNaturalWeapon(NaturalWeapon.GARRAS_AFIADAS));
        assertEquals(NaturalWeapon.GARRAS_AFIADAS, result.getRecordedAction().attackSource(),
                "the chosen Arma Natural is what the delivered attack was made with");
    }
}
