package us.drullk.noodling.client.noodler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;
import org.lwjgl.sdl.SDLDialog;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDL_DialogFileCallback;
import org.lwjgl.sdl.SDL_DialogFileFilter;
import org.lwjgl.system.MemoryUtil;
import us.drullk.noodling.Noodling;
import us.drullk.noodling.network.NoodlerExportRequestPayload;
import us.drullk.noodling.network.NoodlerExportResultPayload;
import us.drullk.noodling.noodler.NoodlerComponents;
import us.drullk.noodling.noodler.NoodlerItem;
import us.drullk.noodling.noodler.NoodlerState;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Client side of the structure template export.
 *
 * <p>The button opens the operating system's save dialog through SDL3 ({@code SDL_ShowSaveFileDialog}), which
 * returns at once; the dialog's callback may run on any thread, so it only copies the chosen path and hands it to
 * the client thread. The client then asks the server for the template bytes and writes them to the chosen file when
 * the reply arrives. The dialog starts in the game directory with a file name made of the shape and seed.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
public final class NoodlerExportClient {

    private static final String EXTENSION = ".nbt";

    private static final Map<Integer, Path> pending = new HashMap<>();
    private static int nextRequestId = 1;
    private static boolean dialogOpen;
    private static Component status = Component.empty();

    /** Native resources that must outlive the dialog; freed when its callback has run. */
    private static @Nullable SDL_DialogFileCallback callback;
    private static SDL_DialogFileFilter.@Nullable Buffer filters;
    private static @Nullable ByteBuffer filterName;
    private static @Nullable ByteBuffer filterPattern;

    private NoodlerExportClient() {
    }

    /** The last outcome, shown next to the export button. */
    public static Component status() {
        return status;
    }

    public static boolean isBusy() {
        return dialogOpen || !pending.isEmpty();
    }

    /** Opens the save dialog for the Noodler in the main hand. Must be called on the client thread. */
    public static void beginExport() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || isBusy()) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof NoodlerItem)) {
            status = Component.translatable("noodling.export.no_noodler");
            return;
        }
        Path defaultFile = minecraft.gameDirectory.toPath().toAbsolutePath().resolve(defaultFileName(stack.getOrDefault(NoodlerComponents.STATE.get(), NoodlerState.DEFAULT)));

        releaseNativeResources();
        filterName = MemoryUtil.memUTF8("Structure template");
        filterPattern = MemoryUtil.memUTF8("nbt");
        filters = SDL_DialogFileFilter.calloc(1);
        filters.get(0).name(filterName).pattern(filterPattern);
        callback = SDL_DialogFileCallback.create((userdata, fileList, filterIndex) -> {
            String chosen = null;
            String error = null;
            if (fileList == MemoryUtil.NULL) {
                error = SDLError.SDL_GetError();
            } else {
                long first = MemoryUtil.memGetAddress(fileList);
                if (first != MemoryUtil.NULL) {
                    chosen = MemoryUtil.memUTF8(first);
                }
            }
            String chosenPath = chosen;
            String errorText = error;
            Minecraft.getInstance().execute(() -> onDialogClosed(chosenPath, errorText));
        });
        dialogOpen = true;
        status = Component.translatable("noodling.export.choosing");
        SDLDialog.SDL_ShowSaveFileDialog(callback, MemoryUtil.NULL, minecraft.getWindow().handle(), filters, defaultFile.toString());
    }

    static String defaultFileName(NoodlerState state) {
        String shape = state.shape().orElse("random");
        String seed = state.seed().isBlank() ? "unseeded" : state.seed().replaceAll("[^A-Za-z0-9_-]", "_");
        return "noodler_" + shape + "_" + seed + EXTENSION;
    }

    static void onDialogClosed(@Nullable String chosenPath, @Nullable String error) {
        dialogOpen = false;
        releaseNativeResources();
        if (error != null) {
            status = Component.translatable("noodling.export.failed", error);
            return;
        }
        if (chosenPath == null) {
            status = Component.translatable("noodling.export.cancelled");
            return;
        }
        Path path = Path.of(chosenPath.endsWith(EXTENSION) ? chosenPath : chosenPath + EXTENSION);
        int requestId = nextRequestId++;
        pending.put(requestId, path);
        status = Component.translatable("noodling.export.requesting");
        ClientPacketDistributor.sendToServer(new NoodlerExportRequestPayload(requestId));
    }

    /** Handles the server's reply on the client thread. */
    public static void onResult(NoodlerExportResultPayload payload) {
        Path path = pending.remove(payload.requestId());
        if (path == null) {
            return;
        }
        if (!payload.isSuccess()) {
            status = Component.translatable(payload.errorKey());
            tell(status);
            return;
        }
        try {
            Files.write(path, payload.data());
            status = Component.translatable("noodling.export.saved", path.toString());
        } catch (IOException e) {
            Noodling.LOGGER.error("Could not write Noodler export to {}", path, e);
            status = Component.translatable("noodling.export.failed", e.getMessage() == null ? e.toString() : e.getMessage());
        }
        tell(status);
    }

    private static void tell(Component message) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendSystemMessage(message);
        }
    }

    private static void releaseNativeResources() {
        if (callback != null) {
            callback.free();
            callback = null;
        }
        if (filters != null) {
            filters.free();
            filters = null;
        }
        if (filterName != null) {
            MemoryUtil.memFree(filterName);
            filterName = null;
        }
        if (filterPattern != null) {
            MemoryUtil.memFree(filterPattern);
            filterPattern = null;
        }
    }
}
