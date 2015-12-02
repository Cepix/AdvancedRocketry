package zmaster587.advancedRocketry.tile.Satellite;

import java.util.LinkedList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.ForgeDirection;
import zmaster587.advancedRocketry.Inventory.TextureResources;
import zmaster587.advancedRocketry.Inventory.modules.IButtonInventory;
import zmaster587.advancedRocketry.Inventory.modules.IModularInventory;
import zmaster587.advancedRocketry.Inventory.modules.ModuleBase;
import zmaster587.advancedRocketry.Inventory.modules.ModuleButton;
import zmaster587.advancedRocketry.Inventory.modules.ModuleData;
import zmaster587.advancedRocketry.Inventory.modules.ModulePower;
import zmaster587.advancedRocketry.Inventory.modules.ModuleSatellite;
import zmaster587.advancedRocketry.Inventory.modules.ModuleSlotArray;
import zmaster587.advancedRocketry.Inventory.modules.ModuleText;
import zmaster587.advancedRocketry.Inventory.modules.ModuleToggleSwitch;
import zmaster587.advancedRocketry.api.DataStorage;
import zmaster587.advancedRocketry.api.DataStorage.DataType;
import zmaster587.advancedRocketry.api.dimension.DimensionManager;
import zmaster587.advancedRocketry.api.network.PacketHandler;
import zmaster587.advancedRocketry.api.network.PacketMachine;
import zmaster587.advancedRocketry.api.satellite.IDataHandler;
import zmaster587.advancedRocketry.api.satellite.SatelliteBase;
import zmaster587.advancedRocketry.item.ItemData;
import zmaster587.advancedRocketry.item.ItemSatelliteIdentificationChip;
import zmaster587.advancedRocketry.util.IDataInventory;
import zmaster587.libVulpes.tile.TileInventoriedRFConsumer;
import zmaster587.libVulpes.util.INetworkMachine;

public class TileEntitySatelliteControlCenter extends TileInventoriedRFConsumer implements INetworkMachine, IModularInventory, IButtonInventory, IDataInventory, IDataHandler {


	//ModuleText satelliteText;
	ModuleSatellite moduleSatellite;
	ModuleText moduleText;
	DataStorage data;

	public TileEntitySatelliteControlCenter() {
		super(10000, 2);

		data = new DataStorage();
		data.setMaxData(1000);
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int p_94128_1_) {
		return new int[0];
	}

	@Override
	public String getModularInventoryName() {
		return "container.satelliteMonitor";
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		return true;
	}

	@Override
	public boolean canPerformFunction() {
		return worldObj.getTotalWorldTime() % 16 == 0 && getSatelliteFromSlot(0) != null;
	}

	@Override
	public int getPowerPerOperation() {
		return 1;
	}

	@Override
	public void performFunction() {
		if(worldObj.isRemote)
			updateInventoryInfo();
	}

	@Override
	public void writeDataToNetwork(ByteBuf out, byte packetId) {
	}

	@Override
	public void readDataFromNetwork(ByteBuf in, byte packetId,
			NBTTagCompound nbt) {

	}

	@Override
	public void updateEntity() {
		super.updateEntity();
	}

	@Override
	public void useNetworkData(EntityPlayer player, Side side, byte id,
			NBTTagCompound nbt) {

		if(id == 0) {
			storeData();
		}
		else if( id == 100 ) {

			SatelliteBase satellite = moduleSatellite.getSatellite();
			
			if(satellite != null && satellite.getDimensionId() == this.worldObj.provider.dimensionId) {
				satellite.performAction(player, worldObj, xCoord, yCoord, zCoord);
			}
		}
		else if( id == 101) {
			onInventoryButtonPressed(id - 100);
		}
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack stack) {
		super.setInventorySlotContents(slot, stack);
		moduleSatellite.setSatellite(getSatelliteFromSlot(0));
		updateInventoryInfo();
	}

