package us.drullk.noodling.noodler;

import org.jspecify.annotations.Nullable;
import us.drullk.noodling.pasta.MacaroniPlacer;
import us.drullk.noodling.pasta.PennePlacer;
import us.drullk.noodling.pasta.PipettePlacer;
import us.drullk.noodling.pasta.RadiatoriPlacer;
import us.drullk.noodling.pasta.ShellPlacer;
import us.drullk.noodling.pasta.SpiralPlacer;
import us.drullk.noodling.util.Util;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The shapes the Noodler can place, in the order the GUI lists them.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
public final class PastaShapes {

    private static final Util HELPER = new Util();

    public static final List<PastaShape> ALL = List.of(
            PennePlacer.shape(new PennePlacer(), new Util()),
            SpiralPlacer.shape(new SpiralPlacer(), new Util()),
            new ShellPlacer(HELPER).shape(),
            new RadiatoriPlacer(HELPER).shape(),
            new MacaroniPlacer(HELPER).shape(),
            new PipettePlacer(HELPER).shape()
    );

    private static final Map<String, PastaShape> BY_ID = ALL.stream().collect(Collectors.toUnmodifiableMap(PastaShape::id, Function.identity()));

    private PastaShapes() {
    }

    public static @Nullable PastaShape byId(String id) {
        return BY_ID.get(id);
    }
}
