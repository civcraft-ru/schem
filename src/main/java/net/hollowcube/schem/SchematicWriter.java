package net.hollowcube.schem;

import java.util.*;

import it.unimi.dsi.fastutil.Pair;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SchematicWriter {

    public static byte@NotNull [] write(@NotNull Schematic schematic) {
        CompoundBinaryTag.Builder schematicNBT = CompoundBinaryTag.builder();
        Point size = schematic.size();
        schematicNBT.putShort("Width", (short) size.x());
        schematicNBT.putShort("Height", (short) size.y());
        schematicNBT.putShort("Length", (short) size.z());

        Point offset = schematic.offset();
        CompoundBinaryTag.Builder schematicMetadata = CompoundBinaryTag.builder();
        schematicMetadata.putInt("WEOffsetX", offset.blockX());
        schematicMetadata.putInt("WEOffsetY", offset.blockY());
        schematicMetadata.putInt("WEOffsetZ", offset.blockZ());

        schematicNBT.put("Metadata", schematicMetadata.build());

        schematicNBT.putByteArray("BlockData", schematic.blocks());
        Block[] blocks = schematic.palette();

        schematicNBT.putInt("PaletteMax", blocks.length);

        CompoundBinaryTag.Builder palette = CompoundBinaryTag.builder();
        for (int i = 0; i < blocks.length; i++) {
            if (blocks[i] == null) blocks[i] = Block.AIR;
            palette.putInt(BlockUtil.toStateString(blocks[i]), i);
        }
        schematicNBT.put("Palette", palette.build());

        List<CompoundBinaryTag> blockEntitiesNBT = new ArrayList<>();
        schematic.blockEntities().forEach((Vec pos, CompoundBinaryTag tile) ->{
            CompoundBinaryTag withPos = tile.putIntArray("Pos", new int[]{pos.blockX(), pos.blockY(), pos.blockZ()});
            blockEntitiesNBT.add(withPos);
        });
        schematicNBT.put("BlockEntities", ListBinaryTag.from(blockEntitiesNBT));

        var out = new ByteArrayOutputStream();

        try {
            BinaryTagIO.writer().writeNamed(new AbstractMap.SimpleEntry("Schematic", schematicNBT.build()),out, BinaryTagIO.Compression.GZIP);
        } catch (IOException e) {
            // No exceptions when writing to a byte array
            throw new RuntimeException(e);
        }

        return out.toByteArray();
    }

    public static void write(@NotNull Schematic schematic, @NotNull Path schemPath) throws IOException {
        Files.write(schemPath, write(schematic));
    }
}
