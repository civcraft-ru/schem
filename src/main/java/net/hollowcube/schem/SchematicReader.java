package net.hollowcube.schem;


import java.util.*;

import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentBlockState;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Simple schematic file reader.
 */
public final class SchematicReader {

    private SchematicReader() {
    }

    public static @NotNull Schematic read(@NotNull InputStream stream) {
        try {
            return read(BinaryTagIO.unlimitedReader().read(stream));
        } catch (Exception e) {
            throw new SchematicReadException("failed to read schematic NBT", e);
        }
    }

    public static @NotNull Schematic read(@NotNull Path path) {
        try {
            return read(BinaryTagIO.unlimitedReader().read(path));
        } catch (Exception e) {
            throw new SchematicReadException("failed to read schematic NBT", e);
        }
    }

    public static @NotNull Schematic read(@NotNull CompoundBinaryTag root) {
        try {
            // If it has a Schematic tag is sponge v2 or 3
            var schematicTag = root.getCompound("Schematic");
            if (schematicTag != null) {
                Integer version = schematicTag.getInt("Version");
                Check.notNull(version, "Missing required field 'Schematic.Version'");
                return read(schematicTag, version);
            }

            // Otherwise it is hopefully v1
            return read(root, 1);
        } catch (Exception e) {
            throw new SchematicReadException("Invalid schematic file", e);
        }
    }

    private static @NotNull Schematic read(@NotNull CompoundBinaryTag tag, int version) {
        Short width = tag.getShort("Width");
        Check.notNull(width, "Missing required field 'Width'");
        Short height = tag.getShort("Height");
        Check.notNull(height, "Missing required field 'Height'");
        Short length = tag.getShort("Length");
        Check.notNull(length, "Missing required field 'Length'");

        CompoundBinaryTag metadata = tag.getCompound("Metadata");

        var offset = Vec.ZERO;
        if (metadata != null && metadata.keySet().contains("WEOffsetX")) {
            Integer offsetX = metadata.getInt("WEOffsetX");
            Check.notNull(offsetX, "Missing required field 'Metadata.WEOffsetX'");
            Integer offsetY = metadata.getInt("WEOffsetY");
            Check.notNull(offsetY, "Missing required field 'Metadata.WEOffsetY'");
            Integer offsetZ = metadata.getInt("WEOffsetZ");
            Check.notNull(offsetZ, "Missing required field 'Metadata.WEOffsetZ'");

            offset = new Vec(offsetX, offsetY, offsetZ);
        } //todo handle sponge Offset

        CompoundBinaryTag palette;
        byte[] blockArray;
        Integer paletteSize;
        ListBinaryTag blockEntitiesNbt = tag.getList("BlockEntities");

        if (version == 3) {
            var blockEntries = tag.getCompound("Blocks");
            Check.notNull(blockEntries, "Missing required field 'Blocks'");

            palette = blockEntries.getCompound("Palette");
            Check.notNull(palette, "Missing required field 'Blocks.Palette'");
            blockArray = blockEntries.getByteArray("Data");
            Check.notNull(blockArray, "Missing required field 'Blocks.Data'");
            paletteSize = palette.size();

        } else {
            palette = tag.getCompound("Palette");
            Check.notNull(palette, "Missing required field 'Palette'");
            blockArray = tag.getByteArray("BlockData");
            Check.notNull(blockArray, "Missing required field 'BlockData'");
            paletteSize = tag.getInt("PaletteMax");
            Check.notNull(paletteSize, "Missing required field 'PaletteMax'");
        }

        Block[] paletteBlocks = new Block[paletteSize];

        palette.forEach(e -> {
            int assigned = ((IntBinaryTag) e.getValue()).value();
            Block block = ArgumentBlockState.staticParse(e.getKey());
            paletteBlocks[assigned] = block;
        });

        var blockEntities = new HashMap<Vec, CompoundBinaryTag>();

        if(blockEntitiesNbt != null) {
            for (int i=0; i<blockEntitiesNbt.size();i++) {
                CompoundBinaryTag tileNbt=blockEntitiesNbt.getCompound(i);
                int[] pos = tileNbt.getIntArray("Pos");
                if (pos == null)
                    continue;
                Vec handyPos = new Vec(pos[0], pos[1], pos[2]);
                CompoundBinaryTag refinedTileNbt = tileNbt.remove("Pos");
                blockEntities.put(handyPos, refinedTileNbt);
            }
        }

        return new Schematic(
                new Vec(width, height, length),
                offset,
                paletteBlocks,
                blockArray.clone(),
                blockEntities
        );
    }

}
