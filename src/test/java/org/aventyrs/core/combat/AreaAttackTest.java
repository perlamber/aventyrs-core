package org.aventyrs.core.combat;

import org.aventyrs.core.action.Manoeuvre;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.AttackTargetingService;
import org.aventyrs.core.character.services.AttackTargetingServiceImpl;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.effect.DamageInteraction;
import org.aventyrs.core.feat.DuelistaFeat;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.MobilidadeFeat;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.scene.AreaOfEffect;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.esquivaeaparar.EsquivaEAparar;
import org.aventyrs.core.skill.esquivaeaparar.EsquivaEApararCompetencyAbility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.aventyrs.core.util.TranslatableMessages.AREA_OF_EFFECT_NOT_GRANTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Area attacks — an attack a Talento turns into an Área de Efeito, delivered against everyone in
 * its footprint — Evasão's Defesa against one, and a retaliation actually dealt.
 */
class AreaAttackTest {

    /** 9, over a Defesa of 5 and short of any critical. */
    private static final List<Integer> HIT = List.of(3, 3, 3);

    private final AttackTargetingService targeting = new AttackTargetingServiceImpl();
    private final DamageService damageService = new DamageServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet sheet(final Feat... feats) {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>(List.of(feats))).build(), new Player());
    }

    private static SkillRoll roll(final Manoeuvre manoeuvre, final Feat... activated) {
        return new SkillRoll(HIT, null, null, null, manoeuvre, Set.of(activated));
    }

    // ---------- which attacks are area attacks ----------

    @Test
    void ataqueGiratorioIsAnExplosionOnlyWhenTheRollNamesIt() {
        Character duelist = sheet(DuelistaFeat.ATAQUE_GIRATORIO).getCharacter();

        assertEquals(Optional.of(AreaOfEffect.ATTACK_EXPLOSION), targeting.resolveAttackArea(duelist,
                SkillType.ATAQUE_CORPO_A_CORPO, null, roll(null, DuelistaFeat.ATAQUE_GIRATORIO)));
        assertEquals(Optional.empty(), targeting.resolveAttackArea(duelist, SkillType.ATAQUE_CORPO_A_CORPO, null,
                roll(null)));
    }

    @Test
    void investidaSelvagemIsAnExplosionOnAnInvestida() {
        Character charger = sheet(MobilidadeFeat.INVESTIDA_SELVAGEM).getCharacter();

        assertEquals(Optional.of(AreaOfEffect.ATTACK_EXPLOSION), targeting.resolveAttackArea(charger,
                SkillType.ATAQUE_CORPO_A_CORPO, null, roll(Manoeuvre.INVESTIDA)));
        assertEquals(Optional.empty(), targeting.resolveAttackArea(charger, SkillType.ATAQUE_CORPO_A_CORPO, null,
                roll(null)));
    }

    /** "Área de Efeito – Explosão" on an attack: the target and everyone adjacent (table ruling). */
    @Test
    void anAttacksExplosionIsAOneUdBurst() {
        assertEquals(AreaOfEffect.explosion(1), AreaOfEffect.ATTACK_EXPLOSION);
    }

    // ---------- delivering one ----------

    private DeliveredAttack.DeliveredAttackBuilder charge(final CombatantSheet attacker, final int bystanders) {
        DeliveredAttack.DeliveredAttackBuilder builder = DeliveredAttack.builder()
                .attacker(attacker)
                .defender(sheet())
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .defenseType(DefenseType.PHYSICAL)
                .defenseValue(5)
                .attackRoll(roll(Manoeuvre.INVESTIDA))
                .areaOfEffect(AreaOfEffect.ATTACK_EXPLOSION);
        for (int i = 0; i < bystanders; i++) {
            builder.additionalTarget(new AttackTarget(sheet(), 5));
        }
        return builder;
    }

    @Test
    void anAreaAttackReachesEveryoneInItsFootprintUncapped() {
        DeliveredAttackResult result = new AttackDelivery().resolve(charge(sheet(MobilidadeFeat.INVESTIDA_SELVAGEM), 4)
                .build());

        assertEquals(4, result.getAdditionalTargetResults().size());
        assertTrue(result.getAdditionalTargetResults().stream().allMatch(DeliveredAttackTargetResult::getHit));
    }

    /** An area hit is not an "alvo adicional": it takes the damage in full, not halved. */
    @Test
    void theOthersCaughtInTheAreaTakeFullDamage() {
        DeliveredAttackResult result = new AttackDelivery().resolve(charge(sheet(MobilidadeFeat.INVESTIDA_SELVAGEM), 1)
                .build());
        DeliveredAttackTargetResult bystander = result.getAdditionalTargetResults().get(0);
        DamageInteraction head = (DamageInteraction) bystander.getNextInteraction();

        assertEquals(10, head.applyTo(bystander.getDefender(), null, DamageType.PRIMORDIAL, null, 10, false)
                .getResourceLossValue());
    }

    @Test
    void anAreaTheAttackerIsNotGrantedIsRefused() {
        IllegalOperationException error = assertThrows(IllegalOperationException.class,
                () -> new AttackDelivery().resolve(charge(sheet(), 2).build()));

        assertEquals(AREA_OF_EFFECT_NOT_GRANTED, error.getMessage());
    }

    // ---------- Evasão ----------

    private static CharacterSheet evasive(final int graduation) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.ESQUIVA_E_APARAR, CharacterSkill.builder().skill(new EsquivaEAparar())
                        .graduation(SkillGraduation.builder().graduationValue(graduation).build()).build())
                .skillCompetencyAbilities(new ArrayList<>(List.of(EsquivaEApararCompetencyAbility.EVASAO)))
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private int defenseTotal(final CombatantSheet defender, final boolean area) {
        return new AttackReceiver().resolve(IncomingAttack.builder()
                .defender(defender)
                .attacker(sheet())
                .difficultyLevel(DifficultyLevel.MEDIUM)
                .defenseType(DefenseType.PHYSICAL)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .areaOfEffect(area)
                .defenseRoll(new SkillRoll(HIT))
                .build()).getDefenseTotal();
    }

    @Test
    void evasaoAddsThreeAgainstAnAreaAttackOnly() {
        CharacterSheet defender = evasive(3);

        assertEquals(defenseTotal(defender, false) + 3, defenseTotal(defender, true));
    }

    @Test
    void evasaoAddsFiveAtSevenGraduacoes() {
        CharacterSheet defender = evasive(7);

        assertEquals(defenseTotal(defender, false) + 5, defenseTotal(defender, true));
    }

    // ---------- a retaliation dealt ----------

    @Test
    void aRetaliationIsDealtAsATypedHitAndItsMaleficioRidesAlong() {
        CombatantSheet attacker = sheet();
        Retaliation thorns = new Retaliation(3, new DamageDescriptor(DamageType.FISICO_ELEMENTAL, ElementalType.NATURAL),
                ConditionType.ENVENENADO, 2);

        int dealt = thorns.dealTo(attacker, null, null, damageService);

        assertEquals(3, dealt);
        assertEquals(3, attacker.getDamageTaken());
        assertTrue(attacker.hasCondition(ConditionType.ENVENENADO, null));
    }

    @Test
    void aRetaliationFullyMitigatedCarriesNoMaleficio() {
        CombatantSheet attacker = sheet();
        Retaliation thorns = new Retaliation(0, new DamageDescriptor(DamageType.FISICO), ConditionType.ENVENENADO, 2);

        assertEquals(0, thorns.dealTo(attacker, null, null, damageService));
        assertTrue(!attacker.hasCondition(ConditionType.ENVENENADO, null));
    }
}
