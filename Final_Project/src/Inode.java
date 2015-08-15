// Xiaoyu Liang & Bartosz Dabkowski
// CSS430 Final Project
// Inode
public class Inode {
    private final static int iNodeSize = 32;       // fix to 32 bytes
    private final static int directSize = 11;      // # direct pointers


    public int length;                             // file size in bytes
    public short count;                            // # file-table entries pointing to this
    public short flag;                             // 0 = unused, 1 = used, ...
    public short direct[] = new short[directSize]; // direct pointers
    public short indirect;                         // a indirect pointer

    Inode( ) {                                     // a default constructor
        length = 0;
        count = 0;
        flag = 1;
        for ( int i = 0; i < directSize; i++ )
            direct[i] = -1;
        indirect = -1;
    }

    Inode( short iNumber ) {                       // retrieving inode from disk
        // check to see if iNumber is negative
        if(iNumber < 0) {
            System.out.println("****************************************");
            System.out.printf("Inode::Inode() ERROR iNumber is negative\n");
            System.out.println("****************************************");
            return;
        }

        byte[] data = new byte[Disk.blockSize];

        //get offset of current block
        int blockNum = 1 + iNumber / 16;

        SysLib.rawread(blockNum, data);

        int offset = (iNumber % 16) * iNodeSize;

        length = SysLib.bytes2int(data, offset);
        offset += 4;

        count = SysLib.bytes2short(data, offset);
        offset += 2;

        flag = SysLib.bytes2short(data, offset);
        offset += 2;

        for (int i = 0; i < directSize; i++) {
            direct[i] = SysLib.bytes2short(data, offset);
            offset +=2;
        }

        indirect = SysLib.bytes2short(data, offset);
    }

    int toDisk( short iNumber ) {                  // save to disk as the i-th inode
        // check to see if iNumber is negative
        if(iNumber < 0) {
            System.out.println("*****************************************");
            System.out.printf("Inode::toDisk() ERROR iNumber is negative\n");
            System.out.println("*****************************************");
            return -1;
        }

        byte[] data = new byte[Disk.blockSize];
        int blockNum = 1 + iNumber / 16;

        SysLib.rawread(blockNum, data);

        int offset = (iNumber % 16) * iNodeSize;

        SysLib.int2bytes(length, data, offset);
        offset += 4;

        SysLib.short2bytes(count, data, offset);
        offset += 2;

        SysLib.short2bytes(flag, data, offset);
        offset += 2;

        for (int i = 0; i < directSize; i++) {
            SysLib.short2bytes(direct[i], data, offset);
            offset +=2;
        }

        SysLib.short2bytes(indirect, data, offset);
        offset +=2;

        SysLib.rawwrite(blockNum, data);

        return 0;
    }

    short getIndexBlockNumber() {
        return indirect;
    }

    public boolean registerTargetBlock(short freeBlock) {
        // find inode that matches
        for (int i = 0; i < direct.length; i++) {
            if (direct[i] == -1) {
                direct[i] = freeBlock;
                return true;
            }
        }
        return false;
    }

    public final int getNumberOfIndirectBlocks() {
        // return the number of indirect blocks
        int num = (length / Disk.blockSize) - direct.length;
        // default to 0 if negative
        if (num < 0) {
            return 0;
        }
        return num;
    }

    //find the block
    int findTargetBlock(int offset) {
        int targetBlock = offset / Disk.blockSize;

        // its an indirect pointer
        if (direct.length <= targetBlock) {
            // block does not exist
            if (indirect < 0) {
                return -1;
            }
            // get from disk
            byte[] indirectBlock = new byte[Disk.blockSize];
            SysLib.rawread(indirect, indirectBlock);
            return SysLib.bytes2short(indirectBlock, (targetBlock - direct.length) * 2);
        }
        // its a direct pointer
        else {
            return direct[targetBlock];
        }
    }

    // changes direct blocks to -1 and returns the freed blocks
    public short[] freeDirectBlocks() {
        short[] returnedDirectBlocks = new short[directSize];
        // replace direct blocks with -1 & replace returnedDirectBlocks
        // with direct block values
        for (int i = 0; i < directSize; i++) {
            returnedDirectBlocks[i] = direct[i];
            direct[i] = -1;
        }
        return returnedDirectBlocks;
    }

    public byte[] unregisterIndexBlock() {
        //does not exist
        if( indirect < 0 ) {
            return null;
        }
        //exists
        else {
            byte[] returnedIndexBlock = new byte[Disk.blockSize];
            SysLib.rawread( indirect, returnedIndexBlock );
            indirect = -1;
            return returnedIndexBlock;
        }
    }
}
