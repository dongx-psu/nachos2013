package nachos.filesys;

import nachos.machine.Disk;
import nachos.machine.Lib;
import nachos.machine.OpenFile;

public class BitMap {
	public BitMap(int n) {
		nBits = n;
		nWords = nBits /  BitsInWord;
		if (nBits % BitsInWord != 0)
			nWords++;
		
		map = new int[nWords];
		for (int i = 0; i < nBits; ++i)
			clear(i);
	}
	
	public void mark(int x) {
		Lib.assertTrue(x >= 0 && x < nBits);
		map[x / BitsInWord] |= 1 << (x % BitsInWord);
	}
	
	public void clear(int x) {
		Lib.assertTrue(x >= 0 && x < nBits);
		map[x/BitsInWord] &= ~(1 << (x%BitsInWord));
	}
	
	public boolean check(int x) {
		Lib.assertTrue(x >= 0 && x < nBits);

		if ((map[x/BitsInWord] & (1 << (x % BitsInWord))) != 0)
			return true;
		else return false;
	}
	
	public int find() {
		for (int i = 0; i < nBits; ++i)
			if (!check(i)) {
				mark(i);
				return i;
			}
		return -1;
	}
	
	public int numClear() {
		int cnt = 0;
		
		for (int i = 0; i < nBits; ++i)
			if (!check(i))
				cnt++;
		
		return cnt;
	}
	
	public void fetchFrom(OpenFile file) {
		byte buffer[] = new byte[nWords * 4];
		file.read(0, buffer, 0, nWords*4);
		for (int i = 0; i < nWords; ++i)
			map[i] = Disk.intInt(buffer, i*4);
	}
	
	public void writeBack(OpenFile file) {
		byte buffer[] = new byte[nWords * 4];
		for (int i = 0; i < nWords; ++i)
			Disk.extInt(map[i], buffer, i * 4);
		
		file.write(0, buffer, 0, nWords * 4);
	}
		
	public static final int BitsInByte = 8;
	public static final int BitsInWord = 32;
	private int nBits;
	private int nWords;
	private int map[];
}
