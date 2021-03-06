/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.crafting;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.common.IEConfig;
import blusunrize.immersiveengineering.common.crafting.IEConfigConditionSerializer.ConditionIEConfig;
import com.google.gson.JsonObject;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

/**
 * @author BluSunrize
 * @since 09.07.2017
 */
public class IEConfigConditionSerializer implements IConditionSerializer<ConditionIEConfig>
{
	static
	{
		CraftingHelper.register(new IEConfigConditionSerializer());
	}

	public static final ResourceLocation ID = new ResourceLocation(ImmersiveEngineering.MODID, "config");

	@Override
	public void write(JsonObject json, ConditionIEConfig value)
	{
		json.addProperty("key", value.key);
		json.addProperty("value", value.value);
	}

	@Override
	public ConditionIEConfig read(JsonObject json)
	{
		String key = JSONUtils.getString(json, "key");
		boolean value = JSONUtils.getBoolean(json, "value", true);
		return new ConditionIEConfig(value, key);
	}

	@Override
	public ResourceLocation getID()
	{
		return ID;
	}

	public static class ConditionIEConfig implements ICondition
	{
		private final boolean value;
		private final String key;

		public ConditionIEConfig(boolean value, String key)
		{
			this.value = value;
			this.key = key;
		}

		@Override
		public ResourceLocation getID()
		{
			return ID;
		}

		@Override
		public boolean test()
		{
			return IEConfig.ALL.<Boolean>get(key)==value;
		}
	}
}
