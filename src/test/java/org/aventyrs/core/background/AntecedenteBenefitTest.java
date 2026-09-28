package org.aventyrs.core.background;

import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.MovementMode;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DamageBaseServiceImpl;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.character.services.InitiativeServiceImpl;
import org.aventyrs.core.character.services.MagicPointsServiceImpl;
import org.aventyrs.core.character.services.MovementServiceImpl;
import org.aventyrs.core.character.services.SkillGraduationServiceImpl;
import org.aventyrs.core.feat.AntecedenteFeat;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.SpellFamiliarity;
import org.aventyrs.core.magic.catalog.MagicTree;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.monster.SampleMonster;
import org.aventyrs.core.race.CreatureType;
import org.aventyrs.core.rest.RestServiceImpl;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.TerrainType;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.DamageReceipt;
import org.aventyrs.core.sheet.EgoPointType;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.ResourceType;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.conhecimentos.ConhecimentosSpecialization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AntecedenteBenefitTest {

    private static final Weapon ESPADA = AbstractWeapon.builder()
            .name("Espada Longa").category(ItemCategory.HEAVY_BLADE)
            .damageBase(DamageBase.of(2, 0)).skillType(SkillType.ATAQUE_CORPO_A_CORPO).build();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character.CharacterBuilder base(final SkillType... trained) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .skills(Arrays.stream(trained).collect(Collectors.toMap(skill -> skill, skill -> CharacterSkill.builder()
                        .skill(skill.newSkillInstance())
                        .graduation(SkillGraduation.builder().graduationValue(1).build()).build())));
    }

    /** Holding background as stored — builder-bypassed, so no creation grants, just the Benefício. */
    private static Character holding(final Background background, final List<SkillType> graduations,
                                     final List<Object> choices, final SkillType... trained) {
        return base(trained).background(new AcquiredBackground(background, graduations, List.<SkillTrait>of(), choices)).build();
    }

    private static Character holding(final Background background, final SkillType... trained) {
        return holding(background, List.of(), List.of(), trained);
    }

    private static CharacterSheet sheet(final Character character) {
        return CharacterSheet.of(character, new Player());
    }

    private static SkillRoll activating(final int[] dice, final Feat feat) {
        return new SkillRoll(Arrays.stream(dice).boxed().toList(), null, null, null, null, Set.of(feat));
    }

    private static void record(final CharacterSheet sheet, final SkillType skill, final Feat feat) {
        sheet.recordAction(new CombatantAction(skill, null, null, ActionCost.ofActionPoints(2), 1, null, null, Set.of(feat)));
    }

    // ---- flat stat Benefícios -------------------------------------------------------------------

    @Test
    void ofiAddsOneToBothDefesas() {
        DefenseServiceImpl defense = new DefenseServiceImpl();
        Character plain = base().build();
        Character ofi = holding(OriginBackground.OFI);

        assertEquals(defense.getTotalDefense(plain, DefenseType.PHYSICAL) + 1, defense.getTotalDefense(ofi, DefenseType.PHYSICAL));
        assertEquals(defense.getTotalDefense(plain, DefenseType.MAGIC) + 1, defense.getTotalDefense(ofi, DefenseType.MAGIC));
    }

    @Test
    void academicoAventyrAddsIniciativaAndTwoPaThenOne() {
        InitiativeServiceImpl initiative = new InitiativeServiceImpl();
        assertEquals(initiative.getTotalInitiative(base().build()) + 1,
                initiative.getTotalInitiative(holding(CareerBackground.ACADEMICO_AVENTYR)));

        var blessings = AntecedenteFeat.ACADEMICO_AVENTYR.resolveCombatStartBlessings(base().build());
        assertEquals(2, blessings.stream().filter(blessing -> blessing.getModifierType() == ModifierType.ACTION_POINTS
                && blessing.getRounds() >= 1).mapToInt(blessing -> blessing.getValue()).sum());
        assertEquals(1, blessings.stream().filter(blessing -> blessing.getRounds() >= 2)
                .mapToInt(blessing -> blessing.getValue()).sum());
    }

    @Test
    void theMultiplierBenefitsRaiseTheirMultiplier() {
        assertEquals(new DeterminationPointsServiceImpl().getDeterminationMultiplier(base().build()) + 1,
                new DeterminationPointsServiceImpl().getDeterminationMultiplier(holding(OriginBackground.SEM_PATRIA_RECONHECIDA)));
        assertEquals(new MagicPointsServiceImpl().getManaMultiplier(base().build()) + 1,
                new MagicPointsServiceImpl().getManaMultiplier(holding(CareerBackground.RITUALISTA)));
        assertEquals(new HitPointsServiceImpl().getLifeMultiplier(base().build()) + 1,
                new HitPointsServiceImpl().getLifeMultiplier(holding(CareerBackground.SELVAGEM)));
    }

    @Test
    void nortenhoRaisesAWeaponsDanoBase() {
        DamageBaseServiceImpl damageBase = new DamageBaseServiceImpl();
        assertEquals(DamageBase.of(2, 0), damageBase.getDamageBase(base().build(), ESPADA));
        assertNotEquals(DamageBase.of(2, 0), damageBase.getDamageBase(holding(OriginBackground.NORTENHO), ESPADA));
    }

    // ---- movement ---------------------------------------------------------------------------------

    @Test
    void marinheiroSwimsAndDeciembranoSwimsFurther() {
        MovementServiceImpl movement = new MovementServiceImpl();
        assertFalse(movement.hasMovementMode(base().build(), MovementMode.SWIM));

        Character sailor = holding(CareerBackground.MARINHEIRO);
        assertTrue(movement.hasMovementMode(sailor, MovementMode.SWIM));

        Character islander = base().background(new AcquiredBackground(CareerBackground.MARINHEIRO, null, null, null))
                .background(new AcquiredBackground(OriginBackground.DECIEMBRANO, null, null, null)).build();
        assertEquals(movement.getMovementBase(sheet(sailor), MovementMode.SWIM) + 2,
                movement.getMovementBase(sheet(islander), MovementMode.SWIM));
    }

    @Test
    void atletaMovesTwoMoreAndTwiceThatWhenFirstToAct() {
        MovementServiceImpl movement = new MovementServiceImpl();
        assertEquals(movement.getMovementBase(base().build()) + 2, movement.getMovementBase(holding(CareerBackground.ATLETA)));

        var initiativeWin = AntecedenteFeat.ATLETA.resolveInitiativeBlessings();
        assertEquals(ModifierType.MOVEMENT, initiativeWin.get(0).getModifierType());
        assertEquals(2, initiativeWin.get(0).getValue());
        assertEquals(1, initiativeWin.get(0).getRounds());
    }

    // ---- Graduação cost -----------------------------------------------------------------------------

    @Test
    void batedorTakesHalfAnExpOffAtencaoUpToTheFifth() {
        SkillGraduationServiceImpl graduation = new SkillGraduationServiceImpl();
        Character plain = base(SkillType.ATTENTION).build();
        Character scout = holding(CareerBackground.BATEDOR, SkillType.ATTENTION);

        assertEquals(0, new BigDecimal("1.0").compareTo(graduation.getUpgradeCost(plain, SkillType.ATTENTION)));
        assertEquals(0, new BigDecimal("0.5").compareTo(graduation.getUpgradeCost(scout, SkillType.ATTENTION)));
    }

    @Test
    void noDiscountPastTheFifthAndAGraduacaoMayBecomeFree() {
        assertEquals(BigDecimal.ZERO, AntecedenteFeat.BATEDOR.resolveGraduationCostReduction(null, SkillType.ATTENTION, 6));
        assertEquals(BigDecimal.ZERO, AntecedenteFeat.BATEDOR.resolveGraduationCostReduction(null, SkillType.FURTIVIDADE, 2));

        Character traveller = base().background(new AcquiredBackground(OriginBackground.NATUREZA_LONGINQUA,
                List.of(SkillType.PROFISSAO), null, null)).build();
        Character withGrad0 = traveller.toBuilder().clearSkills().skills(Map.of(SkillType.PROFISSAO, CharacterSkill.builder()
                .skill(SkillType.PROFISSAO.newSkillInstance()).build())).build();
        assertEquals(0, BigDecimal.ZERO.compareTo(new SkillGraduationServiceImpl().getUpgradeCost(withGrad0, SkillType.PROFISSAO)),
                "the 1st Graduação costs 0.5, discounted to 0 — free is valid");
    }

    @Test
    void escudeiroDiscountsThePericiaItsBenefitNamed() {
        Character squire = holding(CareerBackground.ESCUDEIRO, List.of(), List.of(SkillType.ESQUIVA_E_APARAR));
        assertEquals(new BigDecimal("0.5"), AntecedenteFeat.ESCUDEIRO.resolveGraduationCostReduction(squire, SkillType.ESQUIVA_E_APARAR, 3));
        assertEquals(BigDecimal.ZERO, AntecedenteFeat.ESCUDEIRO.resolveGraduationCostReduction(squire, SkillType.ATAQUE_A_DISTANCIA, 3));
    }

    // ---- Magias --------------------------------------------------------------------------------------

    @Test
    void estudiosoArcanoCastsTheSementeAndBrotoWithoutLearningThem() {
        MagicTree tree = MagicTree.values()[0];
        Character scholar = holding(CareerBackground.ESTUDIOSO_ARCANO, List.of(SkillType.CONHECIMENTOS), List.of(tree));

        assertTrue(scholar.getSpells().isEmpty(), "the Árvore is not opened");
        assertFalse(scholar.getCastableSpells().isEmpty());
        assertTrue(scholar.getCastableSpells().stream().allMatch(spell -> spell.getTree() == tree
                && (spell.getBranchLevel() == BranchLevel.SEMENTE || spell.getBranchLevel() == BranchLevel.BROTO)));
        assertTrue(SpellFamiliarity.canCast(scholar, scholar.getCastableSpells().get(0)));
    }

    // ---- Descanso -------------------------------------------------------------------------------------

    @Test
    void jullyanoRecoversOneMoreOfEachOnlyOnADescansoVerdadeiro() {
        CharacterSheet plain = sheet(holding(OriginBackground.JULLYANO));
        plain.spendMagicPoints(10);
        new RestServiceImpl().applyRest(plain.getCharacter(), plain, RestType.MINIMO, false);
        int afterFalse = plain.getManaSpent();

        CharacterSheet trueRest = sheet(holding(OriginBackground.JULLYANO));
        trueRest.spendMagicPoints(10);
        new RestServiceImpl().applyRest(trueRest.getCharacter(), trueRest, RestType.MINIMO, true);

        assertEquals(afterFalse - 1, trueRest.getManaSpent());
    }

    @Test
    void vastarePicksPdOrPmAtEachDescanso() {
        RestServiceImpl rest = new RestServiceImpl();
        Character elf = holding(OriginBackground.VASTARE);
        assertEquals(Set.of(ResourceType.MAGIC_POINTS, ResourceType.DETERMINATION_POINTS), rest.getRestBonusChoices(elf));
        assertTrue(rest.getRestBonusChoices(base().build()).isEmpty());

        CharacterSheet none = sheet(elf);
        none.spendMagicPoints(10);
        rest.applyRest(elf, none, RestType.MINIMO, false, null);

        CharacterSheet mana = sheet(elf);
        mana.spendMagicPoints(10);
        rest.applyRest(elf, mana, RestType.MINIMO, false, ResourceType.MAGIC_POINTS);

        assertEquals(none.getManaSpent() - 2, mana.getManaSpent());
    }

    // ---- target- and Cena-scoped -----------------------------------------------------------------------

    @Test
    void cacadorWidensAgainstAnimaisAndMonstrosOnly() {
        Character hunter = holding(CareerBackground.CACADOR);
        CharacterSheet holder = sheet(hunter);
        MonsterSheet beast = SampleMonster.GOBLIN_SELVAGEM.get().toBuilder().creatureType(CreatureType.ANIMAL).build()
                .spawn(new Player());
        CharacterSheet person = sheet(base().build());

        assertEquals(CreatureType.ANIMAL, beast.getCreatureType());
        assertEquals(2, AntecedenteFeat.CACADOR.resolveCriticalMarginIncrease(SkillType.ATAQUE_CORPO_A_CORPO, null,
                hunter, null, holder, beast));
        assertEquals(0, AntecedenteFeat.CACADOR.resolveCriticalMarginIncrease(SkillType.ATAQUE_CORPO_A_CORPO, null,
                hunter, null, holder, person), "a Humano is neither");
        assertEquals(CreatureType.MONSTRUOSO, SampleMonster.GOBLIN_SELVAGEM.get().spawn(new Player()).getCreatureType());
    }

    @Test
    void soldadoResistsOnlyTheFirstHitOfTheCena() {
        CharacterSheet soldier = sheet(holding(CareerBackground.SOLDADO));
        assertEquals(1, AntecedenteFeat.SOLDADO.resolveDamageTakenReduction(soldier.getCharacter(), soldier));

        soldier.recordDamageReceived(new DamageReceipt(3, DamageType.FISICO, null));
        assertEquals(0, AntecedenteFeat.SOLDADO.resolveDamageTakenReduction(soldier.getCharacter(), soldier));

        soldier.startNewScene();
        assertEquals(1, AntecedenteFeat.SOLDADO.resolveDamageTakenReduction(soldier.getCharacter(), soldier));
    }

    @Test
    void selvagemRetaliatesOnlyAgainstWhoDamagedIt() {
        CharacterSheet savage = sheet(holding(CareerBackground.SELVAGEM));
        CharacterSheet foe = sheet(base().build());
        CharacterSheet bystander = sheet(base().build());
        savage.recordDamageReceived(new DamageReceipt(4, DamageType.FISICO, foe));
        SkillRoll roll = activating(new int[]{3, 3, 3}, AntecedenteFeat.SELVAGEM);

        Optional<DamageBonus> onFoe = AntecedenteFeat.SELVAGEM.resolveDamageBonus(SkillType.ATAQUE_CORPO_A_CORPO, null,
                foe, savage.getCharacter(), null, 1, savage, roll);
        assertEquals(new HitPointsServiceImpl().getLifeMultiplier(savage.getCharacter(), savage), onFoe.orElseThrow().getValue());
        assertTrue(AntecedenteFeat.SELVAGEM.resolveDamageBonus(SkillType.ATAQUE_CORPO_A_CORPO, null,
                bystander, savage.getCharacter(), null, 1, savage, roll).isEmpty());

        record(savage, SkillType.ATAQUE_CORPO_A_CORPO, AntecedenteFeat.SELVAGEM);
        assertTrue(savage.isAffectedUntilRest(AntecedenteFeat.SELVAGEM), "spent until a Descanso Longo");
    }

    @Test
    void suditoDoDragaoHasVantagemOnNaturezaInForestsOnly() {
        SceneContext forest = new SceneContext(List.of(), List.of(), Map.of(), TerrainType.FOREST);
        SceneContext desert = new SceneContext(List.of(), List.of(), Map.of(), TerrainType.DESERT);
        Character subject = holding(OriginBackground.SUDITO_DO_DRAGAO);

        assertEquals(Skill.ADVANTAGE_BONUS, AntecedenteFeat.SUDITO_DO_DRAGAO.resolveSkillRollBonus(SkillType.CONHECIMENTOS,
                forest, ConhecimentosSpecialization.NATUREZA, subject, null, null, null));
        assertEquals(0, AntecedenteFeat.SUDITO_DO_DRAGAO.resolveSkillRollBonus(SkillType.CONHECIMENTOS,
                desert, ConhecimentosSpecialization.NATUREZA, subject, null, null, null));
        assertEquals(0, AntecedenteFeat.SUDITO_DO_DRAGAO.resolveSkillRollBonus(SkillType.CONHECIMENTOS,
                forest, ConhecimentosSpecialization.COSMOLOGIA, subject, null, null, null));
    }

    // ---- Ego --------------------------------------------------------------------------------------------

    @Test
    void apostadorGetsATemporarySortePointTheFirstTimeItEmpties() {
        CharacterSheet gambler = sheet(holding(CareerBackground.APOSTADOR));
        gambler.spendEgoPoints(EgoDomain.SORTE, EgoPointType.TEMPORARY, gambler.getTemporaryEgoPoints(EgoDomain.SORTE));
        gambler.spendEgoPoints(EgoDomain.SORTE, EgoPointType.PERMANENT, gambler.getPermanentEgoPoints(EgoDomain.SORTE));
        gambler.startNewRound();

        assertEquals(1, gambler.getAvailableEgoPoints(EgoDomain.SORTE));
        assertTrue(gambler.hasConsumedOncePerSession(AntecedenteFeat.APOSTADOR));
    }

    // ---- rationed activations ---------------------------------------------------------------------------

    @Test
    void aprendizGetsOneVantagemARodadaOnTheChosenPericia() {
        CharacterSheet apprentice = sheet(holding(CareerBackground.APRENDIZ, List.of(SkillType.PROFISSAO), List.of(),
                SkillType.PROFISSAO));
        AntecedenteFeat feat = AntecedenteFeat.APRENDIZ;

        assertTrue(feat.permitsActivation(SkillType.PROFISSAO, null, null, apprentice));
        assertFalse(feat.permitsActivation(SkillType.ARTES, null, null, apprentice));
        assertEquals(Skill.ADVANTAGE_BONUS, feat.resolveSkillRollBonus(SkillType.PROFISSAO, null, null,
                apprentice.getCharacter(), null, apprentice, activating(new int[]{2, 3, 4}, feat)));

        record(apprentice, SkillType.PROFISSAO, feat);
        assertFalse(feat.permitsActivation(SkillType.PROFISSAO, null, null, apprentice));
        apprentice.startNewRound();
        assertTrue(feat.permitsActivation(SkillType.PROFISSAO, null, null, apprentice));
    }

    @Test
    void aldeaoEasesOneRollADescansoLongo() {
        CharacterSheet villager = sheet(holding(CareerBackground.ALDEAO));
        AntecedenteFeat feat = AntecedenteFeat.ALDEAO;

        assertTrue(feat.permitsActivation(SkillType.ARTES, null, null, villager));
        assertEquals(1, feat.resolveDifficultyReduction(SkillType.ARTES, villager.getCharacter(), null,
                activating(new int[]{2, 3, 4}, feat)));

        record(villager, SkillType.ARTES, feat);
        assertFalse(feat.permitsActivation(SkillType.ATLETISMO, null, null, villager));

        new RestServiceImpl().applyRest(villager.getCharacter(), villager, RestType.LONGO);
        assertTrue(feat.permitsActivation(SkillType.ATLETISMO, null, null, villager));
    }

    @Test
    void artistaChoosesACriticoMaiorOnArtesOnceASessao() {
        CharacterSheet artist = sheet(holding(CareerBackground.ARTISTA));
        AntecedenteFeat feat = AntecedenteFeat.ARTISTA;

        assertTrue(feat.permitsActivation(SkillType.ARTES, activating(new int[]{6, 6, 6}, feat), null, artist));
        assertFalse(feat.permitsActivation(SkillType.ARTES, activating(new int[]{6, 6, 5}, feat), null, artist),
                "the chosen result is a Crítico Maior");
        assertFalse(feat.permitsActivation(SkillType.PERSUASAO, activating(new int[]{6, 6, 6}, feat), null, artist));

        record(artist, SkillType.ARTES, feat);
        assertFalse(feat.permitsActivation(SkillType.ARTES, activating(new int[]{6, 6, 6}, feat), null, artist));
        artist.startNewSession();
        assertTrue(feat.permitsActivation(SkillType.ARTES, activating(new int[]{6, 6, 6}, feat), null, artist));
    }

    // ---- through the real roll path ---------------------------------------------------------------

    @Test
    void anActivatedBenefitIsHeldOnTheRollPathAndPaysItsVantagem() {
        CharacterSheet noble = sheet(holding(CareerBackground.NOBRE, SkillType.PERSUASAO));
        CharacterSheet commoner = sheet(base(SkillType.PERSUASAO).build());

        var plain = SkillType.PERSUASAO.newInteraction().applyTo(commoner, null, new SkillRoll(List.of(2, 3, 4)));
        var favoured = SkillType.PERSUASAO.newInteraction().applyTo(noble, null,
                activating(new int[]{2, 3, 4}, AntecedenteFeat.NOBRE));

        assertEquals(plain.getSkillRollBonus() + Skill.ADVANTAGE_BONUS, favoured.getSkillRollBonus());
        org.junit.jupiter.api.Assertions.assertThrows(org.aventyrs.core.sheet.IllegalOperationException.class,
                () -> SkillType.PERSUASAO.newInteraction().applyTo(commoner, null,
                        activating(new int[]{2, 3, 4}, AntecedenteFeat.NOBRE)), "not held without the Antecedente");
    }

    @Test
    void exactlyTheSevenActivatedBenefitsReportThemselves() {
        assertEquals(Set.of(AntecedenteFeat.ALDEAO, AntecedenteFeat.APRENDIZ, AntecedenteFeat.ARTISTA,
                        AntecedenteFeat.NEGOCIADOR, AntecedenteFeat.NOBRE, AntecedenteFeat.SELVAGEM, AntecedenteFeat.TROMBADINHA),
                Arrays.stream(AntecedenteFeat.values()).filter(AntecedenteFeat::isRollActivated).collect(Collectors.toSet()));
    }
}
