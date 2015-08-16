import java.util.Arrays;
import java.util.regex.Pattern;

public class FileSystem {
    
    private Superblock superblock;
    private Directory directory;
    private FileTable fileTable;
    private Inode[] iNodes;
    
    // ----------------------------------------------------------------------------
    
    //most code from powerpoint
    public FileSystem(int diskBlocks) {
        
        superblock = new Superblock(diskBlocks);
        directory = new Directory(superblock.totalInodes);
        iNodes = new Inode[superblock.totalInodes];
        
        for (short i = 0; i < iNodes.length; i++) {
            iNodes[i] = new Inode(i);
        }
        
        fileTable = new FileTable(directory, iNodes);
        
        FileTableEntry ftEnt = open("/", "r");
        
        int ftEntSize = fsize(ftEnt);
        
        if (ftEntSize > 0) {
            byte[] data = new byte[ftEntSize];
            read(ftEnt, data);
            directory.bytes2directory(data);
        }
        close(ftEnt);
    }
    
    // ----------------------------------------------------------------------------
    
    public boolean format( int files ) {
        iNodes = new Inode[superblock.totalInodes];
        
        for(Inode iNode : iNodes) {
            iNode = new Inode();
        }
        
        superblock.format(files);
        directory = new Directory(superblock.totalInodes);
        fileTable = new FileTable(directory, iNodes);
        return true;
    }
    
    // ----------------------------------------------------------------------------
    
    public void sync() {
        FileTableEntry ftEnt = open("/", "w");
        byte[] data = directory.directory2bytes();
        
        write(ftEnt, data);
        close(ftEnt);
        
        iNodes[0].toDisk((short) 0);
        
        ftEnt.inode.toDisk(ftEnt.iNumber);
    }
    
    // ----------------------------------------------------------------------------
    
    public FileTableEntry open(String filename, String mode) {
        
        FileTableEntry ftEnt = fileTable.falloc(filename, mode);
        
        if (mode.equals("w")) {
            if (deallocAllBlocks(ftEnt) == false) {
                return null;
            }
        }
        return ftEnt;
    }
    
    // ----------------------------------------------------------------------------
    
    public boolean close(FileTableEntry ftEnt) {
        synchronized(ftEnt) {
            ftEnt.count -= 1;
            if(ftEnt.count > 0) {
                return true;
            }
        }
        return fileTable.ffree(ftEnt);
    }
    
    // ----------------------------------------------------------------------------
    
    public int fsize(FileTableEntry ftEnt) {
        return ftEnt.inode.length;
    }
    
    // ----------------------------------------------------------------------------
    
    public boolean delete(String filename) {
        short iNum = this.directory.namei(filename);
        if (iNum != -1) {
            return this.directory.ifree(iNum);
        } else {
            return false;
        }
    }
    
    // ----------------------------------------------------------------------------
    
