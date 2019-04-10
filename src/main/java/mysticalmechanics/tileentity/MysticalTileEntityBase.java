package mysticalmechanics.tileentity;

import mysticalmechanics.api.IMechCapability;
import mysticalmechanics.util.Misc;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

public abstract class MysticalTileEntityBase extends TileEntity implements IMechCapability {	
	
	@Override
	public void markDirty() {
		super.markDirty();
		Misc.syncTE(this);
	}
	
	public abstract boolean isBroken();
	
	public abstract double getPower(EnumFacing from);
	
	public abstract void setPower(double value, EnumFacing from);
	
	public abstract void onPowerChange();
	
	public abstract void updateNeighbors();
	
	public abstract IBlockState getBlockState();	
	

}
