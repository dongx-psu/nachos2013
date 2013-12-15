package nachos.vm;

import java.util.Hashtable;
import java.util.LinkedList;

import nachos.machine.OpenFile;
import nachos.machine.Processor;
import nachos.machine.TranslationEntry;
import nachos.threads.ThreadedKernel;

public class SwapManager {
	public SwapManager() {
		size = 0;
		file = ThreadedKernel.fileSystem.open(swapFileName, true);
		byte[] buf = new byte[Processor.pageSize * numSwapPage];
		file.write(buf, 0, buf.length);
	}
	
	public boolean write(int frameNo, byte[] data, int offset) {
		return (file.write(frameNo * Processor.pageSize, data, offset, Processor.pageSize)
				== Processor.pageSize);
	}
	
	public boolean read(int framNo, byte[] data, int offset) {
		return (file.read(framNo*Processor.pageSize, data, offset, Processor.pageSize)
				== Processor.pageSize);
	}
	
	public int newFrameNo() {
		if (freeFrames.isEmpty()) return size++;
		Integer result = freeFrames.removeFirst();
		return result;
	}
	
	public SwapPage newSwapPage(Page page) {
		SwapPage swapPage = swapPageTable.get(page.info);
		if (swapPage == null) {
			swapPage = new SwapPage(page.info, page.entry, newFrameNo());
			swapPageTable.put(page.info, swapPage);
		}
		return swapPage;
	}
	
	public SwapPage getSwapPage(PageInfo pageContent) {
		return swapPageTable.get(pageContent);
	}
	
	public boolean deleteSwapPage(PageInfo pageContent) {
		SwapPage swapPage = getSwapPage(pageContent);
		if (swapPage == null) return false;
		freeFrames.add(swapPage.frameNo);
		return true;
	}
	
	public void close() {
		file.close();
		ThreadedKernel.fileSystem.remove(swapFileName);
	}
	
	public OpenFile getSwapFile() {
		return file;
	}
	
	class SwapPage extends Page {
		public SwapPage(PageInfo item, TranslationEntry entry, int frameNo) {
			super(item, entry);
			this.frameNo = frameNo;
		}
		
		int frameNo;
	}
	
	public static final int numSwapPage = 32;
	public static final String swapFileName = "SWAP";
	
	private int size;
	private OpenFile file;
	private LinkedList<Integer> freeFrames = new LinkedList<Integer>();
	private Hashtable<PageInfo, SwapPage> swapPageTable = new Hashtable<PageInfo, SwapPage>();
}
