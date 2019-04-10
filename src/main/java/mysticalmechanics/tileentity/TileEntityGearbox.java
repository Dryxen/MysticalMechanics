package mysticalmechanics.tileentity;

import mysticalmechanics.MysticalMechanics;
import mysticalmechanics.api.IGearBehavior;
import mysticalmechanics.api.IGearbox;
import mysticalmechanics.api.MysticalMechanicsAPI;
import mysticalmechanics.block.BlockGearbox;
import mysticalmechanics.handler.RegistryHandler;
import mysticalmechanics.util.ISoundController;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class TileEntityGearbox extends MysticalTileEntityBase implements ITickable, IGearbox, ISoundController {
    EnumFacing from = null;   
    private boolean isBroken;
    public int connections = 0;
    public double power = 0;
    public ItemStack[] gears = new ItemStack[]{
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY
    };
    public double angle, lastAngle;    

    //Don't look at me
    public static final int SOUND_SLOW_LV1 = 1;
    public static final int SOUND_SLOW_LV2 = 2;
    public static final int SOUND_SLOW_LV3 = 3;
    public static final int SOUND_MID_LV1 = 4;
    public static final int SOUND_MID_LV2 = 5;
    public static final int SOUND_MID_LV3 = 6;
    public static final int SOUND_FAST_LV1 = 7;
    public static final int SOUND_FAST_LV2 = 8;
    public static final int SOUND_FAST_LV3 = 9;
    public static final int[] SOUND_IDS = new int[]{SOUND_SLOW_LV1,SOUND_SLOW_LV2,SOUND_SLOW_LV3,SOUND_MID_LV1,SOUND_MID_LV2,SOUND_MID_LV3,SOUND_FAST_LV1,SOUND_FAST_LV2,SOUND_FAST_LV3};

    HashSet<Integer> soundsPlaying = new HashSet<>();

    public TileEntityGearbox() {
        super();        
    }  
    
    @Override
    public void updateNeighbors() {
        IBlockState state = world.getBlockState(getPos());        
        if (state.getBlock() instanceof BlockGearbox) {
            from = state.getValue(BlockGearbox.facing);
            TileEntity t = world.getTileEntity(getPos().offset(from));
            if (t != null && t.hasCapability(MysticalMechanicsAPI.MECH_CAPABILITY, from.getOpposite()) && !getGear(from).isEmpty()&& isInput(from)) {
            	if(t.getCapability(MysticalMechanicsAPI.MECH_CAPABILITY, from.getOpposite()).isOutput(from.getOpposite()))
            		setPower(t.getCapability(MysticalMechanicsAPI.MECH_CAPABILITY, from.getOpposite()).getPower(from.getOpposite()), from);
            } else if(getGear(from).isEmpty()&&isInput(from)){
            	setPower(0, from);            	
            	//this check is simply to make debugging easier reducing the setPower calls to the input side only.
            	
            }
                
        }
        connections = 0;
        List<EnumFacing> toUpdate = new ArrayList<>();
        for (EnumFacing f : EnumFacing.values()) {
            if (f != null && f != from) {
                TileEntity t = world.getTileEntity(getPos().offset(f));
                if (t != null && t.hasCapability(MysticalMechanicsAPI.MECH_CAPABILITY, f.getOpposite())) {
                    if (!getGear(f).isEmpty()) {
                    	connections++;
                    }
                    toUpdate.add(f);
                    
                    
                }
            }                  
        }
        for (EnumFacing f : toUpdate) {
            BlockPos p = getPos().offset(f);
            TileEntity t = world.getTileEntity(p);            
            if(t != null && t.hasCapability(MysticalMechanicsAPI.MECH_CAPABILITY, f.getOpposite()) && this.isOutput(f)) {            	            	
            	if(!getGear(f).isEmpty()) {            		
            		t.getCapability(MysticalMechanicsAPI.MECH_CAPABILITY, f.getOpposite()).setPower(getPower(f), f.getOpposite());
            	}else if (getGear(f).isEmpty()) {
            		t.getCapability(MysticalMechanicsAPI.MECH_CAPABILITY, f.getOpposite()).setPower(0, f.getOpposite());
            	}
            }
        }
        markDirty();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setDouble("mech_power", this.power);
        if (from != null) {
            tag.setInteger("from", from.getIndex());
        }
        for (int i = 0; i < 6; i++) {
            tag.setTag("gear" + i, gears[i].writeToNBT(new NBTTagCompound()));
        }
        
        tag.setInteger("connections", connections);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey("mech_power")) {
            this.power = tag.getDouble("mech_power");
        }
        if (tag.hasKey("from")) {
            from = EnumFacing.getFront(tag.getInteger("from"));
        }
        for (int i = 0; i < 6; i++) {
            gears[i] = new ItemStack(tag.getCompoundTag("gear" + i));
        }
        connections = tag.getInteger("connections");
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
        if (capability == MysticalMechanicsAPI.MECH_CAPABILITY) {
            return true;
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

    @Override
    public void attachGear(EnumFacing facing, ItemStack stack) {
        if (facing == null)
            return;
        gears[facing.getIndex()] = stack;
        world.playSound(null,pos,RegistryHandler.GEAR_ADD,SoundCategory.BLOCKS,1.0f,1.0f);        
    }

    @Override
    public ItemStack detachGear(EnumFacing facing) {
        if (facing == null)
            return ItemStack.EMPTY;
        int index = facing.getIndex();
        ItemStack gear = gears[index];
        gears[index] = ItemStack.EMPTY;
        world.playSound(null,pos,RegistryHandler.GEAR_REMOVE,SoundCategory.BLOCKS,1.0f,1.0f);        
        return gear;
    }

    public ItemStack getGear(EnumFacing facing) {
        if (facing == null)
            return ItemStack.EMPTY;
        return gears[facing.getIndex()];
    }

    @Override
    public boolean canAttachGear(EnumFacing facing, ItemStack stack) {
        return true;
    }

    @Override
    public int getConnections() {
        return connections;
    }

    public boolean activate(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                            EnumFacing side, float hitX, float hitY, float hitZ) {
        ItemStack heldItem = player.getHeldItem(hand);
        if (!heldItem.isEmpty() && canAttachGear(side,heldItem)) {
            if (getGear(side).isEmpty() && MysticalMechanicsAPI.IMPL.isValidGear(heldItem)) {
                ItemStack gear = heldItem.copy();
                gear.setCount(1);
                attachGear(side,gear);
                heldItem.shrink(1);
                if (heldItem.isEmpty()) {
                    player.setHeldItem(hand, ItemStack.EMPTY);
                }
                onPowerChange();
                return true;
            }
        } else if (!getGear(side).isEmpty()) {
            ItemStack gear = detachGear(side);
            if (!world.isRemote) {
                world.spawnEntity(new EntityItem(world, player.posX, player.posY + player.height / 2.0f, player.posZ, gear));
            }
            onPowerChange();
            return true;
        }
        return false;
    }

    @Override
    public void playSound(int id) {
        switch (id) {
            case SOUND_SLOW_LV1:
                MysticalMechanics.proxy.playMachineSound(this, SOUND_SLOW_LV1, RegistryHandler.GEARBOX_SLOW_LV1, SoundCategory.BLOCKS, true, 1.0f, 1.0f, (float)pos.getX()+0.5f,(float)pos.getY()+0.5f,(float)pos.getZ()+0.5f);
                break;
            case SOUND_SLOW_LV2:
                MysticalMechanics.proxy.playMachineSound(this, SOUND_SLOW_LV2, RegistryHandler.GEARBOX_SLOW_LV2, SoundCategory.BLOCKS, true, 1.0f, 1.0f, (float)pos.getX()+0.5f,(float)pos.getY()+0.5f,(float)pos.getZ()+0.5f);
                break;
            case SOUND_SLOW_LV3:
                MysticalMechanics.proxy.playMachineSound(this, SOUND_SLOW_LV3, RegistryHandler.GEARBOX_SLOW_LV3, SoundCategory.BLOCKS, true, 1.0f, 1.0f, (float)pos.getX()+0.5f,(float)pos.getY()+0.5f,(float)pos.getZ()+0.5f);
                break;
            case SOUND_MID_LV1:
                MysticalMechanics.proxy.playMachineSound(this, SOUND_MID_LV1, RegistryHandler.GEARBOX_MID_LV1, SoundCategory.BLOCKS, true, 1.0f, 1.0f, (float)pos.getX()+0.5f,(float)pos.getY()+0.5f,(float)pos.getZ()+0.5f);
                break;
            case SOUND_MID_LV2:
                MysticalMechanics.proxy.playMachineSound(this, SOUND_MID_LV2, RegistryHandler.GEARBOX_MID_LV2, SoundCategory.BLOCKS, true, 1.0f, 1.0f, (float)pos.getX()+0.5f,(float)pos.getY()+0.5f,(float)pos.getZ()+0.5f);
                break;
            case SOUND_MID_LV3:
                MysticalMechanics.proxy.playMachineSound(this, SOUND_MID_LV3, RegistryHandler.GEARBOX_MID_LV3, SoundCategory.BLOCKS, true, 1.0f, 1.0f, (float)pos.getX()+0.5f,(float)pos.getY()+0.5f,(float)pos.getZ()+0.5f);
                break;
            case SOUND_FAST_LV1:
                MysticalMechanics.proxy.playMachineSound(this, SOUND_FAST_LV1, RegistryHandler.GEARBOX_FAST_LV1, SoundCategory.BLOCKS, true, 1.0f, 1.0f, (float)pos.getX()+0.5f,(float)pos.getY()+0.5f,(float)pos.getZ()+0.5f);
                break;
            case SOUND_FAST_LV2:
                MysticalMechanics.proxy.playMachineSound(this, SOUND_FAST_LV2, RegistryHandler.GEARBOX_FAST_LV2, SoundCategory.BLOCKS, true, 1.0f, 1.0f, (float)pos.getX()+0.5f,(float)pos.getY()+0.5f,(float)pos.getZ()+0.5f);
                break;
            case SOUND_FAST_LV3:
                MysticalMechanics.proxy.playMachineSound(this, SOUND_FAST_LV2, RegistryHandler.GEARBOX_FAST_LV3, SoundCategory.BLOCKS, true, 1.0f, 1.0f, (float)pos.getX()+0.5f,(float)pos.getY()+0.5f,(float)pos.getZ()+0.5f);
                break;
        }
        soundsPlaying.add(id);
    }

    @Override
    public void stopSound(int id) {
        soundsPlaying.remove(id);
    }

    @Override
    public boolean isSoundPlaying(int id) {
        return soundsPlaying.contains(id);
    }

    @Override
    public int[] getSoundIDs() {
        return SOUND_IDS;
    }

    @Override
    public boolean shouldPlaySound(int id) {        
        int level = getSoundLevel();
        int speedindex = getSpeedindex(this.power);
        return speedindex > 0 && level > 0 && id == SOUND_IDS[speedindex - 1 + level];
    }

    private int getSpeedindex(double power) {
        int speedindex = 0;
        if(power > 50)
            speedindex = 3;
        else if(power > 25)
            speedindex = 2;
        else if(power > 0)
            speedindex = 1;
        return speedindex;
    }

    @Override
    public float getCurrentPitch(int id, float pitch) {        
        int speedindex = getSpeedindex(this.power);
        if(speedindex == 1)
            return (float) (power*2 / 25.0);
        else if(speedindex == 2)
            return (float) (power*2 / 50.0);
        else if(speedindex == 3)
            return (float) (power*2 / 100.0);
        return 0;
    }

    private int getSoundLevel() {
        int level = 0;
        for (EnumFacing.Axis axis : EnumFacing.Axis.values()) {
            if(!getGear(EnumFacing.getFacingFromAxis(EnumFacing.AxisDirection.POSITIVE,axis)).isEmpty() || !getGear(EnumFacing.getFacingFromAxis(EnumFacing.AxisDirection.NEGATIVE,axis)).isEmpty())
                level++;
        }
        return level;
    } 

    public void breakBlock(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        for (int i = 0; i < 6; i++) {
            if (!world.isRemote) {
                world.spawnEntity(new EntityItem(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, gears[i]));
            }
            gears[i] = ItemStack.EMPTY;
        }
        isBroken = true;
        setPower(0f, null);
        updateNeighbors();
    }

    @Override
    public void update() {
        if (world.isRemote) {
            handleSound();
            lastAngle = angle;
            angle += this.power;
            for(EnumFacing facing : EnumFacing.VALUES) {
                ItemStack gear = getGear(facing);
                IGearBehavior behavior = MysticalMechanicsAPI.IMPL.getGearBehavior(gear);
                behavior.visualUpdate(this,facing,gear);
            }               
        }
    }

	@Override
	public IBlockState getBlockState() {		
		return world.getBlockState(pos);
	}

	@Override
	public boolean isBroken() {		
		return this.isBroken;
	}

	@Override
	public double getPower(EnumFacing from) {
		 ItemStack gearStack = getGear(from);
         if (from != null && gearStack.isEmpty()) {
             return 0;
         }
         IGearBehavior behavior = MysticalMechanicsAPI.IMPL.getGearBehavior(gearStack);
         double currentPower = 0;

         if (from == TileEntityGearbox.this.from|| from == null)//
             currentPower = this.power;
         else
             currentPower = this.power / ((double) (Math.max(1, getConnections())));

         return behavior.transformPower(this, from, gearStack, currentPower);
	}

	@Override
	public void setPower(double value, EnumFacing from) {
		ItemStack gearStack = getGear(from);
        if (from != null && gearStack.isEmpty()) {
        	if(this.power != 0) {
        		this.power = 0;
        		onPowerChange();
        	}
            
        }        
        if (isInput(from)&& !gearStack.isEmpty()) {        	
        	double oldPower = power;            
            if (oldPower != value) {
            	this.power = value;
                onPowerChange();
            }          
        }		
	}
	
	@Override
	public boolean isInput(EnumFacing from) {
		if(from!=null) {			
			return this.from == from;
		}
		return false;
	    
	}

	@Override
	public boolean isOutput(EnumFacing from) {
	    return this.from != from;
	}

	@Override
	public void onPowerChange() {		
        this.updateNeighbors();
        markDirty();		
	}	
}
