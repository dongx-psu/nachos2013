package nachos.vm;

import nachos.machine.Coff;
import nachos.machine.CoffSection;
import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.Processor;
import nachos.machine.TranslationEntry;
import nachos.vm.SwapManager.SwapPage;

public class LazyLoader {
	public LazyLoader(Coff coff) {
		int totPages = 0;
		for (int i = 0; i < coff.getNumSections(); ++i) {
			CoffSection section = coff.getSection(i);
			totPages += section.getLength();
		}

		this.coff = coff;
		pages = new CodePage[totPages];

		for (int i = 0; i < coff.getNumSections(); ++i) {
			CoffSection section = coff.getSection(i);
			for (int j = 0; j < section.getLength(); j++) {
				int vpn = section.getFirstVPN() + j;
				pages[vpn] = new CodePage(i, j);
			}
		}
	}

	public boolean isCodePage(int vpn) {
		return (vpn >= 0 && vpn < pages.length);
	}

	public TranslationEntry loadCodePage(int vpn, int ppn) {
		CoffSection section = coff.getSection(pages[vpn].section);
		TranslationEntry entry = new TranslationEntry(vpn, ppn, true, section.isReadOnly(), false, false);
		section.loadPage(pages[vpn].offset, ppn);
		
		return entry;
	}

	public TranslationEntry loadStackPage(int vpn, int ppn) {
		fillMemory(ppn);
		
		return new TranslationEntry(vpn, ppn, true, false, false, false);
	}

	public TranslationEntry load(PageInfo info, int ppn) {
		TranslationEntry entry;
		SwapPage swapPage = VMKernel.getSwapManager().getSwapPage(info);
		if (swapPage != null) {
			entry = swapPage.entry;
			entry.ppn = ppn;
			entry.valid = true;
			entry.used = false;
			entry.dirty = false;
			Lib.assertTrue(VMKernel.getSwapManager().read(swapPage.frameNo, Machine.processor().getMemory(), Processor.makeAddress(ppn, 0)),
						   "swap file read error");
		}
		else {
			if (isCodePage(info.vpn))
				entry = loadCodePage(info.vpn, ppn);
			else entry = loadStackPage(info.vpn, ppn);
		}
		return entry;
	}

	private void fillMemory(int ppn) {
		byte[] data = Machine.processor().getMemory();
		
		int start = Processor.makeAddress(ppn, 0);
		for (int i = start; i < start + Processor.pageSize; i++)
			data[i] = 0;
	}

	class CodePage {
		public CodePage(int section, int offset) {
			this.section = section;
			this.offset = offset;
		}

		public int section;
		public int offset;
	}

	private Coff coff;
	private CodePage[] pages;
}
