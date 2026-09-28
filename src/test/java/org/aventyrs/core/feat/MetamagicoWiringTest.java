package org.aventyrs.core.feat;

import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.ActiveAbilityService;
import org.aventyrs.core.character.services.ActiveAbilityServiceImpl;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.combat.AttackDelivery;
import org.aventyrs.core.combat.AttackReceiver;
import org.aventyrs.core.combat.DeliveredAttack;
import org.aventyrs.core.combat.DeliveredAttackResult;
import org.aventyrs.core.combat.IncomingAttack;
import org.aventyrs.core.combat.IncomingAttackResult;
import org.aventyrs.core.effect.DamageInteraction;
import org.aventyrs.core.effect.SpellEffectContext;
import org.aventyrs.core.effect.SpellHealingEffect;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.item.OffensiveMasterpiece;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.magic.MimetizedSpell;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.SpellFamiliarity;
import org.aventyrs.core.magic.catalog.VidaSpell;
import org.aventyrs.core.rest.RestServiceImpl;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code MetamagicoFeat}, fully wired (0.0.70): the "Magias que você conheça / seja capaz de
 * conjurar" resistance clauses ({@link SpellFamiliarity}), and everything the Barreira Mágica's
 * holder's other Talentos change about it.
 */
class MetamagicoWiringTest {

    /** A Magia the Conjurador below learns, and one they never do. */
    private static final Spell KNOWN = VidaSpell.REVIGORAR;
    private static final Spell UNKNOWN = VidaSpell.ALIVIAR_A_DOR;

