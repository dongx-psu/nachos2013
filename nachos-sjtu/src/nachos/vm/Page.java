package nachos.vm;

import nachos.machine.TranslationEntry;

public class Page {
	public Page(PageInfo info, TranslationEntry entry) {
		this.info = info;
		this.entry = entry;
	}

	PageInfo info;
	TranslationEntry entry;
}
