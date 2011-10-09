package nachos.filesys;

import nachos.machine.Config;
import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.network.NetKernel;

/**
 * A kernel with file system support.
 * 
 */
public class FilesysKernel extends NetKernel {

	public static RealFileSystem realFileSystem;

	public FilesysKernel() {
		super();
	}

	@Override
	public void initialize(String[] args) {
		super.initialize(args);
		boolean format = Config.getBoolean("FilesysKernel.format");
		realFileSystem = new RealFileSystem(format);
		fileSystem = realFileSystem;
	}

	@Override
	public void selfTest() {
		super.selfTest();
		/*
		 * Following code just a example for initial your disk's content
		 */
		String[] files = Machine.stubFileList();

		Lib.debug(dbgFile, "New disk, start moving files");

		for (String f : files) {
			// copy helloworld into your filesystem
			if (f.equalsIgnoreCase("hl.coff")) {
				RealFileSystemUtil.copyFromStub(f, f);
			}
		}
	}

	private static final char dbgFile = 'f';
}
