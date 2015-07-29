/**
 * Created by Xiaoyu on 7/28/15.
 */
import java.util.*;

public class FileTable {
    private Vector table;         // the actual entity of this file table
    private Directory dir;        // the root directory

    public FileTable( Directory directory ) { // constructor
        table = new Vector( );     // instantiate a file (structure) table
        dir = directory;           // receive a reference to the Director
    }                             // from the file system

    // major public methods
    public synchronized FileTableEntry falloc( String filename, String mode ) {
        // allocate a new file (structure) table entry for this file name
        // allocate/retrieve and register the corresponding inode using dir
        // increment this inode's count
        // immediately write back this inode to the disk
        // return a reference to this file (structure) table entry
        Inode inode;
        short iNumber;
        if (mode.equals("r")) {
            iNumber = dir.namei(filename);
        } else {
            iNumber = dir.ialloc(filename);
        }

        inode = new Inode(iNumber);
        inode.count++;
        inode.toDisk(iNumber);
        FileTableEntry tableEntry = new FileTableEntry(inode, iNumber, mode);
        table.addElement(tableEntry);
        return tableEntry;
    }

    public synchronized boolean ffree( FileTableEntry e ) {
        // receive a file table entry reference
        // save the corresponding inode to the disk
        // free this file table entry.
        // return true if this file table entry found in my table
        Inode inode = e.inode;
        if (table.removeElement(e)) {
            inode.count--;
            inode.toDisk(e.iNumber);
            return true;
        }
        return false;
    }

    public synchronized boolean fempty( ) {
        return table.isEmpty( );  // return if table is empty
    }                            // should be called before starting a format
}
