package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.AttackMethod;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.TerrainType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.FeatService;
import org.aventyrs.core.character.services.FeatServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.character.services.MovementService;
import org.aventyrs.core.character.services.MovementServiceImpl;
import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpo;
import org.aventyrs.core.skill.esquivaeaparar.EsquivaEAparar;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The clauses of the general Talento trees that are mechanically <b>real</b>, each tested by the
 * effect it has on a character who legally acquired it — through {@link FeatService#grantFeat},
 * satisfying the Pré-requisito and paying the XP — and read back off the consuming service, never
 * off the hook. See the {@code testing-a-feat} skill.
 *
 * <p>Most of the catalog is a TODO'd entry blocked on a missing system, and deliberately has no
 * test here: there is nothing to observe. These are the exceptions.
 */
class GeneralFeatEffectIntegrationTest {

    private final FeatService featService = new FeatServiceImpl();
    private final MovementService movementService = new MovementServiceImpl();
    private final ActionPointsService actionPointsService = new ActionPointsServiceImpl();
    private final HitPointsService hitPointsService = new HitPointsServiceImpl();
    private final DeterminationPointsService determinationPointsService = new DeterminationPointsServiceImpl();
    private final DefenseService defenseService = new DefenseServiceImpl();
    private final org.aventyrs.core.character.services.AttackTargetingService attackTargetingService =
            new org.aventyrs.core.character.services.AttackTargetingServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character.CharacterBuilder character() {
        return CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>());
    }

    private static CharacterSheet fundedSheet(final Character character) {
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.accumulateExperience(BigDecimal.valueOf(100));
        return sheet;
    }

    private void acquire(final Character character, final Feat... feats) throws IllegalOperationException {
        CharacterSheet sheet = fundedSheet(character);
        for (Feat feat : feats) {
            featService.grantFeat(character, sheet, feat);
        }
    }

    private static CharacterSkill trained(final org.aventyrs.core.skill.Skill skill, final int graduation) {
        return CharacterSkill.builder()
                .skill(skill)
                .graduation(SkillGraduation.builder().graduationValue(graduation).build())
                .build();
    }

    // ---------- Movimento Base ----------

    @Test
    void movimentoRapidoRaisesMovimentoBaseByTwo() throws IllegalOperationException {
        Character character = character().build();
        int before = movementService.getMovementBase(character);

        acquire(character, MobilidadeFeat.MOVIMENTO_RAPIDO);

        assertEquals(before + 2, movementService.getMovementBase(character));
    }

    @Test
    void velocistaStacksItsOwnUnitOnTopOfMovimentoRapido() throws IllegalOperationException {
        Character character = character().build();
        int before = movementService.getMovementBase(character);

        acquire(character, MobilidadeFeat.MOVIMENTO_RAPIDO, MobilidadeFeat.VELOCISTA);

        assertEquals(before + 3, movementService.getMovementBase(character));
    }

    // ---------- Pontos de Ação ----------

    @Test
    void maisVelozQueAVisaoGrantsOnePermanentActionPoint() throws IllegalOperationException {
        Character character = character().build();
        int before = actionPointsService.getMaxActionPoints(character, 0);

        // Its own Pré-requisito is two other Talentos de Mobilidade.
        acquire(character, MobilidadeFeat.MOVIMENTO_RAPIDO, MobilidadeFeat.VELOCISTA,
                MobilidadeFeat.MAIS_VELOZ_QUE_A_VISAO);

        assertEquals(before + 1, actionPointsService.getMaxActionPoints(character, 0));
    }

    // ---------- Multiplicadores ----------

    @Test
    void vitalidadeRaisesTheLifeMultiplierByOne() throws IllegalOperationException {
        Character character = character()
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(4).build())
                        .build())
                .build();
        int multiplierBefore = hitPointsService.getLifeMultiplier(character);
        int hitPointsBefore = hitPointsService.getMaxHitPoints(character);

        acquire(character, SobrevivenciaFeat.VITALIDADE);

        assertEquals(multiplierBefore + 1, hitPointsService.getLifeMultiplier(character));
        // The uplift is per point of Vigor, which is what makes it different from a flat grant.
        assertEquals(hitPointsBefore + character.getAttributes().getVigor().getTotal(),
                hitPointsService.getMaxHitPoints(character));
    }

    @Test
    void coracaoDeFerroRaisesTheDeterminationMultiplierByOne() throws IllegalOperationException {
        Character character = character().build();
        int before = determinationPointsService.getDeterminationMultiplier(character);

        acquire(character, DestinoFeat.CORACAO_DE_FERRO_DO_DESTINO);

        assertEquals(before + 1, determinationPointsService.getDeterminationMultiplier(character));
    }

    // ---------- Defesas ----------

    @Test
    void defesaDeMaosLimpasGrantsTwoToBothDefesasWhileWieldingNoWeapon() throws IllegalOperationException {
        Character character = unarmedMartialArtist();
        int physicalBefore = defenseService.getTotalDefense(character, DefenseType.PHYSICAL);
        int magicBefore = defenseService.getTotalDefense(character, DefenseType.MAGIC);

        acquire(character, ArtesMarciaisFeat.ARTISTA_MARCIAL, ArtesMarciaisFeat.DEFESA_DE_MAOS_LIMPAS);

        assertEquals(physicalBefore + 2, defenseService.getTotalDefense(character, DefenseType.PHYSICAL));
        assertEquals(magicBefore + 2, defenseService.getTotalDefense(character, DefenseType.MAGIC));
    }

    @Test
    void defesaDeMaosLimpasGrowsByOnePerTituloAventyrDesperto() throws IllegalOperationException {
        Character character = unarmedMartialArtist();
        int before = defenseService.getTotalDefense(character, DefenseType.PHYSICAL);

        acquire(character, ArtesMarciaisFeat.ARTISTA_MARCIAL, ArtesMarciaisFeat.DEFESA_DE_MAOS_LIMPAS);
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);

        assertEquals(before + 3, defenseService.getTotalDefense(character, DefenseType.PHYSICAL));
    }

    /**
     * "não estiver utilizando nenhuma arma, <b>exceto Armas Naturais</b>": an {@code
     * ItemCategory.NATURAL_WEAPON} in hand keeps the bonus, an ordinary weapon loses it.
     */
    @Test
    void defesaDeMaosLimpasSurvivesAnArmaNaturalButNotAnOrdinaryWeapon() throws IllegalOperationException {
        Weapon claws = weapon(ItemCategory.NATURAL_WEAPON);
        Weapon dagger = weapon(ItemCategory.LIGHT_BLADE);

        // Drawn, not merely carried: "utilizando" means in hand — see Character#drawnWeapons.
        Character withClaws = unarmedMartialArtistBuilder()
                .equipment(new ArrayList<>(List.of(claws))).drawnWeapons(new ArrayList<>(List.of(claws))).build();
        Character withDagger = unarmedMartialArtistBuilder()
                .equipment(new ArrayList<>(List.of(dagger))).drawnWeapons(new ArrayList<>(List.of(dagger))).build();
        int clawsBefore = defenseService.getTotalDefense(withClaws, DefenseType.PHYSICAL);
        int daggerBefore = defenseService.getTotalDefense(withDagger, DefenseType.PHYSICAL);

        acquire(withClaws, ArtesMarciaisFeat.ARTISTA_MARCIAL, ArtesMarciaisFeat.DEFESA_DE_MAOS_LIMPAS);
        acquire(withDagger, ArtesMarciaisFeat.ARTISTA_MARCIAL, ArtesMarciaisFeat.DEFESA_DE_MAOS_LIMPAS);

        assertEquals(clawsBefore + 2, defenseService.getTotalDefense(withClaws, DefenseType.PHYSICAL));
        assertEquals(daggerBefore, defenseService.getTotalDefense(withDagger, DefenseType.PHYSICAL));
    }

    /**
     * The distinction {@code Character#drawnWeapons} exists for: a weapon sheathed on the belt is
     * carried, not <i>used</i>, so it costs a martial artist nothing until it is actually drawn.
     */
    @Test
    void aSheathedWeaponDoesNotCountAsWieldingOne() throws IllegalOperationException {
        Weapon dagger = weapon(ItemCategory.LIGHT_BLADE);
        Character character = unarmedMartialArtistBuilder()
                .equipment(new ArrayList<>(List.of(dagger))).drawnWeapons(new ArrayList<>()).build();
        acquire(character, ArtesMarciaisFeat.ARTISTA_MARCIAL, ArtesMarciaisFeat.DEFESA_DE_MAOS_LIMPAS);
        int sheathed = defenseService.getTotalDefense(character, DefenseType.PHYSICAL);

        character.drawWeapon(dagger);

        assertEquals(sheathed - 2, defenseService.getTotalDefense(character, DefenseType.PHYSICAL));
        character.sheatheWeapon(dagger);
        assertEquals(sheathed, defenseService.getTotalDefense(character, DefenseType.PHYSICAL));
    }

    /**
     * IMPACTO ROCHOSO's dano Vantagem: a flat +2 on an Ataque Corpo-a-Corpo dano roll while
     * wielding no weapon (an Arma Natural is fine), gone the moment an ordinary weapon is held.
     */
    @Test
    void impactoRochosoGrantsDanoVantagemOnlyWhileUnarmed() throws IllegalOperationException {
        Weapon blade = weapon(ItemCategory.LIGHT_BLADE);
        Character unarmed = impactoRochosoCapable().build();
        Character armed = impactoRochosoCapable()
                .equipment(new ArrayList<>(List.of(blade))).drawnWeapons(new ArrayList<>(List.of(blade))).build();

        acquire(unarmed, ArtesMarciaisFeat.DOMINAR_ARTE_MARCIAL_IMPACTO_ROCHOSO);
        acquire(armed, ArtesMarciaisFeat.DOMINAR_ARTE_MARCIAL_IMPACTO_ROCHOSO);

        CharacterSheet unarmedSheet = CharacterSheet.of(unarmed, new Player());
        CharacterSheet armedSheet = CharacterSheet.of(armed, new Player());
        assertEquals(Skill.ADVANTAGE_BONUS, SkillType.ATAQUE_CORPO_A_CORPO.newInteraction()
                .applyTo(unarmedSheet, null, null).getDamageBonus().getValue());
        assertNull(SkillType.ATAQUE_CORPO_A_CORPO.newInteraction()
                .applyTo(armedSheet, null, null).getDamageBonus());
    }

    private static Character.CharacterBuilder impactoRochosoCapable() {
        return character()
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(2).build())
                        .build())
                .skill(SkillType.ESQUIVA_E_APARAR, trained(new EsquivaEAparar(), 5))
                .primaryTitle(new Santo(List.of(), List.of()));
    }

    /**
     * ARTE FLUIDA's extra target: a bare-handed stylist may name two, and either a drawn
     * non-natural weapon or an equipped Escudo takes it away again.
     */
    @Test
    void arteFluidaGrantsOneExtraTargetOnlyWhileEmptyHandedAndShieldless() throws IllegalOperationException {
        Weapon blade = weapon(ItemCategory.LIGHT_BLADE);
        Character stylist = arteFluidaCapable().build();
        assertEquals(1, attackTargetingService.getMaximumTargets(stylist, SkillType.ATAQUE_CORPO_A_CORPO));

        acquire(stylist, ArtesMarciaisFeat.DOMINAR_ARTE_MARCIAL_ARTE_FLUIDA);
        assertEquals(2, attackTargetingService.getMaximumTargets(stylist, SkillType.ATAQUE_CORPO_A_CORPO));

        stylist.equip(blade);
        stylist.drawWeapon(blade);
        assertEquals(1, attackTargetingService.getMaximumTargets(stylist, SkillType.ATAQUE_CORPO_A_CORPO));

        stylist.sheatheWeapon(blade);
        stylist.equip(org.aventyrs.core.item.AbstractItem.builder()
                .name("Escudo Redondo").category(ItemCategory.SHIELD).build());
        assertEquals(1, attackTargetingService.getMaximumTargets(stylist, SkillType.ATAQUE_CORPO_A_CORPO));
    }

    /**
     * ARTE FLUIDA's price: "enquanto houver mais de um alvo você sofre Desvantagem em rolagens de
     * Danos" — a flat -2 on the one dano roll, and nothing at all against a single target.
     */
    @Test
    void arteFluidaChargesDanoDesvantagemOnlyWhileMoreThanOneTargetIsNamed() throws IllegalOperationException {
        Character stylist = arteFluidaCapable().build();
        acquire(stylist, ArtesMarciaisFeat.DOMINAR_ARTE_MARCIAL_ARTE_FLUIDA);
        CharacterSheet sheet = CharacterSheet.of(stylist, new Player());
        CharacterSheet primary = CharacterSheet.of(character().build(), new Player());
        CharacterSheet extra = CharacterSheet.of(character().build(), new Player());

        assertNull(SkillType.ATAQUE_CORPO_A_CORPO.newInteraction()
                .applyTo(sheet, null, null, primary, null).getDamageBonus());
        assertEquals(Skill.DISADVANTAGE_MALUS, SkillType.ATAQUE_CORPO_A_CORPO.newInteraction()
                .applyTo(sheet, null, null, primary, null, List.of(extra)).getDamageBonus().getValue());
    }

    /** Ataque Corpo-a-Corpo 5 plus one Título Aventyr Desperto — ARTE_FLUIDA's exact ladder. */
    private static Character.CharacterBuilder arteFluidaCapable() {
        return character()
                .equipment(new ArrayList<>())
                .drawnWeapons(new ArrayList<>())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, trained(new AtaqueCorpoACorpo(), 5))
                .primaryTitle(new Santo(List.of(), List.of()));
    }

    /**
     * ARTE MARCIAL MISTA's Defesas half: +1 to DF and DM per Talento de Arte Marcial held, itself
     * included.
     */
    @Test
    void arteMarcialMistaAddsOneDefenseNumberPerArteMarcialTalento() throws IllegalOperationException {
        Character character = character()
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(2).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, trained(new AtaqueCorpoACorpo(), 7))
                .primaryTitle(new Santo(List.of(), List.of()))
                .secondaryTitle(new Santo(List.of(), List.of()))
                .build();
        int physicalBefore = defenseService.getTotalDefense(character, DefenseType.PHYSICAL);

        // ARTISTA_MARCIAL, then MISTA — two Arte Marcial Talentos.
        acquire(character, ArtesMarciaisFeat.ARTISTA_MARCIAL, ArtesMarciaisFeat.ARTE_MARCIAL_MISTA);

        assertEquals(physicalBefore + 2, defenseService.getTotalDefense(character, DefenseType.PHYSICAL));
        assertEquals(physicalBefore + 2, defenseService.getTotalDefense(character, DefenseType.MAGIC));
    }

    /**
     * TIGRE E SERPENTE's flat "+1 número" to the Margem Crítica of an Ataque Corpo-a-Corpo: a
     * roll of two 5s reads as a critical only once the margin is widened, and only for melee.
     */
    @Test
    void tigreESerpenteWidensTheMeleeCriticalMarginByOne() throws IllegalOperationException {
        Character character = character()
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(2).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, trained(new AtaqueCorpoACorpo(), 5))
                .primaryTitle(new Santo(List.of(), List.of()))
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        SkillRoll twoFives = new SkillRoll(List.of(5, 5, 1));

        assertEquals(CriticalResult.NONE, SkillType.ATAQUE_CORPO_A_CORPO.newInteraction()
                .applyTo(sheet, null, twoFives).getCriticalResult());

        acquire(character, ArtesMarciaisFeat.DOMINAR_ARTE_MARCIAL_TIGRE_E_SERPENTE);

        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, SkillType.ATAQUE_CORPO_A_CORPO.newInteraction()
                .applyTo(sheet, null, twoFives).getCriticalResult());
        // scoped to Corpo-a-Corpo — an Atletismo roll of the same faces is untouched.
        assertEquals(CriticalResult.NONE, SkillType.ATLETISMO.newInteraction()
                .applyTo(sheet, null, twoFives).getCriticalResult());
    }

    private static Weapon weapon(final ItemCategory category) {
        return AbstractWeapon.builder().name(category.name()).category(category)
                .damageBase(DamageBase.of(1, 1)).skillType(SkillType.ATAQUE_CORPO_A_CORPO).build();
    }

    private static Character.CharacterBuilder unarmedMartialArtistBuilder() {
        return character()
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(2).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, trained(new AtaqueCorpoACorpo(), 2))
                .skill(SkillType.ESQUIVA_E_APARAR, trained(new EsquivaEAparar(), 4));
    }

    /** Força 2 and Ataque Corpo-a-Corpo 2 for ARTISTA_MARCIAL, Esquiva e Aparar 4 for its dependent. */
    private static Character unarmedMartialArtist() {
        return unarmedMartialArtistBuilder().build();
    }
    // ---------- Vantagem em rolagens de Perícia ----------

    private int rollBonus(final CharacterSheet sheet, final SkillType skillType, final SceneContext sceneContext) {
        return skillType.newInteraction().applyTo(sheet, sceneContext, null).getSkillRollBonus();
    }

    /** A Scene whose opposed combatant — the target of an attack roll — sits at distance. */
    private static SceneContext against(final CombatantSheet target, final Range distance) {
        return new SceneContext(List.of(), List.of(target), Map.of(target, distance),
                null, true, 1, false, target);
    }

    private static CharacterSheet enemySheet() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID()).build(), new Player());
    }

    /**
     * "Vantagem em todas as suas rolagens de ataque feitas com qualquer arma ou se estiver
     * desarmado" enumerates every case, so it applies with no Scene and no target at all.
     */
    @Test
    void dominarArmasGrantsVantagemOnBothPericiasDeAtaqueUnconditionally() throws IllegalOperationException {
        Character character = duelist(6);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        int meleeBefore = rollBonus(sheet, SkillType.ATAQUE_CORPO_A_CORPO, null);
        int rangedBefore = rollBonus(sheet, SkillType.ATAQUE_A_DISTANCIA, null);

        acquire(character, DuelistaFeat.ESPECIALISTA_EM_ARMA, DuelistaFeat.DOMINAR_ARMAS);

        assertEquals(meleeBefore + Skill.ADVANTAGE_BONUS, rollBonus(sheet, SkillType.ATAQUE_CORPO_A_CORPO, null));
        assertEquals(rangedBefore + Skill.ADVANTAGE_BONUS, rollBonus(sheet, SkillType.ATAQUE_A_DISTANCIA, null));
    }

    /** Scoped to Perícias de Ataque — an Atletismo roll must be untouched. */
    @Test
    void dominarArmasDoesNotLeakIntoANonAttackPericia() throws IllegalOperationException {
        Character character = duelist(6);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        int before = rollBonus(sheet, SkillType.ATLETISMO, null);

        acquire(character, DuelistaFeat.ESPECIALISTA_EM_ARMA, DuelistaFeat.DOMINAR_ARMAS);

        assertEquals(before, rollBonus(sheet, SkillType.ATLETISMO, null));
    }

    /**
     * {@code DuelistaFeat.ESPECIALISTA_EM_ARMA} via {@link EspecialistaEmArmaFeat} — Vantagem
     * only on an attack delivered with the chosen {@code AttackMethod}.
     */
    @Test
    void especialistaEmArmaGrantsVantagemOnlyWithTheChosenMethod() throws IllegalOperationException {
        Character character = duelist(2);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        Weapon lightBlade = weapon(ItemCategory.LIGHT_BLADE);
        Weapon heavyBlade = weapon(ItemCategory.HEAVY_BLADE);
        int chosenBefore = attackRollBonus(sheet, lightBlade);
        int otherBefore = attackRollBonus(sheet, heavyBlade);

        acquire(character, EspecialistaEmArmaFeat.of(AttackMethod.LIGHT_BLADE));

        assertEquals(chosenBefore + Skill.ADVANTAGE_BONUS, attackRollBonus(sheet, lightBlade));
        assertEquals(otherBefore, attackRollBonus(sheet, heavyBlade));
    }

    /**
     * {@code DuelistaFeat.MAESTRIA_EM_ARMA} — Margem Crítica Menor +1 only while attacking with
     * the {@code EspecialistaEmArmaFeat} choice.
     */
    @Test
    void maestriaEmArmaWidensMargemCriticaOnlyWithTheChosenMethod() throws IllegalOperationException {
        Character character = duelist(6);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        Weapon lightBlade = weapon(ItemCategory.LIGHT_BLADE);
        Weapon heavyBlade = weapon(ItemCategory.HEAVY_BLADE);
        SkillRoll fives = new SkillRoll(List.of(5, 5, 1));

        acquire(character, EspecialistaEmArmaFeat.of(AttackMethod.LIGHT_BLADE),
                DuelistaFeat.DOMINAR_ARMAS, DuelistaFeat.MAESTRIA_EM_ARMA);

        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, SkillType.ATAQUE_CORPO_A_CORPO.newInteraction()
                .applyTo(sheet, null, fives, null, lightBlade).getCriticalResult());
        assertEquals(CriticalResult.NONE, SkillType.ATAQUE_CORPO_A_CORPO.newInteraction()
                .applyTo(sheet, null, fives, null, heavyBlade).getCriticalResult());
    }

    private int attackRollBonus(final CharacterSheet sheet, final Weapon attackSource) {
        return SkillType.ATAQUE_CORPO_A_CORPO.newInteraction()
                .applyTo(sheet, null, null, null, attackSource).getSkillRollBonus();
    }

    /**
     * {@code ArtilhariaFeat.ATIRADOR_PERFEITO} via {@link AtiradorPerfeitoFeat} — Vantagem only
     * against a target at Distância Média or beyond, and only with the chosen {@code AttackMethod}.
     */
    @Test
    void atiradorPerfeitoNeedsBothTheChosenMethodAndMediaOrBeyond() throws IllegalOperationException {
        Character character = character()
                .skill(SkillType.ATAQUE_A_DISTANCIA, trained(new org.aventyrs.core.skill.ataqueadistancia.AtaqueADistancia(), 2))
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        CharacterSheet target = enemySheet();
        Weapon bow = weapon(ItemCategory.BOW);
        Weapon crossbow = weapon(ItemCategory.CROSSBOW);

        acquire(character, AtiradorPerfeitoFeat.of(AttackMethod.BOW));

        assertEquals(Skill.ADVANTAGE_BONUS, rangedBonus(sheet, bow, against(target, Range.DISTANCIA_MEDIA))
                - rangedBonus(sheet, bow, against(target, Range.DISTANCIA_CURTA)));
        // Wrong method, at the same distance — no Vantagem.
        assertEquals(0, rangedBonus(sheet, crossbow, against(target, Range.DISTANCIA_MEDIA))
                - rangedBonus(sheet, crossbow, against(target, Range.DISTANCIA_CURTA)));
    }

    private int rangedBonus(final CharacterSheet sheet, final Weapon attackSource, final SceneContext context) {
        return SkillType.ATAQUE_A_DISTANCIA.newInteraction()
                .applyTo(sheet, context, null, null, attackSource).getSkillRollBonus();
    }

    @Test
    void lutarEngajadoGrantsVantagemOnlyAgainstAnAdjacentTarget() throws IllegalOperationException {
        Character character = duelist(1);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        CharacterSheet target = enemySheet();
        int adjacentBefore = rollBonus(sheet, SkillType.ATAQUE_CORPO_A_CORPO, against(target, Range.ADJACENTE));

        acquire(character, DuelistaFeat.LUTADOR_NATO, DuelistaFeat.LUTAR_ENGAJADO);

        assertEquals(adjacentBefore + Skill.ADVANTAGE_BONUS,
                rollBonus(sheet, SkillType.ATAQUE_CORPO_A_CORPO, against(target, Range.ADJACENTE)));
    }

    @Test
    void lutarEngajadoGrantsNothingAtRangeWithNoTargetOrWithNoScene() throws IllegalOperationException {
        Character character = duelist(1);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        CharacterSheet target = enemySheet();
        int before = rollBonus(sheet, SkillType.ATAQUE_CORPO_A_CORPO, null);

        acquire(character, DuelistaFeat.LUTADOR_NATO, DuelistaFeat.LUTAR_ENGAJADO);

        assertEquals(before, rollBonus(sheet, SkillType.ATAQUE_CORPO_A_CORPO, against(target, Range.DISTANCIA_MEDIA)));
        // A Scene with nobody opposed, and no Scene at all, both read as "condition not met".
        assertEquals(before, rollBonus(sheet, SkillType.ATAQUE_CORPO_A_CORPO,
                new SceneContext(List.of(), List.of(), Map.of())));
        assertEquals(before, rollBonus(sheet, SkillType.ATAQUE_CORPO_A_CORPO, null));
    }

    /** Scoped to Ataque Corpo-a-Corpo alone — an adjacent target does not help a ranged attack. */
    @Test
    void lutarEngajadoDoesNotReachAtaqueADistancia() throws IllegalOperationException {
        Character character = duelist(1);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        CharacterSheet target = enemySheet();
        int before = rollBonus(sheet, SkillType.ATAQUE_A_DISTANCIA, against(target, Range.ADJACENTE));

        acquire(character, DuelistaFeat.LUTADOR_NATO, DuelistaFeat.LUTAR_ENGAJADO);

        assertEquals(before, rollBonus(sheet, SkillType.ATAQUE_A_DISTANCIA, against(target, Range.ADJACENTE)));
    }

    @Test
    void leituraComportamentalGrantsVantagemOnPersuasaoOnly() throws IllegalOperationException {
        Character character = character()
                .attributes(CharacterAttributes.builder()
                        .instinct(AttributeValue.builder().domain(AttributeDomain.INSTINCT).base(3).build())
                        .build())
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        int persuasaoBefore = rollBonus(sheet, SkillType.PERSUASAO, null);
        int atencaoBefore = rollBonus(sheet, SkillType.ATTENTION, null);

        acquire(character, PeritoFeat.LEITURA_COMPORTAMENTAL);

        assertEquals(persuasaoBefore + Skill.ADVANTAGE_BONUS, rollBonus(sheet, SkillType.PERSUASAO, null));
        // The Atenção half is still TODO'd — it is scoped to one Especialização the hook can't see.
        assertEquals(atencaoBefore, rollBonus(sheet, SkillType.ATTENTION, null));
    }

    /**
     * {@code PeritoFeat.FOCO_EM_PERICIA} via {@link org.aventyrs.core.feat.FocoEmPericiaFeat} —
     * Vantagem on the chosen Perícia only, read back off the Interaction's roll bonus.
     */
    @Test
    void focoEmPericiaGrantsVantagemOnTheChosenPericiaOnly() throws IllegalOperationException {
        Character character = character().build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        int atletismoBefore = rollBonus(sheet, SkillType.ATLETISMO, null);
        int persuasaoBefore = rollBonus(sheet, SkillType.PERSUASAO, null);

        acquire(character, FocoEmPericiaFeat.of(SkillType.ATLETISMO));

        assertEquals(atletismoBefore + Skill.ADVANTAGE_BONUS, rollBonus(sheet, SkillType.ATLETISMO, null));
        assertEquals(persuasaoBefore, rollBonus(sheet, SkillType.PERSUASAO, null));
    }

    /**
     * {@code SobrevivenciaFeat.MESTRE_DE_CACA}'s two reachable halves — Margem Crítica Menor +1
     * and Vantagem on Perícias de Ataque — apply only while in the Terreno Predileto chosen via
     * {@link TerrenoPrediletoFeat}.
     */
    @Test
    void mestreDeCacaAppliesOnlyInTheChosenTerreno() throws IllegalOperationException {
        Character character = character()
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(3).build())
                        .build())
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        SceneContext forest = new SceneContext(List.of(), List.of(), Map.of(), TerrainType.FOREST);
        SceneContext desert = new SceneContext(List.of(), List.of(), Map.of(), TerrainType.DESERT);
        int meleeBefore = rollBonus(sheet, SkillType.ATAQUE_CORPO_A_CORPO, desert);
        int atletismoBefore = rollBonus(sheet, SkillType.ATLETISMO, forest);
        // A pair of 5s: NONE at margin 0, ACERTO_CRITICO_MENOR once MESTRE_DE_CACA's +1 widens
        // the qualifying face from 6 down to 5.
        SkillRoll roll = new SkillRoll(List.of(5, 5, 2));

        acquire(character, TerrenoPrediletoFeat.of(TerrainType.FOREST), SobrevivenciaFeat.MESTRE_DE_CACA);

        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR,
                SkillType.ATAQUE_CORPO_A_CORPO.newInteraction().applyTo(sheet, forest, roll).getCriticalResult());
        assertEquals(CriticalResult.NONE,
                SkillType.ATAQUE_CORPO_A_CORPO.newInteraction().applyTo(sheet, desert, roll).getCriticalResult());
        assertEquals(meleeBefore + Skill.ADVANTAGE_BONUS, rollBonus(sheet, SkillType.ATAQUE_CORPO_A_CORPO, forest));
        assertEquals(meleeBefore, rollBonus(sheet, SkillType.ATAQUE_CORPO_A_CORPO, desert));
        // Not a Perícia de Ataque — no Vantagem even in the chosen terrain.
        assertEquals(atletismoBefore, rollBonus(sheet, SkillType.ATLETISMO, forest));
    }

    /** Ataque Corpo-a-Corpo at graduation, which every Duelista Pré-requisito here counts. */
    private static Character duelist(final int meleeGraduation) {
        return character()
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, trained(new AtaqueCorpoACorpo(), meleeGraduation))
                .build();
    }
    // ---------- Margem Crítica ----------

    private CriticalResult criticalOf(final CharacterSheet sheet, final SkillType skillType, final SkillRoll roll) {
        return skillType.newInteraction().applyTo(sheet, null, roll).getCriticalResult();
    }

    /**
     * "+2 na Margem Crítica de todas as suas rolagens de Perícias", excluding Perícias de Ataque
     * and Esquiva e Aparar. 4+4+2 is no critical at margin 0; widening by 2 lowers the qualifying
     * face to 4, so the pair of 4s now reads as Acerto Crítico Menor.
     */
    @Test
    void controleDaSituacaoWidensTheMargemCriticaOfAnOrdinaryPericia() throws IllegalOperationException {
        Character character = character().build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        SkillRoll roll = new SkillRoll(List.of(4, 4, 2));
        assertEquals(CriticalResult.NONE, criticalOf(sheet, SkillType.ATLETISMO, roll));

        acquire(character, PeritoFeat.CONTROLE_DA_SITUACAO);

        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, criticalOf(sheet, SkillType.ATLETISMO, roll));
    }

    /** "Não afeta rolagens de Perícias de Ataque e Esquivar e Aparar" — both stay narrow. */
    @Test
    void controleDaSituacaoExcludesAtaqueAndEsquivaEAparar() throws IllegalOperationException {
        Character character = character().build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        SkillRoll roll = new SkillRoll(List.of(4, 4, 2));

        acquire(character, PeritoFeat.CONTROLE_DA_SITUACAO);

        assertEquals(CriticalResult.NONE, criticalOf(sheet, SkillType.ATAQUE_CORPO_A_CORPO, roll));
        assertEquals(CriticalResult.NONE, criticalOf(sheet, SkillType.ATAQUE_A_DISTANCIA, roll));
        assertEquals(CriticalResult.NONE, criticalOf(sheet, SkillType.ESQUIVA_E_APARAR, roll));
    }
}
