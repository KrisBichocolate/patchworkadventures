package krisbichocolate.patchworkadventures.dimensions;

import krisbichocolate.patchworkadventures.dimensions.command.ModCommands;
import krisbichocolate.patchworkadventures.dimensions.RespawnHandler;
import krisbichocolate.patchworkadventures.dimensions.NoiseExChunkGenerator;
import net.fabricmc.api.ModInitializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PatchworkAdventures implements ModInitializer {
	public static final String MOD_ID = "pwa_dimensions";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Initializing Patchwork Adventures mod");
		
		// Register mod commands
		ModCommands.registerCommands();
		
		// Initialize respawn handler
		RespawnHandler.init();
		
		// Initialize data storage
		LOGGER.info("Initializing persistent data storage for instances using DataCommandStorage");
		
		// Register chunk generators
		registerChunkGenerators();
	}
	
	private void registerChunkGenerators() {
		LOGGER.info("Registering custom chunk generators");
		Registry.register(
			Registries.CHUNK_GENERATOR,
			Identifier.of(MOD_ID, "noise"),
			NoiseExChunkGenerator.CODEC
		);
	}
}