    private final AttackReceiver attackReceiver = new AttackReceiver();
    private final AttackDelivery attackDelivery = new AttackDelivery();
    private final ActiveAbilityService activeAbilityService = new ActiveAbilityServiceImpl();
    private final DefenseService defenseService = new DefenseServiceImpl();
    private final DamageService damageService = new DamageServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet holding(final Feat... feats) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.of(Map.of(AttributeDomain.INSTINCT, 5)))
                .feats(new ArrayList<>(List.of(feats)))
                .spells(new ArrayList<>(List.of(KNOWN)))
                .mimetizedSpells(new ArrayList<>())
                .equipment(new ArrayList<>())
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private static CharacterSheet plain() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
    }

    private IncomingAttackResult defendAgainst(final CharacterSheet defender, final Spell spell,
                                               final DefenseType defenseType, final SkillRoll roll) {
        return attackReceiver.resolve(IncomingAttack.builder()
                .defender(defender)
                .difficultyLevel(DifficultyLevel.MEDIUM)
                .defenseType(defenseType)
                .attackSource(spell)
                .defenseRoll(roll)
                .build());
    }

    private static ActiveAbility barreiraOf(final Character character) {
        return character.getActiveAbilities().stream()
                .filter(BarreiraMagicaActiveAbility.class::isInstance)
                .findFirst().orElseThrow();
    }

    // ---------- SpellFamiliarity ----------

    @Test
    void aLearnedAMimetizedAndAnAlternateVersionAllCount() {
        CharacterSheet sheet = holding();
        sheet.getCharacter().grantMimetizedSpell(MimetizedSpell.builder().spell(VidaSpell.BENCAO_DA_LUZ).build());

        assertTrue(SpellFamiliarity.canCast(sheet.getCharacter(), KNOWN));
        assertTrue(SpellFamiliarity.canCast(sheet.getCharacter(), VidaSpell.BENCAO_DA_LUZ));
        assertTrue(SpellFamiliarity.canCast(sheet.getCharacter(), KNOWN.getAlternateVersion().orElseThrow()));
        assertFalse(SpellFamiliarity.canCast(sheet.getCharacter(), UNKNOWN));
    }

    // ---------- DM against a castable Magia ----------

    @Test
    void amplaAddsThreeToTheDmAgainstACastableMagiaOnly() {
        CharacterSheet sheet = holding(MetamagicoFeat.APTIDAO_MAGICA_AMPLA);
        SkillRoll roll = new SkillRoll(List.of(3, 3, 3));

        int known = defendAgainst(sheet, KNOWN, DefenseType.MAGIC, roll).getDefenseTotal();
        int unknown = defendAgainst(sheet, UNKNOWN, DefenseType.MAGIC, roll).getDefenseTotal();
        int physical = defendAgainst(sheet, KNOWN, DefenseType.PHYSICAL, roll).getDefenseTotal();
        int physicalUnknown = defendAgainst(sheet, UNKNOWN, DefenseType.PHYSICAL, roll).getDefenseTotal();

        assertEquals(unknown + 3, known);
        assertEquals(physicalUnknown, physical, "a DM clause, never the DF");
    }

    @Test
    void assombrosaReplacesAmplasThreeWithFive() {
        CharacterSheet sheet = holding(MetamagicoFeat.APTIDAO_MAGICA_AMPLA, MetamagicoFeat.APTIDAO_MAGICA_ASSOMBROSA);
        SkillRoll roll = new SkillRoll(List.of(3, 3, 3));

        assertEquals(defendAgainst(sheet, UNKNOWN, DefenseType.MAGIC, roll).getDefenseTotal() + 5,
                defendAgainst(sheet, KNOWN, DefenseType.MAGIC, roll).getDefenseTotal());
    }

    @Test
    void attackDeliveryRaisesTheDefesaTheMagiaMustBeat() {
        CharacterSheet defender = holding(MetamagicoFeat.APTIDAO_MAGICA_AMPLA);

        DeliveredAttackResult result = attackDelivery.resolve(DeliveredAttack.builder()
                .attacker(plain())
                .defender(defender)
                .attackSkill(SkillType.ATAQUE_A_DISTANCIA)
                .attackSource(KNOWN)
                .defenseType(DefenseType.MAGIC)
                .defenseValue(10)
                .attackRoll(new SkillRoll(List.of(3, 3, 3)))
                .build());

        assertEquals(13, result.getRequiredTotal());
    }

    // ---------- GD níveis against a castable Magia ----------

    @Test
    void supremaEasesTheGdToResistACastableMagiaByOneNivel() {
        CharacterSheet sheet = holding(MetamagicoFeat.APTIDAO_MAGICA_SUPREMA);
        SkillRoll roll = new SkillRoll(List.of(3, 3, 3));

        assertEquals(DifficultyLevel.MEDIUM.easier(1),
                defendAgainst(sheet, KNOWN, DefenseType.MAGIC, roll).getEffectiveDifficultyLevel());
        assertEquals(DifficultyLevel.MEDIUM,
                defendAgainst(sheet, UNKNOWN, DefenseType.MAGIC, roll).getEffectiveDifficultyLevel());
    }

    @Test
    void artesaoEasesItOnlyWhileABarreiraRuns() {
        CharacterSheet sheet = holding(MetamagicoFeat.ARCANISTA_EXPERIENTE, MetamagicoFeat.ARTESAO_DE_BARREIRAS);
        SkillRoll roll = new SkillRoll(List.of(3, 3, 3));
        assertEquals(DifficultyLevel.MEDIUM,
                defendAgainst(sheet, KNOWN, DefenseType.MAGIC, roll).getEffectiveDifficultyLevel());

        activeAbilityService.activate(sheet.getCharacter(), sheet, barreiraOf(sheet.getCharacter()), 0);

        assertEquals(DifficultyLevel.MEDIUM.easier(1),
                defendAgainst(sheet, KNOWN, DefenseType.MAGIC, roll).getEffectiveDifficultyLevel());
    }

    @Test
    void attackDeliveryReportsTheNivelUnapplied() {
        DeliveredAttackResult result = attackDelivery.resolve(DeliveredAttack.builder()
                .attacker(plain())
                .defender(holding(MetamagicoFeat.APTIDAO_MAGICA_SUPREMA))
                .attackSkill(SkillType.ATAQUE_A_DISTANCIA)
                .attackSource(KNOWN)
                .defenseType(DefenseType.MAGIC)
                .defenseValue(10)
                .attackRoll(new SkillRoll(List.of(3, 3, 3)))
                .build());

        assertEquals(1, result.getUnappliedSpellResistanceReduction());
    }

    // ---------- Dracônica's immunity ----------

    @Test
    void draconicaDefendsOutrightWhileTenPdRemain() {
        CharacterSheet sheet = holding(MetamagicoFeat.APTIDAO_MAGICA_DRACONICA);
        int pd = new DeterminationPointsServiceImpl().getCurrentDeterminationPoints(sheet.getCharacter(), sheet);
        assertTrue(pd >= 10, "the fixture must start with at least 10PD");
        SkillRoll worst = new SkillRoll(List.of(1, 1, 2));

        assertTrue(defendAgainst(sheet, KNOWN, DefenseType.MAGIC, worst).getDefended());
        assertFalse(defendAgainst(sheet, UNKNOWN, DefenseType.MAGIC, worst).getDefended());

        sheet.spendDeterminationPoints(pd - 9);

        assertFalse(defendAgainst(sheet, KNOWN, DefenseType.MAGIC, worst).getDefended(),
                "below 10PD the immunity lapses");
    }

    @Test
    void draconicaMakesADeliveredMagiaMissAndItsDamageNothing() {
        CharacterSheet defender = holding(MetamagicoFeat.APTIDAO_MAGICA_DRACONICA);

        DeliveredAttackResult result = attackDelivery.resolve(DeliveredAttack.builder()
                .attacker(plain())
                .defender(defender)
                .attackSkill(SkillType.ATAQUE_A_DISTANCIA)
                .attackSource(KNOWN)
                .defenseType(DefenseType.MAGIC)
                .defenseValue(0)
                .attackRoll(new SkillRoll(List.of(6, 6, 5)))
                .build());

        assertFalse(result.getHit());
        assertEquals(0, damageService.calculateFinalDamage(defender, null, DamageType.MAGICO, null, 10,
                false, false, KNOWN));
    }

    // ---------- Arcanista's RM ----------

    @Test
    void arcanistaResistsAKnownMagiasDamageByOneRmInstance() {
        CharacterSheet sheet = holding(MetamagicoFeat.ARCANISTA);

        assertEquals(8, damageService.calculateFinalDamage(sheet, null, DamageType.MAGICO, null, 10, false, false, KNOWN));
        assertEquals(10, damageService.calculateFinalDamage(sheet, null, DamageType.MAGICO, null, 10, false, false, UNKNOWN));
        assertEquals(10, damageService.calculateFinalDamage(sheet, null, DamageType.MAGICO, null, 10, false, false, null));
    }

    @Test
    void aDamageInteractionMarkedFromASpellCarriesItToMitigation() {
        CharacterSheet sheet = holding(MetamagicoFeat.ARCANISTA);

        new DamageInteraction(damageService).fromSpell(KNOWN)
                .applyTo(sheet, null, DamageType.MAGICO, null, 10, false);

        assertEquals(8, sheet.getDamageTaken());
    }

    // ---------- the Barreira Mágica ----------

    @Test
    void artesaoMakesTheBarreiraAnAcaoLivre() {
        Character plainArcanist = holding(MetamagicoFeat.ARCANISTA_EXPERIENTE).getCharacter();
        Character artesao = holding(MetamagicoFeat.ARCANISTA_EXPERIENTE, MetamagicoFeat.ARTESAO_DE_BARREIRAS).getCharacter();

        assertEquals(ActionCost.ofActionPoints(1), barreiraOf(plainArcanist).getActionPointCost(plainArcanist));
        assertEquals(ActionCost.FREE_ACTION, barreiraOf(artesao).getActionPointCost(artesao));
    }

    @Test
    void engenheiroDoManaTakesOnePmOffTheBarreira() {
        CharacterSheet sheet = holding(MetamagicoFeat.ARCANISTA_EXPERIENTE, MetamagicoFeat.ENGENHEIRO_DO_MANA);

        activeAbilityService.activate(sheet.getCharacter(), sheet, barreiraOf(sheet.getCharacter()), 0);

        assertEquals(2, sheet.getManaSpent());
    }

    @Test
    void aPoderosaWeaponExtendsTheBarreiraLikeAnyMagia() {
        CharacterSheet sheet = holding(MetamagicoFeat.ARCANISTA_EXPERIENTE);
        BarreiraMagicaActiveAbility barreira = (BarreiraMagicaActiveAbility) barreiraOf(sheet.getCharacter());
        assertEquals(2, barreira.resolveDurationInRounds(sheet.getCharacter()));

        AbstractWeapon cajado = AbstractWeapon.builder()
                .name("Cajado")
                .category(ItemCategory.LIGHT_BLADE)
                .weightClass(ItemWeightClass.LIGHT)
                .damageBase(DamageBase.of(2, 0))
                .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
                .build();
        cajado.setMasterpiece(OffensiveMasterpiece.PODEROSA);
        sheet.getCharacter().equip(cajado);

        assertEquals(3, barreira.resolveDurationInRounds(sheet.getCharacter()));
    }

    @Test
    void theHigherRungsShieldAdjacentAlliesWhileTheBarreiraRuns() {
        assertEquals(0, allyBonusBeside(false, MetamagicoFeat.ARCANISTA_EXPERIENTE, MetamagicoFeat.MESTRE_ARCANISTA));
        assertEquals(0, allyBonusBeside(true, MetamagicoFeat.ARCANISTA_EXPERIENTE));
        assertEquals(1, allyBonusBeside(true, MetamagicoFeat.ARCANISTA_EXPERIENTE, MetamagicoFeat.MESTRE_ARCANISTA));
        assertEquals(3, allyBonusBeside(true, MetamagicoFeat.ARCANISTA_EXPERIENTE, MetamagicoFeat.MESTRE_ARCANISTA,
                MetamagicoFeat.DESAFIADOR_DA_REALIDADE));
    }

    @Test
    void anAllyFurtherThanAdjacentGetsNothing() {
        CharacterSheet arcanist = holding(MetamagicoFeat.ARCANISTA_EXPERIENTE, MetamagicoFeat.MESTRE_ARCANISTA);
        activeAbilityService.activate(arcanist.getCharacter(), arcanist, barreiraOf(arcanist.getCharacter()), 0);
        CharacterSheet ally = plain();

        SceneContext far = new SceneContext(List.of(arcanist), List.of(), Map.of(arcanist, Range.DISTANCIA_CURTA));

        assertEquals(defenseService.getTotalDefense(ally, DefenseType.PHYSICAL, null),
                defenseService.getTotalDefense(ally, DefenseType.PHYSICAL, far));
    }

    private int allyBonusBeside(final boolean raised, final MetamagicoFeat... rungs) {
        CharacterSheet arcanist = holding(rungs);
        if (raised) {
            activeAbilityService.activate(arcanist.getCharacter(), arcanist, barreiraOf(arcanist.getCharacter()), 0);
        }
        CharacterSheet ally = plain();
        SceneContext beside = new SceneContext(List.of(arcanist), List.of(), Map.of(arcanist, Range.ADJACENTE));

        return defenseService.getTotalDefense(ally, DefenseType.PHYSICAL, beside)
                - defenseService.getTotalDefense(ally, DefenseType.PHYSICAL, null);
    }

    // ---------- healing ----------

    @Test
    void aHealingBonusMovesADescansoEquivalentFigure() {
        CharacterSheet target = plain();
        target.applyDamage(20);
        int offered = new RestServiceImpl().getRecoveredHitPoints(target.getCharacter(), RestType.LONGO);

        new SpellHealingEffect(KNOWN, KNOWN.getHealing().orElseThrow(), false, new RestServiceImpl(), null,
                Skill.DISADVANTAGE_MALUS).applyTo(target);

        assertEquals(20 - Math.max(0, offered + Skill.DISADVANTAGE_MALUS), target.getDamageTaken());
    }

    @Test
    void conjuracaoRapidasDesvantagemReachesTheHealing() {
        assertEquals(Skill.DISADVANTAGE_MALUS, MetamagicoFeat.CONJURACAO_RAPIDA.resolveSpellHealingBonus(KNOWN,
                holding().getCharacter(), java.util.Set.of(MetamagicoFeat.CONJURACAO_RAPIDA)));
        assertEquals(0, MetamagicoFeat.CONJURACAO_RAPIDA.resolveSpellHealingBonus(KNOWN,
                holding().getCharacter(), java.util.Set.of()));
        assertEquals(-2, SpellEffectContext.of(false, null).withHealingBonus(-2).healingBonus());
    }

    /** A Magia cast at a named combatant, which the Conjurador below also learns. */
    private static Spell ranged() {
        return java.util.Arrays.stream(org.aventyrs.core.magic.catalog.MagicTree.values())
                .flatMap(tree -> tree.getSpells().stream())
                .filter(spell -> spell.getTargeting().reach() == org.aventyrs.core.magic.SpellReach.DISTANCIA
                        && spell.getManaCost() > 1)
                .findFirst().orElseThrow();
    }

    @Test
    void manaCostCanBeAskedBeforeCasting() {
        CharacterSheet caster = holding(MetamagicoFeat.ENGENHEIRO_DO_MANA);
        org.aventyrs.core.scene.Scene scene = new org.aventyrs.core.scene.Scene();
        scene.addParticipant(caster, 1);
        org.aventyrs.core.magic.SpellCastRequest request = org.aventyrs.core.magic.SpellCastRequest.builder()
                .caster(caster).spell(ranged()).scene(scene).sceneContext(scene.buildContext(caster, Map.of()))
                .combatantTarget(caster).build();
        org.aventyrs.core.magic.SpellCastingServiceImpl service = new org.aventyrs.core.magic.SpellCastingServiceImpl();

        assertEquals(ranged().getManaCost() - 1, service.resolveManaCost(request));
        assertEquals(service.resolveManaCost(request), service.castSpell(request).getManaCost());
    }

    @Test
    void aCastOnAnImmuneTargetSaysSo() {
        CharacterSheet caster = holding();
        CharacterSheet target = holding(MetamagicoFeat.APTIDAO_MAGICA_DRACONICA);
        target.getCharacter().grantSpell(ranged());
        org.aventyrs.core.scene.Scene scene = new org.aventyrs.core.scene.Scene();
        scene.addParticipant(caster, 1);
        scene.addParticipant(target, 2);

        org.aventyrs.core.magic.SpellCastingResult result = new org.aventyrs.core.magic.SpellCastingServiceImpl()
                .castSpell(org.aventyrs.core.magic.SpellCastRequest.builder()
                        .caster(caster).spell(ranged()).scene(scene).sceneContext(scene.buildContext(caster, Map.of()))
                        .combatantTarget(target).activatedFeat(MetamagicoFeat.CONJURACAO_RAPIDA).build());

        assertTrue(result.isTargetImmune());
        assertEquals(0, result.getHealingBonus(), "Conjuração Rápida not held: nothing");
    }
}
