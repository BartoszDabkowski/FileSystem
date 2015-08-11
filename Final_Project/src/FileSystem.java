// Xiaoyu Liang & Bartosz Dabkowski
// CSS430 Final Project
// FileSystem

public class FileSystem {

    //flags that correspond with Inode
    public static final short UNUSED = 0;
    public static final short USED = 1;
    public static final short READ = 2;
    public static final short WRITE = 3;
    public static final short DELETE =  4;

    private Superblock superblock;
    private Directory directory;
    private FileTable filetable;

//----------------------------------------------------------------------------------------------------------------------

    public FileSystem(int diskBlocks) {
        // create superblock, and format disk with 64 Inodes in default
        superblock = new Superblock(diskBlocks);

        // create directory, and register "/" in directory entry 0
        directory = new Directory(superblock.totalInodes);

        // file table is created, and store directory in file table
        filetable = new FileTable(directory);

        //directory reconstruction
        FileTableEntry dirEnt = open("/", "r");
        int dirSize = fsize(dirEnt);

        if(dirSize > 0) {
            byte[] dirData = new byte[dirSize];
            read(dirEnt, dirData);
            directory.bytes2directory(dirData);
        }

        close(dirEnt);
    }

//----------------------------------------------------------------------------------------------------------------------

    // Formats the disk (Disk.java's data contents). The parameter files specifies
    // the maximum number of files to be created (the number of inodes to be allocated)
    // in your file system. The return value is 0 on success, otherwise -1.
    int format(int files) {
        if(filetable.fempty() == true) {
            superblock.format(files);
            directory = new Directory(superblock.totalInodes);
            filetable = new FileTable(directory);
            return 0;
        } else {
            System.out.println("**********************************************");
            System.out.printf("FileSystem::format() ERROR fileTable not empty\n");
            System.out.println("**********************************************");
            return -1;
        }
    }

//----------------------------------------------------------------------------------------------------------------------

    // Opens the file specified by the fileName string in the given mode
    // (where "r" = ready only, "w" = write only, "w+" = read/write, "a" = append).
    // The call allocates a new file descriptor, fd to this file. The file is created
    // if it does not exist in the mode "w", "w+" or "a". SysLib.open must return a
    // negative number as an error value if the file does not exist in the mode "r".
    // Note that the file descriptors 0, 1, and 2 are reserved as the standard input,
    // output, and error, and therefore a newly opened file must receive a new descriptor
    // numbered in the range between 3 and 31. If the calling thread's user file descriptor
    // table is full, SysLib.open should return an error value. The seek pointer is
    // initialized to zero in the mode "r", "w", and "w+", whereas initialized at the end
    // of the file in the mode "a".
    FileTableEntry open(String filename, String mode) {
        boolean isNewFile; // new file or not

        //checks to see if file is new or already exists
        if(directory.namei(filename) == -1) {
            isNewFile = true;
        } else {
            isNewFile = false;
        }

        short flag;

        FileTableEntry fileTableEntry = filetable.falloc(filename, mode);

        if(mode.equals("r")) {
            if(isNewFile == true) {
                System.out.println("********************************************************");
                System.out.printf("FileSystem::open() ERROR \"r\" cannot read from new file\n");
                System.out.println("********************************************************");
                return null;
            } else {
                flag = READ;
            }
        }
        else if(mode.equals("w")) {
            deallocateAllBlocks(fileTableEntry);
            flag = WRITE;
            isNewFile = true;
        }
        else if(mode.equals("w+")) {
            flag = WRITE;
        }
        else if(mode.equals("a")) {
            seek(fileTableEntry, 0, SEEK_END);
            flag = WRITE;
        } else {
            System.out.println("*************************************");
            System.out.printf("FileSystem::open() ERROR invalid mode\n");
            System.out.println("*************************************");
            return null;
        }

        if(fileTableEntry.count == 1) {
            fileTableEntry.inode.flag = flag;
        }

        if(isNewFile == true) {
            short dirBlock;
            if((dirBlock = (short)superblock.getFreeBlock()) == -1) {
                System.out.println("**************************************");
                System.out.printf("FileSystem::open() ERROR no Free Block\n");
                System.out.println("***************************************");
                return null;
            } else {
                fileTableEntry.inode.toDisk(fileTableEntry.iNumber);
                //change block of next blockNum
            }
        }
        return fileTableEntry;
    }

//----------------------------------------------------------------------------------------------------------------------

    // Closes the file corresponding to fd, commits all file transactions on this file,
    // and unregisters fd from the user file descriptor table of the calling thread's TCB.
    // The return value is 0 in success, otherwise -1.
    int close(FileTableEntry ftEnt) {
        synchronized (ftEnt) {
            if (ftEnt.count == 0) {
                ftEnt.inode.toDisk(ftEnt.iNumber);
                ftEnt.inode.flag = USED;

                if (filetable.ffree(ftEnt) == true) {
                    return 0;
                } else {
                    System.out.println("**************************************************");
                    System.out.printf("FileSystem::close() ERROR ftEnt not found in table\n");
                    System.out.println("**************************************************");
                    return -1;
                }
            } else {
                System.out.println("************************************");
                System.out.printf("FileSystem::close() ERROR count != 0\n");
                System.out.println("************************************");
                return -1;
            }
        }
    }

//----------------------------------------------------------------------------------------------------------------------

