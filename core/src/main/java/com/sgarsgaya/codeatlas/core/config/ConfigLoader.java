package com.sgarsgaya.codeatlas.core.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Loads {@link RepoAtlasConfig} from {@code .repoatlas/config.yaml},
 * merging any present values over the built-in defaults.
 */
public final class ConfigLoader {

    private static final String CONFIG_DIR = ".repoatlas";
    private static final String CONFIG_FILE = "config.yaml";

    private ConfigLoader() {}

    public static RepoAtlasConfig load(Path repoRoot) {
        Path configPath = repoRoot.resolve(CONFIG_DIR).resolve(CONFIG_FILE);
        if (!Files.exists(configPath)) {
            return loadDefaults();
        }

        try (InputStream in = Files.newInputStream(configPath)) {
            Yaml yaml = new Yaml();
            Map<String, Object> raw = yaml.load(in);
            if (raw == null) {
                return loadDefaults();
            }
            return mergeIntoDefaults(raw);
        } catch (IOException e) {
            throw new ConfigLoadException("Failed to read " + configPath, e);
        }
    }

    public static RepoAtlasConfig loadDefaults() {
        return new RepoAtlasConfig();
    }

    @SuppressWarnings("unchecked")
    private static RepoAtlasConfig mergeIntoDefaults(Map<String, Object> raw) {
        RepoAtlasConfig config = loadDefaults();

        if (raw.containsKey("snapshotRetentionCount")) {
            config.setSnapshotRetentionCount(toInt(raw.get("snapshotRetentionCount")));
        }

        if (raw.get("layers") instanceof Map<?, ?> layersMap) {
            mergeLayersConfig(config.getLayers(), (Map<String, Object>) layersMap);
        }

        if (raw.get("ambiguity") instanceof Map<?, ?> ambiguityMap) {
            mergeAmbiguityConfig(config.getAmbiguity(), (Map<String, Object>) ambiguityMap);
        }

        if (raw.get("generated") instanceof Map<?, ?> generatedMap) {
            mergeGeneratedConfig(config.getGenerated(), (Map<String, Object>) generatedMap);
        }

        if (raw.get("serviceClients") instanceof Map<?, ?> scMap) {
            mergeServiceClientsConfig(config.getServiceClients(), (Map<String, Object>) scMap);
        }

        if (raw.get("ci") instanceof Map<?, ?> ciMap) {
            mergeCiConfig(config.getCi(), (Map<String, Object>) ciMap);
        }

        if (raw.get("mcp") instanceof Map<?, ?> mcpMap) {
            mergeMcpConfig(config.getMcp(), (Map<String, Object>) mcpMap);
        }

        return config;
    }

    @SuppressWarnings("unchecked")
    private static void mergeLayersConfig(LayersConfig target, Map<String, Object> raw) {
        if (raw.get("layers") instanceof Map<?, ?> layerDefs) {
            for (Map.Entry<?, ?> entry : layerDefs.entrySet()) {
                String name = entry.getKey().toString();
                if (entry.getValue() instanceof Map<?, ?> defMap) {
                    LayerDef def = new LayerDef();
                    if (defMap.get("packages") instanceof List<?> pkgs) {
                        def.setPackages(toStringList(pkgs));
                    }
                    if (defMap.get("canCall") instanceof List<?> calls) {
                        def.setCanCall(toStringList(calls));
                    }
                    target.getLayers().put(name, def);
                }
            }
        }
    }

    private static void mergeAmbiguityConfig(AmbiguityConfig target, Map<String, Object> raw) {
        if (raw.containsKey("ciThresholdPercent")) {
            target.setCiThresholdPercent(toInt(raw.get("ciThresholdPercent")));
        }
    }

    private static void mergeGeneratedConfig(GeneratedConfig target, Map<String, Object> raw) {
        if (raw.get("paths") instanceof List<?> paths) {
            target.setPaths(toStringList(paths));
        }
    }

    private static void mergeServiceClientsConfig(ServiceClientsConfig target, Map<String, Object> raw) {
        if (raw.get("dependencyPatterns") instanceof List<?> patterns) {
            target.setDependencyPatterns(toStringList(patterns));
        }
    }

    @SuppressWarnings("unchecked")
    private static void mergeCiConfig(CiConfig target, Map<String, Object> raw) {
        if (raw.get("blockOn") instanceof List<?> blockOn) {
            target.setBlockOn(toStringList(blockOn));
        }
        if (raw.containsKey("rejectOverlay")) {
            target.setRejectOverlay(Boolean.TRUE.equals(raw.get("rejectOverlay")));
        }
    }

    @SuppressWarnings("unchecked")
    private static void mergeMcpConfig(McpConfig target, Map<String, Object> raw) {
        if (raw.get("dirtyOverlay") instanceof Map<?, ?> overlayMap) {
            mergeDirtyOverlayConfig(target.getDirtyOverlay(), (Map<String, Object>) overlayMap);
        }
    }

    private static void mergeDirtyOverlayConfig(DirtyOverlayConfig target, Map<String, Object> raw) {
        if (raw.get("defaultMode") instanceof String mode) {
            target.setDefaultMode(mode);
        }
        if (raw.containsKey("allowExperimentalOptIn")) {
            target.setAllowExperimentalOptIn(Boolean.TRUE.equals(raw.get("allowExperimentalOptIn")));
        }
        if (raw.get("forbiddenUses") instanceof List<?> uses) {
            target.setForbiddenUses(toStringList(uses));
        }
    }

    private static int toInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private static List<String> toStringList(List<?> raw) {
        return raw.stream()
                .map(Object::toString)
                .toList();
    }
}
