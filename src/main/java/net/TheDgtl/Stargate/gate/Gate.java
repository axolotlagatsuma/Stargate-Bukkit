package net.TheDgtl.Stargate.gate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.EndGateway;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Orientable;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import net.TheDgtl.Stargate.Stargate;
import net.TheDgtl.Stargate.actions.BlockSetAction;
import net.TheDgtl.Stargate.exception.GateConflict;
import net.TheDgtl.Stargate.exception.InvalidStructure;
import net.TheDgtl.Stargate.network.Network;
import net.TheDgtl.Stargate.network.portal.PortalFlag;
import net.TheDgtl.Stargate.network.portal.SGLocation;

/**
 * Acts as an interface for portals to modify worlds
 * @author Thorin
 *
 */
public class Gate {

	private GateFormat format;
	/*
	 * a vector operation that goes from world -> format. Also contains the inverse
	 * operation for this
	 */
	VectorOperation converter; 
	/**
	 * WARNING: Don't modify this ever, always use .copy()
	 */
	Location topLeft;
	/**
	 * WARNING: Don't modify this ever, always use .copy()
	 */
	BlockVector signPos;
	private BlockFace facing;
	private boolean isOpen = false;

	
	
	static final private Material DEFAULTBUTTON = Material.STONE_BUTTON;
	static final private Material WATERBUTTON = Material.DEAD_TUBE_CORAL_WALL_FAN;
	static final public HashSet<Material> ALLPORTALMATERALS = new HashSet<>();
	
	/**
	 * Compares the format to real world; If the format matches with the world,
	 * independent of rotation and mirroring.
	 * 
	 * @param format
	 * @param loc
	 * @throws InvalidStructure
	 * @throws GateConflict
	 */
	public Gate(GateFormat format, Location loc, BlockFace signFace) throws InvalidStructure, GateConflict {
		this.setFormat(format);
		facing = signFace;
		converter = new VectorOperation(signFace);

		if (matchesFormat(loc))
			return;
		converter.flipZAxis = true;
		if (matchesFormat(loc))
			return;

		throw new InvalidStructure();
	}

	/**
	 * Checks if format matches independent of controlBlock
	 * TODO: symmetric formats will be checked twice, make a way to determine if a format is symmetric to avoid this
	 * @param loc
	 * @return
	 * @throws GateConflict 
	 */
	private boolean matchesFormat(Location loc) throws GateConflict {
		List<BlockVector> controlBlocks = getFormat().getControllBlocks();
		for (BlockVector controlBlock : controlBlocks) {
			/*
			 * Topleft is origo for the format, everything becomes easier if you calculate
			 * this position in the world; this is a hypothetical position, calculated from
			 * the position of the sign minus a vector of a hypothetical sign position in
			 * format.
			 */
			topLeft = loc.clone().subtract(converter.doInverse(controlBlock));
			
			if (getFormat().matches(converter, topLeft)) {
				if(isGateConflict()) {
					throw new GateConflict();
				}
				signPos = controlBlock;
				return true;
			}
		}
		return false;
	}
	
