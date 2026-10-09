package org.aventyrs.core.combat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.effect.EnrijecerMusculatura;
import org.aventyrs.core.feat.AssassinoFeat;
import org.aventyrs.core.feat.GorgonaFeat;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.senhordabriga.SenhorDaBriga;
import org.aventyrs.core.title.senhordabriga.SenhorDaBrigaSpecialization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@link AttackEffectsPreview} reports, before any roll, what {@link AttackDelivery} would chain on a hit. */
class AttackEffectsPreviewTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private static Character blank() {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>())
                .equipment(new ArrayList<>())
                .drawnWeapons(new ArrayList<>())
                .build();
    }

    @Test
    void aWeaponsOwnEfeitoCriticoIsReportedForTheCriticalOnly() {
        CharacterSheet sheet = CharacterSheet.of(blank(), new Player());

        AttackEffectsPreview preview = AttackEffectsPreview.of(sheet, SkillType.ATAQUE_CORPO_A_CORPO,
                NaturalWeapon.GARRAS_AFIADAS, null);

        assertEquals(List.of(CriticalEffectType.DILACERAR), preview.criticalEffects());
        assertTrue(preview.hitCriticalEffects().isEmpty(), "no Finalização, so a plain hit carries none");
        assertTrue(preview.effectChains().isEmpty());
    }

    /** Abrir Feridas: "Seus ataques recebem 'Sangramento' como Efeito Crítico adicional" — after the weapon's own. */
    @Test
    void aTalentosExtraEfeitoCriticoFollowsTheWeaponsOwn() {
        Character character = blank();
        character.grantFeat(AssassinoFeat.ABRIR_FERIDAS);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        AttackEffectsPreview preview = AttackEffectsPreview.of(sheet, SkillType.ATAQUE_CORPO_A_CORPO,
                NaturalWeapon.GARRAS_AFIADAS, null);

        assertEquals(List.of(CriticalEffectType.DILACERAR, CriticalEffectType.SANGRAMENTO), preview.criticalEffects());
        assertTrue(preview.hitCriticalEffects().isEmpty(), "still only on an Acerto Crítico");
    }

    @Test
    void aTitulosAdditionFollowsTheWeaponsOwn() {
        Character character = blank();
        character.grantTitle(new SenhorDaBriga(List.of(SenhorDaBrigaSpecialization.PUNHO_INIGUALAVEL), List.of()),
                TitleSlot.PRIMARY);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        AttackEffectsPreview preview = AttackEffectsPreview.of(sheet, SkillType.ATAQUE_CORPO_A_CORPO,
                NaturalWeapon.GARRAS_AFIADAS, null);

        assertEquals(List.of(CriticalEffectType.DILACERAR, CriticalEffectType.GUILHOTINA), preview.criticalEffects());
    }

    @Test
    void aTalentosCorrenteIsReportedOnlyForTheWeaponItScopesTo() {
        Character gorgona = blank();
        gorgona.grantFeat(GorgonaFeat.MARCA_DA_MALDICAO);
        CharacterSheet sheet = CharacterSheet.of(gorgona, new Player());

        assertTrue(AttackEffectsPreview.of(sheet, SkillType.ATAQUE_CORPO_A_CORPO, NaturalWeapon.PRESAS_LONGAS, null)
                .effectChains().stream().anyMatch(EnrijecerMusculatura.class::isInstance));
        assertTrue(AttackEffectsPreview.of(sheet, SkillType.ATAQUE_CORPO_A_CORPO, NaturalWeapon.GARRAS_AFIADAS, null)
                .effectChains().isEmpty());
    }
}
