package us.drullk.noodling.client.noodler;

import imgui.ImGui;
import imgui.ImGuiViewport;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiSliderFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import imgui.type.ImString;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import org.jspecify.annotations.Nullable;
import tamaized.beanification.Autowired;
import us.drullk.noodling.Noodling;
import us.drullk.noodling.client.imgui.NoodlingImGui;
import us.drullk.noodling.noodler.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The ImGui widgets of the Noodler screen and the state they edit.
 *
 * <p>The first row is the seed field, its lock, a spacer and the pasta dropdown. When a specific pasta is chosen a
 * table follows with one row per parameter: seed field, lock, knob. Knobs are disabled while their parameter is
 * unlocked; editing a knob clears its seed; typing a seed locks the parameter. Slider bounds are recomputed every
 * frame from the current values of the parameters above them. Edits apply to the local copy at once and are
 * committed (stored on the stack and sent to the server) when the widget finishes its edit. The last row exports the
 * shown shape as a structure template through {@link NoodlerExportClient}.
 *
 * <p>The pasta block knob is a dropdown of the pasta blocks; the sauce knob is a dropdown whose first entry is "no
 * sauce" (stored as air) followed by the sauce blocks.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_c3299adc-f781-48a8-9c79-c51c79b26b4b.md}.
 */
final class NoodlerPanel {

    private static final int WINDOW_FLAGS = ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.AlwaysAutoResize | ImGuiWindowFlags.NoSavedSettings;
    private static final int TABLE_FLAGS = ImGuiTableFlags.SizingFixedFit | ImGuiTableFlags.RowBg;
    private static final int SEED_CAPACITY = 64;
    private static final float SEED_WIDTH = 100;
    private static final float PARAM_SEED_WIDTH = 80;
    private static final float KNOB_WIDTH = 150;
    private static final float SPACER = 16;
    private static final float WINDOW_MARGIN = 8;

    /** What a knob reports for one frame: the value it now holds if it changed, and whether the edit finished. */
    private record KnobResult(@Nullable ParamValue value, boolean committed) {
        static final KnobResult NONE = new KnobResult(null, false);
    }

    private final NoodlerScreen screen;
    private final String[] shapeNames;
    private final List<Block> blocks = new ArrayList<>();
    private final String[] blockNames;
    /** Sauce choices; index 0 is "no sauce" and holds air. */
    private final List<Block> sauces = new ArrayList<>();
    private final String[] sauceNames;

    private String seed = "";
    private boolean seedLocked;
    private @Nullable String shapeId;
    private final Map<String, ParamState> params = new LinkedHashMap<>();

    private final ImString seedBuffer = new ImString(SEED_CAPACITY);
    private final ImBoolean seedLockedBox = new ImBoolean();
    private final ImInt shapeIndex = new ImInt();
    private final Map<String, ImString> paramSeedBuffers = new HashMap<>();
    private final float[] floatBuffer = new float[1];
    private final int[] intBuffer = new int[1];
    private final ImFloat fieldBuffer = new ImFloat();
    private final float[] axisBuffer = new float[3];
    private final ImFloat angleBuffer = new ImFloat();
    private final ImInt blockIndex = new ImInt();
    private final ImInt sauceIndex = new ImInt();
    private boolean firstFrame = true;

    private final PastaShapeLookup pastaShapeLookup; // Autowiring causes classloading crash

