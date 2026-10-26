package com.syaru.mekpreview.client.design;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Versioned, atomic client-local design storage. Invalid/unknown files are never overwritten. */
public final class FavoritesStore {
    public record View(String mode, int layer, int rotation, boolean hideShell) {}
    public record Favorite(String id, String label, String equipment, String state, View view) {
        public Favorite {
            Objects.requireNonNull(view);
            if (id == null || label == null || equipment == null || state == null || label.isBlank()
                    || id.length() > 64 || label.length() > 120 || equipment.length() > 256 || state.length() > 65536)
                throw new IllegalArgumentException("Invalid saved design");
        }
    }
    private record FileData(int version, List<Favorite> designs) {}
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Path file;
    public FavoritesStore(Path file) { this.file = file.toAbsolutePath().normalize(); }
    public Path file() { return file; }
    public synchronized List<Favorite> list() throws IOException {
        if (!Files.exists(file)) return List.of();
        if (Files.size(file) > 10 * 1024 * 1024) throw new IOException("Saved designs file is too large");
        try {
            var data = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), FileData.class);
            if (data == null || data.version() != 1 || data.designs() == null || data.designs().size() > 128)
                throw new IOException("Unsupported saved designs format");
            Set<String> ids = new HashSet<>();
            for (var design : data.designs()) {
                if (design == null || !ids.add(design.id())) throw new IOException("Invalid/duplicate saved design");
            }
            return List.copyOf(data.designs());
        } catch (JsonParseException | IllegalArgumentException | NullPointerException e) {
            throw new IOException("Unable to read saved designs; original file preserved", e);
        }
    }
    public synchronized Favorite add(String label, String equipment, String state, View view) throws IOException {
        var designs = new ArrayList<>(list());
        if (designs.size() >= 128) throw new IOException("Saved design limit reached");
        var favorite = new Favorite(UUID.randomUUID().toString(), label.strip(), equipment, state, view);
        designs.add(favorite); write(designs); return favorite;
    }
    public synchronized void remove(String id) throws IOException {
        var designs = new ArrayList<>(list());
        if (designs.removeIf(f -> f.id().equals(id))) write(designs);
    }
    private void write(List<Favorite> designs) throws IOException {
        Files.createDirectories(file.getParent());
        Path temp = Files.createTempFile(file.getParent(), "favorites-", ".tmp");
        try {
            Files.writeString(temp, GSON.toJson(new FileData(1, designs)), StandardCharsets.UTF_8);
            if (Files.exists(file)) Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            try { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
}
