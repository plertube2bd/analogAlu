package com.example.redstonetransportation.vehicle;

/** Resource locations are stored as strings so the core definition is renderer-agnostic. */
public record ModelDefinition(
        String renderer,
        String model,
        String texture,
        String animation
) {
    public ModelDefinition {
        if (renderer == null || renderer.isBlank()) {
            throw new IllegalArgumentException("renderer must not be blank");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("model must not be blank");
        }
        if (texture == null || texture.isBlank()) {
            throw new IllegalArgumentException("texture must not be blank");
        }
        animation = animation == null ? "" : animation;
    }
}