    NoodlerPanel(NoodlerScreen screen, PastaShapeLookup pastaShapeLookup) {
        this.screen = screen;
        this.shapeNames = new String[pastaShapeLookup.size() + 1];
        this.shapeNames[0] = I18n.get("noodling.shape.random");
        for (int i = 0; i < pastaShapeLookup.size(); i++) {
            this.shapeNames[i + 1] = I18n.get(pastaShapeLookup.getList().get(i).descriptionId());
        }
        for (var pasta : Noodling.PASTA_BLOCKS) {
            this.blocks.add(pasta.get());
        }
        this.blockNames = this.blocks.stream().map(block -> I18n.get(block.getDescriptionId())).toArray(String[]::new);
        this.sauces.add(Blocks.AIR);
        for (var sauce : Noodling.SAUCE_BLOCKS) {
            this.sauces.add(sauce.get());
        }
        this.sauceNames = new String[this.sauces.size()];
        this.sauceNames[0] = I18n.get("noodling.gui.no_sauce");
        for (int i = 1; i < this.sauces.size(); i++) {
            this.sauceNames[i] = I18n.get(this.sauces.get(i).getDescriptionId());
        }

        this.pastaShapeLookup = pastaShapeLookup;
    }

    /** Replaces the edited copy with {@code state}. */
    void load(NoodlerState state) {
        this.seed = state.seed();
        this.seedLocked = state.seedLocked();
        this.shapeId = state.shape().filter(id -> this.pastaShapeLookup.byId(id) != null).orElse(null);
        this.params.clear();
        this.params.putAll(state.params());
        this.seedBuffer.set(this.seed);
        this.seedLockedBox.set(this.seedLocked);
        this.shapeIndex.set(this.shapeId == null ? 0 : this.pastaShapeLookup.getList().indexOf(this.pastaShapeLookup.byId(this.shapeId)) + 1);
        this.paramSeedBuffers.clear();
    }

    NoodlerState snapshot() {
        return new NoodlerState(this.seed, this.seedLocked, Optional.ofNullable(this.shapeId), this.params);
    }

    private @Nullable PastaShape shape() {
        return this.shapeId == null ? null : this.pastaShapeLookup.byId(this.shapeId);
    }

    /** The rotation the next placement would use, or null while "random pasta" is selected. */
    ParamValue.@Nullable Rotation currentRotation() {
        PastaShape shape = this.shape();
        PastaParam param = shape == null ? null : shape.param(PastaParam.ROTATION);
        if (param == null) {
            return null;
        }
        ParamState state = this.params.getOrDefault(PastaParam.ROTATION, ParamState.EMPTY);
        return state.value()
                .filter(value -> value instanceof ParamValue.Rotation)
                .map(value -> (ParamValue.Rotation) value)
                .orElseGet(() -> (ParamValue.Rotation) param.roll(RandomSource.create(0L), new ResolvedParams()));
    }

    private static float px(float guiUnits) {
        return NoodlingImGui.px(guiUnits);
    }

    void draw() {
        ImGuiViewport viewport = ImGui.getMainViewport();
        if (this.firstFrame) {
            ImGui.setNextWindowPos(viewport.getPosX() + viewport.getSizeX() * 0.5F, viewport.getPosY() + viewport.getSizeY() * 0.5F, ImGuiCond.Always, 0.5F, 0.5F);
            this.firstFrame = false;
        }
        // Auto-resize to the content, but never beyond the window: small windows get scrollbars instead of clipping.
        float margin = px(WINDOW_MARGIN);
        ImGui.setNextWindowSizeConstraints(0.0F, 0.0F, Math.max(1.0F, viewport.getSizeX() - margin), Math.max(1.0F, viewport.getSizeY() - margin));
        boolean commit = false;
        if (ImGui.begin(I18n.get("noodling.gui.title"), WINDOW_FLAGS)) {
            ImGui.setNextItemWidth(px(SEED_WIDTH));
            ImGui.inputText(I18n.get("noodling.gui.seed"), this.seedBuffer);
            if (ImGui.isItemDeactivatedAfterEdit()) {
                this.seed = this.seedBuffer.get();
                commit = true;
            }
            ImGui.sameLine();
            if (ImGui.checkbox(I18n.get("noodling.gui.lock_seed"), this.seedLockedBox)) {
                this.seedLocked = this.seedLockedBox.get();
                commit = true;
            }
            ImGui.sameLine(0.0F, px(SPACER));
            ImGui.setNextItemWidth(px(SEED_WIDTH));
            if (ImGui.combo(I18n.get("noodling.gui.pasta"), this.shapeIndex, this.shapeNames)) {
                int index = this.shapeIndex.get();
                this.shapeId = index <= 0 ? null : this.pastaShapeLookup.getList().get(index - 1).id();
                this.params.clear();
                this.paramSeedBuffers.clear();
                commit = true;
            }

            PastaShape shape = this.shape();
            if (shape != null) {
                ImGui.separatorText(I18n.get(shape.descriptionId()));
                commit |= this.drawParams(shape);
            }

            ImGui.separator();
            ImGui.beginDisabled(NoodlerExportClient.isBusy());
            if (ImGui.button(I18n.get("noodling.gui.export"))) {
                NoodlerExportClient.beginExport();
            }
            ImGui.endDisabled();
            ImGui.sameLine();
            ImGui.textDisabled(NoodlerExportClient.status().getString());
        }
        ImGui.end();

        if (commit) {
            this.screen.commit(this.snapshot());
        }
    }

