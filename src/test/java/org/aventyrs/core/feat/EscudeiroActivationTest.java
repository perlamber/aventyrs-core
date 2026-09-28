package org.aventyrs.core.feat;

import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.AttackRangeServiceImpl;
import org.aventyrs.core.character.services.DamageBaseServiceImpl;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.character.services.ReactionsServiceImpl;
import org.aventyrs.core.combat.AttackDelivery;
import org.aventyrs.core.combat.DeliveredAttack;
import org.aventyrs.core.combat.DeliveredAttackResult;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.effect.Rugido;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.item.ShieldAttack;
import org.aventyrs.core.item.ShieldItem;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.race.Aviano;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.scene.InitiativePosition;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.TerrainType;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.ActionOutcome;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillInteractionFactory;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpo;
import org.aventyrs.core.skill.esquivaeaparar.EsquivaEAparar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.aventyrs.core.util.TranslatableMessages.FEAT_ACTIVATION_NOT_PERMITTED;
import static org.aventyrs.core.util.TranslatableMessages.SHIELD_ATTACK_NOT_PERMITTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Escudeiro Talentos wired in core 0.0.70: the Ataque com Escudo and the Defesa it costs, the
 * Talentos built on it, Defesa Tartaruga's Turn-end grant, Início Defensivo's opening Rodadas, Mestre
 * Escudeiro's Reações, Criar Refúgio's negations, Asas Adamantinas' wings and Bastião de Vidro's trade.
 */
class EscudeiroActivationTest {

    /** 11 — an ordinary hit, short of any critical. */
    private static final List<Integer> ORDINARY = List.of(4, 4, 3);

    private final DefenseService defenseService = new DefenseServiceImpl();
    private final ActionPointsService actionPointsService = new ActionPointsServiceImpl();
    private final AttackDelivery attackDelivery = new AttackDelivery();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSkill trained(final org.aventyrs.core.skill.Skill skill, final int graduation) {
        return CharacterSkill.builder()
                .skill(skill)
                .graduation(SkillGraduation.builder().graduationValue(graduation).build())
                .build();
    }