    // Returns the size in bytes of the file indicated by fd.
    int fsize(FileTableEntry ftEnt) {
        if(ftEnt == null) {
            System.out.println("***************************************");
            System.out.printf("FileSystem::fsize() ERROR ftEnt is null\n");
            System.out.println("****************************************");
            return -1;
        }
        else if(ftEnt.inode == null) {
            System.out.println("*********************************************");
            System.out.printf("FileSystem::fsize() ERROR ftEnt.inode is null\n");
            System.out.println("*********************************************");
            return -1;
        } else {
            return ftEnt.inode.length;
        }
    }

//----------------------------------------------------------------------------------------------------------------------

    // Reads up to buffer.length bytes from the file indicated by fd, starting at the position
    // currently pointed to by the seek pointer. If bytes remaining between the current seek
    // pointer and the end of file are less than buffer.length, SysLib.read reads as many bytes
    // as possible, putting them into the beginning of buffer. It increments the seek pointer by
    // the number of bytes to have been read. The return value is the number of bytes that have
    // been read, or a negative value upon an error.
    int read(FileTableEntry ftEnt, byte[] buffer) {
        return -1;
    }

//----------------------------------------------------------------------------------------------------------------------

    // Writes the contents of buffer to the file indicated by fd, starting at the position indicated
    // by the seek pointer. The operation may overwrite existing data in the file and/or append to the
    // end of the file. SysLib.write increments the seek pointer by the number of bytes to have been
    // written. The return value is the number of bytes that have been written, or a negative value upon
    // an error.
    int write(FileTableEntry ftEnt, byte[] buffer) {
        return -1;
    }

//----------------------------------------------------------------------------------------------------------------------

    // Destroys the file specified by fileName. If the file is currently open, it is not destroyed until
    // the last open on it is closed, but new attempts to open it will fail.
    int delete(String filename) {
        short iNum;

        // check to see if file exists
        if((iNum = directory.namei(filename)) == -1) {
            System.out.println("**********************************************");
            System.out.printf("FileSystem::delete() ERROR file does not exist\n");
            System.out.println("**********************************************");
            return -1;
        } else {
            if(directory.ifree(iNum) == true) {
                return 0;
            } else {
                System.out.println("****************************************************");
                System.out.printf("FileSystem::delete() ERROR file not found in ifree()\n");
                System.out.println("****************************************************");
                return -1;
            }
        }
    }


//----------------------------------------------------------------------------------------------------------------------

    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;

//----------------------------------------------------------------------------------------------------------------------

    // Updates the seek pointer corresponding to fd as follows:
    //   --If whence is SEEK_SET (= 0), the file's seek pointer is set to offset bytes from the beginning of the file
    //   --If whence is SEEK_CUR (= 1), the file's seek pointer is set to its current value plus the offset.
    //     The offset can be positive or negative.
    //   --If whence is SEEK_END (= 2), the file's seek pointer is set to the size of the file plus the offset.
    //     The offset can be positive or negative.
    // If the user attempts to set the seek pointer to a negative number you must clamp it to zero. If the user
    // attempts to set the pointer to beyond the file size, you must set the seek pointer to the end of the file.
    // In both cases, you should return success.
    int seek (FileTableEntry ftEnt, int offset, int whence) {

        int seekPointer;
        int fileSize;
        synchronized (ftEnt) {
            fileSize = fsize(ftEnt);
            seekPointer = ftEnt.seekPtr;

            if(whence == SEEK_SET) {
                // the file's seek pointer is set to offset bytes from the beginning of the file
                seekPointer = offset;
            }
            else if(whence == SEEK_CUR) {
                // the file's seek pointer is set to its current value plus the offset.
                seekPointer += offset;
            }
            else if(whence == SEEK_END) {
                // the file's seek pointer is set to the size of the file plus the offset.
                seekPointer += offset + fileSize;
            } else {
                System.out.println("*************************************");
                System.out.printf("FileSystem::seek() ERROR whence = %d\n", whence);
                System.out.println("*************************************");
            }

            // If the user attempts to set the seek pointer to a negative number; clamp it to zero
            if(seekPointer < 0) {
                seekPointer = 0;
            }

            // If the user attempts to set the pointer to beyond the file size;
            // set the seek pointer to the end of the file.
            if(seekPointer > fileSize) {
                seekPointer = fileSize;
            }

            // change ftEnt seekPtr to match
            ftEnt.seekPtr = seekPointer;
        }
        return seekPointer;
    }

//----------------------------------------------------------------------------------------------------------------------

    private boolean deallocateAllBlocks(FileTableEntry ftEnt) {
        return false;
    }
}
