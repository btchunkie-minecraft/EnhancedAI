package insane96mcp.enhancedai.modules.base.feature;

import insane96mcp.enhancedai.modules.base.ai.AINearestAttackableTargetGoal;
import insane96mcp.enhancedai.setup.Config;
import insane96mcp.insanelib.base.Feature;
import insane96mcp.insanelib.base.Label;
import insane96mcp.insanelib.base.Module;
import insane96mcp.insanelib.utils.RandomHelper;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.function.Predicate;

@Label(name = "Targeting", description = "Change how mobs target players")
public class TargetingFeature extends Feature {

	private final ForgeConfigSpec.ConfigValue<Integer> followRangeConfig;
	private final ForgeConfigSpec.ConfigValue<Double> xrayConfig;
	private final ForgeConfigSpec.ConfigValue<Boolean> instaTargetConfig;

	public int followRange = 64;
	public double xray = 0.20d;
	public boolean instaTarget = true;

	public TargetingFeature(Module module) {
		super(Config.builder, module);
		Config.builder.comment(this.getDescription()).push(this.getName());
		followRangeConfig = Config.builder
				.comment("How far away can the mobs see the player. This overrides the vanilla value (16 for most mobs). Setting to 0 will leave the follow range as vanilla.")
				.defineInRange("Follow Range Override", this.followRange, 0, 128);
		xrayConfig = Config.builder
				.comment("Chance for a mob to be able to see players through blocks.")
				.defineInRange("XRay Chance", xray, 0d, 1d);
		instaTargetConfig = Config.builder
				.comment("Mobs will no longer take random time to target a player.")
				.define("Instant Target", instaTarget);
		Config.builder.pop();
	}

	@Override
	public void loadConfig() {
		super.loadConfig();
		this.followRange = this.followRangeConfig.get();
		this.xray = this.xrayConfig.get();
		this.instaTarget = this.instaTargetConfig.get();
	}

	@SubscribeEvent
	public void onMobSpawn(EntityJoinWorldEvent event) {
		if (!this.isEnabled())
			return;

		if (!(event.getEntity() instanceof MobEntity))
			return;

		MobEntity mobEntity = (MobEntity) event.getEntity();

		boolean hasTargetGoal = false;

		Predicate predicate = null;

		ArrayList<Goal> goalsToRemove = new ArrayList<>();
		for (PrioritizedGoal prioritizedGoal : mobEntity.targetSelector.goals) {
			//Need to do this to prevent entities like enderman to get their neutral goal to be overwritten
			if (!prioritizedGoal.getGoal().getClass().equals(NearestAttackableTargetGoal.class))
				continue;

			NearestAttackableTargetGoal<?> goal = (NearestAttackableTargetGoal<?>) prioritizedGoal.getGoal();

			if (goal.targetClass != PlayerEntity.class)
				continue;

			predicate = goal.targetEntitySelector.customPredicate;

			goalsToRemove.add(prioritizedGoal.getGoal());
			hasTargetGoal = true;
		}

		if (!hasTargetGoal)
			return;

		goalsToRemove.forEach(mobEntity.goalSelector::removeGoal);

		AINearestAttackableTargetGoal<PlayerEntity> targetPlayer = new AINearestAttackableTargetGoal<PlayerEntity>(mobEntity, PlayerEntity.class, true, false, predicate);
		if (RandomHelper.getDouble(mobEntity.world.rand, 0d, 1d) < this.xray)
			targetPlayer.setXray(true);

		targetPlayer.setInstaTarget(this.instaTarget);
		mobEntity.targetSelector.addGoal(2, targetPlayer);

		if (followRange != 0) {
			ModifiableAttributeInstance followRangeAttribute = mobEntity.getAttribute(Attributes.FOLLOW_RANGE);
			if (followRangeAttribute != null) {
				followRangeAttribute.setBaseValue(this.followRange);

				for (PrioritizedGoal pGoal : mobEntity.targetSelector.goals) {
					if (pGoal.getGoal() instanceof AINearestAttackableTargetGoal) {
						AINearestAttackableTargetGoal nearestAttackableTargetGoal = (AINearestAttackableTargetGoal) pGoal.getGoal();
						nearestAttackableTargetGoal.targetEntitySelector.setDistance(mobEntity.getAttributeValue(Attributes.FOLLOW_RANGE));
					}
				}
			}
		}
	}
}
