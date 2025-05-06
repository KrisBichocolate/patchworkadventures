package krisbichocolate.patchworkadventures.dimensions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.stream.Stream;

import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.structure.StructureSet;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.List;

/**
 * A custom chunk generator that extends NoiseChunkGenerator and adds structure overrides.
 * This requires access widening to remove the final modifier from NoiseChunkGenerator.
 */
public class NoiseExChunkGenerator extends NoiseChunkGenerator {
    public static final MapCodec<NoiseExChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
                BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
                ChunkGeneratorSettings.REGISTRY_CODEC.fieldOf("settings").forGetter(generator -> ((NoiseChunkGenerator)generator).getSettings()),
                        RegistryCodecs.entryList(RegistryKeys.STRUCTURE_SET).lenientOptionalFieldOf("structure_overrides").forGetter(generator -> generator.structureOverrides)
            )
            .apply(instance, instance.stable(NoiseExChunkGenerator::new))
    );

    private final Optional<RegistryEntryList<StructureSet>> structureOverrides;

    public NoiseExChunkGenerator(BiomeSource biomeSource, RegistryEntry<ChunkGeneratorSettings> settings, Optional<RegistryEntryList<StructureSet>> structureOverrides) {
        super(biomeSource, settings);
        this.structureOverrides = structureOverrides;
    }

    @Override
    public StructurePlacementCalculator createStructurePlacementCalculator(RegistryWrapper<StructureSet> structureSetRegistry, NoiseConfig noiseConfig, long seed) {
        if (this.structureOverrides.isPresent()) {
            Stream<RegistryEntry<StructureSet>> stream = this.structureOverrides.get().stream();
            return StructurePlacementCalculator.create(noiseConfig, seed, this.getBiomeSource(), stream);
        }
        return super.createStructurePlacementCalculator(structureSetRegistry, noiseConfig, seed);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }
}
