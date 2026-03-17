package games.alejandrocoria.mapfrontiers.api.model;

/**
 * Banner metadata stored in a frontier.
 *
 * @param baseColorId vanilla dye color id
 * @param patternsNbt serialized banner pattern list as SNBT text, or null
 * @param rotation banner rotation value used by MapFrontiers rendering
 */
public record FrontierBanner(int baseColorId, String patternsNbt, int rotation) {
}
