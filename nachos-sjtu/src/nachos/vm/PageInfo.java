package nachos.vm;

public class PageInfo {
	int pid, vpn;
	
	public PageInfo(int pid, int vpn) {
		this.pid = pid;
		this.vpn = vpn;
	}
	
	public boolean equals(Object o) {
		if (o instanceof PageInfo)
			return (((PageInfo) o).pid == pid && ((PageInfo) o).vpn == vpn);
		return false;
	}
	
	//senior function for accelerate HashTable
	public int hashCode() {
		return pid ^ vpn;
	}
}
