package nachos.filesys;

import java.util.ArrayList;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.threads.*;

public class ReadWriteLock {
	public ReadWriteLock() {
	}
	
	public void acquireWrite() {
		Lib.assertTrue(!readHeldByCurrentThread() && !writeHeldByCurrentThread());
		
		boolean intStatus = Machine.interrupt().disable();
		KThread thread = KThread.currentThread();
		
		if (writeHolder != null || readHolder.size() > 0) {
			writeWaitQueue.waitForAccess(thread);
			++writeWaiting;
			KThread.sleep();
		} else {
			writeWaitQueue.acquire(thread);
			writeHolder = thread;
		}
		
		Lib.assertTrue(writeHolder == thread);
		
		Machine.interrupt().restore(intStatus);
	}
	
	public void releaseWrite() {
		Lib.assertTrue(writeHeldByCurrentThread());
		
		boolean intStatus = Machine.interrupt().disable();
		
		if ((writeHolder = writeWaitQueue.nextThread()) != null) {
			--writeWaiting;
			writeHolder.ready();
		} else {
			KThread reader = null;
			while ((reader = readWaitQueue.nextThread()) != null) {
				reader.ready();
				readHolder.add(reader);
			}
		}
		
		Machine.interrupt().restore(intStatus);
	}
	
	public void acquireRead() {
		Lib.assertTrue(!readHeldByCurrentThread() && !writeHeldByCurrentThread());
		
		boolean intStatus = Machine.interrupt().disable();
		KThread thread = KThread.currentThread();
		
		if (writeHolder != null || writeWaiting > 0) {
			readWaitQueue.waitForAccess(thread);
			KThread.sleep();
		} else {
			readWaitQueue.acquire(thread);
			readHolder.add(thread);
		}
		
		Lib.assertTrue(readHolder.contains(thread));
		
		Machine.interrupt().restore(intStatus);
	}
	
	public void releaseRead() {
		Lib.assertTrue(readHeldByCurrentThread());
		
		boolean intStatus = Machine.interrupt().disable();
		
		readHolder.remove(KThread.currentThread());
		if (readHolder.isEmpty()) {
			if ((writeHolder = writeWaitQueue.nextThread()) != null) {
				--writeWaiting;
				writeHolder.ready();
			}
		}
		
		Machine.interrupt().restore(intStatus);
	}
	
	public boolean writeHeldByCurrentThread() {
		return writeHolder == KThread.currentThread();
	}
	
	public boolean readHeldByCurrentThread() {
		return readHolder.contains(KThread.currentThread());
	}

	private ArrayList<KThread> readHolder = new ArrayList<KThread>();
	private KThread writeHolder = null;
	private ThreadQueue readWaitQueue = ThreadedKernel.scheduler.newThreadQueue(true);
	private ThreadQueue writeWaitQueue = ThreadedKernel.scheduler.newThreadQueue(true);
	int writeWaiting = 0;
}
