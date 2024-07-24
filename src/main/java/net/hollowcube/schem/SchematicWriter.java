package net.hollowcube.schem;

import java.util.*;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.*;
import org.jglrxavpok.hephaistos.nbt.mutable.MutableNBTCompound;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SchematicWriter {

    public static byte@NotNull [] write(@NotNull Schematic schematic) {
        MutableNBTCompound schematicNBT = new MutableNBTCompound();
        Point size = schematic.size();
        schematicNBT.setShort("Width", (short) size.x());
        schematicNBT.setShort("Height", (short) size.y());
        schematicNBT.setShort("Length", (short) size.z());

        Point offset = schematic.offset();
        MutableNBTCompound schematicMetadata = new MutableNBTCompound();
        schematicMetadata.setInt("WEOffsetX", offset.blockX());
        schematicMetadata.setInt("WEOffsetY", offset.blockY());
        schematicMetadata.setInt("WEOffsetZ", offset.blockZ());

        schematicNBT.set("Metadata", schematicMetadata.toCompound());

        schematicNBT.setByteArray("BlockData", schematic.blocks());
        Block[] blocks = schematic.palette();

        schematicNBT.setInt("PaletteMax", blocks.length);

        MutableNBTCompound palette = new MutableNBTCompound();
        for (int i = 0; i < blocks.length; i++) {
            if (blocks[i] == null) blocks[i] = Block.AIR;
            palette.setInt(BlockUtil.toStateString(blocks[i]), i);
        }
        schematicNBT.set("Palette", palette.toCompound());

        List<NBTCompound> blockEntitiesNBT = new ArrayList<>();
        schematic.blockEntities().forEach((pos, tile)->{
            NBTCompound withPos = tile.modify(a -> a.setIntArray("Pos", new int[]{pos.blockX(), pos.blockY(), pos.blockZ()}));
            blockEntitiesNBT.add(withPos);
        });
        schematicNBT.set("BlockEntities", NBT.List(NBTType.TAG_Compound, blockEntitiesNBT));

        var out = new ByteArrayOutputStream();
        try (NBTWriter writer = new NBTWriter(out, CompressedProcesser.GZIP)) {
            writer.writeNamed("Schematic", schematicNBT.toCompound());
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
