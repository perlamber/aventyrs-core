package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.AnaoFeat;
import org.aventyrs.core.feat.ArtificeFeat;
import org.aventyrs.core.feat.DestinoFeat;
import org.aventyrs.core.feat.ElficoFeat;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.FeatCatalog;
import org.aventyrs.core.feat.FeatCategory;
import org.aventyrs.core.feat.FeatPool;
import org.aventyrs.core.feat.FeericoFeat;
import org.aventyrs.core.feat.MobilidadeFeat;
import org.aventyrs.core.feat.MonstruosoFeat;
import org.aventyrs.core.feat.PeritoFeat;
import org.aventyrs.core.feat.StartingFeatSlot;
import org.aventyrs.core.feat.VampiricoFeat;
import org.aventyrs.core.race.Agastias;
import org.aventyrs.core.race.Anao;
import org.aventyrs.core.race.Aquan;
import org.aventyrs.core.race.Aviano;
import org.aventyrs.core.race.Bestial;
import org.aventyrs.core.race.Colosso;
import org.aventyrs.core.race.Dolos;
import org.aventyrs.core.race.Elfo;
import org.aventyrs.core.race.Fada;
import org.aventyrs.core.race.Flaminideo;
import org.aventyrs.core.race.Furia;
import org.aventyrs.core.race.Gigantes;
import org.aventyrs.core.race.Gnomo;
import org.aventyrs.core.race.Goblin;
import org.aventyrs.core.race.Gorgona;
import org.aventyrs.core.race.Guampo;
import org.aventyrs.core.race.HomemFera;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.race.Indomito;
import org.aventyrs.core.race.Invernal;
import org.aventyrs.core.race.MeioElfo;
import org.aventyrs.core.race.NascidoDaFloresta;
import org.aventyrs.core.race.NascidoDoDragao;
import org.aventyrs.core.race.Ogro;
import org.aventyrs.core.race.Orc;
import org.aventyrs.core.race.Pequenino;
import org.aventyrs.core.race.Race;
import org.aventyrs.core.race.Satiro;
import org.aventyrs.core.race.Troll;
import org.aventyrs.core.race.Vampiro;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.profissao.Profissao;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.aventyrs.core.util.TranslatableMessages.FEAT_REQUIRES_CHOICE;
import static org.aventyrs.core.util.TranslatableMessages.INVALID_STARTING_FEAT_SELECTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterCreationServiceStartingFeatsTest {

    private final CharacterCreationService creationService = new CharacterCreationServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character.CharacterBuilder characterOf(final Race race) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .race(race)
                .feats(new ArrayList<>())
                .mimetizedSpells(new ArrayList<>());
    }

    /** One Título Desperto and Vigor 5 — what the racial Talentos used below ask beyond their Raça. */
    private static Character.CharacterBuilder awakenedCharacterOf(final Race race) {
        return characterOf(race)
                .primaryTitle(new Santo(List.of(), List.of()))
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(5).build())
                        .build());
    }

    private StartingFeatSlot raceSlot(final Race race, final int index) {
        return creationService.getStartingFeatSlots(race).get(CharacterCreationService.DEFAULT_GENERAL_FEAT_SLOTS + index);
    }

    // ---- slot shapes ------------------------------------------------------------------------

    @Test
    void everyCharacterStartsWithTwoGeneralSlotsAndGigantesGetNothingMore() {
        List<StartingFeatSlot> slots = creationService.getStartingFeatSlots(new Gigantes());

        assertEquals(2, slots.size());
        assertTrue(slots.stream().allMatch(slot -> slot.source() == StartingFeatSlot.Source.DEFAULT));
        assertTrue(slots.get(0).pools().get(0).admits(DestinoFeat.AUTOCONHECIMENTO));
        assertFalse(slots.get(0).pools().get(0).admits(AnaoFeat.FILHO_DE_YMIR), "a racial tree isn't General");
    }

    @Test
    void humansGetTwoGeneralSlotsOnTopOfTheDefaultTwo() {
        List<StartingFeatSlot> slots = creationService.getStartingFeatSlots(new Human());

        assertEquals(4, slots.size());
        assertEquals(2, slots.stream().filter(slot -> slot.source() == StartingFeatSlot.Source.RACE).count());
    }

    @Test
    void dolosAndFlaminideosReadTheElementalTalentoAsAThirdSlot() {
        assertEquals(5, creationService.getStartingFeatSlots(new Dolos(new Human())).size());
        assertEquals(5, creationService.getStartingFeatSlots(new Flaminideo(new Human())).size());
    }

    @Test
    void anaoRaceSlotOffersOnlySobrevivenciaAndDestino() {
        Character anao = characterOf(new Anao()).build();

        List<Feat> options = creationService.getStartingFeatOptions(anao, raceSlot(new Anao(), 0));

        assertTrue(options.contains(DestinoFeat.AUTOCONHECIMENTO));
        assertFalse(options.contains(MobilidadeFeat.MOVIMENTO_RAPIDO));
        assertTrue(options.stream().allMatch(feat -> feat.getFeatCategory() == FeatCategory.SOBREVIVENCIA
                || feat.getFeatCategory() == FeatCategory.DESTINO));
    }

    @Test
    void satiroSlotExcludesPixieAndAsas() {
        FeatPool pool = raceSlot(new Satiro(), 0).pools().get(0);

        assertFalse(pool.admits(FeericoFeat.PIXIE));
        assertFalse(pool.admits(FeericoFeat.ASAS));
        assertTrue(pool.admits(FeericoFeat.NINFA));
    }

    @Test
    void avianosAreOfferedOssosOcosAsASingleOptionSlot() {
        Character aviano = characterOf(new Aviano(Aviano.Subtipo.RAPINANTE)).build();

        assertEquals(List.of(MonstruosoFeat.OSSOS_OCOS), creationService.getStartingFeatOptions(aviano, raceSlot(new Aviano(Aviano.Subtipo.RAPINANTE), 1)));
    }

    @Test
    void homemFeraUpbringingPicksTheTreeOfItsFirstSlot() {
        HomemFera amongElves = new HomemFera(HomemFera.EspiritoAnimal.WEMIC, HomemFera.Criacao.ELFOS);
        HomemFera amongHumans = new HomemFera(HomemFera.EspiritoAnimal.WEMIC, HomemFera.Criacao.HUMANOS);

        assertTrue(raceSlot(amongElves, 0).pools().get(0).admits(ElficoFeat.GUARDIAO_DOS_BOSQUES));
        assertFalse(raceSlot(amongElves, 0).pools().get(0).admits(MobilidadeFeat.ESQUIVA));
        assertTrue(raceSlot(amongHumans, 0).pools().get(0).admits(MobilidadeFeat.ESQUIVA));
        assertTrue(raceSlot(amongHumans, 1).pools().get(0).categories().contains(FeatCategory.SOBREVIVENCIA));
    }

    @Test
    void meioElfoGetsAnExtraSlotOfTheParentsKindOnlyWhenTheParentHasOne() {
        assertEquals(2 + 1 + 1, creationService.getStartingFeatSlots(new MeioElfo(new Human())).size());
        assertEquals(2 + 1, creationService.getStartingFeatSlots(new MeioElfo(new Gigantes())).size());

        StartingFeatSlot humanKind = raceSlot(new MeioElfo(new Human()), 1);
        assertTrue(humanKind.pools().stream().anyMatch(pool -> pool.admits(MobilidadeFeat.ESQUIVA)));
    }

    @Test
    void meioElfoJudgesTheParentsRacialTreeAgainstTheParent() {
        MeioElfo elfParented = new MeioElfo(new Elfo());
        Character character = awakenedCharacterOf(elfParented).build();

        List<Feat> options = creationService.getStartingFeatOptions(character, raceSlot(elfParented, 1));

        assertTrue(options.contains(ElficoFeat.ALMA_FEERICA), "requiredRace(Elfo) holds for the Elfo parent");
    }

    // ---- judged against another race ---------------------------------------------------------

    @Test
    void agastiasEspecialistaSlotOffersEspecialistaTaggedTalentosOnly() {
        Agastias agastias = new Agastias(new Human(), Agastias.Linhagem.VULCANO);
        CharacterSkill profissao = CharacterSkill.builder()
                .skill(new Profissao())
                .graduation(SkillGraduation.builder().graduationValue(7).build())
                .build();
        Character character = characterOf(agastias).skill(SkillType.PROFISSAO, profissao).build();

        List<Feat> options = creationService.getStartingFeatOptions(character, raceSlot(agastias, 0));

        assertTrue(options.contains(ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR));
        assertTrue(options.stream().allMatch(Feat::isEspecialistaTagged), "a Human parent offers no substitute");
    }

    @Test
    void mesticoParentWithRacialTalentosOffersThemAsASubstitute() {
        Agastias halfElf = new Agastias(new Elfo(), Agastias.Linhagem.VULCANO);
        Agastias halfDwarf = new Agastias(new Anao(), Agastias.Linhagem.VULCANO);

        List<Feat> elfOptions = creationService.getStartingFeatOptions(awakenedCharacterOf(halfElf).build(), raceSlot(halfElf, 0));
        List<Feat> dwarfOptions = creationService.getStartingFeatOptions(awakenedCharacterOf(halfDwarf).build(), raceSlot(halfDwarf, 0));

        assertTrue(elfOptions.contains(ElficoFeat.ALMA_FEERICA));
        assertTrue(dwarfOptions.stream().noneMatch(feat -> feat.getFeatCategory().getType() == FeatCategory.Type.RACIAL),
                "an Anão's own grant is General, so there is nothing racial to substitute");
    }

    @Test
    void vampiroOffersItsLifeRacesTalentosAndVampiricos() {
        Vampiro dwarfVampire = new Vampiro(Vampiro.VampiroLineage.DAMPIRO, new Anao());
        Vampiro humanVampire = new Vampiro(Vampiro.VampiroLineage.DAMPIRO, new Human());

        List<Feat> dwarfOptions = creationService.getStartingFeatOptions(awakenedCharacterOf(dwarfVampire).build(), raceSlot(dwarfVampire, 0));
        List<Feat> humanOptions = creationService.getStartingFeatOptions(awakenedCharacterOf(humanVampire).build(), raceSlot(humanVampire, 0));

        assertTrue(dwarfOptions.contains(AnaoFeat.VIGOR_DO_INVERNO));
        assertTrue(dwarfOptions.contains(VampiricoFeat.ABOMINACAO));
        assertFalse(humanOptions.contains(AnaoFeat.VIGOR_DO_INVERNO));
        assertTrue(humanOptions.contains(VampiricoFeat.ABOMINACAO));
    }

    // ---- granting ----------------------------------------------------------------------------

    @Test
    void grantsAValidSelectionFreeOfExperience() {
        Character character = characterOf(new Anao()).build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.accumulateExperience(BigDecimal.TEN);

        creationService.grantStartingFeats(character,
                List.of(MobilidadeFeat.ESQUIVA, DestinoFeat.PRODIGIO, DestinoFeat.AUTOCONHECIMENTO), sheet);

        assertEquals(List.of(MobilidadeFeat.ESQUIVA, DestinoFeat.PRODIGIO, DestinoFeat.AUTOCONHECIMENTO), character.getFeats());
        assertEquals(0, BigDecimal.TEN.compareTo(sheet.getUnUsedExperience()));
    }

    @Test
    void aLaterPickMayRequireAnEarlierOne() {
        Character character = characterOf(new Gigantes()).build();

        creationService.grantStartingFeats(character, List.of(MobilidadeFeat.MOVIMENTO_RAPIDO, MobilidadeFeat.VELOCISTA));

        assertEquals(List.of(MobilidadeFeat.MOVIMENTO_RAPIDO, MobilidadeFeat.VELOCISTA), character.getFeats());
    }

    @Test
    void refusesTheWrongNumberOfPicks() {
        Character character = characterOf(new Gigantes()).build();

        IllegalOperationException error = assertThrows(IllegalOperationException.class,
                () -> creationService.grantStartingFeats(character, List.of(MobilidadeFeat.ESQUIVA)));
        assertEquals(INVALID_STARTING_FEAT_SELECTION, error.getMessage());
    }

    @Test
    void refusesAPickItsSlotDoesNotOffer() {
        Character character = characterOf(new Anao()).build();

        IllegalOperationException error = assertThrows(IllegalOperationException.class,
                () -> creationService.grantStartingFeats(character,
                        List.of(MobilidadeFeat.ESQUIVA, DestinoFeat.PRODIGIO, MobilidadeFeat.INICIATIVA_APRIMORADA)));
        assertEquals(INVALID_STARTING_FEAT_SELECTION, error.getMessage());
    }

    @Test
    void refusesAnIneligiblePick() {
        Character character = characterOf(new Gigantes()).build();

        IllegalOperationException error = assertThrows(IllegalOperationException.class,
                () -> creationService.grantStartingFeats(character, List.of(MobilidadeFeat.VELOCISTA, MobilidadeFeat.MOVIMENTO_RAPIDO)));
        assertEquals(INVALID_STARTING_FEAT_SELECTION, error.getMessage());
    }

    @Test
    void refusesTheSameTalentoTwice() {
        Character character = characterOf(new Gigantes()).build();

        assertThrows(IllegalOperationException.class,
                () -> creationService.grantStartingFeats(character, List.of(MobilidadeFeat.ESQUIVA, MobilidadeFeat.ESQUIVA)));
    }

    @Test
    void refusesABareChoiceCarryingConstant() {
        Character character = characterOf(new Gigantes()).build();

        IllegalOperationException error = assertThrows(IllegalOperationException.class,
                () -> creationService.grantStartingFeats(character, List.of(PeritoFeat.FOCO_EM_PERICIA, MobilidadeFeat.ESQUIVA)));
        assertEquals(FEAT_REQUIRES_CHOICE, error.getMessage());
    }

    // ---- catalog-wide ------------------------------------------------------------------------

    @Test
    void everyTreeARaceSlotNamesHasAuthoredTalentos() {
        Stream.of(new Anao(), new Elfo(), new Human(), new Pequenino(), new Aviano(Aviano.Subtipo.RAPINANTE), new Orc(), new Bestial(),
                        new Fada(), new Furia(), new Satiro(), new Gorgona(), new MeioElfo(new Elfo()),
                        new Agastias(new Elfo(), Agastias.Linhagem.VULCANO), new Aquan(new Human()),
                        new Colosso(new Human()), new Invernal(new Human()), new Dolos(new Human()),
                        new Flaminideo(new Human()), new NascidoDaFloresta(new Human()),
                        new NascidoDoDragao(new Human(), org.aventyrs.core.magic.ElementalType.FOGO),
                        new Goblin(), new Ogro(Ogro.Aptidao.FORCA_BRUTA), new Guampo(), new Indomito(Indomito.Tribo.values()[0]), new Troll(),
                        new HomemFera(HomemFera.EspiritoAnimal.WEMIC, HomemFera.Criacao.FEERICOS),
                        new Vampiro(Vampiro.VampiroLineage.DAMPIRO, new Human()), new Gnomo())
                .flatMap(race -> race.getStartingFeatSlots().stream())
                .flatMap(slot -> slot.pools().stream())
                .forEach(pool -> assertTrue(FeatCatalog.all().stream().anyMatch(pool::admits),
                        () -> pool + " admits no authored Talento"));
    }
}
