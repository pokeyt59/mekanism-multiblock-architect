package com.syaru.mekpreview.client.design;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class FavoritesStoreTest {
    @TempDir Path temp;
    private FavoritesStore store() { return new FavoritesStore(temp.resolve("config/mekpreview/favorites.json")); }
    private FavoritesStore.View view() { return new FavoritesStore.View("SINGLE", 4, 3, true); }
    @Test void persistsUnicodeLabelNativeStateAndViewAcrossNewStoreInstance() throws Exception {
        var saved = store().add("電気炉・チタン", "nova:blast", "{NovaSpeed:4,Width:7}", view());
        assertEquals(saved, store().list().get(0));
    }
    @Test void duplicateNamesRemainSeparateDesigns() throws Exception {
        var a = store().add("same", "a", "{}", view()); var b = store().add("same", "b", "{}", view());
        assertNotEquals(a.id(), b.id()); assertEquals(2, store().list().size());
    }
    @Test void deletionKeepsOthersAndRetainsPreviousFileAsBackup() throws Exception {
        var a = store().add("first", "a", "{}", view()); var b = store().add("second", "b", "{}", view());
        store().remove(a.id()); assertEquals(java.util.List.of(b), store().list());
        assertTrue(Files.readString(store().file().resolveSibling("favorites.json.bak")).contains(a.id()));
    }
    @Test void malformedFileIsNeverOverwritten() throws Exception {
        var store = store(); Files.createDirectories(store.file().getParent()); Files.writeString(store.file(), "not json");
        assertThrows(IOException.class, () -> store.add("new", "a", "{}", view()));
        assertEquals("not json", Files.readString(store.file()));
    }
    @Test void unknownVersionIsNeverOverwritten() throws Exception {
        var store = store(); Files.createDirectories(store.file().getParent()); String content = "{\"version\":99,\"designs\":[]}";
        Files.writeString(store.file(), content); assertThrows(IOException.class, () -> store.remove("id"));
        assertEquals(content, Files.readString(store.file()));
    }
}
