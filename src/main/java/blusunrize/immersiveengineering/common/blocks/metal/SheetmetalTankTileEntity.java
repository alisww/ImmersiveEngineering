/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.blocks.metal;

import blusunrize.immersiveengineering.api.ApiUtils;
import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IBlockOverlayText;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IComparatorOverride;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IPlayerInteraction;
import blusunrize.immersiveengineering.common.blocks.generic.MultiblockPartTileEntity;
import blusunrize.immersiveengineering.common.blocks.multiblocks.MultiblockSheetmetalTank;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.IFluidTank;

public class SheetmetalTankTileEntity extends MultiblockPartTileEntity<SheetmetalTankTileEntity>
		implements IBlockOverlayText, IPlayerInteraction, IComparatorOverride
{
	public static TileEntityType<SheetmetalTankTileEntity> TYPE;

	public FluidTank tank = new FluidTank(512000);
	private int[] oldComps = new int[4];
	private int masterCompOld;

	public SheetmetalTankTileEntity()
	{
		super(MultiblockSheetmetalTank.instance, TYPE, true);
	}

	@Override
	public String[] getOverlayText(PlayerEntity player, RayTraceResult mop, boolean hammer)
	{
		if(Utils.isFluidRelatedItemStack(player.getHeldItem(Hand.MAIN_HAND)))
		{
			SheetmetalTankTileEntity master = master();
			FluidStack fs = master!=null?master.tank.getFluid(): this.tank.getFluid();
			String s = null;
			if(fs!=null)
				s = fs.getLocalizedName()+": "+fs.amount+"mB";
			else
				s = I18n.format(Lib.GUI+"empty");
			return new String[]{s};
		}
		return null;
	}

	@Override
	public boolean useNixieFont(PlayerEntity player, RayTraceResult mop)
	{
		return false;
	}

	@Override
	public void tick()
	{
		ApiUtils.checkForNeedlessTicking(this);
		if(!isDummy()&&!world.isRemote&&!isRSDisabled())
			for(Direction f : Direction.VALUES)
				if(f!=Direction.UP&&tank.getFluidAmount() > 0)
				{
					int outSize = Math.min(144, tank.getFluidAmount());
					FluidStack out = Utils.copyFluidStackWithAmount(tank.getFluid(), outSize, false);
					BlockPos outputPos = getPos().offset(f);
					FluidUtil.getFluidHandler(world, outputPos, f.getOpposite()).ifPresent(output ->
					{
						int accepted = output.fill(out, false);
						if(accepted > 0)
						{
							int drained = output.fill(Utils.copyFluidStackWithAmount(out, Math.min(out.amount, accepted), false), true);
							this.tank.drain(drained, true);
							this.markContainingBlockForUpdate(null);
							updateComparatorValuesPart2();
						}
					});
				}
	}

	@Override
	public int[] getRedstonePos()
	{
		return new int[]{4};
	}

	@Override
	public void readCustomNBT(CompoundNBT nbt, boolean descPacket)
	{
		super.readCustomNBT(nbt, descPacket);
		tank.readFromNBT(nbt.getCompound("tank"));
	}

	@Override
	public void writeCustomNBT(CompoundNBT nbt, boolean descPacket)
	{
		super.writeCustomNBT(nbt, descPacket);
		CompoundNBT tankTag = tank.writeToNBT(new CompoundNBT());
		nbt.put("tank", tankTag);
	}

	@Override
	public float[] getBlockBounds()
	{
		if(posInMultiblock==9)
			return new float[]{.375f, 0, .375f, .625f, 1, .625f};
		if(posInMultiblock==0||posInMultiblock==2||posInMultiblock==6||posInMultiblock==8)
			return new float[]{.375f, 0, .375f, .625f, 1, .625f};
		return new float[]{0, 0, 0, 1, 1, 1};
	}

	@Override
	public BlockPos getOrigin()
	{
		return getPos().add(-offset[0], -offset[1], -offset[2]).offset(facing.rotateYCCW()).offset(facing.getOpposite());
	}

	@Override
	protected IFluidTank[] getAccessibleFluidTanks(Direction side)
	{
		SheetmetalTankTileEntity master = master();
		if(master!=null&&(posInMultiblock==4||posInMultiblock==40))
			return new FluidTank[]{master.tank};
		return new FluidTank[0];
	}

	@Override
	protected boolean canFillTankFrom(int iTank, Direction side, FluidStack resource)
	{
		return posInMultiblock==4||posInMultiblock==40;
	}

	@Override
	protected boolean canDrainTankFrom(int iTank, Direction side)
	{
		return posInMultiblock==4;
	}

	@Override
	public boolean interact(Direction side, PlayerEntity player, Hand hand, ItemStack heldItem, float hitX, float hitY, float hitZ)
	{
		SheetmetalTankTileEntity master = this.master();
		if(master!=null)
		{
			if(FluidUtil.interactWithFluidHandler(player, hand, master.tank))
			{
				this.updateMasterBlock(null, true);
				return true;
			}
		}
		return false;
	}

	@OnlyIn(Dist.CLIENT)
	private AxisAlignedBB renderAABB;

	@Override
	@OnlyIn(Dist.CLIENT)
	public AxisAlignedBB getRenderBoundingBox()
	{
		if(renderAABB==null)
			if(posInMultiblock==4)
				renderAABB = new AxisAlignedBB(getPos().add(-1, 0, -1), getPos().add(2, 5, 2));
			else
				renderAABB = new AxisAlignedBB(getPos(), getPos());
		return renderAABB;
	}

	@Override
	public int getComparatorInputOverride()
	{
		if(posInMultiblock==4)
			return (15*tank.getFluidAmount())/tank.getCapacity();
		SheetmetalTankTileEntity master = master();
		if(offset[1] >= 1&&offset[1] <= 4&&master!=null)//4 layers of storage
		{
			FluidTank t = master.tank;
			int layer = offset[1]-1;
			int vol = t.getCapacity()/4;
			int filled = t.getFluidAmount()-layer*vol;
			return Math.min(15, Math.max(0, (15*filled)/vol));
		}
		return 0;
	}

	private void updateComparatorValuesPart1()
	{
		int vol = tank.getCapacity()/4;
		for(int i = 0; i < 4; i++)
		{
			int filled = tank.getFluidAmount()-i*vol;
			oldComps[i] = Math.min(15, Math.max((15*filled)/vol, 0));
		}
		masterCompOld = (15*tank.getFluidAmount())/tank.getCapacity();
	}

	private void updateComparatorValuesPart2()
	{
		int vol = tank.getCapacity()/6;
		if((15*tank.getFluidAmount())/tank.getCapacity()!=masterCompOld)
			world.notifyNeighborsOfStateChange(getPos(), getBlockState().getBlock());
		for(int i = 0; i < 4; i++)
		{
			int filled = tank.getFluidAmount()-i*vol;
			int now = Math.min(15, Math.max((15*filled)/vol, 0));
			if(now!=oldComps[i])
			{
				for(int x = -1; x <= 1; x++)
					for(int z = -1; z <= 1; z++)
					{
						BlockPos pos = getPos().add(-offset[0]+x, -offset[1]+i+1, -offset[2]+z);
						world.notifyNeighborsOfStateChange(pos, world.getBlockState(pos).getBlock());
					}
			}
		}
	}
}