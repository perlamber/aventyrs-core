package org.aventyrs.core.feat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.item.RegaliaGrade;
import org.aventyrs.core.skill.SkillType;

/**
 * Talentos de Artífice — the Regalia-crafting ladder. Each constant lets its holder forge one
 * {@link RegaliaGrade} through {@code
 * org.aventyrs.core.character.services.EquipmentCraftingService#forgeRegalia}, which prices the
 * work (GD, days), enforces the gates (the trade Especialização, this Talento, a willing Centelha
 * donor, and — for Divina — a Dragão/Elemental/Abissal/Celestial donor), marks the copy with its
 * grade, and advances the crafter's "criação de 3 ou mais Regalias" history. A <b>Regalia is a
 * property of the owned copy</b> ({@code Item#isRegalia()} / {@code Item#getRegaliaGrade()}), not
 * an {@code ItemRarity}.
 *
 * <h2>Acquiring one is a different question from using one</h2>
 * Each constant carries both, and they are modeled separately.
 *
 * <ul>
 *   <li><b>To acquire</b> — {@link FeatRequirements}, checked by {@link Feat#isEligible}:
 *   {@link #ARTESAO_DE_REGALIAS_MENOR} needs Profissão 7; {@link #ARTESAO_DE_REGALIAS_SUPERIORES}
 *   adds Profissão 10, the Menor Talento and 3 forged Regalias Menores; {@link
 *   #ARTESAO_DE_REGALIAS_DIVINAS} adds the Superiores Talento and 3 forged Regalias Superiores.
 *   The craft-history clauses go through {@code craftedRegaliaGrade}/{@code craftedRegaliaCount}
 *   ({@code Character#getRegaliasCrafted}).</li>
 *   <li><b>To use</b> — {@link Feat#itsAllowedToCraftRegalia(Character)}, checked at forge time
 *   by {@code org.aventyrs.core.item.ItemForgery}: the crafter must have a Regalia of at least
 *   that grade in possession ("a Regalia em sua posse" / "posse de uma Regalia Superior ou
 *   Divina" / "de uma Regalia Divina"), via {@code Character#possessesRegalia}.</li>
 * </ul>
 *
 * <p>Possession is deliberately <b>not</b> an acquisition prerequisite. The rules text describes
 * studying a Regalia you own as how the knowledge is gained, not as something you must keep
 * owning to have learned it — and gating acquisition on it would mean a crafter who sold or lost
 * their Regalia had un-learned the Talento. Instead, they keep it and simply cannot forge until
 * they hold one again, which is exactly what a permission does.
 *
 * <p><b>What stays outside this core</b>, and is the caller's / GM's to adjudicate: the actual
 * loss of the donor's Centelhas (nothing tracks a character's Centelhas — the {@code
 * DestinoFeat#FRAGMENTO_DA_ENCARNACAO_DE_GILGAMESH} gap); the <i>Forja do Olho de Deus</i> —
 * both its "reduz drasticamente" time bonus and the Divina "deve ser feita exclusivamente" in one
 * (this core models no places); the PE cost of the work (no PE economy — the {@code
 * ResourcesAdvantage#BARGANHISTA} gap); and the roll itself, including the Divina Acerto Crítico
 * requirement (this core never rolls — {@code forgeRegalia} assumes the caller resolved it, and
 * {@code regaliaCraftingRequiresCriticalResult} reports the Divina rule for the caller to apply).
 * The "não é possível reduzir GD … exceto Habilidades de Artífice" carve-out is currently exempt
 * from nothing — no Talento reduces a crafting GD.
 */
public enum ArtificeFeat implements Feat {

    /**
     * "Após estudar incansavelmente a Regalia em sua posse, você agora pode criar Regalias
     * Menores." Profissão GD Inimaginável, one sacrificed Centelha, up to 90 days.
     *
     * <p><b>Real</b>, through {@code EquipmentCraftingService#forgeRegalia(…, RegaliaGrade.MENOR, …)}.
     */
    ARTESAO_DE_REGALIAS_MENOR(
            "Após estudar incansavelmente a Regalia em sua posse, você agora pode criar Regalias "
                    + "Menores. Criar uma Regalia Menor exige uma rolagem de Profissão "
                    + "(Especialização conforme o tipo de Regalia) na GD Inimaginável, não é "
                    + "possível reduzir GD desta rolagem com Habilidades, Vantagens ou Efeitos, "
                    + "exceto Habilidades de Artífice. Como custo adicional é necessário que um "
                    + "personagem sacrifique voluntariamente uma de suas Centelhas, despejando seu "
                    + "sangue sobre o equipamento. A Criação de uma Regalia Menor leva até 90 dias "
                    + "de trabalho.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.PROFISSAO)
                    .requiredSkillGraduation(7)
                    .build()) {
        /** "Após estudar incansavelmente a Regalia em sua posse" — any Regalia will do. */
        @Override
        public RegaliaGrade itsAllowedToCraftRegalia(final Character holder) {
            return holder.possessesRegalia(RegaliaGrade.MENOR) ? RegaliaGrade.MENOR : null;
        }
    },

