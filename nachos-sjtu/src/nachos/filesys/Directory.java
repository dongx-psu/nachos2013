package nachos.filesys;

import nachos.machine.Lib;
import nachos.machine.OpenFile;

/**
 * @author Kang Zhang
 * 
 *         This file is derived from the code by 1998 Rice University and
 *         1992-1993 The Regents of the University of California.
 */

// Directory.java
// Class to manage a directory of file names.
//
// The directory is a table of fixed length entries; each
// entry represents a single file, and contains the file name,
// and the location of the file header on disk. The fixed size
// of each directory entry means that we have the restriction
// of a fixed maximum size for file names.
//
// The constructor initializes an empty directory of a certain size;
// we use ReadFrom/WriteBack to fetch the contents of the directory
// from disk, and to write back any modifications back to disk.
//
// Also, this implementation has the restriction that the size
// of the directory cannot expand. In other words, once all the
// entries in the directory are used, no more files can be created.
// Fixing this is one of the parts to the assignment.
//
// A directory is a table of pairs: <file name, sector #>,
// giving the name of each file in the directory, and
// where to find its file header (the data structure describing
// where to find the file's data blocks) on disk.
//
// We assume mutual exclusion is provided by the caller.
//
// The following class defines a UNIX-like "directory". Each entry in
// the directory describes a file, and where to find it on disk.
//
// The directory data structure can be stored in memory, or on disk.
// When it is on disk, it is stored as a regular Nachos file.
//
// The constructor initializes a directory structure in memory; the
// FetchFrom/WriteBack operations shuffle the directory information
// from/to disk.
class Directory {

	private int tableSize; // Number of directory entries

	private DirectoryEntry table[]; // Table of pairs:

	// <file name, file header location>

	// ----------------------------------------------------------------------
	// Directory
	// Initialize a directory; initially, the directory is completely
	// empty. If the disk is being formatted, an empty directory
	// is all we need, but otherwise, we need to call FetchFrom in order
	// to initialize it from disk.
	//
	// "size" is the number of entries in the directory
	// ----------------------------------------------------------------------

	public Directory(int size) {
		table = new DirectoryEntry[size];
		tableSize = size;
		for (int i = 0; i < tableSize; i++) {
			table[i] = new DirectoryEntry();
			table[i].inUse = false;
		}
	}

	// ----------------------------------------------------------------------
	// fetchFrom
	// Read the contents of the directory from disk.
	//
	// "file" -- file containing the directory contents
	// ----------------------------------------------------------------------

	public void fetchFrom(OpenFile file) {
		byte buffer[] = new byte[tableSize * DirectoryEntry.sizeOf()];
		// read the file
		file.read(0, buffer, 0, tableSize * DirectoryEntry.sizeOf());
		// unmarshall
		int pos = 0;
		for (int i = 0; i < tableSize; i++) {
			table[i].internalize(buffer, pos);
			pos += DirectoryEntry.sizeOf();
		}
	}

	// ----------------------------------------------------------------------
	// writeBack
	// Write any modifications to the directory back to disk
	//
	// "file" -- file to contain the new directory contents
	// ----------------------------------------------------------------------

	public void writeBack(OpenFile file) {
		byte buffer[] = new byte[tableSize * DirectoryEntry.sizeOf()];
		// marshall
		int pos = 0;
		for (int i = 0; i < tableSize; i++) {
			table[i].externalize(buffer, pos);
			pos += DirectoryEntry.sizeOf();
		}

		// write the file
		file.write(0, buffer, 0, tableSize * DirectoryEntry.sizeOf());
	}

	// ----------------------------------------------------------------------
	// findIndex
	// Look up file name in directory, and return its location in the table of
	// directory entries. Return -1 if the name isn't in the directory.
	//
	// "name" -- the file name to look up
	// ----------------------------------------------------------------------

	private int findIndex(String name) {
		for (int i = 0; i < tableSize; i++) {
			// if (table[i].inUse) System.out.println("name=" + name +
			// "table[i].name=" + table[i].name);
			if (table[i].inUse
					&& (name.equals(table[i].name) || (name
							.startsWith(table[i].name) && name.length() > DirectoryEntry.FileNameMaxLen)))
				return i;
		}
		return -1; // name not in directory
	}

	// ----------------------------------------------------------------------
	// find
	// Look up file name in directory, and return the disk sector number
	// where the file's header is stored. Return -1 if the name isn't
	// in the directory.
	//
	// "name" -- the file name to look up
	// ----------------------------------------------------------------------

	public int find(String name) {
		int i = findIndex(name);

		if (i != -1)
			return table[i].sector;
		return -1;
	}

	// ----------------------------------------------------------------------
	// add
	// Add a file into the directory. Return TRUE if successful;
	// return FALSE if the file name is already in the directory, or if
	// the directory is completely full, and has no more space for
	// additional file names.
	//
	// "name" -- the name of the file being added
	// "newSector" -- the disk sector containing the added file's header
	// ----------------------------------------------------------------------

	public boolean add(String name, int newSector) {
		if (findIndex(name) != -1)
			return false;

		for (int i = 0; i < tableSize; i++)
			if (!table[i].inUse) {
				table[i].inUse = true;
				table[i].name = name;
				table[i].nameLen = Math.min(name.length(),
						DirectoryEntry.FileNameMaxLen);
				table[i].sector = newSector;
				return true;
			}
		return false; // no space. Fix when we have extensible files.
	}

	// ----------------------------------------------------------------------
	// remove
	// Remove a file name from the directory. Return TRUE if successful;
	// return FALSE if the file isn't in the directory.
	//
	// "name" -- the file name to be removed
	// ----------------------------------------------------------------------

	public boolean remove(String name) {
		int i = findIndex(name);

		if (i == -1)
			return false; // name not in directory
		table[i].inUse = false;
		return true;
	}

	// ----------------------------------------------------------------------
	// list
	// List all the file names in the directory.
	// ----------------------------------------------------------------------

	public void list() {
		for (int i = 0; i < tableSize; i++)
			if (table[i].inUse)
				System.out.println(table[i].name);
	}

	// ----------------------------------------------------------------------
	// print
	// List all the file names in the directory, their FileHeader locations,
	// and the contents of each file. For debugging.
	// ----------------------------------------------------------------------

	public void print() {
		FileHeader hdr = new FileHeader();

		System.out.println("Directory contents:");
		for (int i = 0; i < tableSize; i++)
			if (table[i].inUse) {
				Lib.debug('+', "Name: " + table[i].name + ", Sector: "
						+ table[i].sector);
				hdr.fetchFrom(table[i].sector);
				hdr.print();
			}
		System.out.println("");
	}

}