    private boolean drawParams(PastaShape shape) {
        if (!ImGui.beginTable("params", 3, TABLE_FLAGS)) {
            return false;
        }
        boolean commit = false;
        ResolvedParams siblings = new ResolvedParams();
        ImGui.tableSetupColumn(I18n.get("noodling.gui.param_seed"));
        ImGui.tableSetupColumn(I18n.get("noodling.gui.param_lock"));
        ImGui.tableSetupColumn(I18n.get("noodling.gui.param_value"));
        ImGui.tableHeadersRow();

        for (PastaParam param : shape.params()) {
            String name = param.name();
            ParamState state = this.params.getOrDefault(name, ParamState.EMPTY);
            ParamValue value = state.value().filter(param::accepts).orElseGet(() -> param.roll(RandomSource.create(0L), siblings));

            ImGui.tableNextRow();
            ImGui.tableNextColumn();
            ImGui.pushID(name);

            String storedSeed = state.seed();
            ImString seedBuffer = this.paramSeedBuffers.computeIfAbsent(name, key -> new ImString(storedSeed, SEED_CAPACITY));
            ImGui.setNextItemWidth(px(PARAM_SEED_WIDTH));
            ImGui.inputText("##seed", seedBuffer);
            if (ImGui.isItemDeactivatedAfterEdit()) {
                String text = seedBuffer.get();
                state = state.withSeed(text);
                if (!text.isBlank()) {
                    state = state.withLocked(true);
                }
                commit = true;
            }

            ImGui.tableNextColumn();
            ImBoolean lock = new ImBoolean(state.locked());
            if (ImGui.checkbox("##lock", lock)) {
                state = state.withLocked(lock.get());
                commit = true;
            }

            ImGui.tableNextColumn();
            ImGui.beginDisabled(!state.locked());
            KnobResult result = this.drawKnob(param, value, siblings, state.locked());
            ImGui.endDisabled();
            if (result.value() != null) {
                value = result.value();
                state = state.withValue(value).withSeed("");
                seedBuffer.set("");
            }
            commit |= result.committed();

            this.params.put(name, state);
            siblings.put(name, value);
            ImGui.popID();
        }
        ImGui.endTable();
        return commit;
    }

