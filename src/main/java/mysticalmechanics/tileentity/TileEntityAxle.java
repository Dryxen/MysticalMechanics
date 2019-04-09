package mysticalmechanics.tileentity;

import mysticalmechanics.api.IAxle;
import mysticalmechanics.api.MysticalMechanicsAPI;
import mysticalmechanics.block.BlockAxle;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.AxisDirection;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

public class TileEntityAxle extends MysticalTileEntityBase implements ITickable, IAxle {
	BlockPos front;
	BlockPos back;
	protected double angle, lastAngle;
	private double power = 0;
	private boolean isBroken;
	
	private EnumFacing facing;	
	private EnumFacing inputSide;
	
	public TileEntityAxle() {
		super();		
	}
	
	/*public Axis getAxis() {
		IBlockState state = world.getBlockState(getPos());		
		return state.getValue(BlockAxle.facing).getAxis();
	}*/
	
	public void setConnection() {
		if(facing == null || front == null || back == null) {
		IBlockState state = world.getBlockState(getPos());
		facing = state.getValue(BlockAxle.facing).getOpposite();
		front = getPos().offset(facing);
		back = getPos().offset(facing.getOpposite());		
		markDirty();
		}
	}	
	
	public BlockPos getConnection(AxisDirection dir) {
		return dir == AxisDirection.POSITIVE ? front : back;
	}
	
	/*public boolean isConnection(AxisDirection dir) {
		return getConnection(dir).equals(this.pos);
	}*/
	
	private void checkAndSetInput(EnumFacing from) {
		if(from != null) {
			TileEntity t = world.getTileEntity(getPos().offset(from));
			if(t!=null && t.hasCapability(MysticalMechanicsAPI.MECH_CAPABILITY, from)) {				
				//if the tile entity is a output and we dont have power set the inputside.
				if(t.getCapability(MysticalMechanicsAPI.MECH_CAPABILITY, from).isOutput(from.getOpposite()) && power == 0) {					
					this.inputSide = from;
					
				}
			}
		}
		
	}
	
	//Overloaded this for multiple options to check from.
	public boolean isValidSide(EnumFacing facing){
		return facing == this.facing || facing == this.facing.getOpposite();
	}
	
	public boolean isValidSide(BlockPos from) {		
		return from.equals(front) ||from.equals(back);
	}
	
	@Override
	public void setPos(BlockPos posIn) {
		super.setPos(posIn);		
	}

	public void updatePower() {		
		if(facing == null) {
			setConnection();
		}
		EnumFacing backFacing = facing.getOpposite();
		EnumFacing frontFacing = facing;
		TileEntity frontTile = world.getTileEntity(front);
		TileEntity backTile = world.getTileEntity(back);
		
		if (frontTile != null && frontTile.hasCapability(MysticalMechanicsAPI.MECH_CAPABILITY, backFacing)){
			System.out.println("front check");
			if(frontTile.getCapability(MysticalMechanicsAPI.MECH_CAPABILITY, backFacing).isOutput(backFacing)&&this.isInput(frontFacing)){								
				setPower(frontTile.getCapability(MysticalMechanicsAPI.MECH_CAPABILITY, backFacing).getPower(backFacing), frontFacing);
			}
		}			
			
		if (backTile != null && backTile.hasCapability(MysticalMechanicsAPI.MECH_CAPABILITY, frontFacing)){
			System.out.println("back check");
			if(backTile.getCapability(MysticalMechanicsAPI.MECH_CAPABILITY, frontFacing).isOutput(frontFacing)&&this.isInput(backFacing)) {				
				setPower(backTile.getCapability(MysticalMechanicsAPI.MECH_CAPABILITY, frontFacing).getPower(frontFacing),backFacing);
			}
		}			
		
		/*checkAndUpdateInput(frontTile, backFacing);
		checkAndUpdateInput(backTile, frontFacing);*/
		
		if (frontTile != null && frontTile.hasCapability(MysticalMechanicsAPI.MECH_CAPABILITY, backFacing)){		
			if(frontTile.getCapability(MysticalMechanicsAPI.MECH_CAPABILITY, backFacing).isInput(backFacing)){				
				frontTile.getCapability(MysticalMechanicsAPI.MECH_CAPABILITY, backFacing).setPower(getPower(frontFacing),backFacing);
			}
		}			
			
		if (backTile != null && backTile.hasCapability(MysticalMechanicsAPI.MECH_CAPABILITY, frontFacing)){			
			if(backTile.getCapability(MysticalMechanicsAPI.MECH_CAPABILITY, frontFacing).isInput(frontFacing)) {				
				backTile.getCapability(MysticalMechanicsAPI.MECH_CAPABILITY, frontFacing).setPower(getPower(backFacing),frontFacing);
			}
		}			
	}
	
	public TileEntityAxle getConnectionTile(AxisDirection dir) {
		setConnection();
		TileEntity tile = world.getTileEntity(getConnection(dir));
		if (tile!=null && tile instanceof TileEntityAxle)
			return (TileEntityAxle) tile;
		return null;
	}
	
