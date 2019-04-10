package mysticalmechanics.tileentity;

import java.nio.ByteBuffer;

import mysticalmechanics.api.IGearBehavior;
import mysticalmechanics.api.MysticalMechanicsAPI;
import mysticalmechanics.block.BlockGearbox;
import mysticalmechanics.util.Misc;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

public class TileEntityMergebox extends TileEntityGearbox {
	
	private double[] gearPower = new double[6];
    private int waitTime;
    private int power;  

    @Override
    public void update() {
        super.update();
        reduceWait();       
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
    public void updateNeighbors() {
        IBlockState state = world.getBlockState(getPos());
        //Mergebox inputs.
        for (EnumFacing f : EnumFacing.VALUES) {
            TileEntity t = world.getTileEntity(getPos().offset(f));
            if (t != null && t.hasCapability(MysticalMechanicsAPI.MECH_CAPABILITY, f.getOpposite()) && !getGear(f).isEmpty()&&!isOutput(f)) {
            	setPower(t.getCapability(MysticalMechanicsAPI.MECH_CAPABILITY, f.getOpposite()).getPower(f.getOpposite()), f);
            }else if(this.getGear(f).isEmpty()&&!isOutput(from)) {
            	setPower(0, f);
            }               
        }       
        
        from = state.getValue(BlockGearbox.facing);            
        TileEntity t = world.getTileEntity(getPos().offset(from));
        
        //Mergebox output.
        if(t != null && t.hasCapability(MysticalMechanicsAPI.MECH_CAPABILITY, from.getOpposite()) && this.isOutput(from)) {
        	if(!getGear(from).isEmpty()) {
        		t.getCapability(MysticalMechanicsAPI.MECH_CAPABILITY, from.getOpposite()).setPower(getPower(from), from.getOpposite());
        	}else if(getGear(from).isEmpty()) {
        		t.getCapability(MysticalMechanicsAPI.MECH_CAPABILITY, from.getOpposite()).setPower(0, from.getOpposite());
        	}
        }        
        markDirty();
    }  
    
    @Override
    public double getPower(EnumFacing from) {
        ItemStack gearStack = getGear(from);
        if (from != null && gearStack.isEmpty()) {
            return 0;
        }
        if (from == null || from == TileEntityMergebox.this.from) {
            return power;
        }
        IGearBehavior behavior = MysticalMechanicsAPI.IMPL.getGearBehavior(gearStack);
        return behavior.transformPower(TileEntityMergebox.this, from, gearStack, gearPower[from.getIndex()]);
    }

    @Override
    public void setPower(double value, EnumFacing from) {
        ItemStack gearStack = getGear(from);
        if(from == TileEntityMergebox.this.from)
            return;
        if (from != null && gearStack.isEmpty())
            setPowerInternal(0, from);
        setPowerInternal(value, from);
    }
    
    public void setPowerInternal(double value, EnumFacing enumFacing) {
        if (enumFacing == null)
            for (int i = 0; i < 6; i++)
                gearPower[i] = value;
        else {
            double oldPower = gearPower[enumFacing.getIndex()];
            this.gearPower[enumFacing.getIndex()] = value;
            if (oldPower != value) {
                power = 0;
                waitTime = 20;
                onPowerChange();
            }
        }
    }
    
    public void reduceWait() {
        if (waitTime > 0) {
            waitTime--;
            if (waitTime <= 0) {
                recalculateOutput();
            }
        }
    }

    public void recalculateOutput() {
        double equalPower = Double.POSITIVE_INFINITY;
        for (EnumFacing facing : EnumFacing.VALUES) {
            if (facing == this.from)
                continue;
            double power = getPower(facing);
            if(power > 0)
            equalPower = Math.min(equalPower, power);
        }
        for (EnumFacing facing : EnumFacing.VALUES) {
            if (facing == TileEntityMergebox.this.from)
                continue;
            double power = getPower(facing);
            if (Misc.isRoughlyEqual(equalPower,power))
                power += power;
        }
        onPowerChange();
    }
    
    @Override
    public boolean isInput(EnumFacing from) {
        return this.from != from;
    }

    @Override
    public boolean isOutput(EnumFacing from) {
        return this.from == from;
    }
    
    protected byte [] convertDoubleToByteArray(double number) {
		 ByteBuffer byteBuffer = ByteBuffer.allocate(Double.BYTES);
		 byteBuffer.putDouble(number);
		 return byteBuffer.array();
		}
	
	protected double convertByteArrayToDouble(byte[] doubleBytes){
		 ByteBuffer byteBuffer = ByteBuffer.allocate(Double.BYTES);
		 byteBuffer.put(doubleBytes);
		 byteBuffer.flip();
		 return byteBuffer.getDouble();
		}
}
