// Xiaoyu Liang & Bartosz Dabkowski
// CSS430 Final Project
// FileTable

// The file table allocates new file table entry for the files and
// save the corresponding inode into Disk and free the entry.
import java.util.*;

public class FileTable {
    private Vector table;         // the actual entity of this file table
    private Directory dir;        // the root directory

    public FileTable( Directory directory ) { // constructor
        table = new Vector( );     // instantiate a file (structure) table
        dir = directory;           // receive a reference to the Director
    }                             // from the file system

    //---------------------------------------------------------------------------
    // major public methods
    // allocate a new file (structure) table entry for this file name
    // allocate/retrieve and register the corresponding inode using dir
    // increment this inode's count
    // immediately write back this inode to the disk
    // return a reference to this file (structure) table entry
    public synchronized FileTableEntry falloc( String filename, String mode ) {
        Inode inode;
        short iNumber;

        while(true) {
            if (filename.equals("/")) {
                iNumber = 0;
            } else {
                iNumber = dir.namei(filename);
            }
            if (iNumber < 0) break;
            inode = new Inode(iNumber);
            if (mode.equals("r")) {
                if (inode.flag != 0 && inode.flag != 1) {
                    try {
                        this.wait();
                    } catch (InterruptedException e){}
                    continue;
                }
                inode.flag = 1;
                break;
            }

            if (inode.flag != 0 && inode.flag != 3) {
                if (inode.flag == 1 || inode.flag == 2) {
                    inode.flag = (short)(inode.flag + 3);
                    inode.toDisk(iNumber);
                }
                try {
                    this.wait();;
                } catch (InterruptedException e) {}
                continue;
            }

            inode.flag = 2;
            break;
        }

        if (!mode.equals("r")) {
            iNumber = dir.ialloc(filename);
            inode = new Inode();
            inode.flag = 2;
        } else {
            return null;
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
        Inode inode = e.inode;
        if (table.removeElement(e)) {
            inode.count--;
            if (inode.flag == 1 || inode.flag == 2) {
                inode.flag = 0;
            } else if (inode.flag == 4 || inode.flag == 5) {
                inode.flag = 3;
            }
            inode.toDisk(e.iNumber);
            e = null;
            this.notify();
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
