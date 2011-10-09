package nachos.filesys;

import nachos.machine.Disk;
import nachos.machine.Lib;
import nachos.machine.Machine;

/**
 * @author Kang Zhang
 * 
 *         This file is derived from the code by 1998 Rice University and
 *         1992-1993 The Regents of the University of California.
 */

// FileHeader.jave
// Routines for managing the disk file header (in UNIX, this
// would be called the i-node).
//
// The file header is used to locate where on disk the
// file's data is stored. We implement this as a fixed size
// table of pointers -- each entry in the table points to the
// disk sector containing that portion of the file data
// (in other words, there are no indirect or doubly indirect
// blocks). The table size is chosen so that the file header
// will be just big enough to fit in one disk sector,
//
// Unlike in a real system, we do not keep track of file permissions,
// ownership, last modification date, etc., in the file header.
//
// A file header can be initialized in two ways:
// for a new file, by modifying the in-memory data structure
// to point to the newly allocated data blocks
// for a file already on disk, by reading the file header from disk
//
//
// A file header describes where on disk to find the data in a file,
// along with other information about the file (for instance, its
// length, owner, etc.)
//
// The following class defines the Nachos "file header" (in UNIX terms,
// the "i-node"), describing where on disk to find all of the data in the file.
// The file header is organized as a simple table of pointers to
// data blocks.
//
// The file header data structure can be stored in memory or on disk.
// When it is on disk, it is stored in a single sector -- this means
// that we assume the size of this data structure to be the same
// as one disk sector. Without indirect addressing, this
// limits the maximum file length to just under 4K bytes.
//
class FileHeader {
	public static final int NumDirect = ((Disk.SectorSize - 2 * 4) / 4);

	public static final int MaxFileSize = (NumDirect * Disk.SectorSize);

	private int numBytes; // Number of bytes in the file

	private int numSectors; // Number of data sectors in the file

	private int dataSectors[]; // Disk sector numbers for each data

	// block in the file

	public FileHeader() {
		dataSectors = new int[NumDirect];
	}

	// the following methods deal with conversion between the on-disk and
	// the in-memory representation of a DirectoryEnry.
	// Note: these methods must be modified if any instance variables
	// are added!!

	// return size of flat (on disk) representation
	public static int sizeOf() {
		return 4 + 4 + 4 * NumDirect;
	}

	// initialize from a flat (on disk) representation
	public void internalize(byte[] buffer, int pos) {
		numBytes = Disk.intInt(buffer, pos);
		numSectors = Disk.intInt(buffer, pos + 4);
		for (int i = 0; i < NumDirect; i++)
			dataSectors[i] = Disk.intInt(buffer, pos + 8 + i * 4);
	}

	// externalize to a flat (on disk) representation
	public void externalize(byte[] buffer, int pos) {
		Disk.extInt(numBytes, buffer, pos);
		Disk.extInt(numSectors, buffer, pos + 4);
		for (int i = 0; i < NumDirect; i++)
			Disk.extInt(dataSectors[i], buffer, pos + 8 + i * 4);
	}

	// ----------------------------------------------------------------------
	// allocate
	// Initialize a fresh file header for a newly created file.
	// Allocate data blocks for the file out of the map of free disk blocks.
	// Return FALSE if there are not enough free blocks to accomodate
	// the new file.
	//
	// "freeMap" is the bit map of free disk sectors
	// "fileSize" is size of the new file
	// ----------------------------------------------------------------------

	public boolean allocate(BitMap freeMap, int fileSize) {
		numBytes = fileSize;
		numSectors = fileSize / Disk.SectorSize;
		if (fileSize % Disk.SectorSize != 0)
			numSectors++;

		if (freeMap.numClear() < numSectors || NumDirect < numSectors)
			return false; // not enough space

		for (int i = 0; i < numSectors; i++)
			dataSectors[i] = freeMap.find();
		return true;
	}

	// ----------------------------------------------------------------------
	// deallocate
	// De-allocate all the space allocated for data blocks for this file.
	//
	// "freeMap" is the bit map of free disk sectors
	// ----------------------------------------------------------------------

	public void deallocate(BitMap freeMap) {
		for (int i = 0; i < numSectors; i++) {
			Lib.assertTrue(freeMap.test(dataSectors[i])); // ought to be
			// marked!
			freeMap.clear(dataSectors[i]);
		}
	}

	// ----------------------------------------------------------------------
	// fetchFrom
	// Fetch contents of file header from disk.
	//
	// "sector" is the disk sector containing the file header
	// ----------------------------------------------------------------------

	public void fetchFrom(int sector) {
		byte buffer[] = new byte[Disk.SectorSize];
		// read sector
		Machine.synchDisk().readSector(sector, buffer, 0);
		// unmarshall
		internalize(buffer, 0);
	}

	// ----------------------------------------------------------------------
	// writeBack
	// Write the modified contents of the file header back to disk.
	//
	// "sector" is the disk sector to contain the file header
	// ----------------------------------------------------------------------

	public void writeBack(int sector) {
		byte buffer[] = new byte[Disk.SectorSize];
		// marshall
		externalize(buffer, 0);
		// write sector
		Machine.synchDisk().writeSector(sector, buffer, 0);
	}

	// ----------------------------------------------------------------------
	// byteToSector
	// Return which disk sector is storing a particular byte within the file.
	// This is essentially a translation from a virtual address (the
	// offset in the file) to a physical address (the sector where the
	// data at the offset is stored).
	//
	// "offset" is the location within the file of the byte in question
	// ----------------------------------------------------------------------

	public int byteToSector(int offset) {
		return (dataSectors[offset / Disk.SectorSize]);
	}

	// ----------------------------------------------------------------------
	// fileLength
	// Return the number of bytes in the file.
	// ----------------------------------------------------------------------

	public int fileLength() {
		return numBytes;
	}

	// ----------------------------------------------------------------------
	// print
	// Print the contents of the file header, and the contents of all
	// the data blocks pointed to by the file header.
	// ----------------------------------------------------------------------

	public void print() {
		int i, j, k;
		byte data[] = new byte[Disk.SectorSize];

		Lib.debug('+', "FileHeader contents.  File size: " + numBytes
				+ ". File blocks:\n");
		for (i = 0; i < numSectors; i++)
			Lib.debug('+', dataSectors[i] + " ");

		Lib.debug('+', "\nFile contents:\n");
		for (i = k = 0; i < numSectors; i++) {
			Machine.synchDisk().readSector(dataSectors[i], data, 0);
			for (j = 0; (j < Disk.SectorSize) && (k < numBytes); j++, k++) {
				if ('\040' <= data[j] && data[j] <= '\176') // isprint(data[j])
					Lib.debug('+', "" + (char) (data[j]));
				else
					Lib.debug('+', "\\" + new Integer(((int) data[j]) & 0xff));
			}
			Lib.debug('+', "\n");
		}
	}

}