    /** Força 3, 4 Graduações in both Ataque Corpo-a-Corpo and Esquiva e Aparar. */
    private static CharacterSheet squire(final List<Feat> feats, final Item... equipment) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.of(Map.of(AttributeDomain.STRENGTH, 3)))
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, trained(new AtaqueCorpoACorpo(), 4))
                .skill(SkillType.ESQUIVA_E_APARAR, trained(new EsquivaEAparar(), 4))
                .equipment(new ArrayList<>(List.of(equipment)))
                .drawnWeapons(new ArrayList<>())
                .feats(new ArrayList<>(feats))
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private static CharacterSheet foe() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>()).build(),
                new Player());
    }

    private static AbstractWeapon club() {
        return AbstractWeapon.builder().name("Clava").category(ItemCategory.CLUB).weightClass(ItemWeightClass.LIGHT)
                .damageBase(DamageBase.of(1, 0)).skillType(SkillType.ATAQUE_CORPO_A_CORPO).build();
    }

    private static SkillRoll roll(final List<Integer> dice, final Feat... activated) {
        return new SkillRoll(dice, null, null, null, null, Set.of(activated));
    }

    private static InteractionResult swing(final CombatantSheet attacker, final SkillRoll roll, final Weapon weapon) {
        return SkillInteractionFactory.create(SkillType.ATAQUE_CORPO_A_CORPO).applyTo(attacker, null, roll, null, weapon);
    }

    private static CombatantAction attack(final Weapon weapon, final boolean hit, final CriticalResult critical) {
        return new CombatantAction(SkillType.ATAQUE_CORPO_A_CORPO, AttributeDomain.STRENGTH, weapon,
                ActionCost.ofActionPoints(2), 0, new ActionOutcome(hit, 0, critical, null), null, Set.of());
    }

    private int physical(final CombatantSheet sheet) {
        return defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL);
    }

    // ---------- the Ataque com Escudo ----------

    @Test
    void aShieldSwingsAsAWeaponWithItsOwnColumns() {
        ShieldAttack medio = ShieldAttack.of(ShieldItem.ESCUDO_MEDIO);

        assertEquals(DamageBase.of(1, 2), medio.getDamageBase());
        assertEquals(DamageBase.of(1, 2), new DamageBaseServiceImpl()
                .getDamageBase(squire(List.of(EscudeiroFeat.ATACAR_COM_ESCUDOS), ShieldItem.ESCUDO_MEDIO), medio));
        assertEquals(17, medio.getLesserCriticalMargin());
        assertEquals(CriticalEffectType.ATORDOANTE, medio.getCriticalEffect());
        assertEquals(SkillType.ATAQUE_CORPO_A_CORPO, medio.getAttackSkillType());
        assertEquals(ShieldAttack.of(ShieldItem.ESCUDO_MEDIO), medio.against(DefenseType.MAGIC));
    }

    /** Table ruling: "Metade das Graduações em Esquiva e Aparar + Bônus Defensivos do Escudo" on top. */
    @Test
    void theShieldRollAddsHalfTheDodgeAndTheShieldsDefesa() {
        Weapon clava = club();
        CharacterSheet squire = squire(List.of(EscudeiroFeat.ATACAR_COM_ESCUDOS), ShieldItem.ESCUDO_MEDIO, clava);
        int plain = swing(squire, roll(ORDINARY), clava).getSkillRollBonus();

        ShieldAttack shield = ShieldAttack.of(ShieldItem.ESCUDO_MEDIO);
        // Half of 4 Graduações em Esquiva e Aparar, plus what the shield grants that Defesa — Favor included.
        assertEquals(plain + 2 + ShieldItem.ESCUDO_MEDIO.getEffectiveDefenseBonus(DefenseType.PHYSICAL, squire, null, null),
                swing(squire, roll(ORDINARY), shield).getSkillRollBonus());
        assertEquals(plain + 2 + ShieldItem.ESCUDO_MEDIO.getEffectiveDefenseBonus(DefenseType.MAGIC, squire, null, null),
                swing(squire, roll(ORDINARY), shield.against(DefenseType.MAGIC)).getSkillRollBonus());
    }

    @Test
    void noOneSwingsAShieldWithoutTheTalentoOrTheShield() {
        ShieldAttack shield = ShieldAttack.of(ShieldItem.ESCUDO_MEDIO);

        assertEquals(SHIELD_ATTACK_NOT_PERMITTED, assertThrows(IllegalOperationException.class,
                () -> swing(squire(List.of(), ShieldItem.ESCUDO_MEDIO), roll(ORDINARY), shield)).getMessage());
        assertEquals(SHIELD_ATTACK_NOT_PERMITTED, assertThrows(IllegalOperationException.class,
                () -> swing(squire(List.of(EscudeiroFeat.ATACAR_COM_ESCUDOS)), roll(ORDINARY), shield)).getMessage());
    }

    /**
     * What the shield grants once it has attacked, before any loss — read off a wielder whose Arte do
     * Escudo Atacante keeps it all, since attacking already costs the Escudo Médio its own Favor ("se
     * não realizou ação ofensiva nesta Rodada").
     */
    private int shieldAfterAttacking() {
        CharacterSheet arte = squire(List.of(EscudeiroFeat.ATACAR_COM_ESCUDOS,
                EscudeiroFeat.ARTE_DO_ESCUDO_ATACANTE), ShieldItem.ESCUDO_MEDIO);
        arte.recordAction(attack(ShieldAttack.of(ShieldItem.ESCUDO_MEDIO), true, CriticalResult.NONE));
        return physical(arte) - physical(squire(List.of()));
    }

    @Test
    void attackingOnceHalvesTheShieldsDefesaAndTwiceZeroesIt() {
        CharacterSheet with = squire(List.of(EscudeiroFeat.ATACAR_COM_ESCUDOS), ShieldItem.ESCUDO_MEDIO);
        CharacterSheet without = squire(List.of(EscudeiroFeat.ATACAR_COM_ESCUDOS));
        int shield = shieldAfterAttacking();
        ShieldAttack swing = ShieldAttack.of(ShieldItem.ESCUDO_MEDIO);

        with.recordAction(attack(swing, true, CriticalResult.NONE));
        assertEquals(shield - shield / 2, physical(with) - physical(without));
        with.recordAction(attack(swing, true, CriticalResult.NONE));
        assertEquals(0, physical(with) - physical(without));
    }

    @Test
    void ataqueMultiploKeepsHalfAndArteKeepsEverything() {
        ShieldAttack swing = ShieldAttack.of(ShieldItem.ESCUDO_MEDIO);
        CharacterSheet without = squire(List.of());
        CharacterSheet multiplo = squire(List.of(EscudeiroFeat.ATACAR_COM_ESCUDOS,
                EscudeiroFeat.ATAQUE_MULTIPLO_COM_ESCUDOS), ShieldItem.ESCUDO_MEDIO);
        CharacterSheet arte = squire(List.of(EscudeiroFeat.ATACAR_COM_ESCUDOS,
                EscudeiroFeat.ATAQUE_MULTIPLO_COM_ESCUDOS, EscudeiroFeat.ARTE_DO_ESCUDO_ATACANTE), ShieldItem.ESCUDO_MEDIO);
        int shield = shieldAfterAttacking();
        for (CharacterSheet sheet : List.of(multiplo, arte)) {
            sheet.recordAction(attack(swing, true, CriticalResult.NONE));
            sheet.recordAction(attack(swing, true, CriticalResult.NONE));
        }

        assertEquals(shield - shield / 2, physical(multiplo) - physical(without));
        assertEquals(shield, physical(arte) - physical(without));
    }

    // ---------- Espartano ----------

    @Test
    void espartanoFollowsAnAttackWithACheaperShieldSwingInDesvantagem() {
        Weapon clava = club();
        CharacterSheet squire = squire(List.of(EscudeiroFeat.ATACAR_COM_ESCUDOS, EscudeiroFeat.ESPARTANO),
                ShieldItem.ESCUDO_MEDIO, clava);
        ShieldAttack shield = ShieldAttack.of(ShieldItem.ESCUDO_MEDIO);

        assertEquals(FEAT_ACTIVATION_NOT_PERMITTED, assertThrows(IllegalOperationException.class,
                () -> swing(squire, roll(ORDINARY, EscudeiroFeat.ESPARTANO), shield)).getMessage());
        squire.recordAction(attack(clava, true, CriticalResult.NONE));
        int plain = swing(squire, roll(ORDINARY), shield).getSkillRollBonus();

        assertEquals(plain + Skill.DISADVANTAGE_MALUS,
                swing(squire, roll(ORDINARY, EscudeiroFeat.ESPARTANO), shield).getSkillRollBonus());
        int base = actionPointsService.getSkillRollCost(squire.getCharacter(), 0);
        assertEquals(ActionCost.ofActionPoints(Math.max(1, base - 1)), actionPointsService.getAttackCost(squire,
                SkillType.ATAQUE_CORPO_A_CORPO, shield, Set.of(EscudeiroFeat.ESPARTANO), 0));
    }

    // ---------- Arte do Escudo Atacante and Domínio ----------

    private static CharacterSheet master() {
        return squire(List.of(EscudeiroFeat.ATACAR_COM_ESCUDOS, EscudeiroFeat.ATAQUE_MULTIPLO_COM_ESCUDOS,
                EscudeiroFeat.ARTE_DO_ESCUDO_ATACANTE, EscudeiroFeat.DOMINIO_DA_ARTE_DO_ESCUDO_ATACANTE),
                ShieldItem.ESCUDO_MEDIO);
    }

    /** 15 — a critical only once the Margem Crítica Menor is widened by 2. */
    @Test
    void arteWidensTheMarginOnlyOnTheHoldersTurn() {
        CharacterSheet master = master();
        ShieldAttack shield = ShieldAttack.of(ShieldItem.ESCUDO_MEDIO);

        assertEquals(CriticalResult.NONE, swing(master, roll(List.of(6, 5, 4)), shield).getCriticalResult());
        master.startTurn(1);
        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, swing(master, roll(List.of(6, 5, 4)), shield).getCriticalResult());
    }

    @Test
    void arteGivesACriticalShieldHitTheRugido() {
        CharacterSheet master = master();
        master.startTurn(1);

        DeliveredAttackResult critical = attackDelivery.resolve(DeliveredAttack.builder()
                .attacker(master).defender(foe()).attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .attackSource(ShieldAttack.of(ShieldItem.ESCUDO_MEDIO)).defenseType(DefenseType.PHYSICAL)
                .defenseValue(5).attackRoll(roll(List.of(6, 6, 5))).build());

        assertTrue(critical.getHit());
        Interaction<CombatantSheet> stage = critical.getAttackResult().getNextInteraction();
        boolean rugido = false;
        while (stage != null) {
            rugido |= stage instanceof Rugido;
            stage = stage.getNextInteraction();
        }
        assertTrue(rugido);
    }

    /** The same Corrente rides Tigre e Serpente's melee criticals — the Talento whose TODO waited on it. */
    @Test
    void rugidoAlsoRidesTigreESerpentesCriticals() {
        CharacterSheet monk = squire(List.of(ArtesMarciaisFeat.DOMINAR_ARTE_MARCIAL_TIGRE_E_SERPENTE));

        assertEquals(1, ArtesMarciaisFeat.DOMINAR_ARTE_MARCIAL_TIGRE_E_SERPENTE.resolveCriticalHitEffectChains(
                monk.getCharacter(), SkillType.ATAQUE_CORPO_A_CORPO, club(), monk).stream()
                .filter(Rugido.class::isInstance).count());
        assertTrue(ArtesMarciaisFeat.DOMINAR_ARTE_MARCIAL_TIGRE_E_SERPENTE.resolveCriticalHitEffectChains(
                monk.getCharacter(), SkillType.ATAQUE_A_DISTANCIA, club(), monk).isEmpty());
    }

    @Test
    void rugidoPushesAndHurtsTwoMoreOnACritical() {
        CharacterSheet target = foe();
        InteractionResult pushed = new Rugido(true).applyTo(target);

        assertEquals(Rugido.PUSH_UD, pushed.getPushedBackUd());
        assertEquals(Rugido.CRITICAL_DAMAGE_BONUS, target.getDamageTaken());
    }

    @Test
    void dominioEasesTheTurnsFirstShieldSwingOnly() {
        CharacterSheet master = master();
        ShieldAttack shield = ShieldAttack.of(ShieldItem.ESCUDO_MEDIO);
        master.startTurn(1);
        int first = swing(master, roll(ORDINARY), shield).getDifficultyReduction();

        master.recordAction(attack(shield, false, CriticalResult.NONE));
        assertEquals(first - 1, swing(master, roll(ORDINARY), shield).getDifficultyReduction());
    }

    /** After the Empurrão Violento (a critical shield hit) the shield has Alcance Estendido this Turn. */
    @Test
    void anEmpurraoViolentoGivesTheShieldAlcanceEstendidoUntilTheTurnEnds() {
        CharacterSheet master = master();
        ShieldAttack shield = ShieldAttack.of(ShieldItem.ESCUDO_MEDIO);
        AttackRangeServiceImpl range = new AttackRangeServiceImpl();
        master.startTurn(1);
        int reach = range.getEffectiveRangeInUnidadesDeDistancia(master, shield, Set.of());

        master.recordAction(attack(shield, true, CriticalResult.ACERTO_CRITICO_MENOR));
        assertEquals(reach + 1, range.getEffectiveRangeInUnidadesDeDistancia(master, shield, Set.of()));

        CharacterSheet pushedAway = foe();
        SceneContext context = new SceneContext(List.of(), List.of(pushedAway), Map.of(pushedAway, Range.DISTANCIA_MUITO_CURTA),
                null, true, 1, false, pushedAway);
        int bonus = SkillInteractionFactory.create(SkillType.ATAQUE_CORPO_A_CORPO)
                .applyTo(master, context, roll(ORDINARY), pushedAway, shield).getSkillRollBonus();
        master.finishTurn();
        master.startTurn(2);
        assertEquals(reach, range.getEffectiveRangeInUnidadesDeDistancia(master, shield, Set.of()));
        assertEquals(bonus - 1, SkillInteractionFactory.create(SkillType.ATAQUE_CORPO_A_CORPO)
                .applyTo(master, context, roll(ORDINARY), pushedAway, shield).getSkillRollBonus());
    }

    // ---------- Defesa Tartaruga ----------

    @Test
    void standingStillBehindTheChosenShieldGrantsTwoUntilTheNextTurn() {
        CharacterSheet turtle = squire(List.of(EspecialistaEmEscudoFeat.of(ShieldItem.ESCUDO_MEDIO),
                EscudeiroFeat.DEFESA_TARTARUGA), ShieldItem.ESCUDO_MEDIO);
        turtle.startTurn(1);
        int before = physical(turtle);

        turtle.finishTurn();
        assertEquals(before + 2, physical(turtle));
        turtle.startTurn(2);
        assertEquals(before, physical(turtle));
        turtle.consumeMovementThisRound();
        turtle.finishTurn();
        assertEquals(before, physical(turtle));
    }

    // ---------- Início Defensivo ----------

    private static SceneContext opening(final InitiativePosition position, final int round) {
        return new SceneContext(List.of(), List.of(), Map.of(), TerrainType.URBAN, true, round, false, null, null,
                null, position);
    }

    @Test
    void inicioDefensivoGuardsWhoeverDoesNotActFirstInTheOpeningRodadas() {
        CharacterSheet guarded = squire(List.of(EscudeiroFeat.INICIO_DEFENSIVO));
        CharacterSheet plain = squire(List.of());
        java.util.function.BiFunction<InitiativePosition, Integer, Integer> gain = (position, round) ->
                defenseService.getTotalDefense(guarded, DefenseType.PHYSICAL, opening(position, round))
                        - defenseService.getTotalDefense(plain, DefenseType.PHYSICAL, opening(position, round));

        assertEquals(0, gain.apply(InitiativePosition.FIRST, 1));
        assertEquals(3, gain.apply(InitiativePosition.MIDDLE, 1));
        assertEquals(5, gain.apply(InitiativePosition.LAST, 2));
        assertEquals(0, gain.apply(InitiativePosition.LAST, 3));
    }

    @Test
    void inicioDefensivoIgnoresWhatLowersTheDefesas() {
        CharacterSheet guarded = squire(List.of(EscudeiroFeat.INICIO_DEFENSIVO));
        int clean = defenseService.getTotalDefense(guarded, DefenseType.PHYSICAL, opening(InitiativePosition.MIDDLE, 1));
        guarded.grantBlessing(new Blessing(ModifierType.DEFESAS, -2, 3, TargetScope.SELF, "malus"));

        assertEquals(clean, defenseService.getTotalDefense(guarded, DefenseType.PHYSICAL, opening(InitiativePosition.MIDDLE, 1)));
        assertEquals(clean - 2 - 3, defenseService.getTotalDefense(guarded, DefenseType.PHYSICAL,
                opening(InitiativePosition.MIDDLE, 3)));
    }

    // ---------- Mestre Escudeiro ----------

    @Test
    void mestreEscudeiroReactsThroughWhatPreventsReacoes() {
        ReactionsServiceImpl reactions = new ReactionsServiceImpl();
        CharacterSheet master = squire(List.of(EscudeiroFeat.MESTRE_ESCUDEIRO));
        CharacterSheet plain = squire(List.of());
        for (CharacterSheet sheet : List.of(master, plain)) {
            sheet.grantBlessing(new Blessing(ModifierType.REACTIONS, -100, 1, TargetScope.SELF, "ATORDOANTE"));
        }

        assertEquals(0, reactions.getTotalReactions(plain, 1));
        assertTrue(reactions.getTotalReactions(master, 1) > 0);
    }

    // ---------- Criar Refúgio ----------

    @Test
    void criarRefugioZeroesOneHitPerTituloPlusOneUntilATrueLongRest() {
        DamageServiceImpl damage = new DamageServiceImpl();
        CharacterSheet refuge = squire(List.of(EscudeiroFeat.CRIAR_REFUGIO), ShieldItem.ESCUDO_DE_CORPO);

        assertEquals(0, damage.calculateFinalDamage(refuge, null, DamageType.FISICO, null, 20, false));
        assertTrue(damage.calculateFinalDamage(refuge, null, DamageType.FISICO, null, 20, false) > 0);
        refuge.completeTrueRest(RestType.CURTO);
        assertTrue(damage.calculateFinalDamage(refuge, null, DamageType.FISICO, null, 20, false) > 0);
        refuge.completeTrueRest(RestType.LONGO);
        assertEquals(0, damage.calculateFinalDamage(refuge, null, DamageType.FISICO, null, 20, false));
    }

    @Test
    void criarRefugioHoldsOnlyWhileTheHolderHasNotAttackedThisRodada() {
        DamageServiceImpl damage = new DamageServiceImpl();
        CharacterSheet refuge = squire(List.of(EscudeiroFeat.CRIAR_REFUGIO), ShieldItem.ESCUDO_DE_CORPO);
        refuge.recordAction(attack(club(), true, CriticalResult.NONE));

        assertTrue(damage.calculateFinalDamage(refuge, null, DamageType.FISICO, null, 20, false) > 0);
        assertEquals(0, refuge.getRestScopedUses(EscudeiroFeat.CRIAR_REFUGIO.name()));
    }

    // ---------- Asas Adamantinas ----------

    @Test
    void asasAdamantinasNeedsWings() {
        CharacterSheet wingless = squire(List.of());
        Character aviano = CharacterFixture.blank(CharacterFixture.BLANK).race(new Aviano(Aviano.Subtipo.RAPINANTE))
                .feats(new ArrayList<>()).build();

        assertFalse(EscudeiroFeat.hasWings(wingless.getCharacter()));
        assertTrue(EscudeiroFeat.hasWings(aviano));
    }

    @Test
    void theWingsSwingAsAShieldOnlyOnTheGround() {
        CharacterSheet winged = squire(List.of(EscudeiroFeat.ATACAR_COM_ESCUDOS, EscudeiroFeat.ASAS_ADAMANTINAS));

        assertEquals(DamageBase.of(1, 2), ShieldAttack.wings().getDamageBase());
        swing(winged, roll(ORDINARY), ShieldAttack.wings());
        winged.setFlying(true);
        assertThrows(IllegalOperationException.class, () -> swing(winged, roll(ORDINARY), ShieldAttack.wings()));
    }

    @Test
    void theWingsMayBeTheEspecialistasChoiceAndCountWhileGrounded() {
        CharacterSheet winged = squire(List.of(EscudeiroFeat.ASAS_ADAMANTINAS,
                EspecialistaEmEscudoFeat.of(ShieldSpecialty.ASAS_ADAMANTINAS)));
        CharacterSheet wingsOnly = squire(List.of(EscudeiroFeat.ASAS_ADAMANTINAS));

        assertTrue(EscudeiroFeat.ESPECIALISTA_EM_ESCUDO.resolveRequiredChoices(winged.getCharacter()).get(0).options()
                .contains(ShieldSpecialty.ASAS_ADAMANTINAS));
        assertEquals(2, physical(winged) - physical(wingsOnly));
        winged.setFlying(true);
        wingsOnly.setFlying(true);
        assertEquals(0, physical(winged) - physical(wingsOnly));
    }

    @Test
    void everyCatalogShieldIsASpecialty() {
        for (ShieldItem shield : ShieldItem.values()) {
            assertEquals(shield, ShieldSpecialty.of(shield).getShield());
            assertEquals(shield.name(), ShieldSpecialty.of(shield).name());
        }
    }

    // ---------- Bastião de Vidro ----------

    @Test
    void bastiaoTradesItsReducoesForDefesa() {
        DamageServiceImpl damage = new DamageServiceImpl();
        CharacterSheet glass = squire(List.of(EscudeiroFeat.BASTIAO_DE_VIDRO));
        CharacterSheet plain = squire(List.of());
        for (CharacterSheet sheet : List.of(glass, plain)) {
            sheet.grantBlessing(new Blessing(ModifierType.DAMAGE_REDUCTION, 3, 3, TargetScope.SELF, "rd"));
            sheet.grantBlessing(new Blessing(ModifierType.DAMAGE_TAKEN_REDUCTION, 2, 3, TargetScope.SELF, "rds"));
        }

        assertEquals(10, damage.calculateFinalDamage(glass, null, DamageType.FISICO, null, 10, false));
        assertEquals(5, damage.calculateFinalDamage(plain, null, DamageType.FISICO, null, 10, false));
        // RDS 2 as Defesa, and +1 for having RD.
        assertEquals(3, physical(glass) - physical(plain));
    }
}
