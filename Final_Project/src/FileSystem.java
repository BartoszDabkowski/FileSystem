
public class FileSystem {
    
    private Superblock superblock;
    private Directory directory;
    private FileTable fileTable;
    private Inode[] iNodes;
    
// ----------------------------------------------------------------------------
    
    // most code from powerpoint
    // constructor
    public FileSystem(int diskBlocks) {
        
        superblock = new Superblock(diskBlocks);
        directory = new Directory(superblock.totalInodes);
        iNodes = new Inode[superblock.totalInodes];

        // goes through array of iNodes and initializes
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
    // Formats the disk (Disk.java's data contents). The parameter files
    // specifies the maximum number of files to be created (the number of inodes
    // to be allocated) in your file system. The return value is 0 on success,
    // otherwise -1.

    public int format(int files) {
        if(files < 0) {
            return -1;
        }

        iNodes = new Inode[superblock.totalInodes];
        
        for(Inode iNode : iNodes) {
            iNode = new Inode();
        }
        
        superblock.format(files);
        directory = new Directory(superblock.totalInodes);
        fileTable = new FileTable(directory, iNodes);
        return 0;
    }
    
// ----------------------------------------------------------------------------

    // Opens the file specified by the fileName string in the given mode
    public FileTableEntry open(String filename, String mode) {

        // make file table entry the given file and mode
        FileTableEntry ftEnt = fileTable.falloc(filename, mode);

        // only deallocate blocks if mode is write
        if (mode.equals("w")) {
            if (deallocAllBlocks(ftEnt) == false) {
                return null;
            }
        }
        return ftEnt;
    }
    
// ----------------------------------------------------------------------------

    // Closes the file corresponding to ftEnt, commits all file transactions on
    // this file, and unregisters fd from the user file descriptor table of
    // the calling thread's TCB. The return value is 0 in success, otherwise -1.
    public int close(FileTableEntry ftEnt) {
        if(ftEnt == null) {
            return -1;
        }

        synchronized(ftEnt) {
            ftEnt.count -= 1;
            if(ftEnt.count > 0) {
                return -1;
            }
        }
        // unregisters ftEnt from the user file descriptor
        fileTable.ffree(ftEnt);
        return 0;
    }
    
// ----------------------------------------------------------------------------

    // Returns the size in bytes of the file indicated by ftEnt.
    public int fsize(FileTableEntry ftEnt) {
        if(ftEnt == null) {
            return -1;
        }
        return ftEnt.inode.length;
    }
    
// ----------------------------------------------------------------------------

    // Destroys the file specified by fileName. If the file is currently open,
    // it is not destroyed until the last open on it is closed, but new attempts
    // to open it will fail.
    public boolean delete(String filename) {
        short iNum = this.directory.namei(filename);
        if (iNum != -1) {
            return this.directory.ifree(iNum);
        } else {
            return false;
        }
    }
    
// ----------------------------------------------------------------------------

    // Writes the contents of buffer to the file indicated by ftEnt, starting at
    // the position indicated by the seek pointer.
    // writes to buffer
    public int write(FileTableEntry ftEnt, byte[] buffer) {
        // valid checks; cannot write to read only
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
            // reads indirect data
            SysLib.rawread(iNode.indirect, inBlocks);
        }
        
        int indirectNum = iNode.getNumIndirect();
        // starting at the position indicated by the seek pointer
        short targetBlock = iNode.findTargetBlock(ftEnt.seekPtr);
        
        // target is -1, so read the target block
        if (targetBlock != -1) {
            SysLib.rawread(targetBlock, curBlock);
        }
        else {
            targetBlock = (short)superblock.getFreeBlock();
            if (iNode.setDirBlock(targetBlock) == false) {
                SysLib.short2bytes(targetBlock, inBlocks, indirectNum++ * 2);
            }
        }
        
        int counter = ftEnt.seekPtr % Disk.blockSize;
        
        for(int i = 0; i < buffer.length; i++) {
            // counter has reached the end of the block size allowed
            if (counter == Disk.blockSize) {
                // write to target block
                SysLib.rawwrite(targetBlock, curBlock);
                targetBlock = iNode.findTargetBlock(ftEnt.seekPtr);
                if(targetBlock > 0) {
                    SysLib.rawread(targetBlock, curBlock);
                }
                else {
                // finds new target block if old targetblock reached end
                    targetBlock = (short) superblock.getFreeBlock();
                    if (iNode.setDirBlock((short)targetBlock)== false) {
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
        // assign the inNode length to ftEnt seekptr if
        // the length is smaller than seekptr
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

    // Reads up to buffer.length bytes from the file indicated by fd, starting
    // at the position currently pointed to by the seek pointer.
    public int read(FileTableEntry ftEnt, byte[] buffer) {
        // checks if file table entry exists
        if (ftEnt == null) {
            return -1;
        }
        
        Inode iNode = ftEnt.inode;

        // checks if targeted block exists
        if (iNode.findTargetBlock(ftEnt.seekPtr) == -1) {
            return -1;
        }
        
        byte[] block = new byte[Disk.blockSize];

        // starting at the position currently pointed to by the seek pointer
        short targetBlock = iNode.findTargetBlock(ftEnt.seekPtr);
        SysLib.rawread(targetBlock, block);
        
        for (int i = 0; i < buffer.length; i++) {
            targetBlock = iNode.findTargetBlock(ftEnt.seekPtr);
            int index = ftEnt.seekPtr % Disk.blockSize;
            if (index == 0) {
                if (targetBlock == -1) {
                    return -1;
                }
                
                SysLib.rawread(targetBlock, block);
            }
            buffer[i] = block[index];
            // It increments the seek pointer by the
            // number of bytes that have been read.
            ftEnt.seekPtr++;
        }
        // The return value is the number of bytes
        // that have been read
        return buffer.length;
    }
    
    // ----------------------------------------------------------------------------
    
    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;
    
// ----------------------------------------------------------------------------

    // sets seek pointer
    public int seek(FileTableEntry ftEnt, int offset, int whence) {
        if(ftEnt == null) {
            return -1;
        }

        Inode iNode = ftEnt.inode;
        
        switch (whence) {
            case (SEEK_SET):
                // the file's seek pointer is set to offset bytes
                // from the beginning of the file
                if (offset < iNode.length && offset >= 0) {
                    ftEnt.seekPtr = offset;
                    return ftEnt.seekPtr;
                }
            case (SEEK_CUR):
                // the file's seek pointer is set to its current
                // value plus the offset. The offset can be positive or negative.
                int skPtr = ftEnt.seekPtr + offset;
                if (skPtr < iNode.length && 0 <= skPtr) {
                    ftEnt.seekPtr = skPtr;
                    return skPtr;
                }
            case (SEEK_END):
                // the file's seek pointer is set to the size of
                // the file plus the offset. The offset can be positive or negative.
                if (iNode.length > (-1 * offset) && offset < 0) {
                    ftEnt.seekPtr = iNode.length + offset;
                    return ftEnt.seekPtr;
                }
                
            default:
                // invalid command
                return -1;
        }
    }
    
// ----------------------------------------------------------------------------

    // private helper function that deallocated all of the blocks
    private boolean deallocAllBlocks(FileTableEntry ftEnt) {
        if(ftEnt == null) {
            return false;
        }

        if (ftEnt.inode.length <= 0) {
            return true;
        }
        
        short[] freedBlocks = ftEnt.inode.freeDirBlocks();
        
        int index = 0;
        while(freedBlocks[index] != 1 || index < freedBlocks.length) {
            superblock.returnBlock(freedBlocks[index++]);
        }
        
        byte[] freedIndexBlocks = ftEnt.inode.freeIndexBlocks();

        if (freedIndexBlocks == null) {
            return true;
        }
        else {
            for (int i = 0; i < freedIndexBlocks.length; i += 2) {
                int block = SysLib.bytes2short(freedIndexBlocks, i);
                superblock.returnBlock(block);
            }
            return true;
        }
    }
}