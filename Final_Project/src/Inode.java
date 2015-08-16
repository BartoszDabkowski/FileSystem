// Xiaoyu Liang & Bartosz Dabkowski
// CSS430 Final Project
// Inode

public class Inode {
    
    // Inode has static size which is 32 byte
    private final static int iNodeSize = 32;
    
    // And static directly size is 11
    private final static int directSize = 11;
    
    
    // the state of flag
    private final static int UNUSED = 0;
    private final static int USED = 1;
    private final static int WRITE = 2;
    
    public int length;                             // file size in bytes
    public short count;                            // # file-table entries pointing to this
    public short flag;                             // 0 = unused, 1 = used...
    public short direct[] = new short[directSize]; // set the direct data size
    public short indirect;                         // set the pointer to point the indirect data
    
    
    // ----------------------------------------------------------------------------
    
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
    public Inode(short iNumber) {
        int blockNumber = iNumber / 16 + 1;
        byte[] data = new byte[Disk.blockSize];
        SysLib.rawread(blockNumber, data);
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
    
    public int findIndexBlock( ) {
        return indirect;
    }
    
    // ----------------------------------------------------------------------------
    
    public boolean registerDirectBlock(short freeBlock) {
        for (int i = 0; i < direct.length; i++) {
            if (direct[i] == -1) {
                direct[i] = freeBlock;
                return true;
            }
        }
        return false;
    }
    
    // ----------------------------------------------------------------------------
    
    public boolean registerTargetBlock(short freeBlock) {
        for (short dir : direct) {
            if (dir == -1) {
                dir = freeBlock;
                return true;
            }
        }
        return false;
    }
    
    // ----------------------------------------------------------------------------
    
    public boolean registerIndexBlock(short indexBlockNumber) {
        if(findIndexBlock() == -1) {
            return false;
        }
        else {
            indirect = indexBlockNumber;
            return true;
        }
    }
    
    // ----------------------------------------------------------------------------
    
    public byte[] unregisterIndexBlock( ) {
        if(indirect < 0) {
            return null;
        } else {
            byte[] indexBlock = new byte[Disk.blockSize];
            SysLib.rawread(indirect, indexBlock);
            indirect = -1;
            return indexBlock;
        }
    }
    
    // ----------------------------------------------------------------------------
    
    public short[] freeDirectBlocks() {
        short[] freeDirBlocks = new short[directSize];
        for (int i = 0; i < directSize; i++) {
            freeDirBlocks[i] = direct[i];
            direct[i] = -1;
        }
        return freeDirBlocks;
    }
    
    // ----------------------------------------------------------------------------
    
    public final int numIndirectBlocks() {
        int num = (length / Disk.blockSize) - direct.length;
        if (num < 0) {
            return 0;
        }
        return num;
    }
    
    // ----------------------------------------------------------------------------
    
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
            return direct[targetBlock];
        }
    }
}