    private KnobResult drawKnob(PastaParam param, ParamValue current, ResolvedParams siblings, boolean locked) {
        String label = I18n.get(param.descriptionId());
        switch (param.kind()) {
            case FLOAT_SLIDER -> {
                PastaParam.Bounds bounds = Objects.requireNonNull(param.bounds(siblings));
                float value = ((ParamValue.Num) current).asFloat();
                float clamped = bounds.clamp(value);
                this.floatBuffer[0] = clamped;
                ImGui.setNextItemWidth(px(KNOB_WIDTH));
                boolean edited = ImGui.sliderFloat(label, this.floatBuffer, bounds.min(), bounds.max(), "%.3f", ImGuiSliderFlags.AlwaysClamp);
                boolean finished = ImGui.isItemDeactivatedAfterEdit();
                boolean clampedChange = locked && clamped != value;
                return new KnobResult(edited || clampedChange ? ParamValue.Num.of(this.floatBuffer[0]) : null, finished || clampedChange);
            }
            case INT_SLIDER -> {
                PastaParam.Bounds bounds = Objects.requireNonNull(param.bounds(siblings));
                int value = ((ParamValue.Num) current).asInt();
                int clamped = bounds.clamp(value);
                this.intBuffer[0] = clamped;
                ImGui.setNextItemWidth(px(KNOB_WIDTH));
                boolean edited = ImGui.sliderInt(label, this.intBuffer, Math.round(bounds.min()), Math.round(bounds.max()), "%d", ImGuiSliderFlags.AlwaysClamp);
                boolean finished = ImGui.isItemDeactivatedAfterEdit();
                boolean clampedChange = locked && clamped != value;
                return new KnobResult(edited || clampedChange ? ParamValue.Num.of(this.intBuffer[0]) : null, finished || clampedChange);
            }
            case FLOAT_FIELD -> {
                this.fieldBuffer.set(((ParamValue.Num) current).asFloat());
                ImGui.setNextItemWidth(px(KNOB_WIDTH));
                boolean edited = ImGui.inputFloat(label, this.fieldBuffer, 0.0F, 0.0F, "%.3f");
                boolean finished = ImGui.isItemDeactivatedAfterEdit();
                return new KnobResult(edited ? ParamValue.Num.of(this.fieldBuffer.get()) : null, finished);
            }
            case BLOCK -> {
                Block block = ((ParamValue.Block) current).state().getBlock();
                this.blockIndex.set(Math.max(0, this.blocks.indexOf(block)));
                ImGui.setNextItemWidth(px(KNOB_WIDTH));
                if (ImGui.combo(label, this.blockIndex, this.blockNames)) {
                    return new KnobResult(new ParamValue.Block(this.blocks.get(this.blockIndex.get()).defaultBlockState()), true);
                }
                return KnobResult.NONE;
            }
            case SAUCE -> {
                Block block = ((ParamValue.Block) current).state().getBlock();
                this.sauceIndex.set(Math.max(0, this.sauces.indexOf(block)));
                ImGui.setNextItemWidth(px(KNOB_WIDTH));
                if (ImGui.combo(label, this.sauceIndex, this.sauceNames)) {
                    return new KnobResult(new ParamValue.Block(this.sauces.get(this.sauceIndex.get()).defaultBlockState()), true);
                }
                return KnobResult.NONE;
            }
            case ROTATION -> {
                ParamValue.Rotation rotation = (ParamValue.Rotation) current;
                this.axisBuffer[0] = (float) rotation.axis().x;
                this.axisBuffer[1] = (float) rotation.axis().y;
                this.axisBuffer[2] = (float) rotation.axis().z;
                this.angleBuffer.set(rotation.angle());
                ImGui.setNextItemWidth(px(KNOB_WIDTH));
                boolean axisEdited = ImGui.inputFloat3(I18n.get("noodling.gui.axis"), this.axisBuffer, "%.3f");
                boolean finished = ImGui.isItemDeactivatedAfterEdit();
                ImGui.setNextItemWidth(px(KNOB_WIDTH));
                boolean angleEdited = ImGui.inputFloat(I18n.get("noodling.gui.radians"), this.angleBuffer, 0.0F, 0.0F, "%.4f");
                finished |= ImGui.isItemDeactivatedAfterEdit();
                ParamValue value = axisEdited || angleEdited
                        ? new ParamValue.Rotation(new Vec3(this.axisBuffer[0], this.axisBuffer[1], this.axisBuffer[2]), this.angleBuffer.get())
                        : null;
                return new KnobResult(value, finished);
            }
        }
        return KnobResult.NONE;
    }
}
