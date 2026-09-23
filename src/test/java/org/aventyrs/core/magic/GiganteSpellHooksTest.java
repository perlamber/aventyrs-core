package org.aventyrs.core.magic;

import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.sheet.AreaDamage;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.EgoPointType;
import org.aventyrs.core.sheet.Frenzy;
import org.aventyrs.core.sheet.FrenzyMode;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.aventyrs.core.util.TranslatableMessages.SPELL_CASTING_PREVENTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What a Frenesi does to casting: the block, Titã's +1PA, Frenesi Arcano, Cataclismo, and Fanático's hostility. */
class GiganteSpellHooksTest {

    private final SpellCastingService spellCastingService = new SpellCastingServiceImpl();
    private CharacterSheet caster;
    private CharacterSheet target;
    private Scene scene;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        caster = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
        target = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
        scene = new Scene();
        scene.addParticipant(caster, 1);
        scene.addParticipant(target, 0);
    }

    private SpellCastingResult cast(final Spell spell) {
        return spellCastingService.castSpell(SpellCastRequest.builder()
                .caster(caster).spell(spell).scene(scene)
                .sceneContext(scene.buildContext(caster, Map.of(target, Range.ADJACENTE), target))
                .combatantTarget(target)
                .build());
    }

    private Frenzy titaFrenzy() {
        Frenzy frenzy = new Frenzy(caster.getId(), 3);
        frenzy.addMode(FrenzyMode.TITA_ENLOUQUECIDO);
        frenzy.liftConcentrationBlock();
        caster.startFrenzy(frenzy);
        return frenzy;
    }

    private static Spell damaging(final int dice, final SpellDuration duration) {
        return new TestSpell() {
            @Override
            public Optional<SpellDamage> getPrimaryDamage() {
                return Optional.of(SpellDamage.halfFocusMagical(dice));
            }

            @Override
            public SpellDuration getDuration() {
                return duration;
            }
        };
    }

    private static Spell enchantment(final int rounds) {
        return new TestSpell() {
            @Override
            public MagicType getPrimaryType() {
                return MagicType.ENCANTAMENTO;
            }

            @Override
            public SpellDuration getDuration() {
                return SpellDuration.rodadas(rounds);
            }
        };
    }

    @Test
    void aPlainFrenesiForbidsCasting() {
        caster.startFrenzy(new Frenzy(caster.getId(), 3));

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> cast(damaging(1, SpellDuration.INSTANTANEA)));
        assertEquals(SPELL_CASTING_PREVENTED, refused.getMessage());
        assertTrue(caster.isSpellCastingPrevented(null));
    }

    @Test
    void titaEnlouquecidoLetsTheGiganteCastAtOneMorePa() {
        titaFrenzy();

        assertEquals(3, cast(damaging(1, SpellDuration.INSTANTANEA)).getActivationTime().actionPoints());
    }

    @Test
    void frenesiArcanoAddsADieAndThreePaToTheNextCastOnly() {
        titaFrenzy();
        caster.grantEnhancedAttacks(SpellEmpowerment.DANO, 1);

        SpellCastingResult empowered = cast(damaging(1, SpellDuration.INSTANTANEA));
        SpellCastingResult plain = cast(damaging(1, SpellDuration.INSTANTANEA));

        assertEquals(SpellEmpowerment.DANO, empowered.getEmpowerment());
        assertEquals(2, empowered.getPrimaryDamage().diceCount());
        assertEquals(6, empowered.getActivationTime().actionPoints());
        assertNull(plain.getEmpowerment());
        assertEquals(1, plain.getPrimaryDamage().diceCount());
    }

    @Test
    void frenesiArcanoMaximisesAMagiaAlreadyAtThreeDice() {
        titaFrenzy();
        caster.grantEnhancedAttacks(SpellEmpowerment.DANO, 1);
        int baseline = spellCastingService.resolvePrimaryDamage(damaging(3, SpellDuration.INSTANTANEA), caster)
                .orElseThrow().deterministicAmount();

        ResolvedSpellDamage damage = cast(damaging(3, SpellDuration.INSTANTANEA)).getPrimaryDamage();

        assertEquals(0, damage.diceCount());
        assertEquals(baseline + 18, damage.deterministicAmount());
    }

    @Test
    void frenesiArcanoLengthensAnEncantamentoByTwoRounds() {
        titaFrenzy();
        caster.grantEnhancedAttacks(SpellEmpowerment.DURACAO, 1);

        assertEquals(5, cast(enchantment(3)).getDurationInRounds());
    }

    @Test
    void underCataclismoAnInstantMagiaAlsoHitsEveryoneAround() {
        Frenzy frenzy = titaFrenzy();
        frenzy.addMode(FrenzyMode.CATACLISMO_ELEMENTAL);
        frenzy.setCataclysmElements(Set.of(ElementalType.FOGO));

        List<AreaDamage> hits = cast(damaging(1, SpellDuration.INSTANTANEA)).getAreaDamage();
        assertEquals(1, hits.size());
        assertEquals(2, hits.get(0).amount());

        assertNull(cast(enchantment(3)).getAreaDamage());
    }

    @Test
    void aFanaticoMustBeOvercomeEvenByABeneficialMagia() {
        assertNotEquals(Boolean.TRUE, cast(enchantment(3)).getMustOvercomeMagicDefense());

        Frenzy frenzy = new Frenzy(target.getId(), 3);
        frenzy.markFanatic();
        target.startFrenzy(frenzy);
        target.spendEgoPoints(EgoDomain.AUTOCONTROLE, EgoPointType.TEMPORARY,
                target.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
        titaFrenzy();

        assertTrue(cast(enchantment(3)).getMustOvercomeMagicDefense());
    }

    @Test
    void frenesiEsmeraldaGivesVantagemOnlyWhenCastingADamagingMagia() {
        Frenzy frenzy = titaFrenzy();
        int plainDamaging = cast(damaging(1, SpellDuration.INSTANTANEA)).getDominioDoManaResult().getSkillRollBonus();
        int plainEnchantment = cast(enchantment(3)).getDominioDoManaResult().getSkillRollBonus();

        frenzy.addMode(FrenzyMode.FRENESI_ESMERALDA);

        assertEquals(plainDamaging + 2,
                cast(damaging(1, SpellDuration.INSTANTANEA)).getDominioDoManaResult().getSkillRollBonus());
        assertEquals(plainEnchantment, cast(enchantment(3)).getDominioDoManaResult().getSkillRollBonus());
    }
}
