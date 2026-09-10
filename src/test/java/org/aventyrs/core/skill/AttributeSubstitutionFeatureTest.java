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
import org.aventyrs.core.skill.attention.AttentionInteraction;
import org.aventyrs.core.skill.conhecimentos.ConhecimentosInteraction;
import org.aventyrs.core.skill.empatiaselvagem.EmpatiaSelvagemCompetencyAbility;
import org.aventyrs.core.skill.empatiaselvagem.EmpatiaSelvagemInteraction;
import org.aventyrs.core.skill.dominiodomana.DominioDoManaInteraction;
import org.aventyrs.core.skill.furtividade.FurtividadeInteraction;
import org.aventyrs.core.skill.persuasao.PersuasaoInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end feature test: a single Character carrying every currently-wired unconditional
 * base-Attribute-substituting Habilidade de Competência at once (see the
 * {@code ability-acquisition-and-substitution} skill) — {@code
 * AtaqueCorpoACorpoCompetencyAbility.ACUIDADE}, {@code AtletismoCompetencyAbility.ACROBATA},
 * {@code AtaqueADistanciaCompetencyAbility.DISPARO_ARCANO}, {@code
 * DominioDoManaCompetencyAbility.MAGIA_SELVAGEM}, {@code
 * AttentionCompetencyAbility.ALMA_DE_SHERLOCK}, {@code
 * PersuasaoCompetencyAbility.FORCA_OPRESSORA}, {@code
 * FurtividadeCompetencyAbility.LADINO_TEORICO} and {@code
 * EmpatiaSelvagemCompetencyAbility.ACADEMICO_SELVAGEM} — proving each one resolves
 * independently, without leaking into any other Perícia's roll.
 *
 * <p>There is no per-{@code <Skill>Interaction} wiring behind any of them: {@code
 * AbstractSkillInteraction#applyTo} resolves the substitution generically for every Perícia,
 * which is why each constant needed only its own {@code getSubstituteAttributeDomain()}
 * override. Conhecimentos is the no-leak control — the one Perícia here that names no
 * substituting Habilidade de Competência.
 */
class AttributeSubstitutionFeatureTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        // Needed by the two Characters this class assembles by hand rather than from
        // ATTRIBUTE_SUBSTITUTIONS; without it their CharacterSkillFixture labels aren't
        // registered and the class only passes when some other test class loaded them first.
        CharacterSkillFixture.loadTemplates();
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
    void almaDeSherlockSubstitutesGnoseForInstintoOnAtencao() {
        InteractionResult result = new AttentionInteraction().applyTo(sheet());

        // Gnose(7) + Graduação(1) = 8 — Instinto(11) + 1 = 12 would mean the substitution
        // didn't apply. Atenção is the one case where substituting *lowers* the roll; the
        // ability is unconditional, so it applies regardless (the rules offer no opt-out).
        assertEquals(8, result.getSkillRollBonus());
    }

    @Test
    void ladinoTeoricoSubstitutesGnoseForDestrezaOnFurtividade() {
        InteractionResult result = new FurtividadeInteraction().applyTo(sheet());

        // Gnose(7) + Graduação(2) = 9 — Destreza(5) + 2 = 7 would mean the substitution didn't apply.
        assertEquals(9, result.getSkillRollBonus());
    }

    @Test
    void forcaOpressoraSubstitutesForcaForCarismaOnPersuasao() {
        InteractionResult result = new PersuasaoInteraction().applyTo(sheet());

        // Força(2) + Graduação(0) = 2 — Carisma(1) + 0 = 1 would mean the substitution didn't apply.
        assertEquals(2, result.getSkillRollBonus());
    }

    @Test
    void academicoSelvagemSubstitutesGnoseForCarismaOnEmpatiaSelvagem() {
        InteractionResult result = new EmpatiaSelvagemInteraction().applyTo(sheet());

        // Gnose(7) + Graduação(1) = 8 — Carisma(1) + 1 = 2 would mean the substitution didn't apply.
        assertEquals(8, result.getSkillRollBonus());
    }

    /**
     * Empatia Selvagem is the one Perícia naming two unconditional substitutions of its own
     * (Gnose and Instinto). {@code SkillCompetencyAbility#resolveAttributeDomain} takes the
     * first match, since the rules state no precedence between them — so a character holding
     * only INSTINTO_ANIMAL rolls Instinto, and the pair is never assumed to combine.
     */
    @Test
    void instintoAnimalSubstitutesInstintoForCarismaOnEmpatiaSelvagem() {
        CharacterSkill skill = CharacterSkillFixture.blank(CharacterSkillFixture.EMPATIA_SELVAGEM_1).build();
        skill.increaseGraduation(2);
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .charisma(AttributeValue.builder().domain(AttributeDomain.CHARISMA).base(1).build())
                        .instinct(AttributeValue.builder().domain(AttributeDomain.INSTINCT).base(4).build())
                        .build())
                .skill(SkillType.EMPATIA_SELVAGEM, skill)
                .skillCompetencyAbility(EmpatiaSelvagemCompetencyAbility.INSTINTO_ANIMAL)
                .build();

        InteractionResult result = new EmpatiaSelvagemInteraction()
                .applyTo(CharacterSheet.of(character, new Player()));

        // Instinto(4) + Graduação(2) = 6 — Carisma(1) + 2 = 3 would mean the substitution didn't apply.
        assertEquals(6, result.getSkillRollBonus());
    }

    @Test
    void conhecimentosIsUnaffectedByEveryOtherSkillsSubstitutingAbility() {
        InteractionResult result = new ConhecimentosInteraction().applyTo(sheet());

        // Gnose(7, its own natural Attribute) + Graduação(0) = 7 — proves none of the eight
        // substituting abilities this character holds, each scoped to a different Perícia,
        // leaks into a Perícia that names none.
        assertEquals(7, result.getSkillRollBonus());
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
