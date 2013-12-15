package nachos.vm;

import nachos.machine.Machine;
import nachos.machine.Processor;
import nachos.machine.TranslationEntry;
import nachos.threads.Lock;
import nachos.userprog.UserProcess;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
	/**
	 * Allocate a new process.
	 */
	public VMProcess() {
		super();
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
		VMKernel.currentTLBManager.clear();
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		//super.restoreState();
	}

	/**
	 * Initializes page tables for this process so that the executable can be
	 * demand-paged.
	 * 
	 * @return <tt>true</tt> if successful.
	 */
	protected boolean loadSections() {
		lazyLoader = new LazyLoader(coff);
		
		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		coff.close();
		
		VMKernel.currentTLBManager.clear();
		
		for (int i = 0; i < numPages; ++i) {
			PageInfo info = new PageInfo(PID, i);
			Integer ppn = VMKernel.invertedPageTable.remove(info);
			if (ppn != null) {
				VMKernel.memoryManager.removePage(ppn);
				VMKernel.coreMap[ppn].entry.valid = false;
			}
			
			VMKernel.getSwapManager().deleteSwapPage(info);
		}
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
		case Processor.exceptionTLBMiss:
			handleTLBMissException(Processor.pageFromAddress(processor.readRegister(Processor.regBadVAddr)));
			break;
		default:
			super.handleException(cause);
		}
	}
	
	private void handleTLBMissException(int vpn) {
		TranslationEntry entry = VMKernel.getPageEntry(new PageInfo(PID, vpn));
		
		if (entry == null) {
			entry = handlePageFault(vpn);
			if (entry == null) handleExit(-1);
		}
		
		VMKernel.currentTLBManager.addEntry(entry);
	}
	
	private TranslationEntry handlePageFault(int vpn) {
		lock.acquire();
		numPageFaults++;
		TranslationEntry res = VMKernel.memoryManager.swapIn(new PageInfo(PID, vpn), lazyLoader);
		lock.release();
		return res;
	}
	
	protected TranslationEntry getTranslationEntry(int vpn, boolean isWrite) {
		TranslationEntry res = VMKernel.currentTLBManager.find(vpn, isWrite);
		if (res == null) {
			handleTLBMissException(vpn);
			res = VMKernel.currentTLBManager.find(vpn, isWrite);
		}
		
		return res;
	}

	private static final int pageSize = Processor.pageSize;
	protected static final char dbgProcess = 'a';
	private static final char dbgVM = 'v';
	
	public static int numPageFaults = 0;
	
	private LazyLoader lazyLoader;
	private static Lock lock = new Lock();
}
