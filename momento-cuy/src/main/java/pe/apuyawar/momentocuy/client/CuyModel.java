package pe.apuyawar.momentocuy.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adaptado sin cambios de lógica del {@code CuyModel} del mod antiguo (ver
 * {@code CuyJardineroEntity} para el proceso completo de recuperación):
 * carga el {@code .bbmodel} de Blockbench directamente como JSON en tiempo
 * de ejecución (grupos → huesos, elementos → cubos, animators → curvas de
 * posición/rotación/escala por fotograma clave) y dibuja cada cubo a mano con
 * un {@link VertexConsumer} — sin GeckoLib ni ninguna otra librería de
 * animación externa, autocontenido, igual que en el original.
 */
public final class CuyModel {
    private static final Identifier DATA = Identifier.of("momento_cuy", "models/entity/cuy_jardinero.bbmodel");
    private static final String[] FACES = {"north", "south", "east", "west", "up", "down"};
    private final Node root;
    private int currentOverlay = OverlayTexture.DEFAULT_UV;
    private final Map<String, JsonObject> animations = new HashMap<>();

    public CuyModel() {
        try {
            try (InputStreamReader reader = new InputStreamReader(
                    MinecraftClient.getInstance().getResourceManager().getResource(DATA).orElseThrow().getInputStream(), StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                Map<String, JsonObject> groups = new HashMap<>();
                Map<String, JsonObject> cubes = new HashMap<>();
                for (JsonElement e : json.getAsJsonArray("groups")) {
                    JsonObject g = e.getAsJsonObject();
                    groups.put(g.get("uuid").getAsString(), g);
                }
                for (JsonElement e : json.getAsJsonArray("elements")) {
                    JsonObject cube = e.getAsJsonObject();
                    cubes.put(cube.get("uuid").getAsString(), cube);
                }
                for (JsonElement element : json.getAsJsonArray("animations")) {
                    JsonObject animation = element.getAsJsonObject();
                    this.animations.put(animation.get("name").getAsString(), animation);
                }
                JsonObject first = json.getAsJsonArray("outliner").get(0).getAsJsonObject();
                this.root = loadNode(first, groups, cubes);
            }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cargar el modelo del Cuy Jardinero", e);
        }
    }

    private static float[] vec(JsonObject object, String field) {
        float[] v = {0.0F, 0.0F, 0.0F};
        if (object.has(field) && object.get(field).isJsonArray()) {
            JsonArray a = object.getAsJsonArray(field);
            for (int i = 0; i < 3; i++) v[i] = a.get(i).getAsFloat();
        }
        return v;
    }

    private static Node loadNode(JsonObject descriptor, Map<String, JsonObject> groups, Map<String, JsonObject> cubes) {
        String id = descriptor.get("uuid").getAsString();
        JsonObject group = groups.get(id);
        if (group == null) throw new IllegalStateException("Grupo no encontrado: " + id);
        Node node = new Node(id, vec(group, "origin"), vec(group, "rotation"));
        for (JsonElement e : descriptor.getAsJsonArray("children")) {
            if (e.isJsonPrimitive()) {
                JsonObject cube = cubes.get(e.getAsString());
                if (cube == null) throw new IllegalStateException("Cubo no encontrado: " + e.getAsString());
                node.cubes.add(cube);
            } else if (e.isJsonObject()) {
                node.children.add(loadNode(e.getAsJsonObject(), groups, cubes));
            }
        }
        return node;
    }

    public void setHurtOverlay(boolean hurt) {
        this.currentOverlay = OverlayTexture.getUv(0.0F, hurt);
    }

    public void render(MatrixStack matrices, VertexConsumer vertices, int light) {
        this.drawNode(this.root, new float[]{0.0F, 0.0F, 0.0F}, matrices, vertices, light);
    }

    private void drawNode(Node node, float[] parent, MatrixStack matrices, VertexConsumer vertices, int light) {
        matrices.push();
        matrices.translate((node.pivot[0] - parent[0]) / 16.0, (node.pivot[1] - parent[1]) / 16.0, (node.pivot[2] - parent[2]) / 16.0);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(node.rotation[2]));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(node.rotation[1]));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(node.rotation[0]));
        for (JsonObject cube : node.cubes) this.drawCube(cube, node.pivot, matrices, vertices, light);
        for (Node child : node.children) this.drawNode(child, node.pivot, matrices, vertices, light);
        matrices.pop();
    }

    public void renderAnimated(MatrixStack matrices, VertexConsumer vertices, int light, String animationName, float seconds) {
        JsonObject animation = this.animations.get(animationName);
        if (animation == null) {
            this.render(matrices, vertices, light);
            return;
        }
        float length = animation.get("length").getAsFloat();
        if (length > 0.0F) {
            String loop = animation.get("loop").getAsString();
            seconds = "loop".equals(loop) ? (seconds % length + length) % length : Math.max(0.0F, Math.min(seconds, length));
        }
        this.drawAnimatedNode(this.root, new float[]{0.0F, 0.0F, 0.0F}, matrices, vertices, light, animation, seconds);
    }

    private static float[] keyValue(JsonObject keyframe) {
        JsonObject point = keyframe.getAsJsonArray("data_points").get(0).getAsJsonObject();
        return new float[]{point.get("x").getAsFloat(), point.get("y").getAsFloat(), point.get("z").getAsFloat()};
    }

    private static float[] sample(JsonObject animator, String channel, float seconds, float defaultValue) {
        float[] result = {defaultValue, defaultValue, defaultValue};
        if (animator == null || !animator.has("keyframes")) return result;
        JsonObject before = null, after = null;
        float beforeTime = -Float.MAX_VALUE, afterTime = Float.MAX_VALUE;
        for (JsonElement element : animator.getAsJsonArray("keyframes")) {
            JsonObject frame = element.getAsJsonObject();
            if (channel.equals(frame.get("channel").getAsString())) {
                float time = frame.get("time").getAsFloat();
                if (time <= seconds && time > beforeTime) { before = frame; beforeTime = time; }
                if (time >= seconds && time < afterTime) { after = frame; afterTime = time; }
            }
        }
        if (before == null && after == null) return result;
        if (before == null) return keyValue(after);
        if (after == null) return keyValue(before);
        float[] a = keyValue(before), b = keyValue(after);
        float amount = afterTime > beforeTime ? (seconds - beforeTime) / (afterTime - beforeTime) : 0.0F;
        for (int i = 0; i < 3; i++) result[i] = a[i] + (b[i] - a[i]) * amount;
        return result;
    }

    private void drawAnimatedNode(Node node, float[] parent, MatrixStack matrices, VertexConsumer vertices, int light, JsonObject animation, float seconds) {
        JsonObject animators = animation.getAsJsonObject("animators");
        JsonObject animator = animators == null ? null : animators.getAsJsonObject(node.uuid);
        float[] position = sample(animator, "position", seconds, 0.0F);
        float[] rotation = sample(animator, "rotation", seconds, 0.0F);
        float[] scale = sample(animator, "scale", seconds, 1.0F);
        matrices.push();
        matrices.translate((node.pivot[0] - parent[0] + position[0]) / 16.0, (node.pivot[1] - parent[1] + position[1]) / 16.0, (node.pivot[2] - parent[2] + position[2]) / 16.0);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(node.rotation[2] + rotation[2]));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(node.rotation[1] + rotation[1]));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(node.rotation[0] + rotation[0]));
        matrices.scale(scale[0], scale[1], scale[2]);
        for (JsonObject cube : node.cubes) this.drawCube(cube, node.pivot, matrices, vertices, light);
        for (Node child : node.children) this.drawAnimatedNode(child, node.pivot, matrices, vertices, light, animation, seconds);
        matrices.pop();
    }

    private void drawCube(JsonObject cube, float[] pivot, MatrixStack matrices, VertexConsumer consumer, int light) {
        if (cube.has("visibility") && !cube.get("visibility").getAsBoolean()) return;
        float[] from = vec(cube, "from");
        float[] to = vec(cube, "to");
        float x0 = (from[0] - pivot[0]) / 16.0F, y0 = (from[1] - pivot[1]) / 16.0F, z0 = (from[2] - pivot[2]) / 16.0F;
        float x1 = (to[0] - pivot[0]) / 16.0F, y1 = (to[1] - pivot[1]) / 16.0F, z1 = (to[2] - pivot[2]) / 16.0F;
        float[][][] quads = {
                {{x1, y0, z0}, {x0, y0, z0}, {x0, y1, z0}, {x1, y1, z0}},
                {{x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}},
                {{x1, y0, z1}, {x1, y0, z0}, {x1, y1, z0}, {x1, y1, z1}},
                {{x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}, {x0, y1, z0}},
                {{x0, y1, z0}, {x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}},
                {{x0, y0, z1}, {x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}},
        };
        float[][] normals = {{0, 0, -1}, {0, 0, 1}, {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}};
        JsonObject faces = cube.getAsJsonObject("faces");
        if (faces == null) return;
        MatrixStack.Entry entry = matrices.peek();
        for (int f = 0; f < 6; f++) {
            if (!faces.has(FACES[f])) continue;
            JsonObject face = faces.getAsJsonObject(FACES[f]);
            if (!face.has("uv")) continue;
            JsonArray uv = face.getAsJsonArray("uv");
            float u0 = uv.get(0).getAsFloat() / 64.0F, v0 = uv.get(1).getAsFloat() / 64.0F;
            float u1 = uv.get(2).getAsFloat() / 64.0F, v1 = uv.get(3).getAsFloat() / 64.0F;
            float[][] coords = {{u0, v1}, {u1, v1}, {u1, v0}, {u0, v0}};
            for (int i = 0; i < 4; i++) {
                float[] p = quads[f][i];
                consumer.vertex(entry, p[0], p[1], p[2])
                        .color(255, 255, 255, 255)
                        .texture(coords[i][0], coords[i][1])
                        .overlay(this.currentOverlay)
                        .light(light)
                        .normal(entry, normals[f][0], normals[f][1], normals[f][2]);
            }
        }
    }

    private static final class Node {
        final String uuid;
        final float[] pivot;
        final float[] rotation;
        final List<Node> children = new ArrayList<>();
        final List<JsonObject> cubes = new ArrayList<>();

        Node(String uuid, float[] pivot, float[] rotation) {
            this.uuid = uuid;
            this.pivot = pivot;
            this.rotation = rotation;
        }
    }
}
