package nachos.filesys;

import nachos.machine.Disk;

/**
 * @author Kang Zhang
 * 
 *         This file is derived from the code by 1998 Rice University and
 *         1992-1993 The Regents of the University of California.
 */

// DirectoryEntry.java
// The following class defines a "directory entry", representing a file
// in the directory. Each entry gives the name of the file, and where
// the file's header is to be found on disk.
//
// Internal data structures kept public so that Directory operations can
// access them directly.
class DirectoryEntry {
	// for simplicity, we assume
	// file names are <= 9 characters long
	public static final int FileNameMaxLen = 9;

	public boolean inUse; // Is this directory entry in use?

	public int sector; // Location on disk to find the

	// FileHeader for this file
	public int nameLen; // lenght of filename

	// public char name[FileNameMaxLen]; // Text name for file (on disk)
	public String name; // Text name for file

	public DirectoryEntry() {
		name = "";
	}

	// the following methods deal with conversion between the on-disk and
	// the in-memory representation of a DirectoryEnry.
	// Note: these methods must be modified if any instance variables
	// are added!!

	// return size of flat (on disk) representation
	public static int sizeOf() {
		return 1 + 4 + 4 + FileNameMaxLen;
	}

	// initialize from a flat (on disk) representation
	@SuppressWarnings("deprecation")
	public void internalize(byte[] buffer, int pos) {
		if (buffer[pos] != 0) {
			inUse = true;
			sector = Disk.intInt(buffer, pos + 1);
			nameLen = Disk.intInt(buffer, pos + 5);
			name = new String(buffer, 0, pos + 9, nameLen);
			// System.out.println("internalize, name=" + name + "nameLen=" +
			// nameLen);
		} else
			inUse = false;
	}

	// externalize to a flat (on disk) representation
	@SuppressWarnings("deprecation")
	public void externalize(byte[] buffer, int pos) {
		if (inUse) {
			buffer[pos] = 1;
			Disk.extInt(sector, buffer, pos + 1);
			Disk.extInt(nameLen, buffer, pos + 5);
			name.getBytes(0, nameLen, buffer, pos + 9);
			// System.out.println("externalize, name=" + name + "nameLen=" +
			// nameLen);
		} else
			buffer[pos] = 0;
	}

}