    /**
     * "Você se tornou capaz de criar Regalias Superiores." Profissão GD Milagre, all of the
     * donor's Centelhas, up to 145 days.
     *
     * <p><b>Real</b>, through {@code EquipmentCraftingService#forgeRegalia(…, RegaliaGrade.SUPERIOR, …)}.
     * Its "bem-sucedido na criação de 3 ou mais Regalias Menores" clause is enforced via {@code
     * craftedRegaliaGrade(MENOR)}/{@code craftedRegaliaCount(3)}.
     */
    ARTESAO_DE_REGALIAS_SUPERIORES(
            "Após estudar Regalias Superior e ter sido bem-sucedido na criação de 3 ou mais "
                    + "Regalias Menores, você se tornou capaz de criar Regalias Superiores. Criar "
                    + "uma Regalia Superior exige uma rolagem de Profissão na GD Milagre. Como "
                    + "custo adicional é necessário que um personagem sacrifique voluntariamente "
                    + "todas as suas Centelhas. A Criação leva até 145 dias de trabalho.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.PROFISSAO)
                    .requiredSkillGraduation(10)
                    .requiredFeat(ARTESAO_DE_REGALIAS_MENOR)
                    .craftedRegaliaGrade(RegaliaGrade.MENOR)
                    .craftedRegaliaCount(3)
                    .build()) {
        /** "Após estudar Regalias Superior" — a Regalia Superior, or the Divina above it. */
        @Override
        public RegaliaGrade itsAllowedToCraftRegalia(final Character holder) {
            return holder.possessesRegalia(RegaliaGrade.SUPERIOR) ? RegaliaGrade.SUPERIOR : null;
        }
    },

    /**
     * "Você dominou a arte da criação de Regalias e agora pode criar Regalias Divinas." Profissão
     * GD Milagre with a mandatory Acerto Crítico, a Dragão/Elemental/Abissal/Celestial donor's
     * Centelhas, up to 180 days, in a Forja do Olho de Deus.
     *
     * <p><b>Real</b>, through {@code EquipmentCraftingService#forgeRegalia(…, RegaliaGrade.DIVINA, …)}
     * — the donor essence is checked ({@code CreatureType.DRAGAO}/{@code ELEMENTAL}/{@code
     * ABISSAL}/{@code CELESTIAL}). The source's craft-history clause reads "3 ou mais Regalias
     * Divinas" (circular); modeled as the sensible rung — 3 forged Regalias Superiores. The
     * mandatory-Crítico and Forja do Olho de Deus clauses are the caller's — see the class javadoc.
     */
    ARTESAO_DE_REGALIAS_DIVINAS(
            "Você dominou a arte da criação de Regalias e agora pode criar Regalias Divinas. "
                    + "Criar uma Regalia Divina exige uma rolagem de Profissão na GD Milagre que "
                    + "tenha por resultado obrigatório um Acerto Crítico. Como custo adicional é "
                    + "necessário que um Dragão, Elemental, Abissal ou Celestial sacrifique "
                    + "voluntariamente suas Centelhas. A Criação leva até 180 dias de trabalho e "
                    + "deve ser feita exclusivamente em uma Forja do Olho de Deus.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.PROFISSAO)
                    .requiredSkillGraduation(10)
                    .requiredFeat(ARTESAO_DE_REGALIAS_SUPERIORES)
                    .craftedRegaliaGrade(RegaliaGrade.SUPERIOR)
                    .craftedRegaliaCount(3)
                    .build()) {
        /** Only a Regalia Divina in hand unlocks forging another. */
        @Override
        public RegaliaGrade itsAllowedToCraftRegalia(final Character holder) {
            return holder.possessesRegalia(RegaliaGrade.DIVINA) ? RegaliaGrade.DIVINA : null;
        }
    };

    private final String description;
    private final FeatRequirements featRequirements;

    ArtificeFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.ARTIFICE;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public FeatRequirements getFeatRequirements() {
        return featRequirements;
    }
}
