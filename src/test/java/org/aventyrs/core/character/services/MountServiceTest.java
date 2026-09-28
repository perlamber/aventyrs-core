package org.aventyrs.core.character.services;

import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.combat.AttackDelivery;
import org.aventyrs.core.combat.AttackTarget;
import org.aventyrs.core.combat.DeliveredAttack;
import org.aventyrs.core.combat.DeliveredAttackResult;
import org.aventyrs.core.effect.DamageInteraction;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.CavalariaFeat;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.item.SpearItem;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.catalog.MagicTree;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.Riding;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.dirigirecavalgar.DirigirECavalgarCompetencyAbility;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.ALREADY_RIDING;
import static org.aventyrs.core.util.TranslatableMessages.MOUNT_REACTION_NOT_PERMITTED;
import static org.aventyrs.core.util.TranslatableMessages.NOT_RIDING;
import static org.aventyrs.core.util.TranslatableMessages.SKILL_USE_PREVENTED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Phase G: montaria and veículo, and the Cavalaria Talentos. */
class MountServiceTest {

    private final MountService mounts = new MountServiceImpl();
    private final ActionPointsService actionPoints = new ActionPointsServiceImpl();
    private final AttackTargetingService targeting = new AttackTargetingServiceImpl();
    private final DamageBaseService damageBase = new DamageBaseServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet rider(final List<SkillCompetencyAbility> abilities, final Feat... feats) {
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>(List.of(feats)));
        abilities.forEach(builder::skillCompetencyAbility);
        return CharacterSheet.of(builder.build(), new Player());
    }

    private static CharacterSheet rider(final Feat... feats) {
        return rider(List.of(), feats);
    }

    private static CharacterSheet steed() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
    }

    private static int bonus(final SkillType skill, final CharacterSheet sheet) {
        return skill.newInteraction().applyTo(sheet, null, new SkillRoll(List.of(3, 3, 3))).getSkillRollBonus();
    }

    // ---------- montar / desmontar ----------

    @Test
    void mountingCostsAPaAndCannotBeRepeated() {
        CharacterSheet rider = rider();

        assertEquals(MountService.DEFAULT_MOUNT_COST, mounts.mount(rider, Riding.montaria(steed())));
        assertTrue(rider.isRiding());
        IllegalOperationException again = assertThrows(IllegalOperationException.class,
                () -> mounts.mount(rider, Riding.veiculo()));
        assertEquals(ALREADY_RIDING, again.getMessage());

        assertEquals(MountService.DEFAULT_MOUNT_COST, mounts.dismount(rider, false));
        assertFalse(rider.isRiding());
        IllegalOperationException notRiding = assertThrows(IllegalOperationException.class,
                () -> mounts.dismount(rider, false));
        assertEquals(NOT_RIDING, notRiding.getMessage());
    }

    @Test
    void desmontarAsAReactionNeedsTheTalento() {
        CharacterSheet rider = rider();
        mounts.mount(rider, Riding.veiculo());

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> mounts.dismount(rider, true));
        assertEquals(MOUNT_REACTION_NOT_PERMITTED, refused.getMessage());
    }

    @Test
    void montarEDesmontarInstantaneoIsAnAcaoLivreOncePerTurn() {
        CharacterSheet rider = rider(CavalariaFeat.MONTAR_E_DESMONTAR_INSTANTANEO);

        assertEquals(ActionCost.FREE_ACTION, mounts.mount(rider, Riding.montaria(steed())));
        assertEquals(MountService.DEFAULT_MOUNT_COST, mounts.dismount(rider, false));

        rider.startTurn(2);
        mounts.mount(rider, Riding.montaria(steed()));
        CharacterSheet fresh = rider(CavalariaFeat.MONTAR_E_DESMONTAR_INSTANTANEO);
        fresh.startRiding(Riding.montaria(steed()));
        assertEquals(ActionCost.REACTION, mounts.dismount(fresh, true));
        assertEquals(1, fresh.getReactionsSpentThisRound());
    }

    // ---------- the riding restriction and Ginete ----------

    @Test
    void ridingWithoutGineteForbidsEveryPericiaButDirigirECavalgar() {
        CharacterSheet rider = rider();
        rider.startRiding(Riding.veiculo());

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> bonus(SkillType.ATTENTION, rider));
        assertEquals(SKILL_USE_PREVENTED, refused.getMessage());
        assertDoesNotThrow(() -> bonus(SkillType.DIRIGIR_E_CAVALGAR, rider));
    }

    @Test
    void gineteLiftsItForADesvantagemOnForcaAndDestrezaRolls() {
        CharacterSheet rider = rider(List.of(DirigirECavalgarCompetencyAbility.GINETE));
        int atletismo = bonus(SkillType.ATLETISMO, rider);
        int attention = bonus(SkillType.ATTENTION, rider);
        rider.startRiding(Riding.veiculo());

        assertEquals(atletismo + Skill.DISADVANTAGE_MALUS, bonus(SkillType.ATLETISMO, rider));
        assertEquals(attention, bonus(SkillType.ATTENTION, rider));
    }

    @Test
    void grandeGineteTurnsItIntoVantagem() {
        CharacterSheet rider = rider(List.of(DirigirECavalgarCompetencyAbility.GINETE), CavalariaFeat.GRANDE_GINETE);
        int atletismo = bonus(SkillType.ATLETISMO, rider);
        rider.startRiding(Riding.veiculo());

        assertEquals(atletismo + Skill.ADVANTAGE_BONUS, bonus(SkillType.ATLETISMO, rider));
    }

    // ---------- the mount's Pontos de Ação ----------

    @Test
    void aMovementSpendsTheWholeMountUnlessItIsAMontariaDeCombate() {
        CharacterSheet steed = steed();
        int steedPa = actionPoints.getMaxActionPoints(steed, 1);
        CharacterSheet plain = rider();
        plain.startRiding(Riding.montaria(steed));
        assertEquals(steedPa, mounts.getMountMovementActionPointCost(plain, 1));
        assertEquals(0, mounts.getMountActionAllowance(plain, 1));

        CharacterSheet trained = rider(CavalariaFeat.MONTARIA_DE_COMBATE);
        trained.getCharacter().grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        trained.startRiding(Riding.montaria(steed));
        assertEquals(Math.min(2, steedPa), mounts.getMountMovementActionPointCost(trained, 1));
        assertEquals(Math.max(0, Math.min(1, steedPa - 2)), mounts.getMountActionAllowance(trained, 1));
    }

    @Test
    void direcaoCaoticaPermitsInterruptingOnlyWhileRiding() {
        CharacterSheet rider = rider(CavalariaFeat.DIRECAO_CAOTICA);
        assertFalse(mounts.mayInterruptMountMovement(rider));

        rider.startRiding(Riding.veiculo());
        assertTrue(mounts.mayInterruptMountMovement(rider));
        assertFalse(mounts.mayInterruptMountMovement(rider()));
    }

    // ---------- Ataque em Arco ----------

    @Test
    void ataqueEmArcoAddsATargetWhileRidingAndHalvesEveryOne() {
        CharacterSheet rider = rider(CavalariaFeat.ATAQUE_EM_ARCO);
        assertEquals(1, targeting.getMaximumTargets(rider, SkillType.ATAQUE_CORPO_A_CORPO, SpearItem.ALABARDA_OU_NAGINATA));

        rider.startRiding(Riding.montaria(steed()));
        assertEquals(2, targeting.getMaximumTargets(rider, SkillType.ATAQUE_CORPO_A_CORPO, SpearItem.ALABARDA_OU_NAGINATA));
        assertTrue(targeting.halvesEveryTarget(rider, SkillType.ATAQUE_CORPO_A_CORPO, SpearItem.ALABARDA_OU_NAGINATA, 1));
        assertFalse(targeting.halvesEveryTarget(rider, SkillType.ATAQUE_CORPO_A_CORPO, SpearItem.ALABARDA_OU_NAGINATA, 0));

        Spell touch = MagicTree.values()[0].getSpells().get(0);
        assertEquals(1, targeting.getMaximumTargets(rider, SkillType.ATAQUE_CORPO_A_CORPO, touch));
    }

    @Test
    void attackDeliveryHalvesThePrimaryTooUnderAtaqueEmArco() {
        CharacterSheet rider = rider(List.of(DirigirECavalgarCompetencyAbility.GINETE), CavalariaFeat.ATAQUE_EM_ARCO);
        rider.startRiding(Riding.montaria(steed()));
        CharacterSheet defender = steed();

        DeliveredAttackResult result = new AttackDelivery().resolve(DeliveredAttack.builder()
                .attacker(rider)
                .defender(defender)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .attackSource(SpearItem.ALABARDA_OU_NAGINATA)
                .defenseType(DefenseType.PHYSICAL)
                .defenseValue(5)
                .attackRoll(new SkillRoll(List.of(3, 3, 3)))
                .additionalTarget(new AttackTarget(steed(), 5))
                .build());

        assertTrue(result.isEveryTargetHalved());
        DamageInteraction head = (DamageInteraction) result.getAttackResult().getNextInteraction();
        assertEquals(5, head.applyTo(defender, null, DamageType.PRIMORDIAL, null, 10, false).getResourceLossValue());
    }

    // ---------- the Alabarda ----------

    @Test
    void theAlabardaHitsForTwoD6WhileMounted() {
        CharacterSheet rider = rider();
        DamageBase afoot = damageBase.getDamageBase(rider, SpearItem.ALABARDA_OU_NAGINATA);
        assertEquals(damageBase.getDamageBase(rider.getCharacter(), SpearItem.ALABARDA_OU_NAGINATA), afoot);

        rider.startRiding(Riding.montaria(steed()));
        assertEquals(DamageBase.of(2, 0), damageBase.getDamageBase(rider, SpearItem.ALABARDA_OU_NAGINATA));
    }
}
