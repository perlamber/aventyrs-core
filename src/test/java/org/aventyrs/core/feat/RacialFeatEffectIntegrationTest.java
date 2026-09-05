package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.Deity;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.character.services.DamageBaseService;
import org.aventyrs.core.character.services.DamageBaseServiceImpl;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.character.services.FeatService;
import org.aventyrs.core.character.services.FeatServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.MovementService;
import org.aventyrs.core.character.services.MovementServiceImpl;
import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.race.Anao;
import org.aventyrs.core.race.Aviano;
import org.aventyrs.core.race.AbstractMesticoRace;
import org.aventyrs.core.race.Bestial;
import org.aventyrs.core.race.Colosso;
import org.aventyrs.core.race.Elfo;
import org.aventyrs.core.race.Fada;
import org.aventyrs.core.race.Gigantes;
import org.aventyrs.core.race.Gnomo;
import org.aventyrs.core.race.Gorgona;
import org.aventyrs.core.race.HomemFera;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.race.MeioElfo;
import org.aventyrs.core.race.NascidoDoDragao;
import org.aventyrs.core.race.Invernal;
import org.aventyrs.core.race.Orc;
import org.aventyrs.core.race.Pequenino;
import org.aventyrs.core.race.Race;
import org.aventyrs.core.race.Satiro;
import org.aventyrs.core.race.Troll;
import org.aventyrs.core.race.Vampiro;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.skill.conhecimentos.Conhecimentos;
import org.aventyrs.core.skill.attention.Attention;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.TerrainType;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.attention.AttentionInteraction;
import org.aventyrs.core.skill.conhecimentos.ConhecimentosSpecialization;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.title.AventyrTitle;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.AventyrTitleSpecialization;
import org.aventyrs.core.title.TitleArchetype;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The clauses of the <b>racial</b> Talento trees that are mechanically real, each tested by the
 * effect it has on a character who legally acquired it — through {@link FeatService#grantFeat},
 * satisfying the Pré-requisito and paying the XP — and read back off the consuming service, never
 * off the hook. The general trees' equivalent is {@code GeneralFeatEffectIntegrationTest}.
 *
 * <p>Most racial Talentos are TODO'd catalog entries blocked on a missing system and have nothing
 * to observe. These are the exceptions, plus the {@code requiredRace} gate itself — which is
 * worth its own coverage here because the racial trees are the first to use it in bulk.
 */
class RacialFeatEffectIntegrationTest {

    private final FeatService featService = new FeatServiceImpl();
    private final HitPointsService hitPointsService = new HitPointsServiceImpl();
    private final DamageBaseService damageBaseService = new DamageBaseServiceImpl();
    private final AttentionInteraction attentionInteraction = new AttentionInteraction();
    private final DefenseService defenseService = new DefenseServiceImpl();
    private final DamageService damageService = new DamageServiceImpl();
    private final MovementService movementService = new MovementServiceImpl();
    private final ActionPointsService actionPointsService = new ActionPointsServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character.CharacterBuilder character() {
        return CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>());
    }

    /** A character of the Vampiro race — a Nosferatu (Humanoide in life), for the Vampírico tree. */
    private static Character.CharacterBuilder vampiro() {
        return character().race(new Vampiro(Vampiro.VampiroLineage.NOSFERATU, new Human()));
    }

    private static CharacterSheet fundedSheet(final Character character) {
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.accumulateExperience(BigDecimal.valueOf(100));
        return sheet;
    }

    private void acquire(final Character character, final Feat... feats) throws IllegalOperationException {
        CharacterSheet sheet = fundedSheet(character);
        for (Feat feat : feats) {
            featService.grantFeat(character, sheet, feat);
        }
    }

    // ---------- Anão ----------

    @Test
    void filhoDeYmirRaisesTheLifeMultiplierAndTheDanoBaseOfAWieldedWeaponButNotBareHands()
            throws IllegalOperationException {
        Character character = character()
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(4).build())
                        .build())
                .build();
        Weapon machado = AbstractWeapon.builder().name("Machado").category(ItemCategory.HEAVY_BLADE)
                .damageBase(DamageBase.of(2, 0)).skillType(SkillType.ATAQUE_CORPO_A_CORPO).build();
        int multiplierBefore = hitPointsService.getLifeMultiplier(character);

        acquire(character, AnaoFeat.FILHO_DE_YMIR);

        assertEquals(multiplierBefore + 1, hitPointsService.getLifeMultiplier(character));
        // "Dano Base de armas" — a wielded weapon scales up, a bare-handed strike does not.
        assertEquals(DamageBase.of(2, 1), damageBaseService.getDamageBase(character, machado));
        assertEquals(DamageBase.UNARMED,
                damageBaseService.getDamageBase(character, SkillType.ATAQUE_CORPO_A_CORPO));
    }

    /**
     * Its Pré-requisito is a bare "Vigor 4" — no race clause — so despite the Anão tag any race
     * that reaches the Atributo qualifies. Pinned because it looks like an omission.
     */
    @Test
    void filhoDeYmirIsOpenToAnyRaceBecauseItsPrerequisiteNamesNone() {
        Character human = character()
                .race(new Human())
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(4).build())
                        .build())
                .build();

        assertTrue(AnaoFeat.FILHO_DE_YMIR.isEligible(human));
    }

    @Test
    void filhoDeYmirStillNeedsItsVigor() {
        assertFalse(AnaoFeat.FILHO_DE_YMIR.isEligible(character().build()));
    }

    @Test
    void vigorDoInvernoRaisesTheLifeMultiplierByOne() throws IllegalOperationException {
        Character character = anaoWithVigorAndTitle(5);
        int multiplierBefore = hitPointsService.getLifeMultiplier(character);
        int hitPointsBefore = hitPointsService.getMaxHitPoints(character);

        acquire(character, AnaoFeat.VIGOR_DO_INVERNO);

        assertEquals(multiplierBefore + 1, hitPointsService.getLifeMultiplier(character));
        // The uplift is per point of Vigor, not a flat PV grant.
        assertEquals(hitPointsBefore + character.getAttributes().getVigor().getTotal(),
                hitPointsService.getMaxHitPoints(character));
    }

    @Test
    void vigorDoInvernoRefusesANonAnaoHoweverQualifiedOtherwise() {
        Character human = character().race(new Human())
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(5).build())
                        .build())
                .build();
        human.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);

        assertFalse(AnaoFeat.VIGOR_DO_INVERNO.isEligible(human));
        assertThrows(IllegalOperationException.class,
                () -> featService.grantFeat(human, fundedSheet(human), AnaoFeat.VIGOR_DO_INVERNO));
    }

    @Test
    void vigorDoInvernoRefusesAnAnaoWithNoTituloDesperto() {
        Character anao = character().race(new Anao())
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(5).build())
                        .build())
                .build();

        assertFalse(AnaoFeat.VIGOR_DO_INVERNO.isEligible(anao));
    }

    private static Character anaoWithVigorAndTitle(final int vigorBase) {
        Character character = character().race(new Anao())
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(vigorBase).build())
                        .build())
                .build();
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        return character;
    }

    // ---------- Aviano ----------

    @Test
    void visaoDaVerdadeTakesOneNivelOffEveryAtencaoRoll() throws IllegalOperationException {
        Character character = avianoWithTitle();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        int before = attentionInteraction.applyTo(sheet).getDifficultyReduction();

        acquire(character, AvianoFeat.VISAO_DA_VERDADE);

        InteractionResult result = attentionInteraction.applyTo(sheet);
        assertEquals(before + 1, result.getDifficultyReduction());
    }

    /**
     * The hook is scoped to one Perícia by the constant itself — {@code AbstractSkillInteraction}
     * applies no filter — so a Talento naming Atenção must not quietly ease every other roll.
     */
    @Test
    void visaoDaVerdadeDoesNotEaseAnotherPericiasRoll() throws IllegalOperationException {
        Character character = avianoWithTitle();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        int before = SkillType.FURTIVIDADE.newInteraction().applyTo(sheet).getDifficultyReduction();

        acquire(character, AvianoFeat.VISAO_DA_VERDADE);

        assertEquals(before, SkillType.FURTIVIDADE.newInteraction().applyTo(sheet).getDifficultyReduction());
    }

    @Test
    void everyAvianoTalentoRefusesANonAviano() {
        Character human = character().race(new Human()).build();
        human.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);

        assertFalse(AvianoFeat.CORACAO_ALADO.isEligible(human));
        assertFalse(AvianoFeat.VISAO_DA_VERDADE.isEligible(human));
    }

    @Test
    void eternoViajanteWaitsForCoracaoAlado() throws IllegalOperationException {
        Character character = avianoWithTitle();

        assertFalse(AvianoFeat.ETERNO_VIAJANTE.isEligible(character));

        acquire(character, AvianoFeat.CORACAO_ALADO);

        assertTrue(AvianoFeat.ETERNO_VIAJANTE.isEligible(character));
    }

    // ---------- Dracônico ----------

    @Test
    void asasDeDragaoGrantsTwoToBothDefesas() throws IllegalOperationException {
        Character character = character()
                .race(new NascidoDoDragao(new Human(), ElementalType.FOGO))
                .build();
        int physicalBefore = defenseService.getTotalDefense(character, DefenseType.PHYSICAL);
        int magicBefore = defenseService.getTotalDefense(character, DefenseType.MAGIC);

        acquire(character, DraconicoFeat.ASAS_DE_DRAGAO);

        // "Bônus de +2 as suas Defesas" is the broad form — both DF and DM.
        assertEquals(physicalBefore + 2, defenseService.getTotalDefense(character, DefenseType.PHYSICAL));
        assertEquals(magicBefore + 2, defenseService.getTotalDefense(character, DefenseType.MAGIC));
    }

    @Test
    void everyDraconicoTalentoRefusesANonNascidoDoDragao() {
        Character human = character().race(new Human()).build();

        assertFalse(DraconicoFeat.ASAS_DE_DRAGAO.isEligible(human));
        assertFalse(DraconicoFeat.ARMAMENTO_DRACONICO.isEligible(human));
    }

    // ---------- Órquico ----------

    @Test
    void terraNasVeiasScalesTheLifeMultiplierWithTitulosDespertos() throws IllegalOperationException {
        Character character = orcDevotoDeEpona(3);
        int before = hitPointsService.getLifeMultiplier(character);

        acquire(character, OrquicoFeat.TERRA_NAS_VEIAS);

        // One Título Desperto so far.
        assertEquals(before + 1, hitPointsService.getLifeMultiplier(character));

        // It recomputes live: a second Título is worth another point, with nothing to migrate.
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.SECONDARY);
        assertEquals(before + 2, hitPointsService.getLifeMultiplier(character));
    }

    @Test
    void terraNasVeiasGrantsNothingToAHolderWithNoTitulo() throws IllegalOperationException {
        Character character = character().race(new Orc())
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(3).build())
                        .build())
                .build();
        int before = hitPointsService.getLifeMultiplier(character);

        acquire(character, OrquicoFeat.TERRA_NAS_VEIAS);

        assertEquals(before, hitPointsService.getLifeMultiplier(character));
    }

    @Test
    void paladinoDeEponaRefusesAnOrcDevotedToAnotherDeity() {
        Character character = orcDevotoDeEpona(5).toBuilder().deity(Deity.YMIR).build();

        assertFalse(OrquicoFeat.PALADINO_DE_EPONA.isEligible(character));
    }

    @Test
    void paladinoDeEponaAcceptsAnOrcDevotedToEpona() {
        assertTrue(OrquicoFeat.PALADINO_DE_EPONA.isEligible(orcDevotoDeEpona(5)));
    }

    /**
     * Tremor's Efeito Passivo needs two Títulos, one of them Abençoado — Santo is the Abençoado
     * one, so a single Santo is not enough and two of them are.
     */
    @Test
    void tremorsPassiveHalfWaitsForTwoTitulosOneOfThemAbencoado() throws IllegalOperationException {
        Character character = orcDevotoDeEpona(5);
        acquire(character, OrquicoFeat.PALADINO_DE_EPONA, OrquicoFeat.TREMOR);
        int withOneTitulo = hitPointsService.getLifeMultiplier(character);

        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.SECONDARY);

        assertEquals(TitleArchetype.ABENCOADO, new Santo(List.of(), List.of()).getArchetype());
        assertEquals(withOneTitulo + 1, hitPointsService.getLifeMultiplier(character));
    }

    private static Character orcDevotoDeEpona(final int vigorBase) {
        Character character = character().race(new Orc())
                .deity(Deity.EPONA)
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(vigorBase).build())
                        .build())
                .build();
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        return character;
    }

    // ---------- Gnomo ----------

    @Test
    void duendeGrantsOneToDefesaMagicaOnly() throws IllegalOperationException {
        Character character = character().race(new Gnomo()).build();
        int physicalBefore = defenseService.getTotalDefense(character, DefenseType.PHYSICAL);
        int magicBefore = defenseService.getTotalDefense(character, DefenseType.MAGIC);

        acquire(character, GnomoFeat.DUENDE);

        // "Bônus de +1 na DM" is the narrow form — unlike Asas de Dragão's "suas Defesas".
        assertEquals(magicBefore + 1, defenseService.getTotalDefense(character, DefenseType.MAGIC));
        assertEquals(physicalBefore, defenseService.getTotalDefense(character, DefenseType.PHYSICAL));
    }

    @Test
    void favoritosDeTeslaTakesOneNivelOffEveryProfissaoRoll() throws IllegalOperationException {
        Character character = character().race(new Gnomo()).build();
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        int before = SkillType.PROFISSAO.newInteraction().applyTo(sheet).getDifficultyReduction();

        acquire(character, GnomoFeat.FAVORITOS_DE_TESLA);

        assertEquals(before + 1,
                SkillType.PROFISSAO.newInteraction().applyTo(sheet).getDifficultyReduction());
        // Scoped by the constant itself — Atenção must stay untouched.
        assertEquals(0, attentionInteraction.applyTo(sheet).getDifficultyReduction());
    }

    /**
     * Two Talentos reduce a Profissão roll's GD; only one of them qualifies. Favoritos de Tesla
     * eases every Profissão roll, while Engenheiro de Improvisos eases only rolls "para criar
     * equipamento" — a narrative purpose this core does not track, so it grants nothing rather
     * than easing every roll.
     */
    @Test
    void engenheiroDeImprovisosGrantsNoBlanketProfissaoReduction() {
        Character gnomo = character().race(new Gnomo()).build();

        assertEquals(1, GnomoFeat.FAVORITOS_DE_TESLA.resolveDifficultyReduction(SkillType.PROFISSAO, gnomo));
        assertEquals(0, GoblinFeat.ENGENHEIRO_DE_IMPROVISOS.resolveDifficultyReduction(SkillType.PROFISSAO, gnomo));
    }

    // ---------- Troll ----------

    /**
     * Vigor Tróllico is the first racial Talento gated on {@code requiredFeatCategory}: two other
     * Talentos of its own tree, with itself never counted among them.
     */
    @Test
    void vigorTrollicoWaitsForTwoOtherTrollTalentos() throws IllegalOperationException {
        Character troll = trollWithTitle();

        assertFalse(TrollFeat.VIGOR_TROLLICO.isEligible(troll));

        acquire(troll, TrollFeat.SONO_LEVE);
        assertFalse(TrollFeat.VIGOR_TROLLICO.isEligible(troll));

        acquire(troll, TrollFeat.REGENERACAO_REATIVA_SUPERIOR);
        assertTrue(TrollFeat.VIGOR_TROLLICO.isEligible(troll));
    }

    /**
     * Superior is declared before the two Talentos that name it, against the source document's
     * printed order, because Java forbids a forward reference between enum constants.
     */
    @Test
    void theRegeneracaoReativaChainOpensOneRungAtATime() throws IllegalOperationException {
        Character troll = trollWithTitle();

        assertFalse(TrollFeat.REGENERACAO_REATIVA_ESPINHOSA.isEligible(troll));
        assertFalse(TrollFeat.REGENERACAO_REATIVA_INVERNAL.isEligible(troll));

        acquire(troll, TrollFeat.REGENERACAO_REATIVA_SUPERIOR);

        assertTrue(TrollFeat.REGENERACAO_REATIVA_ESPINHOSA.isEligible(troll));
        assertTrue(TrollFeat.REGENERACAO_REATIVA_INVERNAL.isEligible(troll));
    }

    private static Character trollWithTitle() {
        Character character = character().race(new Troll())
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(3).build())
                        .instinct(AttributeValue.builder().domain(AttributeDomain.INSTINCT).base(3).build())
                        .build())
                .build();
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        return character;
    }

    // ---------- Mestiço ----------

    /**
     * Its "apenas personagens Mestiços" clause is the one racial Pré-requisito {@code
     * FeatRequirements} cannot express — there is no common Mestiço supertype for {@code
     * requiredRace} to name. Pinned in both directions so the looseness is deliberate and
     * visible, not discovered later as a bug.
     */
    @Test
    void caracteristicaRacialAdicionalIsOpenToNonMesticosBecauseTheClauseIsInexpressible() {
        assertTrue(MesticoFeat.CARACTERISTICA_RACIAL_ADICIONAL.isEligible(
                character().race(new MeioElfo(new Human())).build()));
        assertTrue(MesticoFeat.CARACTERISTICA_RACIAL_ADICIONAL.isEligible(
                character().race(new Human()).build()));
    }

    // ---------- Bestial ----------

    /**
     * Every Herança opens with "+1 de bônus racial em &lt;Atributo&gt;", now real through {@code
     * Feat#resolveAttributeBonus} — so a Herança-governed Perícia roll gets the +1 and a roll
     * governed by any other Atributo does not. Herança Bovídea grants Força, and Atletismo is
     * Força-governed.
     */
    @Test
    void herancaBovideaRaisesAForcaGovernedRollButNotACharismaGovernedOne() throws IllegalOperationException {
        Character bestial = character().race(new Bestial()).build();
        CharacterSheet sheet = CharacterSheet.of(bestial, new Player());
        int atletismoBefore = rollBonusIn(sheet, SkillType.ATLETISMO, null);
        int artesBefore = rollBonusIn(sheet, SkillType.ARTES, null);

        acquire(bestial, BestialFeat.HERANCA_BOVIDEA);

        assertEquals(atletismoBefore + 1, rollBonusIn(sheet, SkillType.ATLETISMO, null));
        assertEquals(artesBefore, rollBonusIn(sheet, SkillType.ARTES, null));
    }

    /** Each Herança grants +1 to its own fixed Atributo and to no other. */
    @Test
    void eachHerancaGrantsExactlyItsOwnAtributoBonus() {
        Character bestial = character().race(new Bestial()).build();
        Map<BestialFeat, AttributeDomain> expected = Map.of(
                BestialFeat.HERANCA_ANFIBIA, AttributeDomain.VIGOR,
                BestialFeat.HERANCA_AVIANA, AttributeDomain.FOCUS,
                BestialFeat.HERANCA_BOVIDEA, AttributeDomain.STRENGTH,
                BestialFeat.HERANCA_CANINA, AttributeDomain.INSTINCT,
                BestialFeat.HERANCA_CETACEA, AttributeDomain.GNOSE,
                BestialFeat.HERANCA_FELINA, AttributeDomain.DEXTERITY,
                BestialFeat.HERANCA_REPTILIANA, AttributeDomain.CHARISMA);

        expected.forEach((feat, granted) -> {
            for (AttributeDomain domain : AttributeDomain.values()) {
                assertEquals(domain == granted ? 1 : 0, feat.resolveAttributeBonus(domain, bestial),
                        feat + " should grant +1 to " + granted + " only");
            }
        });
        // The two Aventyr-tier Talentos grant no Atributo bonus.
        assertEquals(0, BestialFeat.ACEITAR_A_LACERTO.resolveAttributeBonus(AttributeDomain.INSTINCT, bestial));
        assertEquals(0, BestialFeat.METAMORFOSE_SELVAGEM.resolveAttributeBonus(AttributeDomain.STRENGTH, bestial));
    }

    /**
     * Herança Anfíbia's clause is "+1 de bônus racial em Vigor", and no Perícia is Vigor-governed
     * — so the bonus is computed for real yet currently reaches no roll. Pinned so the "compute it
     * even with no reader" discipline is visible rather than read later as a bug.
     */
    @Test
    void herancaAnfibiaComputesItsVigorBonusEvenThoughNoPericiaReadsIt() {
        Character bestial = character().race(new Bestial()).build();

        assertEquals(1, BestialFeat.HERANCA_ANFIBIA.resolveAttributeBonus(AttributeDomain.VIGOR, bestial));
    }

    /**
     * What the tree also delivers is the enforced ladder: Aceitar a Lacerto counts three held
     * Heranças, never itself.
     */
    @Test
    void aceitarALacertoWaitsForThreeHerancasBestiais() throws IllegalOperationException {
        Character bestial = bestialWithTitle();

        assertFalse(BestialFeat.ACEITAR_A_LACERTO.isEligible(bestial));

        acquire(bestial, BestialFeat.HERANCA_ANFIBIA, BestialFeat.HERANCA_BOVIDEA);
        assertFalse(BestialFeat.ACEITAR_A_LACERTO.isEligible(bestial));

        acquire(bestial, BestialFeat.HERANCA_FELINA);
        assertTrue(BestialFeat.ACEITAR_A_LACERTO.isEligible(bestial));
    }

    @Test
    void everyHerancaRefusesANonBestial() {
        Character human = character().race(new Human()).build();

        for (BestialFeat feat : BestialFeat.values()) {
            if (feat.getFeatRequirements().requiredRace() != null) {
                assertFalse(feat.isEligible(human), feat + " should refuse a non-Bestial");
            }
        }
    }

    /**
     * Aceitar a Lacerto is the one Bestial Talento gated on a count rather than on the race, so
     * a non-Bestial holding three Heranças would qualify — which cannot happen, because the
     * Heranças themselves are race-gated. Pinned so the indirection is visible.
     */
    @Test
    void aceitarALacertoIsGatedOnTheCountAndReachedOnlyThroughRaceGatedHerancas() {
        assertEquals(null, BestialFeat.ACEITAR_A_LACERTO.getFeatRequirements().requiredRace());
        assertEquals(FeatCategory.BESTIAL,
                BestialFeat.ACEITAR_A_LACERTO.getFeatRequirements().requiredFeatCategory());
        assertEquals(Bestial.class, BestialFeat.HERANCA_ANFIBIA.getFeatRequirements().requiredRace());
    }

    private static Character bestialWithTitle() {
        Character character = character().race(new Bestial()).build();
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        return character;
    }

    // ---------- Feral ----------

    @Test
    void theTransformacaoLadderOpensOneRungAtATime() throws IllegalOperationException {
        Character homemFera = homemFeraWithTitle();

        assertFalse(FeralFeat.TRANSFORMACAO_RAPIDA.isEligible(homemFera));
        assertFalse(FeralFeat.TRANSFORMACAO_DURADOURA.isEligible(homemFera));

        acquire(homemFera, FeralFeat.PRESAS_COM_DESTREZA_MANUAL, FeralFeat.BENCAO_DE_MAPINGUARI);
        assertTrue(FeralFeat.TRANSFORMACAO_RAPIDA.isEligible(homemFera));
        assertFalse(FeralFeat.TRANSFORMACAO_DURADOURA.isEligible(homemFera));

        acquire(homemFera, FeralFeat.DESPREZO_NATURAL);
        assertTrue(FeralFeat.TRANSFORMACAO_DURADOURA.isEligible(homemFera));
    }

    @Test
    void aspectoDasFerasNamesTransformacaoDuradouraDirectly() throws IllegalOperationException {
        Character homemFera = homemFeraWithTitle();
        acquire(homemFera, FeralFeat.PRESAS_COM_DESTREZA_MANUAL, FeralFeat.BENCAO_DE_MAPINGUARI,
                FeralFeat.DESPREZO_NATURAL);

        assertFalse(FeralFeat.ASPECTO_DAS_FERAS.isEligible(homemFera));

        acquire(homemFera, FeralFeat.TRANSFORMACAO_DURADOURA);

        assertTrue(FeralFeat.ASPECTO_DAS_FERAS.isEligible(homemFera));
    }

    /**
     * "Não pode ser usado em conjunto com" is an exclusion, and {@code FeatRequirements} carries
     * only thresholds that must be met. Pinned so the looseness is deliberate and visible.
     */
    @Test
    void theTwoTransformacaoTalentosAreNotMutuallyExclusiveBecauseExclusionsAreInexpressible()
            throws IllegalOperationException {
        Character homemFera = homemFeraWithTitle();
        acquire(homemFera, FeralFeat.PRESAS_COM_DESTREZA_MANUAL, FeralFeat.BENCAO_DE_MAPINGUARI,
                FeralFeat.DESPREZO_NATURAL, FeralFeat.TRANSFORMACAO_RAPIDA,
                FeralFeat.TRANSFORMACAO_DURADOURA);

        assertTrue(homemFera.getFeats().contains(FeralFeat.TRANSFORMACAO_RAPIDA));
        assertTrue(homemFera.getFeats().contains(FeralFeat.TRANSFORMACAO_DURADOURA));
    }

    private static Character homemFeraWithTitle() {
        Character character = character().race(new HomemFera(HomemFera.EspiritoAnimal.LICANTROPO))
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(3).build())
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(3).build())
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(3).build())
                        .build())
                .build();
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        return character;
    }

    // ---------- Elemental ----------

    /**
     * "Apenas personagens de Raças Elementais" is gated through {@link AbstractMesticoRace},
     * which is exactly the six Mestiços Elementais — {@code requiredRace} tests with {@code
     * isInstance}, so naming the base covers the whole family in one clause.
     */
    @Test
    void ganaElementalAcceptsEveryMesticoElementalAndNothingElse() {
        assertTrue(ElementalFeat.GANA_ELEMENTAL.isEligible(
                character().race(new Colosso(new Human())).build()));
        assertTrue(ElementalFeat.GANA_ELEMENTAL.isEligible(
                character().race(new Invernal(new Human())).build()));

        // A Mestiço that is not Elemental does not extend AbstractMesticoRace.
        assertFalse(ElementalFeat.GANA_ELEMENTAL.isEligible(
                character().race(new MeioElfo(new Human())).build()));
        assertFalse(ElementalFeat.GANA_ELEMENTAL.isEligible(
                character().race(new Human()).build()));
    }

    @Test
    void theElementalTreeHasTwoIndependentLaddersOffItsTwoEntryPoints() throws IllegalOperationException {
        Character elemental = character().race(new Colosso(new Human())).build();
        elemental.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        elemental.grantTitle(new Santo(List.of(), List.of()), TitleSlot.SECONDARY);

        assertFalse(ElementalFeat.ARCANISMO_ELEMENTAL.isEligible(elemental));
        assertFalse(ElementalFeat.RESISTENCIA_ELEMENTAL_SUPERIOR.isEligible(elemental));

        acquire(elemental, ElementalFeat.GANA_ELEMENTAL);
        assertTrue(ElementalFeat.ARCANISMO_ELEMENTAL.isEligible(elemental));
        assertTrue(ElementalFeat.GOLPE_CATACLISMICO.isEligible(elemental));
        assertFalse(ElementalFeat.RESISTENCIA_ELEMENTAL_SUPERIOR.isEligible(elemental));

        acquire(elemental, ElementalFeat.RESISTENCIA_ELEMENTAL);
        assertTrue(ElementalFeat.RESISTENCIA_ELEMENTAL_SUPERIOR.isEligible(elemental));
        assertTrue(ElementalFeat.REPARACAO_ELEMENTAL.isEligible(elemental));
        assertFalse(ElementalFeat.IMUNIDADE_ELEMENTAL.isEligible(elemental));

        acquire(elemental, ElementalFeat.RESISTENCIA_ELEMENTAL_SUPERIOR);
        assertTrue(ElementalFeat.IMUNIDADE_ELEMENTAL.isEligible(elemental));
    }

    /**
     * Transformação Elemental's Pré-requisito names two Talentos and {@code requiredFeat} is
     * singular, so only Resistência Elemental Superior is enforced. Pinned so the looseness is
     * deliberate and visible rather than found later as a bug.
     */
    @Test
    void transformacaoElementalEnforcesOnlyOneOfItsTwoRequiredTalentos() throws IllegalOperationException {
        Character elemental = character().race(new Colosso(new Human())).build();
        elemental.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        elemental.grantTitle(new Santo(List.of(), List.of()), TitleSlot.SECONDARY);
        acquire(elemental, ElementalFeat.RESISTENCIA_ELEMENTAL,
                ElementalFeat.RESISTENCIA_ELEMENTAL_SUPERIOR);

        // Reparação Elemental is never acquired, yet the gate opens.
        assertFalse(elemental.getFeats().contains(ElementalFeat.REPARACAO_ELEMENTAL));
        assertTrue(ElementalFeat.TRANSFORMACAO_ELEMENTAL.isEligible(elemental));
    }

    // ---------- Gigante ----------

    /**
     * Escudo Que Anda's {@code Descrição:} line opens with the bare text "Talento Zelo pelos
     * Frágeis" — a prerequisite that slipped into the description field in the source document.
     * Read as a prerequisite, which is what this pins.
     */
    @Test
    void escudoQueAndaReadsItsMisplacedDescriptionLineAsAPrerequisite() throws IllegalOperationException {
        Character gigante = character().race(new Gigantes()).build();
        gigante.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);

        assertFalse(GiganteFeat.ESCUDO_QUE_ANDA.isEligible(gigante));

        acquire(gigante, GiganteFeat.ZELO_PELOS_FRAGEIS);

        assertTrue(GiganteFeat.ESCUDO_QUE_ANDA.isEligible(gigante));
    }

    /**
     * Both Clã Talentos are withheld whole rather than half-implemented: Empusa's "-2 em suas
     * Defesas" is expressible today, but granting only the malus would leave a character
     * strictly worse off for acquiring the Talento.
     */
    @Test
    void neitherClaTalentoAppliesItsMalusWithoutItsBonuses() throws IllegalOperationException {
        Character gigante = character().race(new Gigantes()).build();
        gigante.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        int physicalBefore = defenseService.getTotalDefense(gigante, DefenseType.PHYSICAL);

        acquire(gigante, GiganteFeat.GIGANTE_DO_CLA_EMPUSA);

        assertEquals(physicalBefore, defenseService.getTotalDefense(gigante, DefenseType.PHYSICAL));
    }

    // ---------- Pequenino ----------

    /**
     * Each Linhagem's whole Pré-requisito is an exclusion of the other, and exclusions are
     * inexpressible — so a character can legally hold both, which the text forbids.
     */
    @Test
    void theTwoLinhagemTalentosAreNotMutuallyExclusiveBecauseExclusionsAreInexpressible()
            throws IllegalOperationException {
        Character pequenino = character().race(new Pequenino()).build();
        pequenino.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);

        acquire(pequenino, PequeninoFeat.LINHAGEM_DE_FLORA, PequeninoFeat.LINHAGEM_DE_LACERTO);

        assertTrue(pequenino.getFeats().contains(PequeninoFeat.LINHAGEM_DE_FLORA));
        assertTrue(pequenino.getFeats().contains(PequeninoFeat.LINHAGEM_DE_LACERTO));
    }

    /**
     * Silêncio Pré-Surpresa also says "GD reduzido em -1 nível", but unlike Favoritos de Tesla it
     * is gated on being unseen and carves out two Perícias — so it grants nothing.
     */
    @Test
    void silencioPreSurpresaGrantsNoBlanketDifficultyReduction() {
        Character pequenino = character().race(new Pequenino()).build();

        for (SkillType skillType : SkillType.values()) {
            assertEquals(0, PequeninoFeat.SILENCIO_PRE_SURPRESA
                    .resolveDifficultyReduction(skillType, pequenino));
        }
    }

    // ---------- Élfico ----------

    private static SceneContext terrain(final TerrainType terrainType) {
        return new SceneContext(List.of(), List.of(), Map.of(), terrainType);
    }

    private int rollBonusIn(final CharacterSheet sheet, final SkillType skillType,
                             final SceneContext sceneContext) {
        return skillType.newInteraction().applyTo(sheet, sceneContext, null).getSkillRollBonus();
    }

    @Test
    void guardiaoDosBosquesGrantsVantagemOnItsFourScopesWhileInAForest() throws IllegalOperationException {
        Character elfo = character().race(new Elfo()).build();
        CharacterSheet sheet = CharacterSheet.of(elfo, new Player());
        SceneContext forest = terrain(TerrainType.FOREST);
        int attackBefore = rollBonusIn(sheet, SkillType.ATAQUE_CORPO_A_CORPO, forest);
        int furtividadeBefore = rollBonusIn(sheet, SkillType.FURTIVIDADE, forest);
        int empatiaBefore = rollBonusIn(sheet, SkillType.EMPATIA_SELVAGEM, forest);

        acquire(elfo, ElficoFeat.GUARDIAO_DOS_BOSQUES);

        assertEquals(attackBefore + Skill.ADVANTAGE_BONUS,
                rollBonusIn(sheet, SkillType.ATAQUE_CORPO_A_CORPO, forest));
        assertEquals(furtividadeBefore + Skill.ADVANTAGE_BONUS,
                rollBonusIn(sheet, SkillType.FURTIVIDADE, forest));
        assertEquals(empatiaBefore + Skill.ADVANTAGE_BONUS,
                rollBonusIn(sheet, SkillType.EMPATIA_SELVAGEM, forest));
    }

    @Test
    void guardiaoDosBosquesGrantsNothingOutsideAForest() throws IllegalOperationException {
        Character elfo = character().race(new Elfo()).build();
        CharacterSheet sheet = CharacterSheet.of(elfo, new Player());
        int before = rollBonusIn(sheet, SkillType.FURTIVIDADE, terrain(TerrainType.DESERT));

        acquire(elfo, ElficoFeat.GUARDIAO_DOS_BOSQUES);

        assertEquals(before, rollBonusIn(sheet, SkillType.FURTIVIDADE, terrain(TerrainType.DESERT)));
        // A roll with no Scene at all must read as "condition not met", never as an error.
        assertEquals(before, rollBonusIn(sheet, SkillType.FURTIVIDADE, null));
    }

    @Test
    void guardiaoDasDunasIsTheSameClauseOnADifferentTerrain() throws IllegalOperationException {
        Character elfo = character().race(new Elfo()).build();
        CharacterSheet sheet = CharacterSheet.of(elfo, new Player());
        int before = rollBonusIn(sheet, SkillType.FURTIVIDADE, terrain(TerrainType.DESERT));

        acquire(elfo, ElficoFeat.GUARDIAO_DAS_DUNAS);

        assertEquals(before + Skill.ADVANTAGE_BONUS,
                rollBonusIn(sheet, SkillType.FURTIVIDADE, terrain(TerrainType.DESERT)));
        assertEquals(before, rollBonusIn(sheet, SkillType.FURTIVIDADE, terrain(TerrainType.FOREST)));
    }

    /**
     * "Conhecimentos: Natureza" is scoped through the roll's own {@code requestedAbility}, the
     * identical technique {@code AnoesRacialAbility#FILHOS_DA_MONTANHA} uses for the same clause
     * — so a plain Conhecimentos roll in a forest gets nothing.
     */
    @Test
    void guardiaoDosBosquesEasesConhecimentosOnlyWhenTheRollNamesNatureza() throws IllegalOperationException {
        Character elfo = character().race(new Elfo())
                .skill(SkillType.CONHECIMENTOS, trainedConhecimentosWithNatureza())
                .build();
        CharacterSheet sheet = CharacterSheet.of(elfo, new Player());
        SceneContext forest = terrain(TerrainType.FOREST);
        acquire(elfo, ElficoFeat.GUARDIAO_DOS_BOSQUES);

        int plain = SkillType.CONHECIMENTOS.newInteraction()
                .applyTo(sheet, forest, null).getSkillRollBonus();
        int naming = SkillType.CONHECIMENTOS.newInteraction()
                .applyTo(sheet, forest, new SkillRoll(List.of(1, 1, 1), ConhecimentosSpecialization.NATUREZA))
                .getSkillRollBonus();

        assertEquals(plain + Skill.ADVANTAGE_BONUS, naming);
    }

    @Test
    void sentidosAbsolutosTakesOneNivelOffEveryAtencaoRoll() throws IllegalOperationException {
        Character elfo = character().race(new Elfo())
                .attributes(CharacterAttributes.builder()
                        .instinct(AttributeValue.builder().domain(AttributeDomain.INSTINCT).base(3).build())
                        .build())
                .skill(SkillType.ATTENTION, trainedAttention())
                .build();
        elfo.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        CharacterSheet sheet = CharacterSheet.of(elfo, new Player());
        int before = attentionInteraction.applyTo(sheet).getDifficultyReduction();

        acquire(elfo, ElficoFeat.SENTIDOS_ABSOLUTOS);

        assertEquals(before + 1, attentionInteraction.applyTo(sheet).getDifficultyReduction());
    }

    // ---------- Feérico ----------

    @Test
    void ninfaGrantsVantagemOnEmpatiaSelvagemOnly() throws IllegalOperationException {
        Character fada = character().race(new Fada()).build();
        fada.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        CharacterSheet sheet = CharacterSheet.of(fada, new Player());
        int empatiaBefore = rollBonusIn(sheet, SkillType.EMPATIA_SELVAGEM, null);
        int artesBefore = rollBonusIn(sheet, SkillType.ARTES, null);

        acquire(fada, FeericoFeat.NINFA);

        assertEquals(empatiaBefore + Skill.ADVANTAGE_BONUS,
                rollBonusIn(sheet, SkillType.EMPATIA_SELVAGEM, null));
        assertEquals(artesBefore, rollBonusIn(sheet, SkillType.ARTES, null));
    }

    @Test
    void lupercalGrantsVantagemOnBothItsNamedPericias() throws IllegalOperationException {
        Character satiro = character().race(new Satiro()).build();
        CharacterSheet sheet = CharacterSheet.of(satiro, new Player());
        int artesBefore = rollBonusIn(sheet, SkillType.ARTES, null);
        int atencaoBefore = rollBonusIn(sheet, SkillType.ATTENTION, null);
        int persuasaoBefore = rollBonusIn(sheet, SkillType.PERSUASAO, null);

        acquire(satiro, FeericoFeat.LUPERCAL);

        assertEquals(artesBefore + Skill.ADVANTAGE_BONUS, rollBonusIn(sheet, SkillType.ARTES, null));
        assertEquals(atencaoBefore + Skill.ADVANTAGE_BONUS, rollBonusIn(sheet, SkillType.ATTENTION, null));
        // Persuasão is Fauno's scope, not Lupercal's.
        assertEquals(persuasaoBefore, rollBonusIn(sheet, SkillType.PERSUASAO, null));
    }

    /**
     * "Apenas personagens de raça Feérica" spans five race classes with no common supertype,
     * which is why {@code requiredCreatureType} exists.
     */
    @Test
    void feericoTalentosGateOnCreatureTypeRatherThanOnOneRaceClass() {
        assertTrue(FeericoFeat.NINFA.isEligible(feericoWithTitle(new Fada())));
        assertTrue(FeericoFeat.NINFA.isEligible(feericoWithTitle(new Satiro())));
        assertFalse(FeericoFeat.NINFA.isEligible(feericoWithTitle(new Human())));
    }

    private static Character feericoWithTitle(final Race race) {
        Character character = character().race(race).build();
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        return character;
    }

    // ---------- Anão, opposed-combatant clauses ----------

    /** A SceneContext naming who is on the other side of this roll. */
    private static SceneContext opposedBy(final CombatantSheet opposed) {
        return new SceneContext(List.of(), List.of(), Map.of(), null, true, 1, false, opposed);
    }

    private static CharacterSheet sheetOfSize(final SizeCategory size) {
        return CharacterSheet.of(character().sizeCategory(size).build(), new Player());
    }

    /**
     * Both Anão Talentos below gate on a Título Aventyr <b>Bruto</b>, and {@code Santo} — the only
     * concrete {@code AventyrTitle} in this core — is {@code ABENCOADO}. Same stub {@code
     * FeatRequirementsGateTest} uses for the identical reason.
     */
    private record BrutoTitle() implements AventyrTitle {
        @Override public String getName() { return "Bruto de Teste"; }
        @Override public TitleArchetype getArchetype() { return TitleArchetype.BRUTO; }
        @Override public String getBaseEffectDescription() { return ""; }
        @Override public List<AventyrTitleSpecialization> getSpecializations() { return List.of(); }
        @Override public List<AventyrTitleAbility> getAbilities() { return List.of(); }
        @Override public void grantAbility(final AventyrTitleAbility ability) { }
    }

    private static Character anaoBrutoWithVigor(final int vigorBase) {
        Character anao = character().race(new Anao())
                .sizeCategory(SizeCategory.MINUS_ONE)
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(vigorBase).build())
                        .build())
                .build();
        anao.grantTitle(new BrutoTitle(), TitleSlot.PRIMARY);
        return anao;
    }

    @Test
    void vantagemDeTamanhoGrantsHalfVigorOnlyAgainstALargerAttacker() throws IllegalOperationException {
        Character anao = anaoBrutoWithVigor(4);
        acquire(anao, AnaoFeat.VANTAGEM_DE_TAMANHO);

        assertEquals(2, defenseService.getTotalDefense(anao, DefenseType.PHYSICAL,
                opposedBy(sheetOfSize(SizeCategory.PLUS_TWO))));
        assertEquals(0, defenseService.getTotalDefense(anao, DefenseType.PHYSICAL,
                opposedBy(sheetOfSize(SizeCategory.MINUS_TWO))));
        // Equal size is not "superior à sua".
        assertEquals(0, defenseService.getTotalDefense(anao, DefenseType.PHYSICAL,
                opposedBy(sheetOfSize(SizeCategory.MINUS_ONE))));
    }

    @Test
    void vantagemDeTamanhoScalesWithTheHoldersOwnVigor() throws IllegalOperationException {
        Character weaker = anaoBrutoWithVigor(3);
        Character stronger = anaoBrutoWithVigor(5);
        acquire(weaker, AnaoFeat.VANTAGEM_DE_TAMANHO);
        acquire(stronger, AnaoFeat.VANTAGEM_DE_TAMANHO);
        SceneContext vsGiant = opposedBy(sheetOfSize(SizeCategory.PLUS_TWO));

        // "Metade do Vigor", rounded down.
        assertEquals(1, defenseService.getTotalDefense(weaker, DefenseType.PHYSICAL, vsGiant));
        assertEquals(2, defenseService.getTotalDefense(stronger, DefenseType.PHYSICAL, vsGiant));
    }

    /**
     * A Defesa asked for outside a roll has no attacker to compare against, so the clause grants
     * nothing rather than falling through to an unconditional bonus — which would hand it out
     * against the smaller opponents the text excludes.
     */
    @Test
    void vantagemDeTamanhoGrantsNothingWithNoSceneAndNoOpponent() throws IllegalOperationException {
        Character anao = anaoBrutoWithVigor(4);
        acquire(anao, AnaoFeat.VANTAGEM_DE_TAMANHO);

        assertEquals(0, defenseService.getTotalDefense(anao, DefenseType.PHYSICAL));
        assertEquals(0, defenseService.getTotalDefense(anao, DefenseType.PHYSICAL, opposedBy(null)));
    }

    /**
     * The SceneContext-aware overload *defaults* to the unconditional one rather than replacing
     * it, so every constant that overrides only the 2-arg form keeps working through the longer
     * call. Pinned because getting that direction wrong would silently zero four constants.
     */
    @Test
    void anUnconditionalDefenseBonusStillAppliesThroughTheSceneContextOverload() throws IllegalOperationException {
        Character monstro = vigorousCharacter(4);
        acquire(monstro, MonstruosoFeat.PELE_RIJA);
        SceneContext vsAnyone = opposedBy(sheetOfSize(SizeCategory.ZERO));

        assertEquals(defenseService.getTotalDefense(monstro, DefenseType.PHYSICAL),
                defenseService.getTotalDefense(monstro, DefenseType.PHYSICAL, vsAnyone));
        assertEquals(2, defenseService.getTotalDefense(monstro, DefenseType.PHYSICAL, vsAnyone));
    }

    @Test
    void gloriaYmirianaGrantsVantagemAgainstAnyTargetThatIsNotSmaller() throws IllegalOperationException {
        Character anao = anaoBrutoWithVigor(4);
        CharacterSheet self = CharacterSheet.of(anao, new Player());
        SceneContext vsEqual = opposedBy(sheetOfSize(SizeCategory.MINUS_ONE));
        SceneContext vsSmaller = opposedBy(sheetOfSize(SizeCategory.MINUS_TWO));
        int equalBefore = rollBonusIn(self, SkillType.ATAQUE_CORPO_A_CORPO, vsEqual);
        int smallerBefore = rollBonusIn(self, SkillType.ATAQUE_CORPO_A_CORPO, vsSmaller);

        acquire(anao, AnaoFeat.GLORIA_YMIRIANA);

        assertEquals(equalBefore + Skill.ADVANTAGE_BONUS,
                rollBonusIn(self, SkillType.ATAQUE_CORPO_A_CORPO, vsEqual));
        assertEquals(smallerBefore, rollBonusIn(self, SkillType.ATAQUE_CORPO_A_CORPO, vsSmaller));
    }

    @Test
    void gloriaYmirianaReachesBothAttackPericiasAndNoOthers() throws IllegalOperationException {
        Character anao = anaoBrutoWithVigor(4);
        CharacterSheet self = CharacterSheet.of(anao, new Player());
        SceneContext vsGiant = opposedBy(sheetOfSize(SizeCategory.PLUS_TWO));
        int meleeBefore = rollBonusIn(self, SkillType.ATAQUE_CORPO_A_CORPO, vsGiant);
        int rangedBefore = rollBonusIn(self, SkillType.ATAQUE_A_DISTANCIA, vsGiant);
        int furtividadeBefore = rollBonusIn(self, SkillType.FURTIVIDADE, vsGiant);

        acquire(anao, AnaoFeat.GLORIA_YMIRIANA);

        assertEquals(meleeBefore + Skill.ADVANTAGE_BONUS,
                rollBonusIn(self, SkillType.ATAQUE_CORPO_A_CORPO, vsGiant));
        assertEquals(rangedBefore + Skill.ADVANTAGE_BONUS,
                rollBonusIn(self, SkillType.ATAQUE_A_DISTANCIA, vsGiant));
        assertEquals(furtividadeBefore, rollBonusIn(self, SkillType.FURTIVIDADE, vsGiant));
    }

    /**
     * The Margem Crítica half is stricter than the Vantagem half — "maiores que você", not
     * merely "não menor" — so an equal-sized target gets the roll bonus but not the widened
     * margin. Two 5s only read as a critical once the margin is widened by 2.
     */
    @Test
    void gloriaYmirianaWidensTheCriticalMarginOnlyAgainstLargerTargets() throws IllegalOperationException {
        Character anao = anaoBrutoWithVigor(4);
        CharacterSheet self = CharacterSheet.of(anao, new Player());
        acquire(anao, AnaoFeat.GLORIA_YMIRIANA);
        SkillRoll twoFives = new SkillRoll(List.of(5, 5, 1));

        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR,
                SkillType.ATAQUE_CORPO_A_CORPO.newInteraction()
                        .applyTo(self, opposedBy(sheetOfSize(SizeCategory.PLUS_TWO)), twoFives)
                        .getCriticalResult());
        assertEquals(CriticalResult.NONE,
                SkillType.ATAQUE_CORPO_A_CORPO.newInteraction()
                        .applyTo(self, opposedBy(sheetOfSize(SizeCategory.MINUS_ONE)), twoFives)
                        .getCriticalResult());
    }

    /**
     * Feats and Habilidades Raciais stack — the whole progression is built on accumulating
     * bonuses. Against a target 2+ Categorias larger both Glória Ymiriana and the Anão's own
     * Abatedores de Gigantes apply, and the roll gets both.
     */
    @Test
    void gloriaYmirianaStacksWithAbatedoresDeGigantes() throws IllegalOperationException {
        Character anao = anaoBrutoWithVigor(4);
        CharacterSheet self = CharacterSheet.of(anao, new Player());
        CharacterSheet giant = sheetOfSize(SizeCategory.PLUS_TWO);
        CharacterSheet equal = sheetOfSize(SizeCategory.MINUS_ONE);

        // Abatedores alone, from the Race: it needs a 2+ category gap, so only the giant triggers it.
        int racialOnlyVsGiant = SkillType.ATAQUE_CORPO_A_CORPO.newInteraction()
                .applyTo(self, opposedBy(giant), null, giant).getSkillRollBonus();
        int racialOnlyVsEqual = SkillType.ATAQUE_CORPO_A_CORPO.newInteraction()
                .applyTo(self, opposedBy(equal), null, equal).getSkillRollBonus();
        assertEquals(racialOnlyVsEqual + Skill.ADVANTAGE_BONUS, racialOnlyVsGiant);

        acquire(anao, AnaoFeat.GLORIA_YMIRIANA);

        // Both now apply against the giant; only the Talento applies against the equal target.
        assertEquals(racialOnlyVsGiant + Skill.ADVANTAGE_BONUS,
                SkillType.ATAQUE_CORPO_A_CORPO.newInteraction()
                        .applyTo(self, opposedBy(giant), null, giant).getSkillRollBonus());
        assertEquals(racialOnlyVsEqual + Skill.ADVANTAGE_BONUS,
                SkillType.ATAQUE_CORPO_A_CORPO.newInteraction()
                        .applyTo(self, opposedBy(equal), null, equal).getSkillRollBonus());
    }

    private static CharacterSkill trainedConhecimentosWithNatureza() {
        return CharacterSkill.builder()
                .skill(new Conhecimentos())
                .graduation(SkillGraduation.builder().graduationValue(2).build())
                .specializations(List.of(ConhecimentosSpecialization.NATUREZA))
                .build();
    }

    private static CharacterSkill trainedAttention() {
        return CharacterSkill.builder()
                .skill(new Attention())
                .graduation(SkillGraduation.builder().graduationValue(1).build())
                .build();
    }

    // ---------- Górgona ----------

    /**
     * "RDS e RD" names one stat twice — RDS *is* Redução de Danos Sofridos — so it is read as a
     * single grant, and since the clause states no figure it uses the service's own default.
     */
    @Test
    void protecaoDoDeusDosMonstrosGrantsTheDefaultDamageReductionOnce() throws IllegalOperationException {
        Character gorgona = character().race(new Gorgona())
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(4).build())
                        .build())
                .build();
        int before = damageService.getTotalDamageReduction(gorgona);

        acquire(gorgona, GorgonaFeat.PROTECAO_DO_DEUS_DOS_MONSTROS);

        assertEquals(before + DamageService.DEFAULT_DAMAGE_REDUCTION,
                damageService.getTotalDamageReduction(gorgona));
    }

    /**
     * Cabelo Serpentino is withheld whole: its Desvantagem em Persuasão is expressible today, but
     * the Vantagem that pays for it is scoped to the unbuilt Olhar de Lacerto.
     */
    @Test
    void cabeloSerpentinoAppliesNoMalusWithoutItsPairedBonus() throws IllegalOperationException {
        Character gorgona = character().race(new Gorgona()).build();
        gorgona.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        CharacterSheet sheet = CharacterSheet.of(gorgona, new Player());
        int before = rollBonusIn(sheet, SkillType.PERSUASAO, null);

        acquire(gorgona, GorgonaFeat.CABELO_SERPENTINO);

        assertEquals(before, rollBonusIn(sheet, SkillType.PERSUASAO, null));
    }

    // ---------- Monstruoso ----------

    @Test
    void peleRijaGrantsTwoToDefesaFisicaAndTwoRD() throws IllegalOperationException {
        Character monstro = vigorousCharacter(4);
        int defenseBefore = defenseService.getTotalDefense(monstro, DefenseType.PHYSICAL);
        int magicBefore = defenseService.getTotalDefense(monstro, DefenseType.MAGIC);
        int rdBefore = damageService.getTotalDamageReduction(monstro);

        acquire(monstro, MonstruosoFeat.PELE_RIJA);

        assertEquals(defenseBefore + 2, defenseService.getTotalDefense(monstro, DefenseType.PHYSICAL));
        assertEquals(magicBefore, defenseService.getTotalDefense(monstro, DefenseType.MAGIC));
        assertEquals(rdBefore + 2, damageService.getTotalDamageReduction(monstro));
    }

    @Test
    void peleRijaReducesEveryHitByTwo() throws IllegalOperationException {
        Character monstro = vigorousCharacter(4);
        acquire(monstro, MonstruosoFeat.PELE_RIJA);

        assertEquals(8, damageService.calculateFinalDamage(monstro, 10, false));
        // RD, not RA — an attack that ignores RD skips it.
        assertEquals(10, damageService.calculateFinalDamage(monstro, 10, true));
    }

    /**
     * The catalog's first Talento to apply a real malus. Both sides of the trade the rules text
     * frames — lighter, therefore faster, therefore frailer — are expressible, which is what
     * distinguishes it from the Clã and Cabelo Serpentino Talentos withheld whole.
     */
    @Test
    void ossosOcosRaisesMovimentoAndLowersTheLifeMultiplier() throws IllegalOperationException {
        Character monstro = vigorousCharacter(3);
        int movementBefore = movementService.getMovementBase(monstro);
        int multiplierBefore = hitPointsService.getLifeMultiplier(monstro);
        int hitPointsBefore = hitPointsService.getMaxHitPoints(monstro);

        acquire(monstro, MonstruosoFeat.OSSOS_OCOS);

        assertEquals(movementBefore + 1, movementService.getMovementBase(monstro));
        assertEquals(multiplierBefore - 1, hitPointsService.getLifeMultiplier(monstro));
        // The malus is per point of Vigor, exactly like the bonus form.
        assertEquals(hitPointsBefore - monstro.getAttributes().getVigor().getTotal(),
                hitPointsService.getMaxHitPoints(monstro));
    }

    /**
     * Both scope their "+1 Dano Base" off the weapon, in opposite directions: Selvageria covers
     * Armas Naturais <em>only</em>, Filho de Ymir's "de armas" covers any wielded weapon but not
     * a bare-handed Ataque Desarmado.
     */
    @Test
    void selvageriaRaisesOnlyArmasNaturaisWhileFilhoDeYmirRaisesAnyWieldedWeapon() {
        Character monstro = vigorousCharacter(4);
        Weapon claws = AbstractWeapon.builder().name("Garras").category(ItemCategory.NATURAL_WEAPON)
                .damageBase(DamageBase.of(1, 0)).skillType(SkillType.ATAQUE_CORPO_A_CORPO).build();
        Weapon blade = AbstractWeapon.builder().name("Espada").category(ItemCategory.HEAVY_BLADE)
                .damageBase(DamageBase.of(2, 0)).skillType(SkillType.ATAQUE_CORPO_A_CORPO).build();

        assertEquals(1, MonstruosoFeat.SELVAGERIA.resolveDamageBaseIncrease(monstro, claws));
        assertEquals(0, MonstruosoFeat.SELVAGERIA.resolveDamageBaseIncrease(monstro, blade));
        assertEquals(0, MonstruosoFeat.SELVAGERIA.resolveDamageBaseIncrease(monstro, null));

        assertEquals(1, AnaoFeat.FILHO_DE_YMIR.resolveDamageBaseIncrease(monstro, claws));
        assertEquals(1, AnaoFeat.FILHO_DE_YMIR.resolveDamageBaseIncrease(monstro, blade));
        assertEquals(0, AnaoFeat.FILHO_DE_YMIR.resolveDamageBaseIncrease(monstro, null));
    }

    @Test
    void monstruosoTalentosGateOnCreatureTypeRatherThanOnOneRaceClass() {
        Character troll = character().race(new Troll()).build();
        troll.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        Character human = character().race(new Human()).build();
        human.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);

        assertTrue(MonstruosoFeat.FEROCIDADE.isEligible(troll));
        assertFalse(MonstruosoFeat.FEROCIDADE.isEligible(human));
    }

    private static Character vigorousCharacter(final int vigorBase) {
        return character().race(new Troll())
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(vigorBase).build())
                        .build())
                .build();
    }

    // ---------- Elemental, revisited ----------

    /**
     * Transformação Elemental's RDS became real once batch 7 added the RD hook — the permanent
     * transformation makes it unconditional, unlike every other RD clause in the catalog.
     */
    @Test
    void transformacaoElementalGrantsTheDefaultDamageReduction() throws IllegalOperationException {
        Character elemental = character().race(new Colosso(new Human())).build();
        elemental.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        elemental.grantTitle(new Santo(List.of(), List.of()), TitleSlot.SECONDARY);
        acquire(elemental, ElementalFeat.RESISTENCIA_ELEMENTAL,
                ElementalFeat.RESISTENCIA_ELEMENTAL_SUPERIOR);
        int before = damageService.getTotalDamageReduction(elemental);

        acquire(elemental, ElementalFeat.TRANSFORMACAO_ELEMENTAL);

        assertEquals(before + DamageService.DEFAULT_DAMAGE_REDUCTION,
                damageService.getTotalDamageReduction(elemental));
    }

    // ---------- Vampírico ----------

    /**
     * Every constant reads "Apenas personagens da Raça Vampiro", and {@code Vampiro} now exists —
     * so the whole tree gates on {@code requiredRace(Vampiro.class)}, and still not on {@code
     * requiredCreatureType} (a Vampiro's prerequisite type is its life-race's, not a single value).
     */
    @Test
    void everyVampiricoTalentoGatesOnTheVampiroRace() {
        for (VampiricoFeat feat : VampiricoFeat.values()) {
            assertEquals(Vampiro.class, feat.getFeatRequirements().requiredRace(),
                    feat + " must gate on the Vampiro race");
            assertEquals(null, feat.getFeatRequirements().requiredCreatureType(),
                    feat + " must not use CreatureType as a stand-in for the Vampiro race");
        }
    }

    /**
     * A Poder Vampírico grants nothing while merely <em>held</em> — its buff only exists once
     * {@code ActiveAbilityService#activate} enters the timed state. See {@code
     * PoderVampiricoActivationTest} for the activated side.
     */
    @Test
    void aHeldPoderVampiricoGrantsNothingUntilActivated() throws IllegalOperationException {
        Character vampiro = vampiro().build();
        int physicalBefore = defenseService.getTotalDefense(vampiro, DefenseType.PHYSICAL);
        int magicBefore = defenseService.getTotalDefense(vampiro, DefenseType.MAGIC);
        int movementBefore = movementService.getMovementBase(vampiro);
        int actionPointsBefore = actionPointsService.getMaxActionPoints(vampiro, 0);

        acquire(vampiro, VampiricoFeat.OSTEOMANCIA);

        assertEquals(physicalBefore, defenseService.getTotalDefense(vampiro, DefenseType.PHYSICAL));
        assertEquals(magicBefore, defenseService.getTotalDefense(vampiro, DefenseType.MAGIC));

        Character celere = vampiro().build();
        celere.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        acquire(celere, VampiricoFeat.CELERIDADE_VAMPIRICA);

        assertEquals(movementBefore, movementService.getMovementBase(celere));
        assertEquals(actionPointsBefore, actionPointsService.getMaxActionPoints(celere, 0));
    }

    @Test
    void poderVampiricoDuradouroWaitsForTwoOtherVampiricoTalentos() throws IllegalOperationException {
        Character vampiro = vampiro().build();

        assertFalse(VampiricoFeat.PODER_VAMPIRICO_DURADOURO.isEligible(vampiro));

        acquire(vampiro, VampiricoFeat.OSTEOMANCIA, VampiricoFeat.DOM_DE_MIRCALLA);

        assertTrue(VampiricoFeat.PODER_VAMPIRICO_DURADOURO.isEligible(vampiro));
    }

    /**
     * Abominação's entire Descrição line in the source document is the single character "V".
     * Recorded as a named placeholder — the catalog integrity test requires a non-blank
     * description, and pretending the text exists would be worse than saying it is missing.
     */
    @Test
    void abominacaoRecordsItsMissingSourceDescription() {
        assertTrue(VampiricoFeat.ABOMINACAO.getDescription().contains("ausente"));
    }

    private static Character avianoWithTitle() {
        Character character = character().race(new Aviano(Aviano.Subtipo.RAPINANTE)).build();
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        return character;
    }
}
