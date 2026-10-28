package dev.taikin.mcplugins.cifviewer.cif;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.taikin.mcplugins.cifviewer.model.Crystal;
import org.junit.jupiter.api.Test;

class CifParserTest {

    @Test
    void parsesSingleSiteWithUncertainty() {
        String cif = """
                data_test
                _cell_length_a 3.350(2)
                _cell_length_b 3.350(2)
                _cell_length_c 3.350(2)
                _cell_angle_alpha 90
                _cell_angle_beta 90
                _cell_angle_gamma 90
                _symmetry_equiv_pos_as_xyz 'x, y, z'
                loop_
                _atom_site_label
                _atom_site_type_symbol
                _atom_site_fract_x
                _atom_site_fract_y
                _atom_site_fract_z
                _atom_site_occupancy
                Po1 Po 0 0 0 1
                """;

        Crystal crystal = new CifParser().parse("po.cif", cif);

        assertEquals(1, crystal.atoms().size());
        assertEquals("Po", crystal.atoms().getFirst().element());
        assertEquals(3.35, crystal.lattice().a(), 1.0e-9);
        assertEquals("Po", crystal.contents());
    }

    @Test
    void appliesTranslationsAndDeduplicates() {
        String cif = """
                data_test
                _cell_length_a 5.0
                _cell_length_b 5.0
                _cell_length_c 5.0
                _cell_angle_alpha 90
                _cell_angle_beta 90
                _cell_angle_gamma 90
                loop_
                _symmetry_equiv_pos_as_xyz
                x,y,z
                x+1/2,y,z
                -x,-y,-z
                loop_
                _atom_site_label
                _atom_site_type_symbol
                _atom_site_fract_x
                _atom_site_fract_y
                _atom_site_fract_z
                Na1 Na 0 0 0
                """;

        Crystal crystal = new CifParser().parse("na.cif", cif);

        assertEquals(2, crystal.atoms().size());
        assertEquals(0.0, crystal.atoms().get(0).x(), 1.0e-9);
        assertEquals(0.5, crystal.atoms().get(1).x(), 1.0e-9);
    }

    @Test
    void readsIonicChargeFromTypeSymbol() {
        String cif = """
                data_test
                _cell_length_a 5.0
                _cell_length_b 5.0
                _cell_length_c 5.0
                _cell_angle_alpha 90
                _cell_angle_beta 90
                _cell_angle_gamma 90
                loop_
                _atom_site_label
                _atom_site_type_symbol
                _atom_site_fract_x
                _atom_site_fract_y
                _atom_site_fract_z
                Na1 Na1+ 0 0 0
                """;

        Crystal crystal = new CifParser().parse("na.cif", cif);

        assertEquals(1, crystal.atoms().getFirst().charge());
    }

    @Test
    void convertsCartesianCoordinates() {
        String cif = """
                data_test
                _cell_length_a 4.0
                _cell_length_b 4.0
                _cell_length_c 4.0
                _cell_angle_alpha 90
                _cell_angle_beta 90
                _cell_angle_gamma 90
                loop_
                _atom_site_label
                _atom_site_type_symbol
                _atom_site_Cartn_x
                _atom_site_Cartn_y
                _atom_site_Cartn_z
                Fe1 Fe 2.0 0.0 0.0
                """;

        Crystal crystal = new CifParser().parse("fe.cif", cif);

        assertEquals(1, crystal.atoms().size());
        assertEquals(0.5, crystal.atoms().getFirst().x(), 1.0e-9);
        assertEquals(0.0, crystal.atoms().getFirst().y(), 1.0e-9);
        assertEquals(0.0, crystal.atoms().getFirst().z(), 1.0e-9);
    }

    @Test
    void normalizesDottedTags() {
        String cif = """
                data_test
                _cell.length_a 2.0
                _cell.length_b 2.0
                _cell.length_c 2.0
                _cell.angle_alpha 90
                _cell.angle_beta 90
                _cell.angle_gamma 90
                _space_group_symop.operation_xyz 'x, y, z'
                loop_
                _atom_site.label
                _atom_site.type_symbol
                _atom_site.fract_x
                _atom_site.fract_y
                _atom_site.fract_z
                H1 H 0 0 0
                """;

        Crystal crystal = new CifParser().parse("h.cif", cif);

        assertEquals(1, crystal.atoms().size());
        assertEquals("H", crystal.atoms().getFirst().element());
        assertEquals(2.0, crystal.lattice().a(), 1.0e-9);
    }
}
