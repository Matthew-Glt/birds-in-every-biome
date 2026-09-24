package com.mg3dp.birdsineverybiome;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;

/**
 * /birds check  - why a bird would (not) spawn where you are standing
 * /birds spawn  - force one flock attempt and report exactly what happened
 */
public final class BirdsCommand {
	private BirdsCommand() {}

	public static void register(BirdsConfig config) {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("birds")
					.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER));

			root.then(Commands.literal("check").executes(context -> {
				CommandSourceStack source = context.getSource();
				ServerLevel level = source.getLevel();
				BlockPos pos = BlockPos.containing(source.getPosition());
				RandomSource random = level.getRandom();

				boolean jungle = level.getBiome(pos).is(BiomeTags.IS_JUNGLE);
				ParrotSpawnRules.Denial denial = ParrotSpawnRules.check(EntityTypes.PARROT, level, EntitySpawnReason.NATURAL, pos, random);
				int light = level.getRawBrightness(pos, 0);
				int nearby = BirdSpawner.countNearby(level, pos, FlockSpawner.NEARBY_RADIUS);

				String report = "Birds in Every Biome - position check at " + pos.toShortString() + "\n"
						+ "  biome: " + level.getBiome(pos).getRegisteredName() + (jungle ? " (jungle)" : "") + "\n"
						+ "  skin a parrot would get here: " + ParrotSkins.skinFor(level.getBiome(pos)) + "\n"
						+ "  spawning enabled in this biome: "
						+ (config.isBiomeAllowed(level.getBiome(pos).getRegisteredName()) ? "yes"
								: "no (switched off in the config)")
						+ "\n"
						+ "  natural spawn entry: weight " + config.spawnWeight
						+ (jungle && config.skipJungles ? " - skipped in jungles" : "") + "\n"
						+ "  spawn rules here: " + denial.message() + " (light " + light + ")\n"
						+ "  parrots within " + FlockSpawner.NEARBY_RADIUS + " blocks: " + nearby
						+ " (flock limit " + config.maxBirdsNearby + ")\n"
						+ "  flock spawner: " + (config.flockEnabled ? "on, every " + config.flockDelaySeconds + "s" : "off")
						+ " - vanilla's own passive spawner is capped and throttled, so flocks are what you normally see";

				source.sendSuccess(() -> Component.literal(report), false);
				return 1;
			}));

			root.then(Commands.literal("spawn").executes(context -> {
				CommandSourceStack source = context.getSource();
				FlockSpawner.Attempt attempt = FlockSpawner.attempt(source.getLevel(), BlockPos.containing(source.getPosition()), config);

				source.sendSuccess(() -> Component.literal("Flock attempt: " + attempt.detail()), false);
				return attempt.spawned() ? 1 : 0;
			}));

			dispatcher.register(root);
		});
	}
}
