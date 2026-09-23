package org.aventyrs.core.title.giganteenfurecido;

import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.AreaDamage;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Frenzy;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

import java.util.ArrayList;
import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.FRENZY_REQUIRED;
import static org.aventyrs.core.util.TranslatableMessages.INVALID_DIE_ROLL;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_ACTIVATION_LIMIT_REACHED;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_CHOICE_REQUIRED;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_REQUIRES_SCENE;

/**
 * Gritos de Guerra, "Enquanto estiver em Frenesi", once a Rodada, one of three:
 * <ul>
 *   <li><b>Desdenho</b> — a Meio-Dano {@link Blessing} for 1 Rodada to the holder and allies in
 *       Distância Média ({@link Blessing#getReach()}), granted by the caller like any other.</li>
 *   <li><b>Espinhoso</b> — "1d6 + Metade de seus Multiplicador de PV" Físico Primordial to each enemy
 *       in Distância Curta, doubled adjacent, as {@link AreaDamage} for the caller to apply. The die is
 *       rolled once, off the request's {@code DiceRoller}.</li>
 *   <li><b>Inspirador</b> — an inspired copy of the holder's Frenesi ({@link Frenzy#inspiredCopy}) for
 *       1 Rodada to each ally in Distância Curta, applied to the sheets in hand and reported.</li>
 * </ul>
 * Under Cataclismo Elemental every Grito also deals its elemental damage ({@link AreaDamage#cataclysm}).
 */
public class GritosDeGuerraInteraction extends AbstractTitleAbilityInteraction {

    /** "durante 1 Rodadas" / "por 1 Rodada". */
    static final int GRITO_ROUNDS = 1;

    public GritosDeGuerraInteraction() {
        super(TitaEnlouquecidoAbility.GRITOS_DE_GUERRA);
    }

    @Override
    protected void validate(final TitleAbilityActivationRequest request) {
        CombatantSheet activator = request.getActivator();
        if (activator.getOwnFrenzy().isEmpty()) {
            throw new IllegalOperationException(FRENZY_REQUIRED);
        }
        GritoDeGuerra grito = request.getChoice(GritoDeGuerra.class)
                .orElseThrow(() -> new IllegalOperationException(TITLE_ABILITY_CHOICE_REQUIRED));
        if (activator.isAffectedThisCombat(roundMark(request))) {
            throw new IllegalOperationException(TITLE_ABILITY_ACTIVATION_LIMIT_REACHED);
        }
        if (grito != GritoDeGuerra.DESDENHO && request.getSceneContext() == null) {
            throw new IllegalOperationException(TITLE_ABILITY_REQUIRES_SCENE);
        }
        if (grito == GritoDeGuerra.ESPINHOSO && request.getDiceRoller() == null) {
            throw new IllegalOperationException(INVALID_DIE_ROLL);
        }
    }

    /** "Não é possível ativar Gritos de Guerra mais de uma vez na mesma Rodada." */
    private static GiganteEnfurecido.OncePerRound roundMark(final TitleAbilityActivationRequest request) {
        return new GiganteEnfurecido.OncePerRound(TitaEnlouquecidoAbility.GRITOS_DE_GUERRA,
                GiganteEnfurecido.currentRound(request.getSceneContext()), null);
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        CombatantSheet activator = request.getActivator();
        activator.markAffectedThisCombat(roundMark(request));
        SceneContext context = request.getSceneContext();
        InteractionResult.InteractionResultBuilder result = InteractionResult.builder();
        List<AreaDamage> damage = new ArrayList<>();
        switch (request.getChoice(GritoDeGuerra.class).orElseThrow()) {
            case DESDENHO -> result.blessings(List.of(new Blessing(ModifierType.HALF_DAMAGE, 1, GRITO_ROUNDS,
                    TargetScope.SELF_AND_ALLIES, "Grito de Desdenho")
                    .reaching(Range.DISTANCIA_MEDIA)
                    .countingDownAtTurnStart()));
            case ESPINHOSO -> {
                int lifeMultiplier = new HitPointsServiceImpl().getLifeMultiplier(activator.getCharacter(), activator);
                int amount = request.getDiceRoller().rollD6() + lifeMultiplier / 2;
                for (CombatantSheet enemy : context.getEnemiesWithin(Range.DISTANCIA_CURTA)) {
                    boolean adjacent = context.getDistanceTo(enemy) == Range.ADJACENTE;
                    damage.add(new AreaDamage(enemy, adjacent ? amount * 2 : amount,
                            new DamageDescriptor(DamageType.PRIMORDIAL), activator));
                }
            }
            case INSPIRADOR -> {
                Frenzy own = activator.getOwnFrenzy().orElseThrow();
                List<CombatantSheet> recipients = context.getAlliesWithin(Range.DISTANCIA_CURTA).stream()
                        .filter(ally -> ally != activator)
                        .toList();
                recipients.forEach(ally -> ally.startFrenzy(own.inspiredCopy(GRITO_ROUNDS)));
                result.grantedFrenzy(own.inspiredCopy(GRITO_ROUNDS)).frenzyRecipients(recipients);
            }
            default -> throw new IllegalStateException();
        }
        List<AreaDamage> cataclysm = AreaDamage.cataclysm(activator, context);
        if (cataclysm != null) {
            damage.addAll(cataclysm);
        }
        if (!damage.isEmpty()) {
            result.areaDamage(List.copyOf(damage));
        }
        return result.build();
    }
}
