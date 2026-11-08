package net.inertiamc.anythinginabucket.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlatYamlTest {

    @Test
    void readsTheShapesConfigUses() {
        Map<String, Object> yaml = FlatYaml.parse("""
                # a comment
                catch-with: capture-bucket   # trailing comment
                give-bucket-back: true
                cooldown: 1s
                blacklist:
                  - wither
                  - "ender_dragon"
                disabled-worlds: []
                capture-bucket:
                  recipe:
                    enabled: false
                    shape:
                      - " e "
                      - 'ebe'
                    ingredients:
                      e: ender_pearl
                sounds:
                  catch: item.bucket.fill_axolotl
                  release: { name: block.note_block.bell, volume: 0.5, pitch: 2 }
                  deny: ""
                list-at-key-indent:
                - a
                - b
                empty:
                """);
        assertEquals("capture-bucket", yaml.get("catch-with"));
        assertEquals(true, yaml.get("give-bucket-back"));
        assertEquals(List.of("wither", "ender_dragon"), yaml.get("blacklist"));
        assertEquals(List.of(), yaml.get("disabled-worlds"));
        Map<?, ?> recipe = (Map<?, ?>) ((Map<?, ?>) yaml.get("capture-bucket")).get("recipe");
        assertEquals(false, recipe.get("enabled"));
        assertEquals(List.of(" e ", "ebe"), recipe.get("shape"));
        assertEquals("ender_pearl", ((Map<?, ?>) recipe.get("ingredients")).get("e"));
        Map<?, ?> release = (Map<?, ?>) ((Map<?, ?>) yaml.get("sounds")).get("release");
        assertEquals("block.note_block.bell", release.get("name"));
        assertEquals(0.5, release.get("volume"));
        assertEquals(2, release.get("pitch"));
        assertEquals("", ((Map<?, ?>) yaml.get("sounds")).get("deny"));
        assertEquals(List.of("a", "b"), yaml.get("list-at-key-indent"));
        assertTrue(yaml.containsKey("empty"));
        assertNull(yaml.get("empty"));
    }

    @Test
    void badLinesNameTheirNumber() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> FlatYaml.parse("a: 1\nthis is not a setting\n"));
        assertTrue(e.getMessage().startsWith("line 2"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> FlatYaml.parse("a: { b: 1\n"));
    }
}
