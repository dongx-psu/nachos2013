package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;

import java.io.EOFException;
import java.util.HashSet;
import java.util.Hashtable;

/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 * 
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 * 
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
	/**
	 * Allocate a new process.
	 */
	public UserProcess() {
		PID = processCount++;
		returnStatus = -1;
		
		processList.put(PID, this);
		childProcessList = new HashSet<Integer>();
		over = new Semaphore(0);
		
		fileManager = new FileDescriptorManager();
		fileManager.add(0, UserKernel.console.openForReading());
		fileManager.add(1, UserKernel.console.openForWriting());
	}

	/**
	 * Allocate and return a new process of the correct class. The class name is
	 * specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 * 
	 * @return a new process of the correct class.
	 */
	public static UserProcess newUserProcess() {
		return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
	}

	/**
	 * Execute the specified program with the specified arguments. Attempts to
	 * load the program, and then forks a thread to run it.
	 * 
	 * @param name
	 *            the name of the file containing the executable.
	 * @param args
	 *            the arguments to pass to the executable.
	 * @return <tt>true</tt> if the program was successfully executed.
	 */
	public boolean execute(String name, String[] args) {
		if (!load(name, args))
			return false;

		new UThread(this).setName(name).fork();

		return true;
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		Machine.processor().setPageTable(pageTable);
	}
	
	protected TranslationEntry getTranslationEntry(int pageNumber, boolean isWrite) {
		if (pageNumber < 0 || pageNumber >= numPages)
			return null;
		TranslationEntry ans = pageTable[pageNumber];
		if (ans == null) return null;
		if (ans.readOnly && isWrite) return null;
		ans.used = true;
		if (isWrite) ans.dirty = true;
		return ans;
	}

	/**
	 * Read a null-terminated string from this process's virtual memory. Read at
	 * most <tt>maxLength + 1</tt> bytes from the specified address, search for
	 * the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 * 
	 * @param vaddr
	 *            the starting virtual address of the null-terminated string.
	 * @param maxLength
	 *            the maximum number of characters in the string, not including
	 *            the null terminator.
	 * @return the string read, or <tt>null</tt> if no null terminator was
	 *         found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for (int length = 0; length < bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr
	 *            the first byte of virtual memory to read.
	 * @param data
	 *            the array where the data will be stored.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data) {
		return readVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr
	 *            the first byte of virtual memory to read.
	 * @param data
	 *            the array where the data will be stored.
	 * @param offset
	 *            the first byte to write in the array.
	 * @param length
	 *            the number of bytes to transfer from virtual memory to the
	 *            array.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();
		
		int headPageNumber = Processor.pageFromAddress(vaddr);
		int headOffset = Processor.offsetFromAddress(vaddr);
		int tailPageNumber = Processor.pageFromAddress(vaddr+length);
		
		TranslationEntry entry = getTranslationEntry(headPageNumber, false);
		if (entry == null) return 0;
		
		int amount = Math.min(length, pageSize - headOffset);
		System.arraycopy(memory, Processor.makeAddress(entry.ppn, headOffset), data, offset, amount);
		offset += amount;

		for (int i = headPageNumber + 1; i <= tailPageNumber; ++i) {
			entry = getTranslationEntry(i, false);
			if (entry == null) return amount;
			
			int len = Math.min(length - amount, pageSize);
			System.arraycopy(memory, Processor.makeAddress(entry.ppn, 0), data, offset, len);
			
			offset += len;
			amount += len;
		}
		
		return amount;
	}

	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory. Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr
	 *            the first byte of virtual memory to write.
	 * @param data
	 *            the array containing the data to transfer.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return writeVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr
	 *            the first byte of virtual memory to write.
	 * @param data
	 *            the array containing the data to transfer.
	 * @param offset
	 *            the first byte to transfer from the array.
	 * @param length
	 *            the number of bytes to transfer from the array to virtual
	 *            memory.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		int headPageNumber = Processor.pageFromAddress(vaddr);
		int headOffset = Processor.offsetFromAddress(vaddr);
		int tailPageNumber = Processor.pageFromAddress(vaddr+length);
		
		TranslationEntry entry = getTranslationEntry(headPageNumber, true);
		if (entry == null) return 0;

		int amount = Math.min(length, pageSize - headOffset);
		System.arraycopy(data, offset, memory, Processor.makeAddress(entry.ppn, headOffset), amount);
		offset += amount;
		
		for (int i = headPageNumber + 1; i <= tailPageNumber; ++i) {
			entry = getTranslationEntry(i, true);
			if (entry == null) return amount;
			
			int len = Math.min(length - amount, pageSize);
			System.arraycopy(data, offset, memory, Processor.makeAddress(entry.ppn, 0), len);
			
			offset += len;
			amount += len;
		}

		return amount;
	}

	/**
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 * 
	 * @param name
	 *            the name of the file containing the executable.
	 * @param args
	 *            the arguments to pass to the executable.
	 * @return <tt>true</tt> if the executable was successfully loaded.
	 */
	private boolean load(String name, String[] args) {
		Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

		OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
		if (executable == null) {
			Lib.debug(dbgProcess, "\topen failed");
			return false;
		}

		try {
			coff = new Coff(executable);
		} catch (EOFException e) {
			executable.close();
			Lib.debug(dbgProcess, "\tcoff load failed");
			return false;
		}

		// make sure the sections are contiguous and start at page 0
		numPages = 0;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if (section.getFirstVPN() != numPages) {
				coff.close();
				Lib.debug(dbgProcess, "\tfragmented executable");
				return false;
			}
			numPages += section.getLength();
		}

		// make sure the argv array will fit in one page
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i = 0; i < args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += 4 + argv[i].length + 1;
		}
		if (argsSize > pageSize) {
			coff.close();
			Lib.debug(dbgProcess, "\targuments too long");
			return false;
		}

		// program counter initially points at the program entry point
		initialPC = coff.getEntryPoint();

		// next comes the stack; stack pointer initially points to top of it
		numPages += stackPages;
		initialSP = numPages * pageSize;

		// and finally reserve 1 page for arguments
		numPages++;

		if (!loadSections())
			return false;

		// store arguments in last page
		int entryOffset = (numPages - 1) * pageSize;
		int stringOffset = entryOffset + args.length * 4;

		this.argc = args.length;
		this.argv = entryOffset;

		for (int i = 0; i < argv.length; i++) {
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			Lib
					.assertTrue(writeVirtualMemory(entryOffset,
							stringOffsetBytes) == 4);
			entryOffset += 4;
			Lib
					.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
			stringOffset += argv[i].length;
			Lib
					.assertTrue(writeVirtualMemory(stringOffset,
							new byte[] { 0 }) == 1);
			stringOffset += 1;
		}

		return true;
	}

	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be run
	 * (this is the last step in process initialization that can fail).
	 * 
	 * @return <tt>true</tt> if the sections were successfully loaded.
	 */
	protected boolean loadSections() {
		int[] physicalPages = UserKernel.allocatePages(numPages);
		
		if (physicalPages == null) {
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
			return false;
		}
		
		pageTable = new TranslationEntry[numPages];

		// load sections
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);

			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");

			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;
				int ppn = physicalPages[vpn];
				pageTable[vpn] = new TranslationEntry(vpn, ppn, true, section.isReadOnly(), false, false);
				section.loadPage(i, ppn);
			}
		}
		
		for (int i = numPages - stackPages - 1; i < numPages; ++i)
			pageTable[i] = new TranslationEntry(i, physicalPages[i], true, false, false, false);
			
		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		coff.close();
		
		for (int i = 0; i < numPages; ++i)
			UserKernel.releasePage(pageTable[i].ppn);
		pageTable = null;
	}

	/**
	 * Initialize the processor's registers in preparation for running the
	 * program loaded into this process. Set the PC register to point at the
	 * start function, set the stack pointer register to point at the top of the
	 * stack, set the A0 and A1 registers to argc and argv, respectively, and
	 * initialize all other registers to 0.
	 */
	public void initRegisters() {
		Processor processor = Machine.processor();

		// by default, everything's 0
		for (int i = 0; i < Processor.numUserRegisters; i++)
			processor.writeRegister(i, 0);

		// initialize PC and SP according
		processor.writeRegister(Processor.regPC, initialPC);
		processor.writeRegister(Processor.regSP, initialSP);

		// initialize the first two argument registers to argc and argv
		processor.writeRegister(Processor.regA0, argc);
		processor.writeRegister(Processor.regA1, argv);
	}

	/**
	 * Handle the halt() system call.
	 */
	private int handleHalt() {
		if (this != UserKernel.rootProcess) return 0;

		Kernel.kernel.terminate();

		Lib.assertNotReached("Machine.halt() did not halt machine!");
		return 0;
	}
	
	private int handleExit(int status) {
		this.returnStatus = status;
		
		for (int i = 2; i < maxFileDescriptorNum; ++i)
			fileManager.close(i);
		
		unloadSections();
		
		processList.remove(PID);
		deadProcessList.put(PID, this);
		
		over.V();
		
		if (processList.isEmpty())
			Kernel.kernel.terminate();
		
		UThread.finish();
		return 0;
	}
	
	private int handleExec(int name, int argc, int argv) {
		String filename = readVirtualMemoryString(name, maxFilenameLength);
		
		if (filename == null || !filename.endsWith(".coff")) {
			Lib.debug(dbgProcess, "Invalid file name for Exec()");
			return -1;
		}
		
		if (argc < 0) {
			Lib.debug(dbgProcess, "argc < 0");
			return -1;
		}
		
		String[] args = new String[argc];
		
		for (int i = 0; i < argc; ++i) {
			byte[] buffer = new byte[4];
			if (readVirtualMemory(argv + i*4, buffer) != 4)
				return -1;
			
			args[i] = readVirtualMemoryString(Lib.bytesToInt(buffer, 0), maxFilenameLength);
			
			if (args[i] == null)
				return -1;
		}
		
		UserProcess child = newUserProcess();
		childProcessList.add(child.PID);
		
		saveState();
		
		if (!child.execute(filename, args)) {
			Lib.debug(dbgProcess, "faild to execute child process");
			return -1;
		}
		
		return child.PID;
	}
	
	private int handleJoin(int processID, int status) {
		if (!childProcessList.contains(processID)) {
			Lib.debug(dbgProcess, "processID does not refer to a child process of current process");
			return -1;
		}
		
		childProcessList.remove(processID);
		
		UserProcess child = processList.get(processID);
		
		if (child == null) {
			Lib.debug(dbgProcess, "joining a dead process");
			child = deadProcessList.get(processID);
			if (child == null) {
				Lib.debug(dbgProcess, "error in join()");
				return -1;
			}
		}
		
		child.over.P();
		
		writeVirtualMemory(status, Lib.bytesFromInt(child.returnStatus));
		
		if (child.exitNormally) return 1;
		else return 0;
	}
	
	private int handleCreate(int name) {
		String filename = readVirtualMemoryString(name, maxFilenameLength);
		
		if (filename == null) {
			Lib.debug(dbgProcess, "Invalid file name pointer");
			return -1;
		}
		
		if (deletingList.contains(filename)) {
			Lib.debug(dbgProcess, "File is now being deleted");
			return -1;
		}
		
		OpenFile file = UserKernel.fileSystem.open(filename, true);
		
		if (file == null) {
			Lib.debug(dbgProcess, "Create file failed");
			return -1;
		}
		
		return fileManager.add(file);
	}
	
	private int handleOpen(int name) {
		String filename = readVirtualMemoryString(name, maxFilenameLength);
		
		if (filename == null) {
			Lib.debug(dbgProcess, "Invalid file name pointer");
			return -1;
		}
		
		if (deletingList.contains(filename)) {
			Lib.debug(dbgProcess, "File is now being deleted");
			return -1;
		}
		
		OpenFile file = UserKernel.fileSystem.open(filename, false);
		
		if (file == null) {
			Lib.debug(dbgProcess, "Cannot open file");
			return -1;
		}
		
		return fileManager.add(file);
	}
	
	private int handleRead(int fd, int buffer, int size) {
		OpenFile file = fileManager.get(fd);
		
		if (file == null) {
			Lib.debug(dbgProcess, "Invaild file descriptor");
			return -1;
		}
		
		if (!(buffer >= 0 && size >=0)) {
			Lib.debug(dbgProcess, "Buffer and Size should be greater than zero");
			return -1;
		}
		
		byte buf[] = new byte[size];
		int length = file.read(buf, 0, size);
		
		if (length == -1) {
			Lib.debug(dbgProcess, "Fail to read the file");
			return -1;
		}
		
		length = writeVirtualMemory(buffer, buf, 0, length);
		
		return length;
	}
	
	private int handleWrite(int fd, int buffer, int size) {
		OpenFile file = fileManager.get(fd);
		
		if (file == null) {
			Lib.debug(dbgProcess, "Invaild file descriptor");
			return -1;
		}
		
		if (!(buffer >= 0 && size >=0)) {
			Lib.debug(dbgProcess, "Buffer and Size should be greater than zero");
			return -1;
		}
		
		byte buf[] = new byte[size];
		int length = readVirtualMemory(buffer, buf, 0, size);

		length = file.write(buf, 0, length);

		return length;
	}
	
	private int handleClose(int fd) {
		return fileManager.close(fd);
	}
	
	private int handleUnlink(int name) {
		String fileName = readVirtualMemoryString(name, maxFilenameLength);

		if (fileName == null) {
			Lib.debug(dbgProcess, "Cannot read file name");
			return -1;
		}

		if (usingFiles.containsKey(fileName)) {
			deletingList.add(fileName);
		} else if (!UserKernel.fileSystem.remove(fileName))
			return -1;

		return 0;
	}

	private static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2,
			syscallJoin = 3, syscallCreate = 4, syscallOpen = 5,
			syscallRead = 6, syscallWrite = 7, syscallClose = 8,
			syscallUnlink = 9;

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 * 
	 * <table>
	 * <tr>
	 * <td>syscall#</td>
	 * <td>syscall prototype</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td><tt>void halt();</tt></td>
	 * </tr>
	 * <tr>
	 * <td>1</td>
	 * <td><tt>void exit(int status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td><tt>int  join(int pid, int *status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>4</td>
	 * <td><tt>int  creat(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>5</td>
	 * <td><tt>int  open(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>6</td>
	 * <td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>7</td>
	 * <td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>8</td>
	 * <td><tt>int  close(int fd);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>9</td>
	 * <td><tt>int  unlink(char *name);</tt></td>
	 * </tr>
	 * </table>
	 * 
	 * @param syscall
	 *            the syscall number.
	 * @param a0
	 *            the first syscall argument.
	 * @param a1
	 *            the second syscall argument.
	 * @param a2
	 *            the third syscall argument.
	 * @param a3
	 *            the fourth syscall argument.
	 * @return the value to be returned to the user.
	 */
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		switch (syscall) {
		case syscallHalt:
			return handleHalt();
		case syscallExit:
			return handleExit(a0);
		case syscallExec:
			return handleExec(a0, a1, a2);
		case syscallJoin:
			return handleJoin(a0, a1);
		case syscallCreate:
			return handleCreate(a0);
		case syscallOpen:
			return handleOpen(a0);
		case syscallRead:
			return handleRead(a0, a1, a2);
		case syscallWrite:
			return handleWrite(a0, a1, a2);
		case syscallClose:
			return handleClose(a0);
		case syscallUnlink:
			return handleUnlink(a0);
		default:
			Lib.debug(dbgProcess, "Unknown syscall " + syscall);
			exitNormally = false;
			handleExit(-1);
			Lib.assertNotReached("Unknown system call!");
		}
		return 0;
	}

	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 * 
	 * @param cause
	 *            the user exception that occurred.
	 */
	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
		case Processor.exceptionSyscall:
			int result = handleSyscall(processor.readRegister(Processor.regV0),
					processor.readRegister(Processor.regA0), processor
							.readRegister(Processor.regA1), processor
							.readRegister(Processor.regA2), processor
							.readRegister(Processor.regA3));
			processor.writeRegister(Processor.regV0, result);
			processor.advancePC();
			break;

		default:
			Lib.debug(dbgProcess, "Unexpected exception: "
					+ Processor.exceptionNames[cause]);
			exitNormally = false;
			handleExit(-1);
			Lib.assertNotReached("Unexpected exception");
		}
	}
	
	public class FileDescriptorManager {
		public int add(OpenFile file) {
			for (int i = 0; i < maxFileDescriptorNum; ++i)
				if (handler[i] == null)
					return add(i, file);
			return -1;
		}
		
		public int add(int i, OpenFile file) {
			if (i < 0 || i > maxFileDescriptorNum)
				return -1;
			
			if (handler[i] == null) {
				handler[i] = file;
				if (usingFiles.get(file.getName()) != null)
					usingFiles.put(file.getName(), usingFiles.get(file.getName()) + 1);
				else usingFiles.put(file.getName(), 1);
				return i;
			}
			
			return -1;
		}
		
		public OpenFile get(int i) {
			if (i < 0 || i > maxFileDescriptorNum)
				return null;
			
			return handler[i];
		}
		
		public int close(int i) {
			if (i < 0 || i > maxFileDescriptorNum)
				return -1;
			
			if (handler[i] == null) {
				Lib.debug(dbgProcess, "file descriptor " + i + "doesn't exist");
				
				return -1;
			}
			
			OpenFile file = handler[i];
			handler[i] = null;
			file.close();
			
			String name = file.getName();
			if (usingFiles.get(name) > 1)
				usingFiles.put(name, usingFiles.get(name) -1);
			else {
				usingFiles.remove(name);
				if (deletingList.contains(name)) {
					deletingList.remove(name);
					UserKernel.fileSystem.remove(name);
				}
			}
			
			return 0;
		}
		
		public OpenFile handler[] = new OpenFile[maxFileDescriptorNum];
	}

	/** The program being run by this process. */
	protected Coff coff;

	/** This process's page table. */
	protected TranslationEntry[] pageTable;
	/** The number of contiguous pages occupied by the program. */
	protected int numPages;

	/** The number of pages in the program's stack. */
	protected final int stackPages = Config.getInteger("Processor.numStackPages", 8);

	private int initialPC, initialSP;
	private int argc, argv;
	
	protected int PID;
	protected static int processCount = 0;
	
	protected int returnStatus;
	protected boolean exitNormally = true;
	
	protected Semaphore over;

	protected FileDescriptorManager fileManager;
	protected static final int maxFileDescriptorNum = 16;
	protected static final int maxFilenameLength = 256;
	protected static Hashtable<String, Integer> usingFiles = new Hashtable<String, Integer>();
	protected static HashSet<String> deletingList = new HashSet<String>();
	
	protected static final int pageSize = Processor.pageSize;
	protected static final char dbgProcess = 'a';
	
	protected HashSet<Integer> childProcessList;
	protected static Hashtable<Integer, UserProcess> processList = new Hashtable<Integer, UserProcess>();
	protected static Hashtable<Integer, UserProcess> deadProcessList = new Hashtable<Integer, UserProcess>();
};
