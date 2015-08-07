// Xiaoyu Liang & Bartosz Dabkowski
// CSS430 Final Project
// Superblock

// Superblock class keeps track of total number of disk blocks, total number
// of indoes, and the block number of the head block of the free list. It can
// format the disk, sync, get and return the free block to free list.
public class Superblock {
    private final int defaultInodeBlock = 64;
    public int totalBlocks; // the number of disk blocks
    public int totalInodes; // the number of inodes
    public int freeList;    // the block number of the free list's head

    // constructor initializes class fields from reading from disk
    public Superblock( int diskSize ) {
        byte[] data = new byte[512];
        SysLib.rawread(0, data);
        totalBlocks = SysLib.bytes2int(data, 0);
        totalInodes = SysLib.bytes2int(data, 4);
        freeList = SysLib.bytes2int(data, 8);

        if (totalBlocks != diskSize || totalInodes <= 0 || freeList < 2) {
            totalBlocks = diskSize;
            format(defaultInodeBlock);
        }
    }

    // format block to the right format and write to disk
    public void format(int size) {
        totalInodes = size;

        for (int i = 0; i < totalInodes; i++) {
            Inode temp = new Inode();
            temp.flag = 0;
            temp.toDisk((short)i);
        }

        freeList = 2 + totalInodes * 32 / 512;

        for(int i = freeList; i < totalBlocks; i++) {
            byte[] data = new byte[512];

            for (int j = 0; j < 512; j++) {
                data[j] = 0;
            }

            SysLib.int2bytes(i + 1, data, 0);
            SysLib.rawwrite(i, data);
        }
        sync();
    }

    // sync current superblock class fields to the disk
    public void sync() {
        byte[] data = new byte[512];
        SysLib.int2bytes(totalBlocks, data, 0);
        SysLib.int2bytes(totalInodes, data, 4);
        SysLib.int2bytes(freeList, data, 8);
        SysLib.rawwrite(0, data);
    }

    // get the index of the first free block and return it.
    public int getFreeBlock() {
        int index = freeList;
        if (index == -1) return -1;
        byte[] data = new byte[512];
        SysLib.rawread(index, data);
        freeList = SysLib.bytes2int(data, 0);
        SysLib.int2bytes(0, data, 0);
        SysLib.rawwrite(index, data);
        return index;
    }

    // return a free block to the free list.
    public boolean returnBlock(int index) {
        if (index < 0) return false;
        byte[] data = new byte[512];

        for(int i = 0; i < 512; i++) {
            data[i] = 0;
        }

        SysLib.int2bytes(freeList, data, 0);
        SysLib.rawwrite(index, data);
        freeList = index;
        return true;
    }

}
