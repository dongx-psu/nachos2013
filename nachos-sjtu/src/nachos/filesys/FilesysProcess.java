package nachos.filesys;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.OpenFile;
import nachos.machine.Processor;
import nachos.userprog.UserKernel;
import nachos.userprog.UserProcess;
import nachos.vm.VMProcess;

/**
 * FilesysProcess is used to handle syscall and exception through some callback
 * methods.
 * 
 * @author starforever
 */
public class FilesysProcess extends VMProcess {
	public static final int maxPathLength = RealFileSystem.maxNameLength;
	
	protected static final int SYSCALL_MKDIR = 14;
	protected static final int SYSCALL_RMDIR = 15;
	protected static final int SYSCALL_CHDIR = 16;
	protected static final int SYSCALL_GETCWD = 17;
	protected static final int SYSCALL_READDIR = 18;
	protected static final int SYSCALL_STAT = 19;
	protected static final int SYSCALL_LINK = 20;
	protected static final int SYSCALL_SYMLINK = 21;
	
	public FilesysProcess() {
		super();
		fileManager = new FileDescriptorManager();
		fileManager.add(0, UserKernel.console.openForReading());
		fileManager.add(1, UserKernel.console.openForWriting());
	}

	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		switch (syscall) {
		case syscallCreate:
			return handleCreate(a0);
		case syscallOpen:
			return handleOpen(a0);
		case syscallClose:
			return handleClose(a0);
		case syscallUnlink:
			return handleUnlink(a0);
		case SYSCALL_MKDIR:
			return fileSys().createFolder(readVirtualMemoryString(a0, maxPathLength)) ? 0 : -1;
		case SYSCALL_RMDIR:
			return fileSys().removeFolder(readVirtualMemoryString(a0, maxPathLength)) ? 0 : -1;
		case SYSCALL_CHDIR:
			return fileSys().changeCurFolder(readVirtualMemoryString(a0, maxPathLength)) ? 0 : -1;
		case SYSCALL_GETCWD:
			return handleGetCWD(a0, a1);
		case SYSCALL_READDIR:
			return handleReadDir(a0, a1, a2, a3);
		case SYSCALL_STAT:
			return handleStat(a0, a1);
		case SYSCALL_LINK:
			return handleLink(a0, a1);
		case SYSCALL_SYMLINK:
			return handleSymLink(a0, a1);
		default:
			return super.handleSyscall(syscall, a0, a1, a2, a3);
		}
	}
	
	protected int handleCreate(int name) {
		String fileName = readVirtualMemoryString(name, maxFilenameLength);

		if (fileName == null) {
			Lib.debug(dbgProcess, "Invalid file name pointer");
			return -1;
		}

		OpenFile file = fileSys().open(fileName, true);

		if (file == null) {
			Lib.debug(dbgProcess, "Create file failed");
			return -1;
		}

		return fileManager.add(file);
	}

	protected int handleOpen(int name) {
		String fileName = readVirtualMemoryString(name, maxFilenameLength);

		if (fileName == null) {
			Lib.debug(dbgProcess, "Invalid file name");
			return -1;
		}

		OpenFile file = fileSys().open(fileName, false);

		if (file == null) {
			Lib.debug(dbgProcess, "Open file failed");
			return -1;
		}

		return fileManager.add(file);
	}

	protected int handleClose(int fileDescriptor) {
		return fileManager.close(fileDescriptor);
	}

	protected int handleUnlink(int name) {
		String fileName = readVirtualMemoryString(name, maxFilenameLength);

		if (fileName == null) {
			Lib.debug(dbgProcess, "Invalid file name");
			return -1;
		}

		return fileSys().remove(fileName) ? 0 : -1;
	}

	protected int handleSymLink(int src, int dst) {
		String srcPath = readVirtualMemoryString(src, maxPathLength);
		String dstPath = readVirtualMemoryString(dst, maxPathLength);
		return fileSys().createSymlink(srcPath, dstPath) ? 0 : -1;
	}

	protected int handleLink(int src, int dst) {
		String srcPath = readVirtualMemoryString(src, maxPathLength);
		String dstPath = readVirtualMemoryString(dst, maxPathLength);
		return fileSys().createLink(srcPath, dstPath) ? 0 : -1;
	}

	protected int handleStat(int filename, int ptr_stat) {
		String path = readVirtualMemoryString(filename, maxPathLength);
		FileStat stat = fileSys().getStat(path);
		if (stat == null)
			return -1;
		writeVirtualMemory(ptr_stat, stat.getBytes());
		return FileStat.STAT_SIZE;
	}

	protected int handleReadDir(int dirname, int buf, int size, int namesize) {
		String path = readVirtualMemoryString(dirname, maxPathLength);
		String[] list = fileSys().readDir(path);
		if (list == null)
			return -1;
		if (list.length > size)
			return -1;
		for (String st : list)
			if (st.length() >= namesize)
				return -1;
		byte[] data = new byte[size * namesize];
		for (int i = 0; i < list.length; ++i) {
			byte[] st = list[i].getBytes();
			System.arraycopy(st, 0, data, namesize * i, st.length);
			data[namesize * i + st.length] = 0;
		}
		writeVirtualMemory(buf, data);
		return list.length;
	}

	protected int handleGetCWD(int buf, int size) {
		String path = fileSys().getCurPath();
		if (path.length() >= size)
			return -1;
		byte[] data = new byte[path.length() + 1];
		System.arraycopy(path.getBytes(), 0, data, 0, path.length());
		data[data.length - 1] = 0;
		writeVirtualMemory(buf, data);
		return path.length();
	}

	protected static RealFileSystem fileSys() {
		return FilesysKernel.realFileSystem;
	}

	public void handleException(int cause) {
		if (cause == Processor.exceptionSyscall) {
			Processor processor = Machine.processor();
			int result = handleSyscall(processor.readRegister(Processor.regV0),
					processor.readRegister(Processor.regA0), processor
							.readRegister(Processor.regA1), processor
							.readRegister(Processor.regA2), processor
							.readRegister(Processor.regA3));
			processor.writeRegister(Processor.regV0, result);
			processor.advancePC();
		} else super.handleException(cause);
	}
	
	public class FileDescriptorManager extends UserProcess.FileDescriptorManager {
		public int add(int x, OpenFile file) {
			if (x < 0 || x >= maxFileDescriptorNum)
				return -1;

			if (handler[x] == null) {
				handler[x] = file;
				return x;
			}

			return -1;
		}
		
		public int close(int x) {
			if (handler[x] == null) {
				Lib.debug(dbgProcess, "file descriptor " + x + " doesn't exist");
				return -1;
			}

			OpenFile file = handler[x];
			handler[x] = null;
			file.close();
			return 0;
		}
	}
}
