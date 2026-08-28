package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.conhecimentos.Conhecimentos;
import org.aventyrs.core.skill.dominiodomana.DominioDoMana;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetamagicoFeatTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSkill trained(final Skill skill, final int graduationValue) {
        return CharacterSkill.builder()
                .skill(skill)
                .graduation(SkillGraduation.builder().graduationValue(graduationValue).build())
                .build();
    }

    private static Character.CharacterBuilder character() {
        return CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>());
    }

    private static Character withConhecimentos(final int graduation) {
        return character().skill(SkillType.CONHECIMENTOS, trained(new Conhecimentos(), graduation)).build();
    }

    private static Character withDominioDoMana(final int graduation) {
        return character().skill(SkillType.DOMINIO_DO_MANA, trained(new DominioDoMana(), graduation)).build();
    }

    private static Character withInstinct(final int base) {
        return character()
                .attributes(CharacterAttributes.builder()
                        .instinct(AttributeValue.builder().domain(AttributeDomain.INSTINCT).base(base).build())
                        .build())
                .build();
    }

    // ---------- catalog shape ----------

    @Test
    void everyConstantBelongsToTheMetamagicoTree() {
        for (MetamagicoFeat feat : MetamagicoFeat.values()) {
            assertEquals(FeatCategory.METAMAGICO, feat.getFeatCategory());
        }
    }

    @Test
    void everyConstantHasADescriptionAndRequirements() {
        for (MetamagicoFeat feat : MetamagicoFeat.values()) {
            assertFalse(feat.getDescription().isBlank(), feat.name());
            assertNotNull(feat.getFeatRequirements(), feat.name());
        }
    }

    @Test
    void theTreeHasThirteenTalentos() {
        assertEquals(13, MetamagicoFeat.values().length);
    }

    @Test
    void everyPrerequisiteTalentoIsDeclaredBeforeItsDependent() {
        List<MetamagicoFeat> declared = List.of(MetamagicoFeat.values());
        for (MetamagicoFeat feat : declared) {
            Feat required = feat.getFeatRequirements().requiredFeat();
            if (required != null) {
                assertTrue(declared.indexOf(required) < declared.indexOf(feat),
                        feat.name() + " must be declared after its prerequisite");
            }
        }
    }

    // ---------- the cap ladder ----------

    @Test
    void everyRungOfTheLadderRaisesTheCapByExactlyOne() {
        Character any = character().build();

        assertEquals(1, MetamagicoFeat.ARCANISTA.resolveBranchLevelIncrease(any));
        assertEquals(1, MetamagicoFeat.ARCANISTA_EXPERIENTE.resolveBranchLevelIncrease(any));
        assertEquals(1, MetamagicoFeat.MESTRE_ARCANISTA.resolveBranchLevelIncrease(any));
        assertEquals(1, MetamagicoFeat.DESAFIADOR_DA_REALIDADE.resolveBranchLevelIncrease(any));
    }

    @Test
    void noOtherTalentoRaisesTheCap() {
        Character any = character().build();
        List<MetamagicoFeat> rungs = List.of(MetamagicoFeat.ARCANISTA,
                MetamagicoFeat.ARCANISTA_EXPERIENTE, MetamagicoFeat.MESTRE_ARCANISTA,
                MetamagicoFeat.DESAFIADOR_DA_REALIDADE);

        for (MetamagicoFeat feat : MetamagicoFeat.values()) {
            if (!rungs.contains(feat)) {
                assertEquals(0, feat.resolveBranchLevelIncrease(any), feat.name());
            }
        }
    }

    @Test
    void arcanistaAloneReachesBroto() {
        int rungs = MetamagicoFeat.ARCANISTA.resolveBranchLevelIncrease(character().build());

        assertEquals(BranchLevel.BROTO, BranchLevel.SEMENTE.advancedBy(rungs));
    }

    // ---------- ARCANISTA's DM bonus ----------

    @Test
    void arcanistaGrantsHalfTheDominioDoManaGraduationAsMagicDefense() {
        assertEquals(3, MetamagicoFeat.ARCANISTA.resolveDefenseBonus(DefenseType.MAGIC, withDominioDoMana(6)));
    }

    @Test
    void arcanistaRoundsTheMagicDefenseBonusDown() {
        assertEquals(3, MetamagicoFeat.ARCANISTA.resolveDefenseBonus(DefenseType.MAGIC, withDominioDoMana(7)));
        assertEquals(0, MetamagicoFeat.ARCANISTA.resolveDefenseBonus(DefenseType.MAGIC, withDominioDoMana(1)));
    }

    @Test
    void arcanistaGrantsNoPhysicalDefense() {
        assertEquals(0, MetamagicoFeat.ARCANISTA.resolveDefenseBonus(DefenseType.PHYSICAL, withDominioDoMana(6)));
    }

    @Test
    void anUntrainedDominioDoManaGrantsNoMagicDefense() {
        assertEquals(0, MetamagicoFeat.ARCANISTA.resolveDefenseBonus(DefenseType.MAGIC, character().build()));
    }

    @Test
    void noOtherTalentoGrantsADefenseBonus() {
        Character caster = withDominioDoMana(6);
        for (MetamagicoFeat feat : MetamagicoFeat.values()) {
            if (feat != MetamagicoFeat.ARCANISTA) {
                assertEquals(0, feat.resolveDefenseBonus(DefenseType.MAGIC, caster), feat.name());
                assertEquals(0, feat.resolveDefenseBonus(DefenseType.PHYSICAL, caster), feat.name());
            }
        }
    }

    // ---------- MENTE_EXPANDIDA ----------

    @Test
    void menteExpandidaRaisesTheManaMultiplierByOne() {
        assertEquals(1, MetamagicoFeat.MENTE_EXPANDIDA.resolveManaMultiplierIncrease(character().build()));
    }

    @Test
    void menteExpandidaRecoversTwoExtraManaOnEveryKindOfDescanso() {
        Character titleless = character().build();

        for (RestType restType : RestType.values()) {
            assertEquals(2, MetamagicoFeat.MENTE_EXPANDIDA.resolveRestMagicPointsBonus(restType, titleless),
                    restType.name());
        }
    }

    @Test
    void menteExpandidaRecoversOneMoreManaPerDespertoTitle() {
        Character oneTitle = character().primaryTitle(new Santo(List.of(), List.of())).build();

        assertEquals(3, MetamagicoFeat.MENTE_EXPANDIDA.resolveRestMagicPointsBonus(RestType.LONGO, oneTitle));
    }

    @Test
    void noOtherTalentoTouchesManaMultiplierOrRestRecovery() {
        Character any = character().build();
        for (MetamagicoFeat feat : MetamagicoFeat.values()) {
            if (feat != MetamagicoFeat.MENTE_EXPANDIDA) {
                assertEquals(0, feat.resolveManaMultiplierIncrease(any), feat.name());
                assertEquals(0, feat.resolveRestMagicPointsBonus(RestType.LONGO, any), feat.name());
            }
        }
    }

    // ---------- prerequisites ----------

    @Test
    void arcanistaNeedsTrainingInConhecimentos() {
        assertFalse(MetamagicoFeat.ARCANISTA.isEligible(character().build()));
        assertTrue(MetamagicoFeat.ARCANISTA.isEligible(withConhecimentos(1)));
    }

    @Test
    void arcanistaExperienteNeedsArcanistaItself() {
        assertFalse(MetamagicoFeat.ARCANISTA_EXPERIENTE.isEligible(withConhecimentos(3)));
    }

    @Test
    void arcanistaExperienteNeedsThreeGraduacoes() {
        Character shortOfGraduations = withConhecimentos(2);
        shortOfGraduations.grantFeat(MetamagicoFeat.ARCANISTA);

        assertFalse(MetamagicoFeat.ARCANISTA_EXPERIENTE.isEligible(shortOfGraduations));
    }

    @Test
    void arcanistaExperienteIsEligibleOnceBothAreMet() {
        Character ready = withConhecimentos(3);
        ready.grantFeat(MetamagicoFeat.ARCANISTA);

        assertTrue(MetamagicoFeat.ARCANISTA_EXPERIENTE.isEligible(ready));
    }

    @Test
    void aptidaoMagicaAmplaNeedsTheDominioDoManaTraining() {
        assertFalse(MetamagicoFeat.APTIDAO_MAGICA_AMPLA.isEligible(withInstinct(4)));
    }

    @Test
    void aptidaoMagicaAmplaNeedsTheInstinctFloor() {
        assertFalse(MetamagicoFeat.APTIDAO_MAGICA_AMPLA.isEligible(withDominioDoMana(1)));
    }

    @Test
    void aptidaoMagicaAmplaIsEligibleOnceBothAreMet() {
        Character ready = character()
                .attributes(CharacterAttributes.builder()
                        .instinct(AttributeValue.builder().domain(AttributeDomain.INSTINCT).base(4).build())
                        .build())
                .skill(SkillType.DOMINIO_DO_MANA, trained(new DominioDoMana(), 1))
                .build();

        assertTrue(MetamagicoFeat.APTIDAO_MAGICA_AMPLA.isEligible(ready));
    }

    @Test
    void theChainedTalentosEachNameTheirPredecessor() {
        assertEquals(MetamagicoFeat.ARCANISTA,
                MetamagicoFeat.ARCANISTA_EXPERIENTE.getFeatRequirements().requiredFeat());
        assertEquals(MetamagicoFeat.ARCANISTA_EXPERIENTE,
                MetamagicoFeat.MESTRE_ARCANISTA.getFeatRequirements().requiredFeat());
        assertEquals(MetamagicoFeat.MESTRE_ARCANISTA,
                MetamagicoFeat.DESAFIADOR_DA_REALIDADE.getFeatRequirements().requiredFeat());
        assertEquals(MetamagicoFeat.CONJURACAO_RAPIDA,
                MetamagicoFeat.PROCRASTINAR_CONJURACAO.getFeatRequirements().requiredFeat());
        assertEquals(MetamagicoFeat.ARCANISTA_EXPERIENTE,
                MetamagicoFeat.ARTESAO_DE_BARREIRAS.getFeatRequirements().requiredFeat());
        assertEquals(MetamagicoFeat.CONJURACAO_RAPIDA,
                MetamagicoFeat.ARMAZENAR_MAGIA.getFeatRequirements().requiredFeat());
        assertEquals(MetamagicoFeat.APTIDAO_MAGICA_AMPLA,
                MetamagicoFeat.APTIDAO_MAGICA_ASSOMBROSA.getFeatRequirements().requiredFeat());
        assertEquals(MetamagicoFeat.APTIDAO_MAGICA_ASSOMBROSA,
                MetamagicoFeat.APTIDAO_MAGICA_SUPREMA.getFeatRequirements().requiredFeat());
        assertEquals(MetamagicoFeat.MENTE_EXPANDIDA,
                MetamagicoFeat.ENGENHEIRO_DO_MANA.getFeatRequirements().requiredFeat());
    }

    /** The four cap rungs form one unbroken chain, so none can be acquired out of order. */
    @Test
    void theCapLadderIsAnUnbrokenChain() {
        assertNull(MetamagicoFeat.ARCANISTA.getFeatRequirements().requiredFeat());
        assertEquals(MetamagicoFeat.ARCANISTA,
                MetamagicoFeat.ARCANISTA_EXPERIENTE.getFeatRequirements().requiredFeat());
        assertEquals(MetamagicoFeat.ARCANISTA_EXPERIENTE,
                MetamagicoFeat.MESTRE_ARCANISTA.getFeatRequirements().requiredFeat());
        assertEquals(MetamagicoFeat.MESTRE_ARCANISTA,
                MetamagicoFeat.DESAFIADOR_DA_REALIDADE.getFeatRequirements().requiredFeat());
    }

    @Test
    void mestreArcanistaNeedsArcanistaExperienteAndSixGraduacoes() {
        assertFalse(MetamagicoFeat.MESTRE_ARCANISTA.isEligible(withConhecimentos(6)), "Talento missing");

        Character shortOfGraduations = withConhecimentos(5);
        shortOfGraduations.grantFeat(MetamagicoFeat.ARCANISTA_EXPERIENTE);
        assertFalse(MetamagicoFeat.MESTRE_ARCANISTA.isEligible(shortOfGraduations), "Graduações missing");

        Character ready = withConhecimentos(6);
        ready.grantFeat(MetamagicoFeat.ARCANISTA_EXPERIENTE);
        assertTrue(MetamagicoFeat.MESTRE_ARCANISTA.isEligible(ready));
    }
}