	private boolean isGateConflict() {
		List<SGLocation> locations = this.getLocations(GateStructureType.FRAME);
		for(SGLocation loc : locations) {
			if(Network.getPortal(loc, GateStructureType.values()) != null ) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Set button and draw sign
	 * 
	 * @param signLines an array with 4 elements, representing each line of a sign
	 */
	public void drawControll(String[] signLines, boolean isDrawButton) {
		Location signLoc = topLeft.clone().add(converter.doInverse(signPos));
		BlockState signState = signLoc.getBlock().getState();
		if (!(signState instanceof Sign)) {
			Stargate.log(Level.FINE, "Could not find sign at position " + signLoc.toString());
			return;
		}

		Sign sign = (Sign) signState;
		for (int i = 0; i < 4; i++) {
			sign.setLine(i, signLines[i]);
		}
		new BlockSetAction(Stargate.syncTickPopulator, sign, true);
		if(!isDrawButton)
			return;
		/*
		 * Just a cheat to exclude the sign location, and determine the position of the
		 * button. Note that this will have weird behaviour if there's more than 3
		 * controllblocks
		 */
		Location buttonLoc = topLeft.clone();
		for (BlockVector buttonVec : getFormat().getControllBlocks()) {
			if (signPos == buttonVec)
				continue;
			buttonLoc.add(converter.doInverse(buttonVec));
			break;
		}
		/*
		 * Set a button with the same facing as the sign
		 */
		Material buttonMat = getButtonMaterial();
        Directional buttonData = (Directional) Bukkit.createBlockData(buttonMat);
        buttonData.setFacing(getButtonFacing(buttonMat,(Directional) sign.getBlockData()));
        
		buttonLoc.getBlock().setBlockData(buttonData);
		
	}
	
	public Location getSignLoc() {
		return topLeft.clone().add(converter.doInverse(signPos));
	}
	
	private BlockFace getButtonFacing(Material buttonMat, Directional signDirection) {
		return signDirection.getFacing();
	}
	
	private Material getButtonMaterial() {
		Material portalClosedMat = getFormat().getIrisMat(false);
		switch(portalClosedMat){
		case AIR:
			return DEFAULTBUTTON;
		case WATER:
			return WATERBUTTON;
		default:
			Stargate.log(Level.INFO, portalClosedMat.name() + " is currently not suported as a portal closed material");
			return DEFAULTBUTTON;
		}
	}
	
	/**
	 * 
	 * @param structKey , key for the structuretype to be retrieved
	 * @return
	 */
	public List<SGLocation> getLocations(GateStructureType structKey) {
		List<SGLocation> output = new ArrayList<>();
		for(BlockVector vec : getFormat().portalParts.get(structKey).getPartsPos()) {
			Location loc = topLeft.clone().add(converter.doInverse(vec));
			output.add(new SGLocation(loc));
		}
		return output;
	}
	/**
	 * Set the iris mat, note that nether portals have to be oriented in the right axis, and 
	 * force a location to prevent exit gateway generation.
	 * @param mat
	 */
	private void setIrisMat(Material mat) {
		GateStructureType targetType = GateStructureType.IRIS;
		List<SGLocation> locs = getLocations(targetType);
		BlockData blockData = Bukkit.createBlockData(mat);

		
		if(blockData instanceof Orientable) {
			Orientable orientation = (Orientable) blockData;
			orientation.setAxis(converter.irisNormal);
			blockData = orientation;
		}
		
		
		for (SGLocation loc : locs) {
			Block blk = loc.getLocation().getBlock();
			blk.setBlockData(blockData);
			
			if (mat == Material.END_GATEWAY) {
                // force a location to prevent exit gateway generation
                EndGateway gateway = (EndGateway) blk.getState();
                // https://github.com/stargate-bukkit/Stargate-Bukkit/issues/36
                gateway.setAge(-9223372036854775808L);
                if(blk.getWorld().getEnvironment() == World.Environment.THE_END){
                      gateway.setExitLocation(blk.getWorld().getSpawnLocation());
                      gateway.setExactTeleport(true);
                }
                gateway.update(false, false);
            }
		}
	}
	
	public void open() {
		Material mat = getFormat().getIrisMat(true);
		setIrisMat(mat);
		setOpen(true);
		
	}
	
	public void close() {
		Material mat = getFormat().getIrisMat(false);
		setIrisMat(mat);
		setOpen(false);
	}

	public Location getExit(boolean isBackwards) {
		BlockVector formatExit = getFormat().getExit();
		Location exit = topLeft.clone().add(converter.doInverse(formatExit));
		
		Vector offsett = facing.getDirection();
		if(isBackwards)
			return exit.subtract(offsett);
		return exit.add(offsett);
	}
	
	public boolean isOpen() {
		return isOpen;
	}

	public void setOpen(boolean isOpen) {
		this.isOpen = isOpen;
	}

	public GateFormat getFormat() {
		return format;
	}

	public void setFormat(GateFormat format) {
		this.format = format;
	}

	public class VectorOperation {
		/*
		 * EVERY OPERATION DOES NOT MUTE/CHANGE THE INITIAL OBJECT!!!!
		 */
		Vector rotationAxis;
		double rotation; // radians
		Axis irisNormal; // mathematical term for the normal axis orthogonal to the iris plane
		boolean flipZAxis = false;
		MatrixYRotation matrixRotation;
		private MatrixYRotation matrixInverseRotation;
		
		/**
		 * Compiles a vector operation which matches with the direction of a sign
		 * @param signFace
		 * @throws InvalidStructure 
		 */
		private VectorOperation(BlockFace signFace) throws InvalidStructure {
			switch(signFace) {
			case EAST:
				rotation = 0;
				irisNormal = Axis.Z;
				break;
			case SOUTH:
				rotation = Math.PI/2;
				irisNormal = Axis.X;
				break;
			case WEST:
				rotation = Math.PI;
				irisNormal = Axis.Z;
				break;
			case NORTH:
				rotation = -Math.PI/2;
				irisNormal = Axis.X;
				break;
			default:
				throw new InvalidStructure();
			}
			
			
			matrixRotation = new MatrixYRotation(rotation);
			matrixInverseRotation = matrixRotation.getInverse();
			Stargate.log(Level.FINER, "Chose a format rotation of " + rotation + " radians");
		}

		/**
		 * Inverse operation of doInverse; A vector operation that rotates around origo
		 * and flips z axis. Does not permute input vector
		 * 
		 * @param vector
		 * @return vector
		 */
		public BlockVector doOperation(BlockVector vector) {
			BlockVector output = matrixRotation.operation(vector);
			if (flipZAxis)
				output.setZ(-output.getZ());
			return output;
		}

		/**
		 * Inverse operation of doOperation; A vector operation that rotates around
		 * origo and flips z axis. Does not permute input vector
		 * 
		 * @param vector
		 * @return vector
		 */
		public BlockVector doInverse(BlockVector vector) {
			BlockVector output = vector.clone();
			if (flipZAxis)
				output.setZ(-output.getZ());
			return matrixInverseRotation.operation(output);
		}
		/**
		 * A vector rotation limited to n*pi/2 radians rotations. Rotates around y-axis.
		 * Rounded to avoid Math.sin and Math.cos approximation errors.<p>
		 * 
		 * </p>DOES NOT PERMUTE INPUT VECTOR
		 * @author Thorin
		 */
		class MatrixYRotation{
			private int sinTheta;
			private int cosTheta;
			private double rot;
			private MatrixYRotation(double rot) {
				sinTheta = (int) Math.round(Math.sin(rot));
				cosTheta = (int) Math.round(Math.cos(rot));
				this.rot = rot;
			}
			
			BlockVector operation(BlockVector vector) {
				BlockVector newVector = new BlockVector();
				
				newVector.setX(sinTheta*vector.getBlockZ() + cosTheta*vector.getBlockX());
				newVector.setY(vector.getBlockY());
				newVector.setZ(cosTheta*vector.getBlockZ()-sinTheta*vector.getBlockX());
				return newVector;
			}
			
			MatrixYRotation getInverse() {
				return new MatrixYRotation(-rot);
			}
		}
	}
	
}
