package net.hollowcube.schem;


import java.util.*;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentBlockState;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.collections.*;
import org.jglrxavpok.hephaistos.nbt.*;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Simple schematic file reader.
 */
public final class SchematicReader {

    private SchematicReader() {
    }

    public static @NotNull Schematic read(@NotNull InputStream stream) {
        try (var reader = new NBTReader(stream, CompressedProcesser.GZIP)) {
            return read(reader);
        } catch (Exception e) {
            throw new SchematicReadException("failed to read schematic NBT", e);
        }
    }

    public static @NotNull Schematic read(@NotNull Path path) {
        try (var reader = new NBTReader(path, CompressedProcesser.GZIP)) {
            return read(reader);
        } catch (Exception e) {
            throw new SchematicReadException("failed to read schematic NBT", e);
        }
    }

    public static @NotNull Schematic read(@NotNull NBTReader reader) {
        try {
            NBTCompound tag = (NBTCompound) reader.read();

            // If it has a Schematic tag is sponge v2 or 3
            var schematicTag = tag.getCompound("Schematic");
            if (schematicTag != null) {
                Integer version = schematicTag.getInt("Version");
                Check.notNull(version, "Missing required field 'Schematic.Version'");
                return read(schematicTag, version);
            }

            // Otherwise it is hopefully v1
            return read(tag, 1);
        } catch (Exception e) {
            throw new SchematicReadException("Invalid schematic file", e);
        }
    }

    private static @NotNull Schematic read(@NotNull NBTCompound tag, int version) {
        Short width = tag.getShort("Width");
        Check.notNull(width, "Missing required field 'Width'");
        Short height = tag.getShort("Height");
        Check.notNull(height, "Missing required field 'Height'");
        Short length = tag.getShort("Length");
        Check.notNull(length, "Missing required field 'Length'");

        NBTCompound metadata = tag.getCompound("Metadata");

        var offset = Vec.ZERO;
        if (metadata != null && metadata.containsKey("WEOffsetX")) {
            Integer offsetX = metadata.getInt("WEOffsetX");
            Check.notNull(offsetX, "Missing required field 'Metadata.WEOffsetX'");
            Integer offsetY = metadata.getInt("WEOffsetY");
            Check.notNull(offsetY, "Missing required field 'Metadata.WEOffsetY'");
            Integer offsetZ = metadata.getInt("WEOffsetZ");
            Check.notNull(offsetZ, "Missing required field 'Metadata.WEOffsetZ'");

            offset = new Vec(offsetX, offsetY, offsetZ);
        } //todo handle sponge Offset

        NBTCompound palette;
        ImmutableByteArray blockArray;
        Integer paletteSize;
        NBTList<NBTCompound> blockEntitiesNbt;

        if(version == 1) {
            //blockEntitiesNbt = tag.getList("TileEntities");
            throw new IllegalArgumentException("unsupported tile entities in version 1");

        }else {
            blockEntitiesNbt = tag.getList("BlockEntities");
        }

        if (version == 3) {
            var blockEntries = tag.getCompound("Blocks");
            Check.notNull(blockEntries, "Missing required field 'Blocks'");

            palette = blockEntries.getCompound("Palette");
            Check.notNull(palette, "Missing required field 'Blocks.Palette'");
            blockArray = blockEntries.getByteArray("Data");
            Check.notNull(blockArray, "Missing required field 'Blocks.Data'");
            paletteSize = palette.getSize();

        } else {
            palette = tag.getCompound("Palette");
            Check.notNull(palette, "Missing required field 'Palette'");
            blockArray = tag.getByteArray("BlockData");
            Check.notNull(blockArray, "Missing required field 'BlockData'");
            paletteSize = tag.getInt("PaletteMax");
            Check.notNull(paletteSize, "Missing required field 'PaletteMax'");
        }

        Block[] paletteBlocks = new Block[paletteSize];

        palette.forEach((key, value) -> {
            int assigned = ((NBTInt) value).getValue();
            Block block = ArgumentBlockState.staticParse(key);
            paletteBlocks[assigned] = block;
        });

        var blockEntities = new HashMap<Vec, NBTCompound>();

        if(blockEntitiesNbt != null) {
            for (NBTCompound tileNbt : blockEntitiesNbt) {
                ImmutableIntArray pos = tileNbt.getIntArray("Pos");
                if (pos == null)
                    continue;
                Vec handyPos = new Vec(pos.get(0), pos.get(1), pos.get(2));
                NBTCompound refinedTileNbt = tileNbt.withRemovedKeys("Pos");
                blockEntities.put(handyPos, refinedTileNbt);
            }
        }

        return new Schematic(
                new Vec(width, height, length),
                offset,
                paletteBlocks,
                blockArray.copyArray(),
                blockEntities
        );
    }

}
