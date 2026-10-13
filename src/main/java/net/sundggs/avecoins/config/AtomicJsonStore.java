/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AtomicJsonStore<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AtomicJsonStore.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Path path;
    private final Class<T> type;
    private final Supplier<T> defaults;
    private final UnaryOperator<T> validator;
    private T current;

    public AtomicJsonStore(Path path, Class<T> type, Supplier<T> defaults, UnaryOperator<T> validator) {
        this.path = Objects.requireNonNull(path).toAbsolutePath().normalize();
        this.type = Objects.requireNonNull(type);
        this.defaults = Objects.requireNonNull(defaults);
        this.validator = Objects.requireNonNull(validator);
        this.current = validator.apply(defaults.get());
    }

    public synchronized T load() {
        this.loadCurrent();
        return this.current;
    }

    public synchronized boolean reload() {
        return this.loadCurrent();
    }

    private boolean loadCurrent() {
        try {
            Files.createDirectories(this.path.getParent(), new FileAttribute[0]);
            if (Files.notExists(this.path, new LinkOption[0])) {
                return this.save(this.current);
            }
            String json = Files.readString(this.path, StandardCharsets.UTF_8);
            AtomicJsonStore.validateStrictJson(json);
            JsonElement root = JsonParser.parseString((String)json);
            AtomicJsonStore.validateKnownFields(root, this.type);
            T candidate = GSON.fromJson(root, this.type);
            this.current = this.validator.apply(candidate);
            return true;
        }
        catch (Exception exception) {
            LOGGER.error("Could not load {}. Keeping the last valid configuration.", this.path, exception);
            return false;
        }
    }

    public synchronized boolean save(T candidate) {
        T validated;
        try {
            validated = this.validator.apply(candidate);
        }
        catch (RuntimeException exception) {
            LOGGER.warn("Rejected invalid configuration for {}: {}", this.path, exception.getMessage());
            return false;
        }
        Path temporary = null;
        try {
            Files.createDirectories(this.path.getParent(), new FileAttribute[0]);
            temporary = Files.createTempFile(this.path.getParent(), this.path.getFileName().toString(), ".tmp", new FileAttribute[0]);
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8, new OpenOption[0]);){
                GSON.toJson(validated, (Appendable)writer);
            }
            try {
                Files.move(temporary, this.path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, this.path, StandardCopyOption.REPLACE_EXISTING);
            }
            this.current = validated;
            return true;
        }
        catch (IOException exception) {
            LOGGER.error("Could not save {}. Keeping the previous in-memory state.", this.path, exception);
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                }
                catch (IOException cleanupException) {
                    LOGGER.debug("Could not remove temporary file {}", temporary, cleanupException);
                }
            }
            return false;
        }
    }

    public synchronized T get() {
        return this.current;
    }

    public Path path() {
        return this.path;
    }

    public static Gson gson() {
        return GSON;
    }

    public static void requireStrictJson(String json) {
        try {
            AtomicJsonStore.validateStrictJson(json);
        }
        catch (IOException exception) {
            throw new IllegalArgumentException("Invalid strict JSON", exception);
        }
    }

    public static <T> T parseStrict(String json, Class<T> type) {
        AtomicJsonStore.requireStrictJson(json);
        JsonElement root = JsonParser.parseString((String)json);
        try {
            AtomicJsonStore.validateKnownFields(root, type);
        }
        catch (IOException exception) {
            throw new IllegalArgumentException("Invalid fields for " + type.getSimpleName(), exception);
        }
        Object value = GSON.fromJson(root, type);
        if (value == null) {
            throw new IllegalArgumentException("JSON root cannot be null");
        }
        return (T)value;
    }

    private static void validateStrictJson(String json) throws IOException {
        try (JsonReader reader = new JsonReader((Reader)new StringReader(json));){
            reader.setStrictness(Strictness.STRICT);
            if (reader.peek() == JsonToken.NULL) {
                throw new IOException("Configuration root cannot be null");
            }
            AtomicJsonStore.readValue(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new IOException("Unexpected content after JSON root");
            }
        }
    }

    private static void validateKnownFields(JsonElement element, Type targetType) throws IOException {
        if (element == null) {
            return;
        }
        if (element.isJsonNull()) {
            Class targetClass;
            if (targetType instanceof Class && (targetClass = (Class)targetType).isPrimitive()) {
                throw new IOException("Primitive JSON field cannot be null");
            }
            return;
        }
        if (targetType instanceof ParameterizedType) {
            Class rawClass;
            ParameterizedType parameterized = (ParameterizedType)targetType;
            Type raw = parameterized.getRawType();
            if (raw instanceof Class && Map.class.isAssignableFrom(rawClass = (Class)raw)) {
                if (!element.isJsonObject()) {
                    throw new IOException("Expected JSON object");
                }
                Type valueType = parameterized.getActualTypeArguments()[1];
                for (JsonElement value : element.getAsJsonObject().asMap().values()) {
                    AtomicJsonStore.validateKnownFields(value, valueType);
                }
                return;
            }
            if (raw instanceof Class && Collection.class.isAssignableFrom(rawClass = (Class)raw)) {
                if (!element.isJsonArray()) {
                    throw new IOException("Expected JSON array");
                }
                Type valueType = parameterized.getActualTypeArguments()[0];
                for (JsonElement value : element.getAsJsonArray()) {
                    AtomicJsonStore.validateKnownFields(value, valueType);
                }
                return;
            }
            targetType = raw;
        }
        if (targetType instanceof GenericArrayType) {
            GenericArrayType arrayType = (GenericArrayType)targetType;
            if (!element.isJsonArray()) {
                throw new IOException("Expected JSON array");
            }
            for (JsonElement value : element.getAsJsonArray()) {
                AtomicJsonStore.validateKnownFields(value, arrayType.getGenericComponentType());
            }
            return;
        }
        if (!(targetType instanceof Class)) {
            return;
        }
        Class targetClass = (Class)targetType;
        if (targetClass.isArray()) {
            if (!element.isJsonArray()) {
                throw new IOException("Expected JSON array");
            }
            for (JsonElement value : element.getAsJsonArray()) {
                AtomicJsonStore.validateKnownFields(value, targetClass.getComponentType());
            }
            return;
        }
        if (targetClass == String.class || targetClass == Character.class || targetClass == Character.TYPE || targetClass.isEnum()) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                throw new IOException("Expected JSON string");
            }
            return;
        }
        if (targetClass == Boolean.class || targetClass == Boolean.TYPE) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
                throw new IOException("Expected JSON boolean");
            }
            return;
        }
        if (Number.class.isAssignableFrom(targetClass) || targetClass == Byte.TYPE || targetClass == Short.TYPE || targetClass == Integer.TYPE || targetClass == Long.TYPE || targetClass == Float.TYPE || targetClass == Double.TYPE) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
                throw new IOException("Expected JSON number");
            }
            return;
        }
        if (!element.isJsonObject()) {
            throw new IOException("Expected JSON object for " + targetClass.getSimpleName());
        }
        JsonObject object = element.getAsJsonObject();
        LinkedHashMap<String, Field> fields = new LinkedHashMap<String, Field>();
        for (Class cursor = targetClass; cursor != null && cursor != Object.class; cursor = cursor.getSuperclass()) {
            for (Field field : cursor.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic()) continue;
                fields.putIfAbsent(field.getName(), field);
            }
        }
        for (Map.Entry entry : object.entrySet()) {
            Field field = (Field)fields.get(entry.getKey());
            if (field == null) {
                throw new IOException("Unknown JSON field: " + (String)entry.getKey());
            }
            AtomicJsonStore.validateKnownFields((JsonElement)entry.getValue(), field.getGenericType());
        }
    }

    private static void readValue(JsonReader reader) throws IOException {
        switch (reader.peek()) {
            case BEGIN_OBJECT: {
                reader.beginObject();
                HashSet<String> names = new HashSet<String>();
                while (reader.hasNext()) {
                    if (!names.add(reader.nextName())) {
                        throw new IOException("Duplicate JSON field");
                    }
                    AtomicJsonStore.readValue(reader);
                }
                reader.endObject();
                break;
            }
            case BEGIN_ARRAY: {
                reader.beginArray();
                while (reader.hasNext()) {
                    AtomicJsonStore.readValue(reader);
                }
                reader.endArray();
                break;
            }
            case STRING: 
            case NUMBER: {
                reader.nextString();
                break;
            }
            case BOOLEAN: {
                reader.nextBoolean();
                break;
            }
            case NULL: {
                reader.nextNull();
                break;
            }
            default: {
                throw new IOException("Invalid JSON token");
            }
        }
    }
}

