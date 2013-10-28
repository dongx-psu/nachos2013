package nachos.threads;

import java.util.LinkedList;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>, and multiple
 * threads can be waiting to <i>listen</i>. But there should never be a time
 * when both a speaker and a listener are waiting, because the two threads can
 * be paired off at this point.
 */
public class Communicator {
	/**
	 * Allocate a new communicator.
	 */
	public Communicator() {
	}

	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 * 
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 * 
	 * @param word
	 *            the integer to transfer.
	 */
	public void speak(int word) {
		lock.acquire();
		
		if (!listenerList.isEmpty()) {
			Info listener = listenerList.removeFirst();
			listener.word = word;
			listener.condition.wake();
		} else {
			Info speaker = new Info();
			speaker.word = word;
			speakerList.add(speaker);
			speaker.condition.sleep();
		}
		
		lock.release();
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 * 
	 * @return the integer transferred.
	 */
	public int listen() {
		lock.acquire();
		
		int res = 0;
		
		if (!speakerList.isEmpty()) {
			Info speaker = speakerList.removeFirst();
			res = speaker.word;
			speaker.condition.wake();
		} else {
			Info listener = new Info();
			listenerList.add(listener);
			listener.condition.sleep();
			res = listener.word;
		}
		
		lock.release();
		
		return res;
	}
	
	private class Info {
		int word = 0;
		Condition condition = new Condition(lock);
	}
	
	Lock lock = new Lock();
	LinkedList<Info> speakerList = new LinkedList<Info>();
	LinkedList<Info> listenerList = new LinkedList<Info>();
	
}
