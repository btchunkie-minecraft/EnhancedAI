package insane96mcp.enhancedai.modules.creeper;

import insane96mcp.enhancedai.EnhancedAI;
import insane96mcp.enhancedai.modules.Modules;
import insane96mcp.enhancedai.setup.EASounds;
import insane96mcp.enhancedai.setup.NBTUtils;
import insane96mcp.insanelib.base.Feature;
import insane96mcp.insanelib.base.Label;
import insane96mcp.insanelib.base.Module;
import insane96mcp.insanelib.base.config.Config;
import insane96mcp.insanelib.base.config.LoadFeature;
import insane96mcp.insanelib.network.MessageCreeperDataSync;
import insane96mcp.insanelib.setup.ILStrings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.function.Supplier;

@Label(name = "Creeper Swell", description = "Various changes to Creepers exploding. Ignoring Walls, Walking Fuse and smarter exploding based off explosion size. Only creepers in the enhancedai:change_creeper_swell entity type tag are affected by this feature.")
@LoadFeature(module = Modules.Ids.CREEPER)
public class CreeperSwell extends Feature {
	public static final TagKey<EntityType<?>> CHANGE_CREEPER_SWELL = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(EnhancedAI.MOD_ID, "change_creeper_swell"));

	public static final String LAUNCH = EnhancedAI.RESOURCE_PREFIX + "launch";
	public static final String BREACH = EnhancedAI.RESOURCE_PREFIX + "breach";
	public static final String ANGRY = EnhancedAI.RESOURCE_PREFIX + "angry";
	public static final String WALKING_FUSE = EnhancedAI.RESOURCE_PREFIX + "walking_fuse";
	public static final String IGNORE_WALLS = EnhancedAI.RESOURCE_PREFIX + "ignore_walls";
	public static final String BETA = EnhancedAI.RESOURCE_PREFIX + "beta";
	@Config(min = 0d, max = 1d)
	@Label(name = "Walking Fuse Chance", description = "Percentage chance for a Creeper to keep walking while exploding. This is overwritten if the creeper has the beta property.")
	public static Double walkingFuseChance = 0.1d;
	@Config(min = -1d, max = 64d)
	@Label(name = "Walking Fuse Speed Modifier", description = "Speed modifier when a walking fuse creeper is swelling.")
	public static Double walkingFuseSpeedModifier = -0.5d;
	@Config(min = 0d, max = 1d)
	@Label(name = "Ignore Walls Chance", description = "Percentage chance for a Creeper to ignore walls while targeting a player. This means that a creeper will be able to explode if it's in the correct range from a player even if there's a wall between.")
	public static Double ignoreWallsChance = 0.65d;
	@Config(min = 0d, max = 1d)
	@Label(name = "Launch.Chance", description = "Launching creepers will try ignite and throw themselves at the player.")
	public static Double launchChance = 0.05d;
	@Config
	@Label(name = "Launch.Particles", description = "If true, Launching Creepers emit particles")
	public static Boolean launchParticles = true;

	@Config(min = 0d, max = 8d)
	@Label(name = "Launch.inaccuracy", description = "The inaccuracy of the launching creeper in Normal difficulty, easy is increased, hard is decreased.")
	public static Double launchInaccuracy = 0.5d;
	@Config(min = 0, max = 127)
	@Label(name = "Launch.Explosion Radius", description = "The explosion radius of launching creepers. Set to 0 to not change. (Overrides Cena creepers explosion radius)")
	public static Integer launchExplosionRadius = 2;
	@Config(min = 0d, max = 1d)
	@Label(name = "Breach.Chance", description = "Breaching creepers will try to open a hole in the wall to let mobs in.")
	public static Double breachChance = 0.075d;
	@Config(min = 0)
	@Label(name = "Breach.Horizontal Range", description = "How far away (horizontally) from the target breaching creepers can breach.")
	public static Integer breachHorizontalRange = 24;
	@Config(min = 0d, max = 1d)
	@Label(name = "Beta Creeper Chance", description = "Beta creepers when exploding will walk around the target, like the creepers in pre-1.2.")
	public static Double betaCreeperChance = 0.35d;
	@Config
	@Label(name = "Disable falling swelling", description = "Disables the creeper feature that makes them start swelling when falling.")
	public static Boolean disableFallingSwelling = true;

	@Config
	@Label(name = "TNT Like", description = "If true creepers will ignite if damaged by an explosion.")
	public static Boolean tntLike = false;
	//Cena
	@Config(min = 0d, max = 1d)
	@Label(name = "Angry Creeper.Chance", description = "Chance for a creeper to spawn angry")
	public static Double angryChance = 0.03d;
	@Config
	@Label(name = "Angry Creeper.Particles", description = "If true, Angry Creeper emits particles")
	public static Boolean angryParticles = true;
	@Config
	@Label(name = "Angry Creeper.Sounds", description = "The special sound effect that the Angry Creeper plays")
	public static AngryCreeperSounds angryCreeperSounds = AngryCreeperSounds.WTF_BOOM;
	@Config
	@Label(name = "Angry Creeper.Name", description = "If true, Angry Creeper will have a name")
	public static Boolean angryName = true;
	@Config
	@Label(name = "Angry Creeper.Force Explosion", description = "When ignited, Angry Creeper will not stop swelling")
	public static Boolean angryForceExplosion = false;
	@Config
	@Label(name = "Angry Creeper.Explode on death", description = "Makes angry creepers blow up on death like when they were added back in 0.30")
	public static Boolean angryExplodeOnDeath = true;
	@Config
	@Label(name = "Angry Creeper.Generates fire", description = "If true, Angry Creeper explosion will generate fire")
	public static Boolean angryFire = false;
	@Config(min = 0d, max = 12d)
	@Label(name = "Angry Creeper.Explosion power", description = "Explosion power of Angry Creeper")
	public static Double angryExplosionPower = 4d;
	@Config
	@Label(name = "Angry Creeper.Forced Explosion", description = "Angry Creeper will not stop swelling when triggered")
	public static Boolean angryForcedExplosion = true;
	@Config
	@Label(name = "Blow up on death", description = "Makes creepers blow up on death like when they were added back in 0.30")
	public static BlowUpOnDeath blowUpOnDeath = BlowUpOnDeath.NONE;
	@Config
	@Label(name = "IguanaTweaks Reborn Integration", description = "If IguanaTweaks Reborn is installed and Explosion Overhaul is enabled, Angry creeper will deal more knockback and break more blocks and breaching creepers will break more blocks")
	public static Boolean iguanaTweaksIntegration = true;

	public CreeperSwell(Module module, boolean enabledByDefault, boolean canBeDisabled) {
		super(module, enabledByDefault, canBeDisabled);
	}

	@SubscribeEvent
	public void explosionStartEvent(ExplosionEvent.Detonate event) {
		if (!this.isEnabled())
			return;

		Explosion e = event.getExplosion();

		if (!(e.getExploder() instanceof Creeper creeper))
			return;

		if (creeper.getPersistentData().getBoolean(ANGRY) && angryCreeperSounds.explode != null) {
			creeper.playSound(angryCreeperSounds.explode.get(), 4.0f, 1.0f);
		}
	}

	//Lowest priority so other mods can set persistent data
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void eventEntityJoinWorld(EntityJoinLevelEvent event) {
		if (!this.isEnabled()
				|| event.getLevel().isClientSide
				|| !(event.getEntity() instanceof Creeper creeper)
				|| !creeper.getType().is(CHANGE_CREEPER_SWELL))
			return;

		boolean hasSwellGoal = false;
		//Remove Creeper Swell Goal
		ArrayList<Goal> goalsToRemove = new ArrayList<>();
		for (WrappedGoal prioritizedGoal : creeper.goalSelector.availableGoals) {
			if (prioritizedGoal.getGoal() instanceof SwellGoal) {
				goalsToRemove.add(prioritizedGoal.getGoal());
				hasSwellGoal = true;
			}
		}

		if (!hasSwellGoal)
			return;

		goalsToRemove.forEach(creeper.goalSelector::removeGoal);

		CompoundTag persistentData = creeper.getPersistentData();

		boolean walkingFuse = NBTUtils.getBooleanOrPutDefault(persistentData, WALKING_FUSE, creeper.getRandom().nextDouble() < walkingFuseChance);
		boolean ignoreWalls = NBTUtils.getBooleanOrPutDefault(persistentData, IGNORE_WALLS, creeper.getRandom().nextDouble() < ignoreWallsChance);
		boolean breach = NBTUtils.getBooleanOrPutDefault(persistentData, BREACH, creeper.getRandom().nextDouble() < breachChance);
		boolean launch = NBTUtils.getBooleanOrPutDefault(persistentData, LAUNCH, creeper.getRandom().nextDouble() < launchChance);
		boolean angry = NBTUtils.getBooleanOrPutDefault(persistentData, ANGRY, creeper.getRandom().nextDouble() < angryChance);
		boolean beta = NBTUtils.getBooleanOrPutDefault(persistentData, BETA, creeper.getRandom().nextDouble() < betaCreeperChance);

		CompoundTag compoundNBT = new CompoundTag();
		creeper.addAdditionalSaveData(compoundNBT);
		if (angry) {
			compoundNBT.putShort("Fuse", (short) 36);
			compoundNBT.putByte("ExplosionRadius", angryExplosionPower.byteValue());
			if (angryName)
				creeper.setCustomName(Component.literal("Angry Creeper"));
			if (angryFire)
				persistentData.putBoolean(ILStrings.Tags.EXPLOSION_CAUSES_FIRE, true);
			if (iguanaTweaksIntegration) {
				persistentData.putFloat("iguanatweaksreborn:explosion_knockback_multiplier", 2f);
				persistentData.putFloat("iguanatweaksreborn:explosion_ray_strength_multiplier", 0.03f);
			}
		}

		EACreeperSwellGoal swellGoal = new EACreeperSwellGoal(creeper)
				.setWalkingFuse(walkingFuse && !beta)
				.setIgnoreWalls(ignoreWalls)
				.setBreaching(breach)
				.setBeta(beta);
		if (angry && angryForcedExplosion)
			swellGoal.setForceExplode(angryForceExplosion);
		creeper.goalSelector.addGoal(2, swellGoal);

		if (launch) {
			creeper.goalSelector.addGoal(1, new EACreeperLaunchGoal(creeper));
			if (launchExplosionRadius > 0)
				compoundNBT.putByte("ExplosionRadius", launchExplosionRadius.byteValue());
		}
		creeper.readAdditionalSaveData(compoundNBT);
		MessageCreeperDataSync.syncCreeperToPlayers(creeper);
	}

	@SubscribeEvent
	public void livingDamageEvent(LivingDamageEvent event) {
		if (!this.isEnabled()
				|| !tntLike
				|| !event.getSource().is(DamageTypeTags.IS_EXPLOSION)
				|| !(event.getEntity() instanceof Creeper creeper))
			return;

		creeper.ignite();
	}

	@SubscribeEvent
	public void onCreeperRemoved(EntityLeaveLevelEvent event) {
		if (!this.isEnabled()
				|| !(event.getEntity() instanceof Creeper creeper)
				|| !creeper.isDeadOrDying())
			return;

		if (blowUpOnDeath == BlowUpOnDeath.ALL || (blowUpOnDeath == BlowUpOnDeath.CHARGED && creeper.isPowered()) || (creeper.getPersistentData().getBoolean(ANGRY) && angryExplodeOnDeath)) {
			if (!creeper.level().isClientSide) {
				float f = creeper.isPowered() ? 2.0F : 1.0F;
				creeper.level().explode(creeper, creeper.getX(), creeper.getY(), creeper.getZ(), (float)creeper.explosionRadius * f, Level.ExplosionInteraction.MOB);
				creeper.spawnLingeringCloud();
			}
		}
	}

	@SubscribeEvent
	public void onCreeperTick(LivingEvent.LivingTickEvent event) {
		if (!this.isEnabled()
				|| !(event.getEntity() instanceof Creeper creeper)
				|| creeper.level().isClientSide)
			return;

		onLaunchCreeperTick(creeper);
		onCenaCreeperTick(creeper);
	}

	public void onLaunchCreeperTick(Creeper creeper) {
		if (creeper.tickCount % 20 != 0
				|| !launchParticles)
			return;
		ServerLevel serverLevel = (ServerLevel) creeper.level();
		if (creeper.getPersistentData().getBoolean(LAUNCH)) {
			for(int j = 0; j < serverLevel.players().size(); ++j) {
				ServerPlayer serverplayer = serverLevel.players().get(j);
				serverLevel.sendParticles(serverplayer, ParticleTypes.CLOUD, true, creeper.getX(), creeper.getY() + 0.25d, creeper.getZ(), 8, 0.05, 0.05, 0.05, 0.025);
			}
		}
	}

	public void onCenaCreeperTick(Creeper creeper) {
		if (creeper.tickCount % 40 != 5
				|| !angryParticles)
			return;
		ServerLevel serverLevel = (ServerLevel) creeper.level();
		if (creeper.getPersistentData().getBoolean(ANGRY)) {
			for(int j = 0; j < serverLevel.players().size(); ++j) {
				ServerPlayer serverplayer = serverLevel.players().get(j);
				BlockPos blockpos = serverplayer.blockPosition();
				if (!blockpos.closerToCenterThan(new Vec3(creeper.getX(), creeper.getY() + 0.5d, creeper.getZ()), 16))
					continue;
				serverLevel.sendParticles(serverplayer, ParticleTypes.ANGRY_VILLAGER, false, creeper.getX(), creeper.getY() + 1.1d, creeper.getZ(), 1, 0.15, 0.15, 0.15, 0);
			}
		}
	}

	public static boolean shouldDisableFallingSwelling() {
		return Feature.isEnabled(CreeperSwell.class) && disableFallingSwelling;
	}

	/*public static boolean onCreeperScale(Creeper creeper, PoseStack poseStack, float partialTicks) {
		if (!Feature.isEnabled(CreeperSwell.class))
			return false;

		float swelling = creeper.getSwelling(partialTicks);
		float explosionPower = CreeperUtils.getExplosionSize(creeper);
		float f1 = 1.0F + Mth.sin(swelling * 100.0F) * swelling * 0.01F;
		swelling = Mth.clamp(swelling, 0.0F, 1.0F);
		swelling *= swelling;
		swelling *= swelling;
		swelling *= 5;
		float f2 = (1.0F + swelling * 0.4F) / f1;
		float f3 = (1.0F + swelling * 0.2F) / f1;
		poseStack.scale(f2, f3, f2);
		return true;
	}*/

	public enum AngryCreeperSounds {
		NONE(null, null),
		CENA(EASounds.CREEPER_CENA_FUSE, EASounds.CREEPER_CENA_EXPLODE),
		WTF_BOOM(EASounds.WTF_BOOM_FUSE, EASounds.WTF_BOOM_EXPLODE);

		@Nullable
		public final Supplier<SoundEvent> fuse;
		@Nullable
		public final Supplier<SoundEvent> explode;

		AngryCreeperSounds(@Nullable Supplier<SoundEvent> fuse, @Nullable Supplier<SoundEvent> explode) {
			this.fuse = fuse;
			this.explode = explode;
		}
	}

	public enum BlowUpOnDeath {
		NONE,
		CHARGED,
		ALL
	}
}
