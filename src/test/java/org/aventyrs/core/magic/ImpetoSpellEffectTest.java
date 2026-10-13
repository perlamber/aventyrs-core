package org.aventyrs.core.magic;

import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.ego.InitiativeAdvantage;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Ímpeto's "Vantagem em... Efeitos de Magia": +2 to a Magia's Dano and Cura, first two Rodadas, initiative won (0.1.6.4). */
class ImpetoSpellEffectTest {

    private final SpellCastingService service = new SpellCastingServiceImpl();
    private CharacterSheet plain;
    private CharacterSheet impetuous;
    private CharacterSheet target;
    private Scene scene;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        plain = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
        impetuous = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                .egoAdvantage(EgoDomain.INICIATIVA, InitiativeAdvantage.IMPETO).build(), new Player());
        target = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
        scene = new Scene();
        scene.addParticipant(plain, 2);
        scene.addParticipant(impetuous, 1);
        scene.addParticipant(target, 0);
    }

    private static final Spell DAMAGING = new TestSpell() {
        @Override
        public Optional<SpellDamage> getPrimaryDamage() {
            return Optional.of(SpellDamage.halfFocusMagicalElemental(1, org.aventyrs.core.magic.ElementalType.FOGO));
        }
    };

    private SpellCastingResult cast(final CharacterSheet caster, final int round, final boolean wonInitiative) {
        SceneContext context = new SceneContext(List.of(), List.of(target), Map.of(target, Range.ADJACENTE),
                null, true, round, wonInitiative, target);
        return service.castSpell(SpellCastRequest.builder()
                .caster(caster).spell(DAMAGING).scene(scene)
                .sceneContext(context)
                .combatantTarget(target)
                .build());
    }

    @Test
    void wonInitiativeEarlyGrantsVantagemOnDanoAndCura() {
        SpellCastingResult base = cast(plain, 1, true);
        SpellCastingResult boosted = cast(impetuous, 1, true);

        assertEquals(base.getPrimaryDamage().deterministicAmount() + Skill.ADVANTAGE_BONUS,
                boosted.getPrimaryDamage().deterministicAmount());
        assertEquals(base.getHealingBonus() + Skill.ADVANTAGE_BONUS, boosted.getHealingBonus());
    }

    @Test
    void withoutTheInitiativeOrPastTheSecondRodadaNothingIsAdded() {
        SpellCastingResult base = cast(plain, 1, true);

        assertEquals(base.getPrimaryDamage().deterministicAmount(),
                cast(impetuous, 1, false).getPrimaryDamage().deterministicAmount());
        assertEquals(base.getHealingBonus(), cast(impetuous, 3, true).getHealingBonus());
    }
}
