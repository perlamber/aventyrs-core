package org.aventyrs.core.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.AttackRangeServiceImpl;
import org.aventyrs.core.character.services.CriticalServiceImpl;
import org.aventyrs.core.combat.AttackDelivery;
import org.aventyrs.core.combat.DeliveredAttack;
import org.aventyrs.core.effect.MarcasDaGuerra;
import org.aventyrs.core.effect.MarcasDaProtecao;
import org.aventyrs.core.effect.ToqueDivino;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.SpellCastRequest;
import org.aventyrs.core.magic.SpellCastingResult;
import org.aventyrs.core.magic.SpellCastingService;
import org.aventyrs.core.magic.SpellCastingServiceImpl;
import org.aventyrs.core.magic.SpellDamage;
import org.aventyrs.core.magic.TestSpell;
import org.aventyrs.core.magic.TestSpellTree;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.GenericMonster;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.TemporaryEffect;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Artefatos de Conjuração (core 0.1.6.8). */
class CastingArtifactTest {

    private final SpellCastingService casting = new SpellCastingServiceImpl();

    /** A Florescente (7PM) damaging Magia, so a PM reduction is visible. */
    private static final Spell DAMAGING = new TestSpell(SkillType.ATAQUE_A_DISTANCIA, TestSpellTree.DIVERGING,
            BranchLevel.FLORESCENTE, null) {
        @Override
        public Optional<SpellDamage> getPrimaryDamage() {
            return Optional.of(SpellDamage.halfFocusMagical(1));
        }
    };

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    /** A caster with Foco, Força and Destreza all at focus, wielding artifact — drawn when drawn is true, merely worn otherwise. */
    private static CharacterSheet wielder(final CastingArtifactItem artifact, final int focus, final boolean drawn) {
        Weapon copy = (Weapon) AbstractItem.fromTemplate(artifact);
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>())
                .attributes(CharacterAttributes.of(Map.of(AttributeDomain.FOCUS, focus,
                        AttributeDomain.STRENGTH, focus, AttributeDomain.DEXTERITY, focus)))
                .equipment(new ArrayList<>(List.of(copy)))
                .drawnWeapons(new ArrayList<>())
                .build();
        if (drawn) {
            character.drawWeapon(copy);
        }
        return CharacterSheet.of(character, new Player());
    }

    private SpellCastingResult cast(final CharacterSheet caster) {
        CharacterSheet target = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
        Scene scene = new Scene();
        scene.addParticipant(caster, 1);
        scene.addParticipant(target, 0);
        SceneContext context = new SceneContext(List.of(), List.of(target), Map.of(target, Range.ADJACENTE),
                null, true, 0, false, target);
        return casting.castSpell(SpellCastRequest.builder()
                .caster(caster).spell(DAMAGING).scene(scene).sceneContext(context).combatantTarget(target).build());
    }

    @Test
    void catalogsTheNineArtefactsThreeOfThemPureFoci() {
        assertEquals(9, CastingArtifactItem.values().length);
        assertTrue(ItemCatalog.all().containsAll(List.of(CastingArtifactItem.values())));
        assertEquals(List.of(CastingArtifactItem.BOLA_DE_CRISTA, CastingArtifactItem.GRIMORIO,
                        CastingArtifactItem.VARINHA),
                Arrays.stream(CastingArtifactItem.values()).filter(artifact -> !artifact.canAttack()).toList());
        assertEquals(ItemCategory.CASTING_ARTIFACT, CastingArtifactItem.BACULO.getCategory());
    }

    @Test
    void aPureFocusCannotStrike() {
        CharacterSheet caster = wielder(CastingArtifactItem.VARINHA, 3, true);
        Weapon varinha = caster.getCharacter().getDrawnWeapons().get(0);
        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> new AttackDelivery().resolve(DeliveredAttack.from(GenericMonster.CAPANGA.spawn(new Player()),
                                DefenseType.PHYSICAL)
                        .attacker(caster).attackSkill(SkillType.ATAQUE_CORPO_A_CORPO).attackSource(varinha)
                        .attackRoll(new SkillRoll(List.of(3, 3, 3))).build()));
        assertEquals("WEAPON_CANNOT_ATTACK", refused.getMessage());
    }

    @Test
    void anArtefactHelpsOnlyDrawnAndWithItsRequisitosMet() {
        assertEquals(Skill.ADVANTAGE_BONUS,
                CastingArtifacts.castingRollBonus(wielder(CastingArtifactItem.BACULO, 3, true).getCharacter()));
        assertEquals(0, CastingArtifacts.castingRollBonus(wielder(CastingArtifactItem.BACULO, 3, false).getCharacter()));
        assertEquals(0, CastingArtifacts.castingRollBonus(wielder(CastingArtifactItem.BACULO, 2, true).getCharacter()));
    }

    @Test
    void theVarinhaGivesVantagemAndAPmOffTheFirstSpell() {
        SpellCastingResult plain = cast(wielder(CastingArtifactItem.VARINHA, 3, false));
        SpellCastingResult focused = cast(wielder(CastingArtifactItem.VARINHA, 3, true));

        assertEquals(plain.getDominioDoManaResult().getSkillRollBonus() + Skill.ADVANTAGE_BONUS,
                focused.getDominioDoManaResult().getSkillRollBonus());
        assertEquals(plain.getManaCost() - 1, focused.getManaCost());
    }

    @Test
    void theGrimorioShortensTheFirstSpellAndDoublesItsCriticals() {
        CharacterSheet caster = wielder(CastingArtifactItem.GRIMORIO, 4, true);
        assertEquals(cast(wielder(CastingArtifactItem.GRIMORIO, 4, false)).getActivationTime().actionPoints() - 1,
                cast(caster).getActivationTime().actionPoints());
        assertEquals(2, casting.getCastingCriticalEffectApplications(caster));
        assertEquals(1, casting.getCastingCriticalEffectApplications(wielder(CastingArtifactItem.VARINHA, 3, true)));
    }

    @Test
    void spellMarginsSumOnTheConjuracaoRollOnly() {
        CharacterSheet caster = wielder(CastingArtifactItem.VARINHA, 3, true);
        CharacterSheet bare = wielder(CastingArtifactItem.VARINHA, 3, false);
        CriticalServiceImpl critical = new CriticalServiceImpl();

        assertEquals(critical.sumCriticalMarginIncrease(bare, SkillType.DOMINIO_DO_MANA, null, null) + 2,
                critical.sumCriticalMarginIncrease(caster, SkillType.DOMINIO_DO_MANA, null, null));
        assertEquals(critical.sumCriticalMarginIncrease(bare, SkillType.ATAQUE_CORPO_A_CORPO, null, null),
                critical.sumCriticalMarginIncrease(caster, SkillType.ATAQUE_CORPO_A_CORPO, null, null));
    }

    @Test
    void theCajadoDeGuerraRaisesDamageGuardsItsWielderAndCarriesMarcasDaGuerra() {
        CharacterSheet caster = wielder(CastingArtifactItem.CAJADO_DE_GUERRA, 3, true);
        SpellCastingResult plain = cast(wielder(CastingArtifactItem.CAJADO_DE_GUERRA, 3, false));
        SpellCastingResult result = cast(caster);

        assertEquals(plain.getPrimaryDamage().deterministicAmount() + 1, result.getPrimaryDamage().deterministicAmount());
        assertEquals(1, caster.getTemporaryBonus(ModifierType.DEFESAS));
        assertTrue(result.getGrantedEffectChains().stream().anyMatch(MarcasDaGuerra.class::isInstance));
    }

    @Test
    void marcasDaProtecaoReplacesTheCastBonusRatherThanStacking() {
        CharacterSheet caster = wielder(CastingArtifactItem.CAJADO_DO_AMPARO, 3, true);
        cast(caster);
        assertEquals(1, caster.getTemporaryBonus(ModifierType.DEFESAS));

        new MarcasDaProtecao(caster).applyTo(caster);

        assertEquals(MarcasDaProtecao.DEFESAS_BONUS, caster.getTemporaryBonus(ModifierType.DEFESAS));
    }

    @Test
    void toqueDivinoAddsTwoToDamageOrHealing() {
        CharacterSheet enemy = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
        assertEquals(2, new ToqueDivino(true).applyTo(enemy).getResourceLossValue());

        CharacterSheet ally = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
        ally.applyDamage(5);
        new ToqueDivino(false).applyTo(ally);
        assertEquals(3, ally.getDamageTaken());
    }

    @Test
    void theBolaDeCristaWidensDistanceMagias() {
        Spell distant = new TestSpell();
        Character bare = wielder(CastingArtifactItem.BOLA_DE_CRISTA, 4, false).getCharacter();
        Character focused = wielder(CastingArtifactItem.BOLA_DE_CRISTA, 4, true).getCharacter();
        AttackRangeServiceImpl range = new AttackRangeServiceImpl();

        range.getEffectiveRange(bare, distant).ifPresent(base -> assertEquals(base.increasedBy(1),
                range.getEffectiveRange(focused, distant).orElseThrow()));
        assertTrue(CastingArtifacts.suppressesSumMinorCriticals(focused));
        assertEquals(1, CastingArtifacts.touchReachIncreaseUd(focused));
    }

    @Test
    void theActivatedArtefactsAreOfferedWhileDrawnAndTheirFavorRaisesTheFigure() {
        Character drawn = wielder(CastingArtifactItem.GLADIO_CANALIZADOR, 3, true).getCharacter();
        Character weak = wielder(CastingArtifactItem.GLADIO_CANALIZADOR, 2, true).getCharacter();
        Character sheathed = wielder(CastingArtifactItem.GLADIO_CANALIZADOR, 3, false).getCharacter();

        assertTrue(drawn.getActiveAbilities().contains(ArtifactActiveAbility.GLADIO_CANALIZADOR));
        assertFalse(sheathed.getActiveAbilities().contains(ArtifactActiveAbility.GLADIO_CANALIZADOR));
        assertEquals(3, value(ArtifactActiveAbility.GLADIO_CANALIZADOR.resolveEffects(drawn)));
        assertEquals(2, value(ArtifactActiveAbility.GLADIO_CANALIZADOR.resolveEffects(weak)));
        assertEquals(3, ArtifactActiveAbility.BISARMA_CANALIZADORA.resolveEffects(drawn).size());
        assertEquals(2, ArtifactActiveAbility.GLADIO_CANALIZADOR.getMagicPointCost());
    }

    @Test
    void onlyACasterMayActivateOne() {
        CharacterSheet nonCaster = wielder(CastingArtifactItem.ADAGA_DE_CERIMONIAS, 3, true);
        assertFalse(ArtifactActiveAbility.ADAGA_DE_CERIMONIAS.isUsableBy(nonCaster));
    }

    @Test
    void theAdagasDanoBaseReachesAWeaponSwing() {
        CharacterSheet caster = wielder(CastingArtifactItem.ADAGA_DE_CERIMONIAS, 3, true);
        Weapon adaga = caster.getCharacter().getDrawnWeapons().get(0);
        org.aventyrs.core.character.services.DamageBaseServiceImpl damageBase =
                new org.aventyrs.core.character.services.DamageBaseServiceImpl();
        var before = damageBase.getDamageBase(caster, adaga);

        ArtifactActiveAbility.ADAGA_DE_CERIMONIAS.resolveEffects(caster.getCharacter()).forEach(caster::applyEffect);

        assertEquals(before.scaledUp(2), damageBase.getDamageBase(caster, adaga));
    }

    private static int value(final List<TemporaryEffect> effects) {
        return ((org.aventyrs.core.sheet.TemporaryBonus) effects.get(0)).getValue();
    }
}
