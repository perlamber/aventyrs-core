package org.aventyrs.core.skill;

import org.aventyrs.core.skill.artes.ArtesCompetencyAbility;
import org.aventyrs.core.skill.artes.ArtesSpecialization;
import org.aventyrs.core.skill.ataqueadistancia.AtaqueADistanciaCompetencyAbility;
import org.aventyrs.core.skill.ataqueadistancia.AtaqueADistanciaSpecialization;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoCompetencyAbility;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoSpecialization;
import org.aventyrs.core.skill.atletismo.AtletismoCompetencyAbility;
import org.aventyrs.core.skill.atletismo.AtletismoSpecialization;
import org.aventyrs.core.skill.attention.AttentionCompetencyAbility;
import org.aventyrs.core.skill.attention.AttentionSpecialization;
import org.aventyrs.core.skill.conhecimentos.ConhecimentosCompetencyAbility;
import org.aventyrs.core.skill.conhecimentos.ConhecimentosSpecialization;
import org.aventyrs.core.skill.dirigirecavalgar.DirigirECavalgarCompetencyAbility;
import org.aventyrs.core.skill.dirigirecavalgar.DirigirECavalgarSpecialization;
import org.aventyrs.core.skill.dominiodomana.DominioDoManaCompetencyAbility;
import org.aventyrs.core.skill.dominiodomana.DominioDoManaSpecialization;
import org.aventyrs.core.skill.empatiaselvagem.EmpatiaSelvagemCompetencyAbility;
import org.aventyrs.core.skill.empatiaselvagem.EmpatiaSelvagemSpecialization;
import org.aventyrs.core.skill.esquivaeaparar.EsquivaEApararCompetencyAbility;
import org.aventyrs.core.skill.esquivaeaparar.EsquivaEApararSpecialization;
import org.aventyrs.core.skill.furtividade.FurtividadeCompetencyAbility;
import org.aventyrs.core.skill.furtividade.FurtividadeSpecialization;
import org.aventyrs.core.skill.medicinaecura.MedicinaECuraCompetencyAbility;
import org.aventyrs.core.skill.medicinaecura.MedicinaECuraSpecialization;
import org.aventyrs.core.skill.persuasao.PersuasaoCompetencyAbility;
import org.aventyrs.core.skill.persuasao.PersuasaoSpecialization;
import org.aventyrs.core.skill.profissao.ProfissaoCompetencyAbility;
import org.aventyrs.core.skill.profissao.ProfissaoSpecialization;

import java.util.List;

/**
 * The catalog of each Perícia's own {@link SkillSpecialization}s and {@link
 * SkillCompetencyAbility}s — the per-Perícia {@code *Specialization}/{@code *CompetencyAbility}
 * enums, indexed by {@link SkillType}. Racial abilities that report a {@code SkillType} (e.g.
 * {@code AnoesRacialAbility}) are deliberately absent: they are a Raça's, never a pick.
 *
 * <p>Earned by its second consumer: an Antecedente offering "uma Especialização adicional" or "uma
 * Habilidade de Competência da Perícia escolhida" ({@code org.aventyrs.core.background}) has to
 * enumerate the options itself, and a client picking a trained Perícia's Especializações needed the
 * same list (it kept its own switch until now).
 */
public final class SkillTraitCatalog {

    private SkillTraitCatalog() {
    }

    /** Every Especialização of skillType, in its enum's declaration order. */
    public static List<SkillSpecialization> specializationsOf(final SkillType skillType) {
        final SkillSpecialization[] values = switch (skillType) {
            case ATTENTION -> AttentionSpecialization.values();
            case ARTES -> ArtesSpecialization.values();
            case ATLETISMO -> AtletismoSpecialization.values();
            case DIRIGIR_E_CAVALGAR -> DirigirECavalgarSpecialization.values();
            case DOMINIO_DO_MANA -> DominioDoManaSpecialization.values();
            case ATAQUE_A_DISTANCIA -> AtaqueADistanciaSpecialization.values();
            case ATAQUE_CORPO_A_CORPO -> AtaqueCorpoACorpoSpecialization.values();
            case ESQUIVA_E_APARAR -> EsquivaEApararSpecialization.values();
            case EMPATIA_SELVAGEM -> EmpatiaSelvagemSpecialization.values();
            case FURTIVIDADE -> FurtividadeSpecialization.values();
            case MEDICINA_E_CURA -> MedicinaECuraSpecialization.values();
            case PERSUASAO -> PersuasaoSpecialization.values();
            case PROFISSAO -> ProfissaoSpecialization.values();
            case CONHECIMENTOS -> ConhecimentosSpecialization.values();
        };
        return List.of(values);
    }

    /** Every Habilidade de Competência of skillType, in its enum's declaration order. */
    public static List<SkillCompetencyAbility> competencyAbilitiesOf(final SkillType skillType) {
        final SkillCompetencyAbility[] values = switch (skillType) {
            case ATTENTION -> AttentionCompetencyAbility.values();
            case ARTES -> ArtesCompetencyAbility.values();
            case ATLETISMO -> AtletismoCompetencyAbility.values();
            case DIRIGIR_E_CAVALGAR -> DirigirECavalgarCompetencyAbility.values();
            case DOMINIO_DO_MANA -> DominioDoManaCompetencyAbility.values();
            case ATAQUE_A_DISTANCIA -> AtaqueADistanciaCompetencyAbility.values();
            case ATAQUE_CORPO_A_CORPO -> AtaqueCorpoACorpoCompetencyAbility.values();
            case ESQUIVA_E_APARAR -> EsquivaEApararCompetencyAbility.values();
            case EMPATIA_SELVAGEM -> EmpatiaSelvagemCompetencyAbility.values();
            case FURTIVIDADE -> FurtividadeCompetencyAbility.values();
            case MEDICINA_E_CURA -> MedicinaECuraCompetencyAbility.values();
            case PERSUASAO -> PersuasaoCompetencyAbility.values();
            case PROFISSAO -> ProfissaoCompetencyAbility.values();
            case CONHECIMENTOS -> ConhecimentosCompetencyAbility.values();
        };
        return List.of(values);
    }

    /** {@link #specializationsOf} or {@link #competencyAbilitiesOf}, by kind. */
    public static List<SkillTrait> traitsOf(final SkillType skillType, final SkillTraitKind kind) {
        return List.copyOf(kind == SkillTraitKind.SPECIALIZATION
                ? specializationsOf(skillType) : competencyAbilitiesOf(skillType));
    }
}
