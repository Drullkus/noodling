package us.drullk.noodling.noodler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import tamaized.beanification.Autowired;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the recording writer produces a tag a vanilla {@link StructureTemplate} loads back with the same
 * size and blocks, that the compressed bytes round-trip, and that export keeps the Noodler's typed seed.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
class NoodlerExportTest {

    @Autowired
    private static PastaShapeLookup pastaShapeLookup;

    @Test
    void recordedBlocksLoadAsStructureTemplate() {
        RecordingLevelWriter writer = new RecordingLevelWriter();
        writer.setBlock(new BlockPos(-2, 5, 7), Blocks.STONE.defaultBlockState(), 3);
        writer.setBlock(new BlockPos(1, 6, 7), Blocks.STONE.defaultBlockState(), 3);
        writer.setBlock(new BlockPos(0, 5, 9), Blocks.DIRT.defaultBlockState(), 3);
        assertEquals(new Vec3i(4, 2, 3), writer.size());

        StructureTemplate template = new StructureTemplate();
        template.load(BuiltInRegistries.BLOCK, writer.toTemplateTag());
        assertEquals(new Vec3i(4, 2, 3), template.getSize());
    }

    @Test
    void emptyWriterGivesEmptyTemplate() {
        CompoundTag tag = new RecordingLevelWriter().toTemplateTag();
        StructureTemplate template = new StructureTemplate();
        template.load(BuiltInRegistries.BLOCK, tag);
        assertEquals(Vec3i.ZERO, template.getSize());
        assertTrue(tag.getListOrEmpty("blocks").isEmpty());
    }

    @Test
    void shapeExportsAndCompressedBytesRoundTrip() throws IOException {
        PastaShape penne = pastaShapeLookup.byId("penne");
        RecordingLevelWriter writer = new RecordingLevelWriter();
        penne.placeRandom(writer, Vec3.ZERO, RandomSource.create(11L));
        assertFalse(writer.blocks().isEmpty());

        byte[] bytes = NoodlerExport.compress(writer.toTemplateTag());
        CompoundTag back = NbtIo.readCompressed(new ByteArrayInputStream(bytes), net.minecraft.nbt.NbtAccounter.unlimitedHeap());
        assertEquals(writer.blocks().size(), back.getListOrEmpty("blocks").size());
        assertTrue(bytes.length < NoodlerExport.MAX_BYTES);
    }

    @Test
    void exportKeepsTypedSeedButPlacementRerollsIt() {
        NoodlerState state = new NoodlerState("77", false, Optional.of("penne"), Map.of());
        NoodlerPlacement.Prepared export = NoodlerPlacement.prepare(state, RandomSource.create(1L), true);
        assertEquals("77", export.nextState().seed());

        NoodlerPlacement.Prepared placement = NoodlerPlacement.prepare(state, RandomSource.create(1L), false);
        assertEquals(NoodlerSeeds.text(RandomSource.create(1L).nextLong()), placement.nextState().seed());

        NoodlerPlacement.Prepared again = NoodlerPlacement.prepare(export.nextState(), RandomSource.create(2L), true);
        assertEquals(export.params().values(), again.params().values(), "exporting twice gives the same shape");
    }

    @Test
    void typedWordSeedTextIsKeptWhenHonoured() {
        NoodlerState locked = new NoodlerState("banana", true, Optional.of("penne"), Map.of());
        assertEquals("banana", NoodlerPlacement.prepare(locked, RandomSource.create(1L), false).nextState().seed());
        NoodlerState unlocked = new NoodlerState("banana", false, Optional.of("penne"), Map.of());
        assertEquals("banana", NoodlerPlacement.prepare(unlocked, RandomSource.create(1L), true).nextState().seed());
        assertEquals(NoodlerSeeds.text(RandomSource.create(1L).nextLong()), NoodlerPlacement.prepare(unlocked, RandomSource.create(1L), false).nextState().seed());
    }

    @Test
    void blankSeedRollsOnceOnExport() {
        NoodlerPlacement.Prepared export = NoodlerPlacement.prepare(NoodlerState.DEFAULT, RandomSource.create(5L), true);
        assertEquals(NoodlerSeeds.text(RandomSource.create(5L).nextLong()), export.nextState().seed());
    }
}