    public int write( FileTableEntry ftEnt, byte[] buffer ) {
        if (ftEnt == null || ftEnt.mode.equals("r")) {
            return -1;
        }
        
        Inode iNode = ftEnt.inode;
        
        byte[] inBlocks = new byte[Disk.blockSize];
        byte[] curBlock = new byte[Disk.blockSize];
        
        if (iNode.indirect <= 0) {
            for(int i = 0; i < inBlocks.length; i++) {
                inBlocks[i] = (byte)-1;
            }
        }
        else {
            SysLib.rawread(iNode.indirect, inBlocks);
        }
        
        int indirectNum = iNode.numIndirectBlocks();
        short targetBlock = iNode.findTargetBlock(ftEnt.seekPtr);
        
        
        if (targetBlock != -1) {
            SysLib.rawread(targetBlock, curBlock);
        }
        else {
            targetBlock = (short)superblock.getFreeBlock();
            if (iNode.registerDirectBlock(targetBlock) == false) {
                SysLib.short2bytes(targetBlock, inBlocks, indirectNum++ * 2);
            }
        }
        
        int counter = ftEnt.seekPtr % Disk.blockSize;
        
        for (int i = 0; i < buffer.length; i++) {
            if (counter == Disk.blockSize) {
                SysLib.rawwrite(targetBlock, curBlock);
                targetBlock = iNode.findTargetBlock(ftEnt.seekPtr);
                if(targetBlock > 0){
                    SysLib.rawread(targetBlock, curBlock);
                }
                else {
                    targetBlock = (short) superblock.getFreeBlock();
                    if (iNode.registerDirectBlock((short)targetBlock)== false) {
                        SysLib.short2bytes(targetBlock, inBlocks, indirectNum * 2);
                        indirectNum++;
                    }
                }
                counter = 0;
            }
            curBlock[counter++] = buffer[i];
            ftEnt.seekPtr++;
        }
        
        SysLib.rawwrite(targetBlock, curBlock);
        if (iNode.length < ftEnt.seekPtr) {
            iNode.length = ftEnt.seekPtr;
        }
        
        if (indirectNum > 0) {
            if (iNode.indirect < 0) {
                iNode.indirect = (short)superblock.getFreeBlock();
            }
            
            SysLib.rawwrite(iNode.indirect, inBlocks);
        }
        
        return buffer.length;
    }
    
    // ----------------------------------------------------------------------------
    
    public int read( FileTableEntry ftEnt, byte[] buffer ) {
        if (ftEnt == null) {
            return -1;
        }
        
        Inode iNode = ftEnt.inode;
        
        if (iNode.findTargetBlock(ftEnt.seekPtr) == -1) {
            return -1;
        }
        
        byte[] block = new byte[Disk.blockSize];
        
        SysLib.rawread(iNode.findTargetBlock(ftEnt.seekPtr), block);
        
        for (int i = 0; i < buffer.length; i++) {
            int index = ftEnt.seekPtr % Disk.blockSize;
            if (index == 0) {
                if (iNode.findTargetBlock(ftEnt.seekPtr) == -1) {
                    return -1;
                }
                
                SysLib.rawread(iNode.findTargetBlock(ftEnt.seekPtr), block);
            }
            buffer[i] = block[index];
            ftEnt.seekPtr++;
        }
        return buffer.length;
    }
    
    // ----------------------------------------------------------------------------
    
    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;
    
    // ----------------------------------------------------------------------------
    
    public int seek(FileTableEntry ftEnt, int offset, int whence) {
        Inode iNode = ftEnt.inode;
        
        switch (whence) {
            case (SEEK_SET):
                if (offset < iNode.length && offset >= 0 ) {
                    ftEnt.seekPtr = offset;
                    return ftEnt.seekPtr;
                }
            case (SEEK_CUR):
                int skPtr = ftEnt.seekPtr + offset;
                if (skPtr < iNode.length && 0 <= skPtr) {
                    ftEnt.seekPtr = skPtr;
                    return skPtr;
                }
            case (SEEK_END):
                if (iNode.length > (-1 * offset) && offset < 0) {
                    ftEnt.seekPtr = iNode.length + offset;
                    return ftEnt.seekPtr;
                }
                
            default:
                return -1;
        }
    }
    
    // ----------------------------------------------------------------------------
    
    private boolean deallocAllBlocks(FileTableEntry ftEnt) {
        if (ftEnt.inode.length <= 0) {
            return true;
        }
        
        short[] freedBlocks = ftEnt.inode.freeDirectBlocks();
        
        int index = 0;
        while(freedBlocks[index] != 1 || index < freedBlocks.length) {
            superblock.returnBlock(freedBlocks[index++]);
        }
        
        byte[] unregisteredIndexBlocks = ftEnt.inode.unregisterIndexBlock();
        
        if (unregisteredIndexBlocks == null) {
            return true;
        }
        else {
            for (int i = 0; i < unregisteredIndexBlocks.length; i += 2) {
                int block = SysLib.bytes2short(unregisteredIndexBlocks, i);
                superblock.returnBlock(block);
            }
            return true;
        }
    }
}