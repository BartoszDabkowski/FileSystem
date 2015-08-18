// Xiaoyu Liang & Bartosz Dabkowski
// CSS430 Final Project
// Inode
// the Inode class keeps data to describe a file. Our Inode keeps track of 11 direct
// pointer and 1 indirect pointer. Additionally, it keeps track of the file size, the
// number of file table entries, and a whether the file is used or unused.

public class Inode {

    private final static int iNodeSize = 32;       // fix to 32s bytes
    private final static int directSize = 11;      // # direct pointers


    public int length;                             // file size in bytes
    public short count;                            // # file-table entries pointing to this
    public short flag;                             // 0 = unused, 1 = used, ...
    public short direct[] = new short[directSize]; // direct pointers
    public short indirect;                         // a indirect pointer
    
    
// ----------------------------------------------------------------------------
    // Default constructor
    public Inode() {
        length = 0;
        count = 0;
        flag = 0;
        for ( int i = 0; i < directSize; i++ )
            direct[i] = -1;
        indirect = -1;
    }
    
// ----------------------------------------------------------------------------
    
    // Code taken directly from power point
    // Retreiving Inode from disk
    public Inode(short iNumber) {
        int blockNumber = iNumber / 16 + 1;
        byte[] data = new byte[Disk.blockSize];
        SysLib.rawread(blockNumber, data);
        // gets offset of disk
        int offset = (iNumber % 16) * iNodeSize;
        
        length = SysLib.bytes2int(data, offset);
        offset += 4;
        count = SysLib.bytes2short(data, offset);
        offset += 2;
        flag = SysLib.bytes2short(data, offset);
        offset += 2;
        
        for (int i = 0; i < directSize; i++) {
            direct[i] = SysLib.bytes2short(data, offset);
            offset += 2;
        }
        indirect = SysLib.bytes2short(data, offset);
    }
    
// ----------------------------------------------------------------------------

    // save to disk as the i-th inode
    public void toDisk( short iNumber ) {
        
        int target = (iNumber / 16) + 1;
        byte[] temp = new byte[Disk.blockSize];
        int offset = (iNumber * iNodeSize) % Disk.blockSize;
        SysLib.int2bytes(length, temp, offset);
        offset += 4;
        SysLib.short2bytes(count, temp, offset);
        offset += 2;
        SysLib.short2bytes(flag, temp, offset);
        offset += 2;
        for(int i = 0; i < directSize; i++) {
            SysLib.short2bytes(direct[i], temp, offset);
            offset += 2;
        }
        SysLib.short2bytes(indirect, temp, offset);
        
        SysLib.rawwrite(target, temp);
    }

// ----------------------------------------------------------------------------

    // sets next free direct block to block
    public boolean setDirBlock(short block) {
        for (int i = 0; i < direct.length; i++) {
            if (direct[i] == -1) {
                direct[i] = block;
                // free block found return true
                return true;
            }
        }
        //all blocks full so return false
        return false;
    }
    
// ----------------------------------------------------------------------------

    // frees indirect block and returns the index block
    public byte[] freeIndexBlocks() {
        // block doesnt exist
        if(indirect < 0) {
            return null;
        } else {
        // block exists
            byte[] indexBlock = new byte[Disk.blockSize];
            SysLib.rawread(indirect, indexBlock);
            indirect = -1;
            return indexBlock;
        }
    }
    
// ----------------------------------------------------------------------------

    // frees direct blocks and returns the previous data
    public short[] freeDirBlocks() {
        short[] freeDirBlocks = new short[directSize];
        for (int i = 0; i < directSize; i++) {
            freeDirBlocks[i] = direct[i];
            // free direct block
            direct[i] = -1;
        }
        // return data from direct blocks
        return freeDirBlocks;
    }
    
// ----------------------------------------------------------------------------

    //gets the number of the indirect block
    public final int getNumIndirect() {
        int num = (length / Disk.blockSize) - direct.length;
        if (num < 0) {
            return 0;
        }
        return num;
    }
    
// ----------------------------------------------------------------------------

    // finds the target block by the offset
    public short findTargetBlock(int offset) {
        int targetBlock = offset / Disk.blockSize;
        if (targetBlock >= direct.length) {
            if (indirect < 0) {
                return -1;
            }
            
            byte[] indirectBlock = new byte[Disk.blockSize];
            SysLib.rawread(indirect, indirectBlock);
            return SysLib.bytes2short(indirectBlock, (targetBlock - direct.length) * 2);
        }
        else {
            // returns the targeted block
            return direct[targetBlock];
        }
    }
}