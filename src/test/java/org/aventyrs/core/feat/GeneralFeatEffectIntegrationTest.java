package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.FeatService;
import org.aventyrs.core.character.services.FeatServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.character.services.MovementService;
import org.aventyrs.core.character.services.MovementServiceImpl;
import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpo;
import org.aventyrs.core.skill.esquivaeaparar.EsquivaEAparar;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The clauses of the general Talento trees that are mechanically <b>real</b>, each tested by the
 * effect it has on a character who legally acquired it — through {@link FeatService#grantFeat},
 * satisfying the Pré-requisito and paying the XP — and read back off the consuming service, never
 * off the hook. See the {@code testing-a-feat} skill.
 *
 * <p>Most of the catalog is a TODO'd entry blocked on a missing system, and deliberately has no
 * test here: there is nothing to observe. These are the exceptions.
 */
class GeneralFeatEffectIntegrationTest {

    private final FeatService featService = new FeatServiceImpl();
    private final MovementService movementService = new MovementServiceImpl();
    private final ActionPointsService actionPointsService = new ActionPointsServiceImpl();
    private final HitPointsService hitPointsService = new HitPointsServiceImpl();
    private final DeterminationPointsService determinationPointsService = new DeterminationPointsServiceImpl();
    private final DefenseService defenseService = new DefenseServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character.CharacterBuilder character() {
        return CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>());
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

    private static CharacterSkill trained(final org.aventyrs.core.skill.Skill skill, final int graduation) {
        return CharacterSkill.builder()
                .skill(skill)
                .graduation(SkillGraduation.builder().graduationValue(graduation).build())
                .build();
    }

    // ---------- Movimento Base ----------

    @Test
    void movimentoRapidoRaisesMovimentoBaseByTwo() throws IllegalOperationException {
        Character character = character().build();
        int before = movementService.getMovementBase(character);

        acquire(character, MobilidadeFeat.MOVIMENTO_RAPIDO);

        assertEquals(before + 2, movementService.getMovementBase(character));
    }

    @Test
    void velocistaStacksItsOwnUnitOnTopOfMovimentoRapido() throws IllegalOperationException {
        Character character = character().build();
        int before = movementService.getMovementBase(character);

        acquire(character, MobilidadeFeat.MOVIMENTO_RAPIDO, MobilidadeFeat.VELOCISTA);

        assertEquals(before + 3, movementService.getMovementBase(character));
    }

    // ---------- Pontos de Ação ----------

    @Test
    void maisVelozQueAVisaoGrantsOnePermanentActionPoint() throws IllegalOperationException {
        Character character = character().build();
        int before = actionPointsService.getMaxActionPoints(character, 0);

        // Its own Pré-requisito is two other Talentos de Mobilidade.
        acquire(character, MobilidadeFeat.MOVIMENTO_RAPIDO, MobilidadeFeat.VELOCISTA,
                MobilidadeFeat.MAIS_VELOZ_QUE_A_VISAO);

        assertEquals(before + 1, actionPointsService.getMaxActionPoints(character, 0));
    }

    // ---------- Multiplicadores ----------

    @Test
    void vitalidadeRaisesTheLifeMultiplierByOne() throws IllegalOperationException {
        Character character = character()
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(4).build())
                        .build())
                .build();
        int multiplierBefore = hitPointsService.getLifeMultiplier(character);
        int hitPointsBefore = hitPointsService.getMaxHitPoints(character);

        acquire(character, SobrevivenciaFeat.VITALIDADE);

        assertEquals(multiplierBefore + 1, hitPointsService.getLifeMultiplier(character));
        // The uplift is per point of Vigor, which is what makes it different from a flat grant.
        assertEquals(hitPointsBefore + character.getAttributes().getVigor().getTotal(),
                hitPointsService.getMaxHitPoints(character));
    }

    @Test
    void coracaoDeFerroRaisesTheDeterminationMultiplierByOne() throws IllegalOperationException {
        Character character = character().build();
        int before = determinationPointsService.getDeterminationMultiplier(character);

        acquire(character, DestinoFeat.CORACAO_DE_FERRO);

        assertEquals(before + 1, determinationPointsService.getDeterminationMultiplier(character));
    }

    // ---------- Defesas ----------

    @Test
    void defesaDeMaosLimpasGrantsTwoToBothDefesasWhileWieldingNoWeapon() throws IllegalOperationException {
        Character character = unarmedMartialArtist();
        int physicalBefore = defenseService.getTotalDefense(character, DefenseType.PHYSICAL);
        int magicBefore = defenseService.getTotalDefense(character, DefenseType.MAGIC);

        acquire(character, ArtesMarciaisFeat.ARTISTA_MARCIAL, ArtesMarciaisFeat.DEFESA_DE_MAOS_LIMPAS);

        assertEquals(physicalBefore + 2, defenseService.getTotalDefense(character, DefenseType.PHYSICAL));
        assertEquals(magicBefore + 2, defenseService.getTotalDefense(character, DefenseType.MAGIC));
    }

    @Test
    void defesaDeMaosLimpasGrowsByOnePerTituloAventyrDesperto() throws IllegalOperationException {
        Character character = unarmedMartialArtist();
        int before = defenseService.getTotalDefense(character, DefenseType.PHYSICAL);

        acquire(character, ArtesMarciaisFeat.ARTISTA_MARCIAL, ArtesMarciaisFeat.DEFESA_DE_MAOS_LIMPAS);
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);

        assertEquals(before + 3, defenseService.getTotalDefense(character, DefenseType.PHYSICAL));
    }

    /** Força 2 and Ataque Corpo-a-Corpo 2 for ARTISTA_MARCIAL, Esquiva e Aparar 4 for its dependent. */
    private static Character unarmedMartialArtist() {
        return character()
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(2).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, trained(new AtaqueCorpoACorpo(), 2))
                .skill(SkillType.ESQUIVA_E_APARAR, trained(new EsquivaEAparar(), 4))
                .build();
    }
}