	public void updateInventoryInfo() {
		if(moduleText != null) {

			SatelliteBase satellite = moduleSatellite.getSatellite();
			if(satellite != null) {
				if(getEnergyStored() < getPowerPerOperation()) 
					moduleText.setText("Not Enough power!");
				else if(satellite.getDimensionId() != this.worldObj.provider.dimensionId) {
					moduleText.setText(satellite.getName() + "\n\nToo Far" );
				}

				else
					moduleText.setText(satellite.getName() + "\n\nInfo:\n" + satellite.getInfo(worldObj));
			}
			else
				moduleText.setText("No Link...");
		}
	}


	public SatelliteBase getSatelliteFromSlot(int slot) {

		ItemStack stack = getStackInSlot(slot);
		if(stack != null && stack.getItem() instanceof ItemSatelliteIdentificationChip) {
			ItemSatelliteIdentificationChip idchip = (ItemSatelliteIdentificationChip)stack.getItem();

			return idchip.getSatellite(stack);
		}

		return null;
	}

	@Override
	public List<ModuleBase> getModules() {

		List<ModuleBase> modules = new LinkedList<ModuleBase>();
		modules.add(new ModulePower(18, 20, this.energy));
		modules.add(new ModuleButton(116, 70, 0, "Connect!", this, TextureResources.buttonBuild));
		modules.add(new ModuleButton(173, 3, 1, "", this, TextureResources.buttonKill, "Destroy Satellite", 24, 24));
		modules.add(new ModuleData(28, 20, 1, this, data));

		moduleSatellite = new ModuleSatellite(152, 10, this, 0);
		modules.add(moduleSatellite);
		
		//Try to assign a satellite ASAP
		moduleSatellite.setSatellite(getSatelliteFromSlot(0));
		
		moduleText = new ModuleText(60, 20, "No Link...", 0x404040);
		modules.add(moduleText);

		updateInventoryInfo();
		return modules;
	}

	@Override
	public void onInventoryButtonPressed(int buttonId) {

		if(buttonId == 0) {
			PacketHandler.sendToServer(new PacketMachine(this, (byte)(100 + buttonId)) );

		}
		else if(buttonId == 1) {
			ItemStack stack = getStackInSlot(0);

			if(stack != null && stack.getItem() instanceof ItemSatelliteIdentificationChip) {
				ItemSatelliteIdentificationChip idchip = (ItemSatelliteIdentificationChip)stack.getItem();

				SatelliteBase satellite = idchip.getSatellite(stack);

				//Somebody might want to erase the chip of an already existing satellite
				if(satellite != null)
					DimensionManager.getInstance().getDimensionProperties(satellite.getDimensionId()).removeSatellite(satellite.getId());

				idchip.erase(stack);
				setInventorySlotContents(0, stack);
			}
		}

	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);

		NBTTagCompound data = new NBTTagCompound();

		this.data.writeToNBT(data);
		nbt.setTag("data", data);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);

		NBTTagCompound data = nbt.getCompoundTag("data");
		this.data.readFromNBT(data);
	}

	@Override
	public void loadData() {
	}

	@Override
	public void storeData() {
		if(!worldObj.isRemote) {
			ItemStack inv = getStackInSlot(1);
			if(inv != null && inv.getItem() instanceof ItemData && inv.stackSize == 1) {
				ItemData dataItem = (ItemData)inv.getItem();
				data.removeData(dataItem.addData(inv, data.getData(), data.getDataType()));
			}
		}
		else {
			PacketHandler.sendToServer(new PacketMachine(this, (byte)0));
		}
	}

	@Override
	public int extractData(int maxAmount, DataType type) {
		//TODO
		return 0;
	}

	@Override
	public int addData(int maxAmount, DataType type) {

		return data.addData(maxAmount, type);
	}

	@Override
	public boolean canInteractWithContainer(EntityPlayer entity) {
		return true;
	}
}
