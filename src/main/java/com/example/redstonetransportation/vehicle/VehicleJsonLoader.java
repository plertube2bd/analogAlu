package com.example.redstonetransportation.vehicle;

import com.example.redstonetransportation.drive.DriveMode;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Loads data-only vehicle variants; behavior implementations must already be registered in Java. */
public final class VehicleJsonLoader {
    private VehicleJsonLoader() {}

    public static List<String> load(
            Path directory,
            VehicleCatalog catalog,
            VehicleBehaviorRegistry behaviors
    ) {
        List<String> messages = new ArrayList<>();
        try {
            Files.createDirectories(directory);
            try (var paths = Files.list(directory)) {
                for (Path file : paths
                        .filter(path -> path.getFileName().toString().endsWith(".json"))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .toList()) {
                    try (Reader reader = Files.newBufferedReader(file)) {
                        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                        JsonObject model = root.getAsJsonObject("model");

                        VehicleDefinition definition = new VehicleDefinition(
                                requiredString(root, "id"),
                                requiredString(root, "behavior"),
                                requiredString(root, "display_name"),
                                requiredDouble(root, "max_speed"),
                                requiredDouble(root, "acceleration"),
                                requiredInt(root, "minimum_signal"),
                                DriveMode.valueOf(requiredString(root, "drive_mode")),
                                new ModelDefinition(
                                        requiredString(model, "renderer"),
                                        requiredString(model, "model"),
                                        requiredString(model, "texture"),
                                        model.has("animation") ? model.get("animation").getAsString() : ""
                                )
                        );

                        if (behaviors.get(definition.behaviorId()).isEmpty()) {
                            throw new IllegalArgumentException(
                                    "unregistered behavior: " + definition.behaviorId()
                            );
                        }
                        catalog.register(definition);
                        messages.add("Loaded vehicle " + definition.id());
                    } catch (Exception exception) {
                        messages.add("Skipped vehicle file " + file.getFileName() + ": " + exception.getMessage());
                    }
                }
            }
        } catch (IOException exception) {
            messages.add("Could not read vehicle directory " + directory + ": " + exception.getMessage());
        }
        return List.copyOf(messages);
    }

    private static String requiredString(JsonObject json, String name) {
        if (json == null || !json.has(name) || json.get(name).isJsonNull()) {
            throw new IllegalArgumentException("missing string field: " + name);
        }
        String value = json.get(name).getAsString();
        if (value.isBlank()) {
            throw new IllegalArgumentException("blank string field: " + name);
        }
        return value;
    }

    private static double requiredDouble(JsonObject json, String name) {
        if (json == null || !json.has(name)) {
            throw new IllegalArgumentException("missing number field: " + name);
        }
        return json.get(name).getAsDouble();
    }

    private static int requiredInt(JsonObject json, String name) {
        if (json == null || !json.has(name)) {
            throw new IllegalArgumentException("missing integer field: " + name);
        }
        return json.get(name).getAsInt();
    }
}
