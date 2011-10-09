package nachos.filesys;

import nachos.machine.Disk;
import nachos.machine.FileSystem;
import nachos.machine.Lib;
import nachos.machine.OpenFile;

/**
 * @author Kang Zhang
 * 
 *         This file is derived from the code by 1998 Rice University and
 *         1992-1993 The Regents of the University of California.
 */

// FileSystemReal.java
// Class to manage the overall operation of the file system.
// Implements methods to map from textual file names to files.
//
// Each file in the file system has:
// A file header, stored in a sector on disk
// (the size of the file header data structure is arranged
// to be precisely the size of 1 disk sector)
// A number of data blocks
// An entry in the file system directory
//
// The file system consists of several data structures:
// A bitmap of free disk sectors (cf. bitmap.h)
// A directory of file names and file headers
//
// Both the bitmap and the directory are represented as normal
// files. Their file headers are located in specific sectors
// (sector 0 and sector 1), so that the file system can find them
// on bootup.
//
// The file system assumes that the bitmap and directory files are
// kept "open" continuously while Nachos is running.
//
// For those operations (such as Create, Remove) that modify the
// directory and/or bitmap, if the operation succeeds, the changes
// are written immediately back to disk (the two files are kept
// open during all this time). If the operation fails, and we have
// modified part of the directory and/or bitmap, we simply discard
// the changed version, without writing it back to disk.
//
// Our implementation at this point has the following restrictions:
//
// there is no synchronization for concurrent accesses
// files have a fixed size, set when the file is created
// files cannot be bigger than about 3KB in size
// there is no hierarchical directory structure, and only a limited
// number of files can be added to the system
// there is no attempt to make the system robust to failures
// (if Nachos exits in the middle of an operation that modifies
// the file system, it may corrupt the disk)
//
// A file system is a set of files stored on disk, organized
// into directories. Operations on the file system have to
// do with "naming" -- creating, opening, and deleting files,
// given a textual file name. Operations on an individual
// "open" file (read, write, close) are to be found in the OpenFile
// class (openfile.h).
//
// We define two separate implementations of the file system.
// This version is a "real" file system, built on top of
// a disk simulator. The disk is simulated using the native UNIX
// file system (in a file named "DISK").
//
// In the "real" implementation, there are two key data structures used
// in the file system. There is a single "root" directory, listing
// all of the files in the file system; unlike UNIX, the baseline
// system does not provide a hierarchical directory structure.
// In addition, there is a bitmap for allocating
// disk sectors. Both the root directory and the bitmap are themselves
// stored as files in the Nachos file system -- this causes an interesting
// bootstrap problem when the simulated disk is initialized.

public class RealFileSystem implements FileSystem {

	// Sectors containing the file headers for the bitmap of free sectors,
	// and the directory of files. These file headers are placed in
	// well-known sectors, so that they can be located on boot-up.
	public static final int FreeMapSector = 0;

	public static final int DirectorySector = 1;

	// Initial file sizes for the bitmap and directory; until the file system
	// supports extensible files, the directory size sets the maximum number
	// of files that can be loaded onto the disk.
	public static final int FreeMapFileSize = (Disk.NumSectors / BitMap.BitsInByte);

	public static final int NumDirEntries = 10;

	public static final int DirectoryFileSize = (DirectoryEntry.sizeOf() * NumDirEntries);

	private OpenFile freeMapFile; // Bit map of free disk blocks,

	// represented as a file
	private OpenFile directoryFile; // "Root" directory -- list of

	// file names, represented as a file

	// ----------------------------------------------------------------------
	// FileSystemReal
	// Initialize the file system. If format = true, the disk has
	// nothing on it, and we need to initialize the disk to contain
	// an empty directory, and a bitmap of free sectors (with almost but
	// not all of the sectors marked as free).
	//
	// If format = false, we just have to open the files
	// representing the bitmap and the directory.
	//
	// "format" -- should we initialize the disk?
	// ----------------------------------------------------------------------

	public RealFileSystem(boolean format) {
		Lib.debug('f', "Initializing the file system.\n");
		if (format) {
			BitMap freeMap = new BitMap(Disk.NumSectors);
			Directory directory = new Directory(NumDirEntries);
			FileHeader mapHdr = new FileHeader();
			FileHeader dirHdr = new FileHeader();

			Lib.debug('f', "Formatting the file system.\n");

			// First, allocate space for FileHeaders for the directory and
			// bitmap
			// (make sure no one else grabs these!)
			freeMap.mark(FreeMapSector);
			freeMap.mark(DirectorySector);

			// Second, allocate space for the data blocks containing the
			// contents
			// of the directory and bitmap files. There better be enough space!

			Lib.assertTrue(mapHdr.allocate(freeMap, FreeMapFileSize));
			Lib.assertTrue(dirHdr.allocate(freeMap, DirectoryFileSize));

			// Flush the bitmap and directory FileHeaders back to disk
			// We need to do this before we can "Open" the file, since open
			// reads the file header off of disk (and currently the disk has
			// garbage on it!).

			Lib.debug('f', "Writing headers back to disk.\n");
			mapHdr.writeBack(FreeMapSector);
			dirHdr.writeBack(DirectorySector);

			// OK to open the bitmap and directory files now
			// The file system operations assume these two files are left open
			// while Nachos is running.

			freeMapFile = new RealOpenFile(FreeMapSector);
			directoryFile = new RealOpenFile(DirectorySector);

			// Once we have the files "open", we can write the initial version
			// of each file back to disk. The directory at this point is
			// completely
			// empty; but the bitmap has been changed to reflect the fact that
			// sectors on the disk have been allocated for the file headers and
			// to hold the file data for the directory and bitmap.

			Lib.debug('f', "Writing bitmap and directory back to disk.\n");
			freeMap.writeBack(freeMapFile); // flush changes to disk
			directory.writeBack(directoryFile);

			if (Lib.test('f')) {
				freeMap.print();
				directory.print();
			}

		} else {
			// if we are not formatting the disk, just open the files
			// representing
			// the bitmap and directory; these are left open while Nachos is
			// running
			freeMapFile = new RealOpenFile(FreeMapSector);
			directoryFile = new RealOpenFile(DirectorySector);
		}
	}

