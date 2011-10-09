package nachos.filesys;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.OpenFile;
import nachos.machine.OpenFileWithPosition;

/**
 * @author Kang Zhang
 * 
 */
public class RealFileSystemUtil {
	// make it small, just to be difficult
	private static final int TransferSize = 10 * 1000;

	public static void copyFromStub(String from, String to) {
		OpenFile fp;
		OpenFileWithPosition fs;
		OpenFile openFile;
		int amountRead;
		long fileLength;
		byte buffer[];

		fp = Machine.stubFileSystem().open(from, false);
		if (fp == null) {
			Lib.debug('f', "Copy: couldn't open input file " + from);
			return;
		}

		fileLength = fp.length();

		Lib.debug('f', "Copying file " + from + ", size " + fileLength
				+ ", to file " + to);
		if (!FilesysKernel.realFileSystem.create(to, (int) fileLength)) {
			Lib.debug('f', "Copy: couldn't create output file " + to);
			return;
		}

		openFile = FilesysKernel.realFileSystem.open(to);
		Lib.assertTrue(openFile != null);

		// Copy the data in TransferSize chunks
		buffer = new byte[TransferSize];
		fs = (OpenFileWithPosition) fp;
		while ((amountRead = fs.read(buffer, 0, TransferSize)) > 0) {
			openFile.write(buffer, 0, amountRead);
		}

		fs.close();
		Lib.debug('f', "File: " + from + " imported");
	}
}
