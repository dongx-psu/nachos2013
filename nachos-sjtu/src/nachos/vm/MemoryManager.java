package nachos.vm;

import nachos.machine.TranslationEntry;

public abstract class MemoryManager {
	public MemoryManager() {
	}
	
	protected abstract void removePage(int ppn);
	
	protected abstract int nextPage();
	
	protected abstract int seekInTLB(int vpn);
	
	public abstract TranslationEntry swapIn(PageInfo content, LazyLoader lazyLoader);
	
	public abstract void swapOut(int ppn);
}