	public TileEntityAxle getConnectionTile(BlockPos pos, EnumFacing facing) {
		setConnection();
		TileEntity tile = world.getTileEntity(pos);
		if (tile!=null && tile instanceof TileEntityAxle)
			if(((TileEntityAxle)tile).isOutput(facing.getOpposite()))
			return (TileEntityAxle) tile;
		return null;
	}
	

	/*public BlockPos checkAndReturnConnection(BlockPos checkPos, AxisDirection dir) {
		if (!world.isBlockLoaded(checkPos))
			return null;
		TileEntity checkTile = world.getTileEntity(checkPos);
		if (checkTile instanceof TileEntityAxle && ((TileEntityAxle) checkTile).getAxis() == getAxis())
			return ((TileEntityAxle) checkTile).getConnection(dir);
		else
			return getPos();
	}	*/

	
	@Override
	public void updateNeighbors() {
		updatePower();		
	}
	
	public void neighborChanged(BlockPos from) {
		setConnection();
		if(isValidSide(from)) {			
			checkAndSetInput(comparePosToSides(from));			
			System.out.println("side was valid");
			updateNeighbors();
		}
	}
	
	private EnumFacing comparePosToSides(BlockPos from) {
			if(from.equals(front)) {
				return facing;
			}else if(from.equals(back)) {				
				return facing.getOpposite();
			}
			return null;			
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setDouble("mech_power", this.power);
		if (facing != null) {
            tag.setInteger("facing", facing.getIndex());
        }
		if (inputSide != null) {
            tag.setInteger("inputSide", inputSide.getIndex());
        }
		if(front != null) {
			int[] pos = {
				front.getX(), front.getY(),front.getZ()				
			};			
        	tag.setIntArray("front", pos);       	
        }
		if(back != null) {
			int[] pos = {
				back.getX(), back.getY(),back.getZ()				
			};			
        	tag.setIntArray("back", pos);       	
        }		
		return tag;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		this.power = tag.getDouble("mech_power");
		if (tag.hasKey("facing")) {
            facing = EnumFacing.getFront(tag.getInteger("from"));
        }
		if(tag.hasKey("inputSide")) {
			inputSide = EnumFacing.getFront(tag.getInteger("inputSide"));
		}
		if(tag.hasKey("front")){
			int[] pos = tag.getIntArray("front");
			this.front = new BlockPos(pos[0],pos[1],pos[2]);
		}
		if(tag.hasKey("back")){
			int[] pos = tag.getIntArray("back");
			this.back = new BlockPos(pos[0],pos[1],pos[2]);
		}		
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return writeToNBT(new NBTTagCompound());
	}

	@Nullable
	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(getPos(), 0, getUpdateTag());
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		readFromNBT(pkt.getNbtCompound());
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		if(this.facing == null) {
			setConnection();
		}
		if (capability == MysticalMechanicsAPI.MECH_CAPABILITY) {
			IBlockState state = getBlockState();						
			if (state.getBlock() instanceof BlockAxle) {				
				if (facing != null && isValidSide(facing)) {
					return true;
				}
			}
		}
		return super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if (capability == MysticalMechanicsAPI.MECH_CAPABILITY) {
			@SuppressWarnings("unchecked")
			T result = (T) this;
			return result;
		}
		return super.getCapability(capability, facing);
	}

	public boolean activate(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
			EnumFacing side, float hitX, float hitY, float hitZ) {
		return false;
	}

	public void breakBlock(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
		isBroken = true;
		setPower(0f, null);
	}

	@Override
	public void update() {
		if (world.isRemote) {
			lastAngle = angle;
			angle += this.power;
		}
	}

	@Override
	public IBlockState getBlockState() {		
		return world.getBlockState(this.getPos());
	}	

	@Override
	public double getPower(EnumFacing from) {		
		if(from == null) {
			//this should only really be called on block break.
			return this.power;
		}else if (isValidSide(from)&&isOutput(from)){				
			return this.power;
		}	
		return 0;				
	}	

	@Override
	public void setPower(double value, EnumFacing from) {		
		if(from == null && this.isBroken()) {
			this.power = 0;
			onPowerChange();
		}else if(isInput(from)){					
			double oldPower = power; 
			if (isValidSide(from)&& oldPower != value){			
				this.power = value;
				//System.out.println(from+" Was a valid Side, power is now: "+power);
				onPowerChange();
			}
		}		
	}	
	
	@Override
	public boolean isInput(EnumFacing from) {		
		checkAndSetInput(from);
		if(inputSide != null && from != null) {
			return inputSide == from;
		}
		return false;
	}
	
	@Override
	public boolean isOutput(EnumFacing from) {
		if(from != null && inputSide != null) {
			//System.out.println("Axle output was "+inputSide.getOpposite()+" and from was "+from+" was output: "+ (inputSide.getOpposite() == from));
			return inputSide.getOpposite() == from;
		}
		return false;
	}

	@Override
	public void onPowerChange() {
		updateNeighbors();
		markDirty();		
	}
	
	@Override
	public boolean isBroken() {		
		return this.isBroken;
	}
}
