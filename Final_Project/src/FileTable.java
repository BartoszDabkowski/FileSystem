// Xiaoyu Liang & Bartosz Dabkowski
// CSS430 Final Project
// FileTable
// File Table class is shared among all user threads to maintain access. The class
// has the actual entity of the file table and the root directory, which is a
// reference to the Directory of the file system. When a user thread opens a file,
// File Table class uses falloc() to allocate a new file table entry based on access
// mode, allocate/retrieve and register the corresponding inode using directory,
// and write back the inode to disk. When a file is closed, ffree() method is used
// to free the file table entry, modify and save inode to the disk, and wake up
// waiting threads.

import java.util.Vector;

public class FileTable {
    private Vector<FileTableEntry> table; // the actual entity of this file table
    private Directory directory;          // the root directory
    private Inode[] iNodes;

    public FileTable(Directory directory, Inode[] iNodes) {
        table = new Vector<FileTableEntry>();   // instantiate a file (structure) table
        this.directory = directory;             // receive a reference to the Director from the file
        this.iNodes = iNodes;
    }

    // allocate a new file (structure) table entry for this file name
    // allocate/retrieve and register the corresponding inode using dir
    // increment this inode's count
    // immediately write back this inode to the disk
    // return a reference to this file (structure) table entry
    public synchronized FileTableEntry falloc( String filename, String mode ) {
        short iNum = -1;
        Inode inode = null;

        // if not directory assign to the inumber corresponding to the file
        if (filename.equals("/") == false){
            iNum = directory.namei(filename);
        }else{
            // if its the directory assign iNum to 0
            iNum = 0;
        }

        // file not found
        if (iNum == -1){
            // checks to see if file mode is equal to "r"
            if(mode.compareTo("r") == 0) {
                // return null since you cannot create a file in read mode
                return null;
            // file not in read mode, therefore create a new file
            } else {
                // allocate a new file (structure) table entry for this file name
                iNum = directory.ialloc(filename);

                //unable to create file return
                if(iNum == -1) {
                    return null;
                }
                // assign iNodes array by iNum to inode
                inode = new Inode();
                iNodes[iNum] = inode;
            }
        // file was found
        }else {
            inode = iNodes[iNum];
        }

        // in read mode
        if (mode.compareTo("r") == 0) {
            // wait until the flag is in write mode
            while (inode.flag > 1) {
                try {
                    wait();
                } catch (InterruptedException e) {}
            }
            //set flag to read
            inode.flag = 1;
            inode.count++;
        }else{
            while (inode.flag > 0) {
                try {
                    wait();
                } catch (InterruptedException e) {}
            }
            inode.flag = 2;
            // increment this inode's count
            inode.count++;
        }

        // immediately write back this inode to the disk
        inode.toDisk(iNum);
        // make new file table entry
        FileTableEntry ftEnt = new FileTableEntry(inode, iNum, mode);
        // add the new entry to the table
        table.addElement(ftEnt);

        // return a reference to this file (structure) table entry
        return ftEnt;

    }

    // receive a file table entry reference
    // save the corresponding inode to the disk
    // free this file table entry.
    // return true if this file table entry found in my table
    public synchronized boolean ffree(FileTableEntry ftEnt) {
        // count is zero so return false
        if (ftEnt.inode.count == 0) {
            return false;
        }
        // subtract 1 from the count
        ftEnt.inode.count -= 1;

        // if the count is zero after the decrement
        // then it can be cahnged to unused mode
        if (ftEnt.inode.count == 0) {
            ftEnt.inode.flag = 0;
            notify();
        }
        //remove the file table entry
        return table.removeElement(ftEnt);
    }

    // copied from assignment description
    public synchronized boolean fempty() {
        return table.isEmpty( ); // return if table is empty
    }                            // should be called before starting a format
}