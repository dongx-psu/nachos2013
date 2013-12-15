package nachos.filesys;

import java.util.HashSet;
import java.util.LinkedList;
import nachos.machine.Disk;
import nachos.machine.Lib;

/**
 * FreeList is a single special file used to manage free space of the
 * filesystem. It maintains a list of sector numbers to indicate those that are
 * available to use. When there's a need to allocate a new sector in the
 * filesystem, call allocate(). And you should call deallocate() to free space
 * at a appropriate time (eg. when a file is deleted) for reuse in the future.
 * 
 * @author starforever
 */
public class FreeList extends File {
	/** the static address */
	public static int STATIC_ADDR = 0;

	/** size occupied in the disk (bitmap) */
	static int size = Lib.divRoundUp(Disk.NumSectors, 8);

	/** maintain address of all the free sectors */
	private LinkedList<Integer> free_list = new LinkedList<Integer>();
	
	private HashSet<Integer> used = new HashSet<Integer>();

	public FreeList(INode inode) {
		super(inode);
	}

	public void init() {
		for (int i = 2; i < Disk.NumSectors; ++i)
			free_list.add(i);
		save();
	}

	/** allocate a new sector in the disk */
	public int allocate() {
		return free_list.removeFirst();
	}

	/** deallocate a sector to be reused */
	public void deallocate(int sec) {
		free_list.add(sec);
	}

	/** save the content of freelist to the disk */
	public void save() {
		BitMap bitMap = new BitMap(Disk.NumSectors);
		for (Integer k : free_list)
			bitMap.mark(k);
		bitMap.writeBack(this);
		inode.save();
	}

	/** load the content of freelist from the disk */
	public void load() {
		BitMap bitMap = new BitMap(Disk.NumSectors);
		bitMap.fetchFrom(this);
		for (int i = 0; i < Disk.NumSectors; ++i)
			if (bitMap.check(i))
				free_list.add(i);
	}
	
	public int size() {
		return free_list.size();
	}

	public void calcUsed() {
		for (int i = 0; i < Disk.NumSectors; ++i)
			used.add(i);
		for (Integer k : free_list)
			used.remove(k);
	}
	
	public boolean isUsed(int i) {
		return used.contains(i);
	}
}
