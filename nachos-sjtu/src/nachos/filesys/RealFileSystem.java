package nachos.filesys;

import java.util.Hashtable;
import java.util.LinkedList;

import nachos.machine.FileSystem;
import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.OpenFile;
import nachos.vm.VMKernel;

/**
 * RealFileSystem provide necessary methods for filesystem syscall. The
 * FileSystem interface already define two basic methods, you should implement
 * your own to adapt to your task.
 * 
 * @author starforever
 */
public class RealFileSystem implements FileSystem {
	/** the free list */
	private static FreeList free_list;

	/** the root folder */
	private Folder root_folder;

	/** the string representation of the current folder */
	private LinkedList<String> cur_path = new LinkedList<String>();
	
	private LinkedList<Folder> cur_folder = new LinkedList<Folder>();
	
	
	private static String path_seperator = "/";
	private static String curDir = ".";
	private static String parentDir = "..";
	
	public static final int maxNameLength = 256;
	private static final int maxPathLength = maxNameLength * 4;
	
	private static Hashtable<Integer, INode> inodeTable = new Hashtable<Integer, INode>();
	
	public static INode getInode(int addr) {
		INode inode = inodeTable.get(addr);
		if (inode == null) {
			inode = new INode(addr);
			if (free_list.isUsed(addr))
				inode.load();
			inodeTable.put(addr, inode);
		}
		return inode;
	}

	public static void removeInode(int addr) {
		inodeTable.remove(addr);
	}

	/**
	 * initialize the file system
	 * 
	 * @param format
	 *            whether to format the file system
	 */
	public void init(boolean format) {
		if (format) {
			INode inode_free_list = new INode(FreeList.STATIC_ADDR);
			inodeTable.put(FreeList.STATIC_ADDR, inode_free_list);
			free_list = new FreeList(inode_free_list);
			free_list.init();
			
			INode inode_root_folder = new INode(Folder.STATIC_ADDR);
			inodeTable.put(Folder.STATIC_ADDR, inode_root_folder);
			root_folder = new Folder(inode_root_folder);
			root_folder.save();
			inode_root_folder.save();

			cur_folder.add(root_folder);
			
			importStub();
			
			Lib.debug(FilesysKernel.DEBUG_FLAG, "finish import stub file system");
		} else {
			INode inode_free_list = new INode(FreeList.STATIC_ADDR);
			inode_free_list.load();
			inodeTable.put(FreeList.STATIC_ADDR, inode_free_list);
			free_list = new FreeList(inode_free_list);
			free_list.load();

			INode inode_root_folder = new INode(Folder.STATIC_ADDR);
			inode_root_folder.load();
			inodeTable.put(Folder.STATIC_ADDR, inode_root_folder);
			root_folder = new Folder(inode_root_folder);
			root_folder.load();
			
			cur_folder.add(root_folder);
		}
		free_list.calcUsed();
	}

	public void finish() {
		VMKernel.getSwapManager().close();
		free_list.save();
		for (INode inode : inodeTable.values())
			inode.save();
	}

	/** import from stub filesystem */
	private void importStub() {
		FileSystem stubFS = Machine.stubFileSystem();
		FileSystem realFS = FilesysKernel.realFileSystem;
		String[] file_list = Machine.stubFileList();
		for (int i = 0; i < file_list.length; ++i) {
			if (!file_list[i].endsWith(".coff"))
				continue;
			OpenFile src = stubFS.open(file_list[i], false);
			if (src == null) {
				continue;
			}
			OpenFile dst = realFS.open(file_list[i], true);
			int size = src.length();
			byte[] buffer = new byte[size];
			src.read(0, buffer, 0, size);
			dst.write(0, buffer, 0, size);
			src.close();
			dst.close();
			Lib.debug(FilesysKernel.DEBUG_FLAG, "copy " + file_list[i] + " to file system");
		}
	}

	/** get the only free list of the file system */
	public FreeList getFreeList() {
		return free_list;
	}

	/** get the only root folder of the file system */
	public Folder getRootFolder() {
		return root_folder;
	}

