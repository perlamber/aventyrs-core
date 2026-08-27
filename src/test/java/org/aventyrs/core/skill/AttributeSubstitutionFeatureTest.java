package org.aventyrs.core.skill;

import org.aventyrs.core.ability.PeritoTeoricoAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.ataqueadistancia.AtaqueADistanciaInteraction;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoInteraction;
import org.aventyrs.core.skill.atletismo.AtletismoInteraction;
import org.aventyrs.core.skill.dominiodomana.DominioDoManaInteraction;
import org.aventyrs.core.skill.furtividade.FurtividadeInteraction;
import org.aventyrs.core.skill.persuasao.PersuasaoInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end feature test: a single Character carrying every currently-wired unconditional
 * base-Attribute-substituting Habilidade de Competência at once (see CLAUDE.md's
 * "Unconditional Perícia base-Attribute substitution" section) — {@code
 * AtaqueCorpoACorpoCompetencyAbility.ACUIDADE}, {@code AtletismoCompetencyAbility.ACROBATA},
 * {@code AtaqueADistanciaCompetencyAbility.DISPARO_ARCANO} and {@code
 * DominioDoManaCompetencyAbility.MAGIA_SELVAGEM} — proving each one's own {@code
 * <Skill>Interaction} resolves its own substitution independently, without leaking into any
 * other Perícia's roll (including one, Persuasão, that has no substituting ability at all).
 */
class AttributeSubstitutionFeatureTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private CharacterSheet sheet() {
        Character character = CharacterFixture.blank(CharacterFixture.ATTRIBUTE_SUBSTITUTIONS).build();
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void acuidadeSubstitutesDestrezaForForcaOnAtaqueCorpoACorpo() {
        InteractionResult result = new AtaqueCorpoACorpoInteraction().applyTo(sheet());

        // Destreza(5) + Graduação(1) = 6 — Força(2) + 1 = 3 would mean the substitution didn't apply.
        assertEquals(6, result.getSkillRollBonus());
    }

    @Test
    void acrobataSubstitutesDestrezaForForcaOnAtletismo() {
        InteractionResult result = new AtletismoInteraction().applyTo(sheet());

        // Destreza(5) + Graduação(2) = 7 — Força(2) + 2 = 4 would mean the substitution didn't apply.
        assertEquals(7, result.getSkillRollBonus());
    }

    @Test
    void disparoArcanoSubstitutesFocoForDestrezaOnAtaqueADistancia() {
        InteractionResult result = new AtaqueADistanciaInteraction().applyTo(sheet());

        // Foco(8) + Graduação(3) = 11 — Destreza(5) + 3 = 8 would mean the substitution didn't apply.
        assertEquals(11, result.getSkillRollBonus());
    }

    @Test
    void magiaSelvagemSubstitutesInstintoForFocoOnDominioDoMana() {
        InteractionResult result = new DominioDoManaInteraction().applyTo(sheet());

        // Instinto(11) + Graduação(4) = 15 — Foco(8) + 4 = 12 would mean the substitution didn't apply.
        assertEquals(15, result.getSkillRollBonus());
    }

    @Test
    void persuasaoIsUnaffectedByEveryOtherSkillsSubstitutingAbility() {
        InteractionResult result = new PersuasaoInteraction().applyTo(sheet());

        // Carisma(1, untouched default) + Graduação(0) = 1 — proves none of ACUIDADE/ACROBATA/
        // DISPARO_ARCANO/MAGIA_SELVAGEM (each scoped to a different Perícia) leaks here.
        assertEquals(1, result.getSkillRollBonus());
    }

    /**
     * PERITO_TEORICO's chosen Perícia is which {@code PeritoTeoricoAbility} constant a
     * character holds, not a fixed enum-constant-to-Perícia mapping like the other four
     * abilities above — so this Character is built separately rather than added to
     * {@link CharacterFixture#ATTRIBUTE_SUBSTITUTIONS}, picking Furtividade (Destreza by
     * default) as the chosen Perícia and Persuasão (Carisma) as the no-leak control.
     */
    private CharacterSheet peritoTeoricoSheet() {
        CharacterSkill furtividadeSkill = CharacterSkillFixture.blank(CharacterSkillFixture.FURTIVIDADE_1).build();
        furtividadeSkill.increaseGraduation(1);
        CharacterSkill persuasaoSkill = CharacterSkillFixture.blank(CharacterSkillFixture.PERSUASAO_1).build();

        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(2).build())
                        .gnose(AttributeValue.builder().domain(AttributeDomain.GNOSE).base(5).build())
                        .charisma(AttributeValue.builder().domain(AttributeDomain.CHARISMA).base(3).build())
                        .build())
                .skill(SkillType.FURTIVIDADE, furtividadeSkill)
                .skill(SkillType.PERSUASAO, persuasaoSkill)
                .attributeAbility(PeritoTeoricoAbility.FURTIVIDADE)
                .build();
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void peritoTeoricoSubstitutesGnoseForDestrezaOnTheChosenSkill() {
        InteractionResult result = new FurtividadeInteraction().applyTo(peritoTeoricoSheet());

        // Gnose(5) + Graduação(1) = 6 — Destreza(2) + 1 = 3 would mean the substitution didn't apply.
        assertEquals(6, result.getSkillRollBonus());
    }

    @Test
    void peritoTeoricoDoesNotLeakIntoAnUnchosenSkill() {
        InteractionResult result = new PersuasaoInteraction().applyTo(peritoTeoricoSheet());

        // Carisma(3) + Graduação(0) = 3 — proves PeritoTeoricoAbility.FURTIVIDADE doesn't leak into Persuasão.
        assertEquals(3, result.getSkillRollBonus());
    }
}
