package org.aventyrs.core.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.MovementMode;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.character.services.MovementServiceImpl;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.SpellCastRequest;
import org.aventyrs.core.magic.SpellCastingResult;
import org.aventyrs.core.magic.SpellCastingService;
import org.aventyrs.core.magic.SpellCastingServiceImpl;
import org.aventyrs.core.magic.catalog.PolimorfismoSpell;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.HealingSource;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.util.DiceRoller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The rest of Polimorfismo (core 0.1.0): Murcha-Corpo and Infla-Músculos with their Correntes, Armada Ôgrica,
 * Dracônecer's Draconato, Enfadecer's Boneca de Porcelana, and Serra-Pernas's extra PM and Corrente Alternativa —
 * each landed the way a client lands a cast.
 */
class PolimorfismoCorrentesTest {

    private final SpellCastingService spellCastingService = new SpellCastingServiceImpl();
    private final DamageService damageService = new DamageServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    /** Força 4, Destreza 3, Vigor 3. */
    private CharacterSheet target() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .sizeCategory(SizeCategory.ZERO)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(4).build())
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(3).build())
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(3).build())
                        .build())
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private InteractionResult land(final CharacterSheet target, final Spell spell, final SpellEffectContext context,
                                   final boolean chainFires) {
        SpellEffect effect = spellCastingService.resolveEffect(spell, context).orElseThrow();
        if (chainFires) {
            spellCastingService.resolveEffectChain(spell, context, DiceRoller.fixed(1))
                    .ifPresent(((AbstractEffect) effect)::chainInto);
        }
        InteractionResult result = target.receiveInteraction(effect);
        while (result.getNextInteraction() != null) {
            result = target.receiveInteraction(result.getNextInteraction());
        }
        return result;
    }

    private static int strength(final CharacterSheet sheet) {
        return sheet.getTemporaryBonus(ModifierType.STRENGTH_BONUS);
    }

    private static int dexterity(final CharacterSheet sheet) {
        return sheet.getTemporaryBonus(ModifierType.DEXTERITY_BONUS);
    }

    @Test
    void murchaCorpoLowersThePickedAtributoAndMurchaAlmasBoth() {
        CharacterSheet picked = target();
        land(picked, PolimorfismoSpell.MURCHA_CORPO,
                SpellEffectContext.HOSTILE.withChosenAttribute(AttributeDomain.STRENGTH), false);
        assertEquals(-2, strength(picked));
        assertEquals(0, dexterity(picked));

        CharacterSheet withered = target();
        land(withered, PolimorfismoSpell.MURCHA_CORPO,
                SpellEffectContext.HOSTILE.withChosenAttribute(AttributeDomain.STRENGTH), true);
        assertEquals(-2, strength(withered), "replaced, not doubled");
        assertEquals(-2, dexterity(withered));
    }

    @Test
    void inflaMusculosRaisesThePickByOneAndInflarOEgoToTwo() {
        CharacterSheet plain = target();
        land(plain, PolimorfismoSpell.INFLA_MUSCULOS,
                SpellEffectContext.FRIENDLY.withChosenAttribute(AttributeDomain.DEXTERITY), false);
        assertEquals(1, dexterity(plain));

        CharacterSheet inflated = target();
        land(inflated, PolimorfismoSpell.INFLA_MUSCULOS,
                SpellEffectContext.FRIENDLY.withChosenAttribute(AttributeDomain.DEXTERITY), true);
        assertEquals(2, dexterity(inflated));
        assertEquals(0, strength(inflated));
    }

    /** "Os bônus concedidos podem ser escolhidos individualmente, este efeito não ativa … Gigantecer." */
    @Test
    void armadaOgricaGivesEachTargetItsOwnPickAndNoCorrente() {
        Spell armada = PolimorfismoSpell.TITANECER.getAlternateVersion().orElseThrow();
        assertEquals(2, armada.getMaxAdditionalTargets());
        assertTrue(armada.getEffectChainKind().isEmpty());

        CharacterSheet caster = target();
        CharacterSheet ally = target();
        land(caster, armada, SpellEffectContext.FRIENDLY.withChosenAttribute(AttributeDomain.STRENGTH), true);
        land(ally, armada, SpellEffectContext.FRIENDLY.withChosenAttribute(AttributeDomain.DEXTERITY), true);

        assertEquals(2, strength(caster));
        assertEquals(0, dexterity(caster));
        assertEquals(2, dexterity(ally));
        assertEquals(0, strength(ally));
        assertEquals(SizeCategory.ZERO,
                new org.aventyrs.core.character.services.CharacterSizeServiceImpl().getEffectiveSizeCategory(ally),
                "Armada Ôgrica grows nobody");
    }

    @Test
    void draconatoGivesWingsAtTheLandMovementAndOneInstanceOfRdAndRm() {
        CharacterSheet target = target();
        MovementServiceImpl movement = new MovementServiceImpl();
        assertFalse(movement.hasMovementMode(target, MovementMode.FLIGHT));
        int rd = damageService.getTotalDamageReduction(target, DamageType.FISICO, null);
        int rm = damageService.getTotalMagicReduction(target);

        land(target, PolimorfismoSpell.DRACONECER, SpellEffectContext.FRIENDLY, true);

        assertTrue(movement.hasMovementMode(target, MovementMode.FLIGHT));
        assertEquals(movement.getMovementBase(target), movement.getMovementBase(target, MovementMode.FLIGHT));
        assertEquals(rd + DamageService.DEFAULT_DAMAGE_REDUCTION,
                damageService.getTotalDamageReduction(target, DamageType.FISICO, null));
        assertEquals(rm + DamageService.DEFAULT_DAMAGE_REDUCTION, damageService.getTotalMagicReduction(target));

        target.finishTurn();
        target.finishTurn();
        target.finishTurn();
        assertFalse(movement.hasMovementMode(target, MovementMode.FLIGHT), "the wings go with the Duração");
    }

    @Test
    void bonecaDePorcelanaStripsRdAndRmAndHalvesHealing() {
        CharacterSheet target = target();
        target.grantBlessing(new Blessing(ModifierType.DAMAGE_REDUCTION, 4, 5, TargetScope.SELF, "armour"));
        target.grantBlessing(new Blessing(ModifierType.MAGIC_REDUCTION, 2, 5, TargetScope.SELF, "ward"));

        land(target, PolimorfismoSpell.ENFADECER, SpellEffectContext.HOSTILE, true);

        assertEquals(0, damageService.getTotalDamageReduction(target, DamageType.FISICO, null));
        assertEquals(0, damageService.getTotalMagicReduction(target));
        target.applyDamage(10);
        target.heal(8, HealingSource.rest(org.aventyrs.core.rest.RestType.MINIMO));
        assertEquals(6, target.getDamageTaken(), "8 halved to 4");

        target.finishTurn();
        assertEquals(4, damageService.getTotalDamageReduction(target, DamageType.FISICO, null), "1 Rodada");
    }

    @Test
    void serraPernasAimedAtItsAlternativeCorrenteWeakensAndCursesInsteadOfSqueezing() {
        CharacterSheet target = target();
        land(target, PolimorfismoSpell.SERRA_PERNAS, SpellEffectContext.HOSTILE.withAlternateEffectChain(true), true);

        assertEquals(-2, target.getTemporaryBonus(ModifierType.PHYSICAL_SKILL_ROLL_BONUS));
        assertTrue(target.hasCondition(ConditionType.AMALDICOADO, null));
        assertEquals(SizeCategory.ZERO,
                new org.aventyrs.core.character.services.CharacterSizeServiceImpl().getEffectiveSizeCategory(target),
                "Espremer did not fire");
        assertInstanceOf(FraquezaMomentanea.class, spellCastingService.resolveEffectChain(PolimorfismoSpell.SERRA_PERNAS,
                SpellEffectContext.HOSTILE.withAlternateEffectChain(true), DiceRoller.fixed(1)).orElseThrow());

        target.finishTurn();
        assertEquals(0, target.getTemporaryBonus(ModifierType.PHYSICAL_SKILL_ROLL_BONUS), "its next Turn only");
        assertTrue(target.hasCondition(ConditionType.AMALDICOADO, null), "cursed for the whole Duração");
    }

    /** "Pode gastar PM adicional em sua conjuração, aumentado a sua duração em 2 rodadas para cada PM gasto." */
    @Test
    void serraPernasExtraPmCostsManaAndLengthensItsDuracao() {
        CharacterSheet caster = target();
        CharacterSheet target = target();
        Scene scene = new Scene();
        scene.addParticipant(caster, 2);
        scene.addParticipant(target, 1);
        SpellCastRequest.SpellCastRequestBuilder request = SpellCastRequest.builder()
                .caster(caster)
                .spell(PolimorfismoSpell.SERRA_PERNAS)
                .scene(scene)
                .sceneContext(scene.buildContext(caster, Map.of(target, Range.DISTANCIA_CURTA)))
                .combatantTarget(target);
        SpellCastingResult plain = spellCastingService.castSpell(request.build());
        SpellCastingResult extended = spellCastingService.castSpell(request.extraMana(2).build());

        assertEquals(plain.getManaCost() + 2, extended.getManaCost());
        assertEquals(plain.getDurationInRounds() + 4, extended.getDurationInRounds());

        target.receiveInteraction(extended.getSpellEffect());
        for (int i = 0; i < plain.getDurationInRounds(); i++) {
            target.finishTurn();
        }
        assertEquals(-2, strength(target), "still weakened past the plain Duração");
    }
}
