package com.kurz.savechests.config;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Type;
import java.util.Map;

public class GsonAdapter {

    public static class ItemStackAdapter implements JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {

        @Override
        public JsonElement serialize(ItemStack item, Type type, JsonSerializationContext context) {
            return context.serialize(item.serialize());
        }

        @Override
        public ItemStack deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            try {
                Map<String, Object> map = context.deserialize(json, new com.google.gson.reflect.TypeToken<Map<String, Object>>() {}.getType());
                return ItemStack.deserialize(map);
            } catch (Exception e) {
                if (json.isJsonPrimitive()) {
                    return new ItemStack(Material.valueOf(json.getAsString()));
                } else if (json.isJsonObject()) {
                    JsonObject obj = json.getAsJsonObject();
                    Material mat = Material.valueOf(obj.get("type").getAsString());
                    int amount = obj.has("amount") ? obj.get("amount").getAsInt() : 1;
                    return new ItemStack(mat, amount);
                }
                throw new JsonParseException("Failed to deserialize ItemStack", e);
            }
        }
    }
}
