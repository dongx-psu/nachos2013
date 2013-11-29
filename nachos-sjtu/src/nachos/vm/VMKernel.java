package nachos.vm;

import java.util.Hashtable;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.TranslationEntry;
import nachos.userprog.UserKernel;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
	/**
	 * Allocate a new VM kernel.
	 */
	public VMKernel() {
		super();
	}

	/**
	 * Initialize this kernel.
	 */
	public void initialize(String[] args) {
		super.initialize(args);
		
		coreMap = new Page[Machine.processor().getNumPhysPages()];
		currentTLBManager = new TLBManager();
		memoryManager = new ClockPagingMemManager();
	}

	/**
	 * Test this kernel.
	 */
	public void selfTest() {
		super.selfTest();
	}

	/**
	 * Start running user programs.
	 */
	public void run() {
		super.run();
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() {
		Lib.debug(dbgVM, "Virtual Memory Terminating");		
		getSwapManager().close();
		Lib.debug(dbgVM, "Page faults count: " + VMProcess.numPageFaults);
		
		super.terminate();
	}

	public static TranslationEntry getPageEntry(PageInfo info) {
		Integer ppn = invertedPageTable.get(info);
		if (ppn == null) return null;
		Page res = coreMap[ppn];
		if (res == null || !res.entry.valid) return null;
		return res.entry;
	}
	
	public static SwapManager getSwapManager() {
		if (swapManager == null) swapManager = new SwapManager();
		return swapManager;
	}
	
	private static final char dbgVM = 'v';
	
	protected static Hashtable<PageInfo, Integer> invertedPageTable = new Hashtable<PageInfo, Integer>();
	protected static Page[] coreMap;
	protected static TLBManager currentTLBManager;
	protected static MemoryManager memoryManager;
	protected static SwapManager swapManager;
}