	class PathResult {
		boolean success = false;
		int addr = -1;
		Folder cur = null;
		String filename = null;
		LinkedList<String> path = null;
		LinkedList<Folder> folder = null;
	}
	
	private PathResult getPath(String name) {
		PathResult res = new PathResult();
		LinkedList<String> path = new LinkedList<String>();
		LinkedList<Folder> folder = new LinkedList<Folder>();
		
		while (name.length() > 1 && name.endsWith(path_seperator))
			name = name.substring(0, name.length() - 1);
		
		if (name.startsWith(path_seperator)) folder.add(root_folder);
		else {
			path.addAll(cur_path);
			folder.addAll(cur_folder);
		}
		
		if (name.equals(path_seperator)) {
			res.success = true;
			res.cur = null;
			res.path = path;
			res.folder = folder;
			res.folder.removeLast();
			res.addr = Folder.STATIC_ADDR;
			return res;
		}
		
		String[] dirName = name.split(path_seperator);
		String filename = dirName[dirName.length - 1];
		for (String st : dirName) {
			if (st.isEmpty() || st.equals(curDir) && st != filename)
				continue;
			if (st.equals(parentDir)) {
				folder.removeLast();
				if (path.isEmpty()) return res;
				path.removeLast();
				if (folder.isEmpty()) return res;
				if (st != filename) continue;
			}
			
			Folder cur = folder.getLast();
			cur.load();
			if (st == filename) { // finally find it..
				res.success = true;
				res.cur = cur;
				res.filename = filename;
				res.path = path;
				res.folder = folder;
				if (st.equals(curDir) || st.equals(parentDir)) {
					if (res.folder.size() > 1) {
						res.cur = folder.get(folder.size() - 2);
						res.filename = path.getLast();
						res.addr = res.cur.getEntry(res.filename);
						return res;
					} else if (res.folder.size() == 1) {
						res.addr = Folder.STATIC_ADDR;
						res.cur = null;
						res.filename = null;
						res.folder.clear();
						res.path.clear();
						return res;
					} else {
						res.success = false;
						return res;
					}
				} else if (cur.contains(st)) {
					res.addr = cur.getEntry(filename);
					return res;
				} else return res;
			}
			
			if (cur.contains(st)) {
				int inodeAddr = cur.getEntry(st);
				INode inode = getInode(inodeAddr);
				if (inode.file_type == INode.TYPE_FOLDER) {
					path.add(st);
					Folder next = new Folder(inode);
					folder.add(next);
				} else return res;
			} else return res;
		}
		
		Lib.assertNotReached();
		return res;
	}
	
	public OpenFile open(String name, boolean create) {
		PathResult res = getPath(name);
		if (res.success) {
			if (res.addr >= 0) {
				INode inode = getInode(res.addr);
				if (inode.file_type == INode.TYPE_FILE) {
					inode.use_count++;
					return new File(inode);
				} else if (inode.file_type == INode.TYPE_SYMLINK)
					return loadSymLink(inode);
			} else if (create) {
				INode inode = getInode(res.cur.create(res.filename));
				inode.use_count++;
				return new File(inode);
			}
		}
		
		return null;
	}

	private OpenFile loadSymLink(INode inode) {
		File file = new File(inode);
		String name = file.readString(0, maxPathLength);
		Lib.assertTrue(name != null);
		return openSym(name);
	}
	
	public OpenFile openSym(String name) {
		PathResult res = getPath(name);
		if (res.success)
			if (res.addr >= 0) {
				INode inode = getInode(res.addr);
				if (inode.file_type == INode.TYPE_FILE) {
					inode.use_count++;
					return new File(inode);
				}
			}
		
		return null;
	}

	public boolean remove(String name) {
		PathResult res = getPath(name);
		if (res.success && res.addr != -1) {
			INode inode = getInode(res.addr);
			if (inode.file_type == INode.TYPE_FILE || inode.file_type == INode.TYPE_SYMLINK) {
				res.cur.removeEntry(res.filename);
				res.cur.save();
				inode.link_count--;
				if (inode.link_count == 0)
					inode.file_type = INode.TYPE_FILE_DEL;
				inode.tryFree();
				return true;
			}
		}
		return false;
	}

