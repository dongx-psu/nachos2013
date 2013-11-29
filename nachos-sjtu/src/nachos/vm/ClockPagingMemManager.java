package nachos.vm;

import java.util.HashSet;
import java.util.LinkedList;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.Processor;
import nachos.machine.TranslationEntry;
import nachos.vm.SwapManager.SwapPage;

public class ClockPagingMemManager extends MemoryManager {
	public ClockPagingMemManager() {
		numPhysPages = Machine.processor().getNumPhysPages();
		for (int i = 0; i < numPhysPages; i++)
			freePages.add(i);
	}
	
	@Override
	protected void removePage(int ppn) {
		usedQueue.remove(new Integer(ppn));
		freePages.add(ppn);
		
	}

	@Override
	protected int nextPage() {
		if (!freePages.isEmpty())
			return freePages.removeFirst();
		
		for (pagePtr %= numPhysPages; ;pagePtr = (pagePtr+1) % numPhysPages) {
			if (usedQueue.contains(pagePtr))
				if (VMKernel.coreMap[pagePtr].entry.used)
					VMKernel.coreMap[pagePtr].entry.used = false;
				else {
					usedQueue.remove(new Integer(pagePtr));
					return pagePtr++;
				}
		}
	}

	@Override
	protected int seekInTLB(int vpn) {
		for (int i = 0; i < Machine.processor().getTLBSize(); i++) {
			TranslationEntry entry = Machine.processor().readTLBEntry(i);
			if (entry.valid && entry.vpn == vpn)
				return i;
		}
		return -1;
	}

	@Override
	public TranslationEntry swapIn(PageInfo info, LazyLoader lazyLoader) {
		VMKernel.currentTLBManager.flush();
		int ppn = nextPage();
		
		swapOut(ppn);
		
		TranslationEntry entry = lazyLoader.load(info, ppn);
		
		usedQueue.add(ppn);
		VMKernel.invertedPageTable.put(info, ppn);
		VMKernel.coreMap[ppn] = new Page(info, entry);
		return entry;
	}

	@Override
	public void swapOut(int ppn) {
		Page page = VMKernel.coreMap[ppn];
		if (page != null && page.entry.valid) {
			page.entry.valid = false;
			VMKernel.invertedPageTable.remove(page.info);
			int index = seekInTLB(page.entry.vpn);
			if (index != -1) VMKernel.currentTLBManager.invalid(index);
			if (page.entry.dirty) {
				SwapPage swapPage = VMKernel.getSwapManager().newSwapPage(page);
				Lib.assertTrue(VMKernel.getSwapManager().write(swapPage.frameNo, Machine.processor().getMemory(),
						Processor.makeAddress(ppn, 0)), "error in writing swap file");
			}
		}
	}

	private int numPhysPages;
	private int pagePtr;
	private HashSet<Integer> usedQueue = new HashSet<Integer>();
	private LinkedList<Integer> freePages = new LinkedList<Integer>();
}
