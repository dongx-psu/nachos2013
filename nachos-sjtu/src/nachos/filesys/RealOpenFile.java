package nachos.filesys;

import nachos.machine.Disk;
import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.OpenFile;

/**
 * @author Kang Zhang
 * 
 *         This file is derived from the code by 1998 Rice University and
 *         1992-1993 The Regents of the University of California.
 */

// OpenFileReal.java
// Class to manage an open Nachos file. As in UNIX, a
// file must be open before we can read or write to it.
// Once we're all done, we can close it (in Nachos, by deleting
// the OpenFile data structure).
//
// Also as in UNIX, for convenience, we keep the file header in
// memory while the file is open.
//
// Suppors opening, closing, reading and writing to
// individual files. The operations supported are similar to
// the UNIX ones -- type 'man open' to the UNIX prompt.
//
// There are two implementations.
// This is the "real" implementation, that turns these
// operations into read and write disk sector requests.
// In this baseline implementation of the file system, we don't
// worry about concurrent accesses to the file system
// by different threads -- this is part of the assignment.
class RealOpenFile extends OpenFile {

	private FileHeader hdr; // Header for this file

	private int seekPosition; // Current position within the file

	// ----------------------------------------------------------------------
	// OpenFileReal
	// Open a Nachos file for reading and writing. Bring the file header
	// into memory while the file is open.
	//
	// "sector" -- the location on disk of the file header for this file
	// ----------------------------------------------------------------------

	public RealOpenFile(int sector) {
		hdr = new FileHeader();
		hdr.fetchFrom(sector);
		seekPosition = 0;
	}

	// ----------------------------------------------------------------------
	// OpenFileReal::seek
	// Change the current location within the open file -- the point at
	// which the next Read or Write will start from.
	//
	// "position" -- the location within the file for the next Read/Write
	// ----------------------------------------------------------------------

	public void seek(long position) {
		seekPosition = (int) position;
	}

	// ----------------------------------------------------------------------
	// OpenFileReal::read/write
	// Read/write a portion of a file, starting from seekPosition.
	// Return the number of bytes actually written or read, and as a
	// side effect, increment the current position within the file.
	//
	// Implemented using the more primitive ReadAt/WriteAt.
	//
	// "into" -- the buffer to contain the data to be read from disk
	// "from" -- the buffer containing the data to be written to disk
	// "numBytes" -- the number of bytes to transfer
	// ----------------------------------------------------------------------

	public int read(byte[] into, int index, int numBytes) {
		int result = readAt(into, index, numBytes, seekPosition);
		seekPosition += result;
		return result;
	}

	public int write(byte[] into, int index, int numBytes) {
		int result = writeAt(into, index, numBytes, seekPosition);
		seekPosition += result;
		return result;
	}

	// ----------------------------------------------------------------------
	// OpenFileReal::readAt/writeAt
	// Read/write a portion of a file, starting at "position".
	// Return the number of bytes actually written or read, but has
	// no side effects (except that Write modifies the file, of course).
	//
	// There is no guarantee the request starts or ends on an even disk sector
	// boundary; however the disk only knows how to read/write a whole disk
	// sector at a time. Thus:
	//
	// For readAt:
	// We read in all of the full or partial sectors that are part of the
	// request, but we only copy the part we are interested in.
	// For writeAt:
	// We must first read in any sectors that will be partially written,
	// so that we don't overwrite the unmodified portion. We then copy
	// in the data that will be modified, and write back all the full
	// or partial sectors that are part of the request.
	//
	// "into" -- the buffer to contain the data to be read from disk
	// "from" -- the buffer containing the data to be written to disk
	// "numBytes" -- the number of bytes to transfer
	// "position" -- the offset within the file of the first byte to be
	// read/written
	// ----------------------------------------------------------------------
	@Override
	public int read(int pos, byte[] buf, int offset, int length) {
		return readAt(buf, offset, length, pos);
	}

	public int readAt(byte[] into, int index, int numBytes, long position) {
		int fileLength = hdr.fileLength();
		int i, firstSector, lastSector, numSectors;
		byte buf[];

		if ((numBytes <= 0) || (position >= fileLength))
			return 0; // check request
		if ((position + numBytes) > fileLength)
			numBytes = fileLength - (int) position;
		Lib.debug('f', "Reading " + numBytes + " bytes at " + position
				+ ", from file of length " + fileLength);

		firstSector = (int) position / Disk.SectorSize;
		lastSector = ((int) position + numBytes - 1) / Disk.SectorSize;
		numSectors = 1 + lastSector - firstSector;

		// read in all the full and partial sectors that we need
		buf = new byte[numSectors * Disk.SectorSize];
		for (i = firstSector; i <= lastSector; i++)
			Machine.synchDisk().readSector(
					hdr.byteToSector(i * Disk.SectorSize), buf,
					(i - firstSector) * Disk.SectorSize);

		// copy the part we want
		System.arraycopy(buf, (int) position - (firstSector * Disk.SectorSize),
				into, index, numBytes);
		return numBytes;
	}

	@Override
	public int write(int pos, byte[] buf, int offset, int length) {
		return writeAt(buf, offset, length, pos);
	}

	public int writeAt(byte from[], int index, int numBytes, long position) {

		int fileLength = hdr.fileLength();
		int i, firstSector, lastSector, numSectors;
		boolean firstAligned, lastAligned;
		byte buf[];

		if ((numBytes <= 0) || (position >= fileLength))
			return 0; // check request
		if ((position + numBytes) > fileLength)
			numBytes = fileLength - (int) position;
		Lib.debug('f', "Writing " + numBytes + " bytes at " + position
				+ ", from file of length " + fileLength);

		firstSector = (int) position / Disk.SectorSize;
		lastSector = ((int) position + numBytes - 1) / Disk.SectorSize;
		numSectors = 1 + lastSector - firstSector;

		buf = new byte[numSectors * Disk.SectorSize];

		firstAligned = (position == (firstSector * Disk.SectorSize));
		lastAligned = ((position + numBytes) == ((lastSector + 1) * Disk.SectorSize));

		// read in first and last sector, if they are to be partially modified
		if (!firstAligned)
			readAt(buf, 0, Disk.SectorSize, firstSector * Disk.SectorSize);
		if (!lastAligned && ((firstSector != lastSector) || firstAligned))
			readAt(buf, (lastSector - firstSector) * Disk.SectorSize,
					Disk.SectorSize, lastSector * Disk.SectorSize);

		// copy in the bytes we want to change
		System.arraycopy(from, index, buf, (int) position
				- (firstSector * Disk.SectorSize), numBytes);

		// write modified sectors back
		for (i = firstSector; i <= lastSector; i++)
			Machine.synchDisk().writeSector(
					hdr.byteToSector(i * Disk.SectorSize), buf,
					(i - firstSector) * Disk.SectorSize);

		return numBytes;
	}

	// ----------------------------------------------------------------------
	// OpenFileReal::length
	// Return the number of bytes in the file.
	// ----------------------------------------------------------------------

	public int length() {
		return hdr.fileLength();
	}

}
