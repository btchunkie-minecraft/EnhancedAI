package insane96mcp.enhancedai.setup;

import insane96mcp.enhancedai.EnhancedAI;
import insane96mcp.enhancedai.modules.spider.entity.projectile.ThrownWebEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EAEntities {
	public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, EnhancedAI.MOD_ID);

	public static final RegistryObject<EntityType<ThrownWebEntity>> THROWN_WEB = ENTITIES.register("thrown_web", () -> EntityType.Builder.<ThrownWebEntity>of(ThrownWebEntity::new, MobCategory.MISC)
			.sized(0.25f, 0.25f)
			.setTrackingRange(4)
			.setUpdateInterval(10)
			.setShouldReceiveVelocityUpdates(true)
			.build("thrown_web"));

	public static final RegistryObject<EntityType<ThrownWebEntity>> FISHING_HOOK = ENTITIES.register("fishing_hook", () -> EntityType.Builder.<ThrownWebEntity>of(ThrownWebEntity::new, MobCategory.MISC)
			.sized(0.25f, 0.25f)
			.setTrackingRange(4)
			.setUpdateInterval(10)
			.setShouldReceiveVelocityUpdates(true)
			.build("thrown_web"));
}
