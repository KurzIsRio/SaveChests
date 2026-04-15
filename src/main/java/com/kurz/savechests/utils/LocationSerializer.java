package com.kurz.savechests.utils;

import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.Type;
import java.util.UUID;

public class LocationSerializer implements JsonSerializer<Location>, JsonDeserializer<Location> {

    @Override
    public JsonElement serialize(Location src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("world", src.getWorld().getUID().toString());
        jsonObject.addProperty("x", src.getX());
        jsonObject.addProperty("y", src.getY());
        jsonObject.addProperty("z", src.getZ());
        jsonObject.addProperty("yaw", src.getYaw());
        jsonObject.addProperty("pitch", src.getPitch());
        return jsonObject;
    }

    @Override
    public Location deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        UUID worldUUID = UUID.fromString(jsonObject.get("world").getAsString());
        World world = Bukkit.getWorld(worldUUID);

        // If the world is not loaded, we can't create the location.
        // This might happen if a world is deleted or renamed.
        // We'll default to the main world as a fallback.
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }

        double x = jsonObject.get("x").getAsDouble();
        double y = jsonObject.get("y").getAsDouble();
        double z = jsonObject.get("z").getAsDouble();
        float yaw = jsonObject.get("yaw").getAsFloat();
        float pitch = jsonObject.get("pitch").getAsFloat();

        return new Location(world, x, y, z, yaw, pitch);
    }
}