	public boolean createFolder(String name) {
		PathResult res = getPath(name);
		if (!res.success || res.addr != -1)
			return false;
		int addr = free_list.allocate();
		INode inode = getInode(addr);
		Folder newFolder = new Folder(inode);
		newFolder.save();
		res.cur.addEntry(res.filename, addr);
		return true;
	}

	public boolean removeFolder(String name) {
		PathResult res = getPath(name);
		if (!res.success || res.addr == -1)
			return false;
		if (res.addr == Folder.STATIC_ADDR)
			return false;
		INode inode = getInode(res.addr);
		if (inode.file_type == INode.TYPE_FOLDER) {
			Folder folder = new Folder(inode);
			folder.load();
			if (folder.isEmpty()) {
				res.cur.removeEntry(res.filename);
				res.cur.save();
				inode.link_count--;
				if (inode == cur_folder.getLast().inode) {
					cur_folder.removeLast();
					cur_path.removeLast();
				}
				inode.tryFree();
				return true;
			} else return false;
		}
		return false;
	}

	public boolean changeCurFolder(String name) {
		PathResult res = getPath(name);
		if (!res.success || res.addr == -1)
			return false;
		INode inode = getInode(res.addr);
		if (inode.file_type == INode.TYPE_FOLDER) {
			Folder folder = new Folder(inode);
			folder.load();
			cur_folder = res.folder;
			cur_folder.add(folder);
			cur_path = res.path;
			if (res.filename != null)
				cur_path.add(res.filename);
			return true;
		}
		return false;
	}

	public String[] readDir(String name) {
		PathResult res = getPath(name);
		if (!res.success || res.addr == -1)
			return null;
		INode inode = getInode(res.addr);
		if (inode.file_type == INode.TYPE_FOLDER) {
			Folder folder = new Folder(inode);
			folder.load();
			return folder.contents();
		}
		return null;
	}

	public FileStat getStat(String name) {
		PathResult res = getPath(name);
		if (!res.success || res.addr == -1)
			return null;
		INode inode = getInode(res.addr);
		if (inode.file_type == INode.TYPE_FILE_DEL || inode.file_type == INode.TYPE_FOLDER_DEL)
			return null;
		FileStat stat = new FileStat();
		stat.name = res.filename;
		if (stat.name == null)
			stat.name = "/";
		stat.size = inode.file_size;
		stat.sectors = inode.sectorNum();
		stat.inode = res.addr;
		stat.links = inode.link_count;
		stat.type = FileStat.TypeMap.get(inode.file_type);
		return stat;
	}

	public boolean createLink(String src, String dst) {
		PathResult resSrc = getPath(src);
		if (!resSrc.success || resSrc.addr == -1)
			return false;
		PathResult resDst = getPath(dst);
		if (!resDst.success || resDst.addr != -1)
			return false;
		resDst.cur.addEntry(resDst.filename, resSrc.addr);
		return true;
	}

	public boolean createSymlink(String src, String dst) {
		PathResult resDst = getPath(dst);
		if (!resDst.success || resDst.addr != -1)
			return false;
		int addr = free_list.allocate();
		INode inode = getInode(addr);
		File file = new File(inode);
		inode.file_type = INode.TYPE_SYMLINK;
		file.writeString(0, src);
		resDst.cur.addEntry(resDst.filename, addr);
		return true;
	}

	public int getSwapFileSectors() {
		INode inode = ((File) VMKernel.getSwapManager().getSwapFile()).inode;
		return inode.file_size;
	}

	public int getFreeSize() {
		return free_list.size();
	}
	
	public String getCurPath() {
		return path2String(cur_path);
	}

	private String path2String(LinkedList<String> path) {
		StringBuffer res = new StringBuffer();
		for (String st : path)
			res.append("/" + st);
		if (res.length() == 0)
			res.append("/");
		return res.toString();
	}
}
