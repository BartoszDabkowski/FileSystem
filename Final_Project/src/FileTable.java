// Xiaoyu Liang & Bartosz Dabkowski
// CSS430 Final Project
// FileTable

// The file table allocates new file table entry for the files and
// save the corresponding inode into Disk and free the entry.


import java.util.*;

public class FileTable {
    private Vector table;         // the actual entity of this file table
    private Directory dir;        // the root directory
    private Inode[] list;          // the list of inodes

    public FileTable( Directory directory, Inode[] list ) { // constructor
        table = new Vector( );     // instantiate a file (structure) table
        dir = directory;           // receive a reference to the Director
        this.list = list;
    }                             // from the file system

    //---------------------------------------------------------------------------
    // major public methods
    // allocate a new file (structure) table entry for this file name
    // allocate/retrieve and register the corresponding inode using dir
    // increment this inode's count
    // immediately write back this inode to the disk
    // return a reference to this file (structure) table entry
    public synchronized FileTableEntry falloc( String filename, String mode ) {
        Inode inode = null;
        short iNumber = -1;

        while(true) {
            if (filename.equals("/")) {
                iNumber = 0;
            } else {
                iNumber = dir.namei(filename);
            }
            if (iNumber >= 0) {
                inode = list[iNumber];
                if (mode.equals("r")) {
                    while (inode.flag > 1) {
                        try {
                            wait();
                        } catch (InterruptedException e) {}
                    }
                    inode.flag = 1;
                    // inode.count++;
                    break;

                }else {
                    while (inode.flag > 0) {
                        try {
                            wait();
                        } catch (InterruptedException e) {}
                    }
                    inode.flag = 2;

                    break;
                }
            } else if (!mode.equals("r")) {
                iNumber = dir.ialloc(filename);
                inode = new Inode();
                list[iNumber] = inode;
                break;
            } else {
                return null;
            }
        }

        inode.count++;
        inode.toDisk(iNumber);
        FileTableEntry tableEntry = new FileTableEntry(inode, iNumber, mode);
        table.addElement(tableEntry);
        return tableEntry;
    }

    //---------------------------------------------------------------------------
    // receive a file table entry reference
    // save the corresponding inode to the disk
    // free this file table entry.
    // return true if this file table entry found in my table
    public synchronized boolean ffree( FileTableEntry e ) {
        if (table.removeElement(e)) {
            e.inode.count--;
            if (e.inode.count == 0) {
                e.inode.flag = 0;
                notify();
            }
            e.inode.toDisk(e.iNumber);
            e = null;
            return true;
        }
        return false;
    }

    //---------------------------------------------------------------------------
    // return if table is empty
    // should be called before starting a format
    public synchronized boolean fempty( ) {
        return table.isEmpty( );
    }
}