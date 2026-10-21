package us.drullk.noodling.noodler;

import org.jspecify.annotations.Nullable;
import tamaized.beanification.Autowired;
import us.drullk.noodling.pasta.MacaroniPlacer;
import us.drullk.noodling.pasta.PennePlacer;
import us.drullk.noodling.pasta.PipettePlacer;
import us.drullk.noodling.pasta.RadiatoriPlacer;
import us.drullk.noodling.pasta.ShellPlacer;
import us.drullk.noodling.pasta.SpiralPlacer;
import us.drullk.noodling.util.Util;

import java.util.List;

/**
 * The shapes the Noodler can place, in the order the GUI lists them.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
@Deprecated
public final class PastaShapes {

    @Autowired
    private static PastaShapeLookup pastaShapeLookup;

    @Deprecated
    private static final Util HELPER = new Util();

    @Deprecated
    public static final List<PastaShape> ALL = List.of(
            PennePlacer.shape(new PennePlacer(), HELPER),
            SpiralPlacer.shape(new SpiralPlacer(), HELPER),
            ShellPlacer.shape(new ShellPlacer(), HELPER),
            RadiatoriPlacer.shape(new RadiatoriPlacer(), HELPER),
            MacaroniPlacer.shape(new MacaroniPlacer(), HELPER),
            PipettePlacer.shape(new PipettePlacer(), HELPER)
    );

    private PastaShapes() {
    }

    @Deprecated
    public static @Nullable PastaShape byId(String id) {
        return pastaShapeLookup.byId(id);
    }

}