	// ----------------------------------------------------------------------
	// FileSystem::create
	// Create a file in the Nachos file system (similar to UNIX create).
	// Since we can't increase the size of files dynamically, we have
	// to give Create the initial size of the file.
	//
	// The steps to create a file are:
	// Make sure the file doesn't already exist
	// Allocate a sector for the file header
	// Allocate space on disk for the data blocks for the file
	// Add the name to the directory
	// Store the new file header on disk
	// Flush the changes to the bitmap and the directory back to disk
	//
	// Return true if everything goes ok, otherwise, return false.
	//
	// Create fails if:
	// file is already in directory
	// no free space for file header
	// no free entry for file in directory
	// no free space for data blocks for the file
	//
	// Note that this implementation assumes there is no concurrent access
	// to the file system!
	//
	// "name" -- name of file to be created
	// "initialSize" -- size of file to be createdn
	// ----------------------------------------------------------------------

	public boolean create(String name, long initialSize) {
		Directory directory;
		BitMap freeMap;
		FileHeader hdr;
		int sector;
		boolean success;

		Lib.debug('f', "Creating file " + name + ", size " + initialSize);

		directory = new Directory(NumDirEntries);
		directory.fetchFrom(directoryFile);

		if (directory.find(name) != -1)
			success = false; // file is already in directory
		else {
			freeMap = new BitMap(Disk.NumSectors);
			freeMap.fetchFrom(freeMapFile);
			sector = freeMap.find(); // find a sector to hold the file header
			if (sector == -1)
				success = false; // no free block for file header
			else if (!directory.add(name, sector))
				success = false; // no space in directory
			else {
				hdr = new FileHeader();
				if (!hdr.allocate(freeMap, (int) initialSize))
					success = false; // no space on disk for data
				else {
					success = true;
					// everthing worked, flush all changes back to disk
					hdr.writeBack(sector);
					directory.writeBack(directoryFile);
					freeMap.writeBack(freeMapFile);
				}
			}
		}
		return success;
	}

	// ----------------------------------------------------------------------
	// FileSystem::open
	// Open a file for reading and writing.
	// To open a file:
	// Find the location of the file's header, using the directory
	// Bring the header into memory
	//
	// "name" -- the text name of the file to be opened
	// ----------------------------------------------------------------------

	public OpenFile open(String name) {
		Directory directory = new Directory(NumDirEntries);
		OpenFile openFile = null;
		int sector;

		Lib.debug('f', "Opening file " + name);
		directory.fetchFrom(directoryFile);
		sector = directory.find(name);
		if (sector >= 0)
			openFile = new RealOpenFile(sector);// name was found in directory
		return openFile; // return null if not found
	}

	// ----------------------------------------------------------------------
	// FileSystem::remove
	// Delete a file from the file system. This requires:
	// Remove it from the directory
	// Delete the space for its header
	// Delete the space for its data blocks
	// Write changes to directory, bitmap back to disk
	//
	// Return true if the file was deleted, false if the file wasn't
	// in the file system.
	//
	// "name" -- the text name of the file to be removed
	// ----------------------------------------------------------------------

	public boolean remove(String name) {
		Directory directory;
		BitMap freeMap;
		FileHeader fileHdr;
		int sector;

		directory = new Directory(NumDirEntries);
		directory.fetchFrom(directoryFile);
		sector = directory.find(name);
		if (sector == -1) {
			return false; // file not found
		}
		fileHdr = new FileHeader();
		fileHdr.fetchFrom(sector);

		freeMap = new BitMap(Disk.NumSectors);
		freeMap.fetchFrom(freeMapFile);

		fileHdr.deallocate(freeMap); // remove data blocks
		freeMap.clear(sector); // remove header block
		directory.remove(name);

		freeMap.writeBack(freeMapFile); // flush to disk
		directory.writeBack(directoryFile); // flush to disk
		return true;
	}

	// ----------------------------------------------------------------------
	// FileSystem::list
	// List all the files in the file system directory.
	// ----------------------------------------------------------------------

	public void list() {
		Directory directory = new Directory(NumDirEntries);

		directory.fetchFrom(directoryFile);
		directory.list();
	}

	// ----------------------------------------------------------------------
	// FileSystem::print
	// Print everything about the file system:
	// the contents of the bitmap
	// the contents of the directory
	// for each file in the directory,
	// the contents of the file header
	// the data in the file
	// ----------------------------------------------------------------------

	public void print() {
		FileHeader bitHdr = new FileHeader();
		FileHeader dirHdr = new FileHeader();
		BitMap freeMap = new BitMap(Disk.NumSectors);
		Directory directory = new Directory(NumDirEntries);

		Lib.debug('+', "Bit map file header:\n");
		bitHdr.fetchFrom(FreeMapSector);
		bitHdr.print();

		Lib.debug('+', "Directory file header:\n");
		dirHdr.fetchFrom(DirectorySector);
		dirHdr.print();

		freeMap.fetchFrom(freeMapFile);
		freeMap.print();

		directory.fetchFrom(directoryFile);
		directory.print();

	}

	public OpenFile open(String name, boolean create) {
		// finish this function.
		return open(name);
	}

}
