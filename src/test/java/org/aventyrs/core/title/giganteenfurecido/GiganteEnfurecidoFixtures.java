package org.aventyrs.core.title.giganteenfurecido;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.EgoValue;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.FrenzyMode;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.TitleAbilityActivationRequest;
import org.aventyrs.core.util.DiceRoller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared builders for the Gigante Enfurecido tests — call {@link #loadTemplates()} in a {@code @BeforeEach}. */
final class GiganteEnfurecidoFixtures {

    /** Força 4, Vigor 4 — so a Frenesi lasts 2 + 4/2 = 4 Rodadas outside the Primário slot. */
    static final int STRENGTH = 4;
    static final int VIGOR = 4;
    static final int AUTOCONTROLE = 4;

    private GiganteEnfurecidoFixtures() {
    }

    static void loadTemplates() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    static GiganteEnfurecido gigante(final List<GiganteEnfurecidoSpecialization> specializations,
                                     final AventyrTitleAbility... abilities) {
        return new GiganteEnfurecido(specializations, List.of(abilities));
    }

    /** A combatant holding title in slot, with Força/Vigor 4, Autocontrole 4 and Ataque Corpo-a-Corpo 3. */
    static CharacterSheet holder(final GiganteEnfurecido title, final TitleSlot slot) {
        CharacterSheet sheet = combatant(AUTOCONTROLE);
        sheet.getCharacter().grantTitle(title, slot);
        return sheet;
    }

    /** Held as the Título Secundário, so no Primário clause applies. */
    static CharacterSheet holder(final GiganteEnfurecido title) {
        return holder(title, TitleSlot.SECONDARY);
    }

    static CharacterSheet combatant(final int autocontrole) {
        CharacterSkill skill = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1).build();
        skill.increaseGraduation(3);
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>())
                .equipment(new ArrayList<>())
                .drawnWeapons(new ArrayList<>())
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(STRENGTH).build())
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(VIGOR).build())
                        .build())
                .egos(CharacterEgos.builder().autocontrole(EgoValue.builder().base(autocontrole).build()).build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, skill)
                .build();
        return CharacterSheet.of(character, new Player());
    }

    /** A request activating the Despertar's Frenesi with modes. */
    static TitleAbilityActivationRequest frenesi(final CombatantSheet activator, final FrenzyMode... modes) {
        return TitleAbilityActivationRequest.builder().activator(activator).choices(Set.of(modes)).build();
    }

    /** Starts activator's Frenesi with modes, returning the title for chaining. */
    static GiganteEnfurecido enterFrenzy(final CombatantSheet activator, final FrenzyMode... modes) {
        GiganteEnfurecido title = GiganteEnfurecido.heldBy(activator).orElseThrow();
        title.activateAbility(GiganteEnfurecidoDespertar.FRENESI, frenesi(activator, modes));
        return title;
    }

    /** activator's snapshot in the given Rodada: allies and enemies at the given distances. */
    static SceneContext context(final int round, final Map<CombatantSheet, Range> allies,
                                final Map<CombatantSheet, Range> enemies) {
        Map<CombatantSheet, Range> distances = new HashMap<>(allies);
        distances.putAll(enemies);
        return new SceneContext(new ArrayList<>(allies.keySet()), new ArrayList<>(enemies.keySet()), distances,
                null, true, round, false);
    }

    static DiceRoller dice(final Integer... faces) {
        return DiceRoller.fixed(faces);
    }
}
