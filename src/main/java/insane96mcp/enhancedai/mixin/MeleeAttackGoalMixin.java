package insane96mcp.enhancedai.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import insane96mcp.enhancedai.modules.mobs.MeleeAttacking;
import insane96mcp.insanelib.base.Feature;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MeleeAttackGoal.class)
public abstract class MeleeAttackGoalMixin extends Goal {
	@Final
	@Shadow
	private double speedModifier;

	@Shadow
	private Path path;

	@Final
	@Shadow
	protected PathfinderMob mob;

	@Shadow private int ticksUntilNextAttack;

	@Shadow protected abstract boolean isTimeToAttack();

	@Shadow protected abstract void resetAttackCooldown();

	@Inject(at = @At(value = "HEAD"), method = "checkAndPerformAttack", cancellable = true)
	public void getAttackReachSqr(LivingEntity attacked, double distanceSqr, CallbackInfo ci) {
		if (!Feature.isEnabled(MeleeAttacking.class))
			return;
		if (MeleeAttacking.isWithinMeleeAttackRange(this.mob, attacked) && this.isTimeToAttack() && this.mob.getSensing().hasLineOfSight(attacked)) {
			this.resetAttackCooldown();
			this.mob.swing(InteractionHand.MAIN_HAND);
			this.mob.doHurtTarget(attacked);
		}
		ci.cancel();
	}

	@ModifyExpressionValue(method = "canUse", at = @At(value = "CONSTANT", args = "longValue=20"))
	public long onLastCanUseCheck(long constant) {
		return 0L;
	}

	@ModifyReturnValue(method = "canUse", at = @At(value = "RETURN", ordinal = 6))
	public boolean onCanUse(boolean original, @Local LivingEntity livingEntity) {
		return MeleeAttacking.isWithinMeleeAttackRange(this.mob, livingEntity);
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/navigation/PathNavigation;moveTo(Lnet/minecraft/world/entity/Entity;D)Z"))
	public boolean onMoveTo(PathNavigation pathNavigation, Entity entity, double speedModifier) {
		Path path = this.mob.getNavigation().createPath(entity, 0);
		return path != null && this.mob.getNavigation().moveTo(path, speedModifier);
	}

	/*@Inject(method = "canContinueToUse", at = @At(value = "RETURN", ordinal = 2), cancellable = true)
	public void onCanContinueToUse(CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue() && this.mob.getTarget() != null) {
			this.path = this.mob.getNavigation().createPath(this.mob.getTarget(), 0);
			if (this.path != null) {
				this.mob.getNavigation().moveTo(this.path, this.speedModifier);
				cir.setReturnValue(true);
			}
		}
		cir.setReturnValue(true);
	}*/

	@ModifyExpressionValue(method = "canContinueToUse", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/ai/goal/MeleeAttackGoal;followingTargetEvenIfNotSeen:Z"))
	public boolean onFollowingTargetEvenIfNotSeen(boolean value) {
		if (Feature.isEnabled(MeleeAttacking.class))
			return true;
		return value;
	}

	@Inject(at = @At(value = "RETURN"), method = "getAttackInterval", cancellable = true)
	public void onGetAttackCooldown(CallbackInfoReturnable<Integer> cir) {
		if (!MeleeAttacking.shouldUseAttackSpeedAttribute())
			return;
		cir.setReturnValue(this.enhancedAI$getTicksUntilNextAttack());
	}

	@Inject(at = @At(value = "HEAD"), method = "resetAttackCooldown", cancellable = true)
	public void onResetAttackCooldown(CallbackInfo ci) {
		if (!MeleeAttacking.shouldUseAttackSpeedAttribute())
			return;
		this.ticksUntilNextAttack = this.enhancedAI$getTicksUntilNextAttack();
		ci.cancel();
	}

	@Unique
	private int enhancedAI$getTicksUntilNextAttack() {
		double attackSpeed = this.mob.getAttributeValue(Attributes.ATTACK_SPEED);
		if (attackSpeed <= 0f) {
			AttributeInstance instance = this.mob.getAttribute(Attributes.ATTACK_SPEED);
			if (instance != null) instance.setDirty();
			attackSpeed = this.mob.getAttributeValue(Attributes.ATTACK_SPEED);
		}
		attackSpeed *= MeleeAttacking.attackSpeedMultiplier.getByDifficulty(this.mob.level());
		if (attackSpeed > MeleeAttacking.attackSpeedMaximum) attackSpeed = MeleeAttacking.attackSpeedMaximum;
		return this.adjustedTickDelay((int) (1d / attackSpeed * 20d));
	}
}
