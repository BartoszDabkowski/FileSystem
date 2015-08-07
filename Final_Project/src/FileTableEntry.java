// Xiaoyu Liang & Bartosz Dabkowski
// CSS430 Final Project
// FileTableEntry

// Copied directly from project description, has the following functions:
// The user thread allocates a new entry of the user file descriptor table
// in its TCB. This entry number itself becomes a file descriptor number.
// The entry maintains a reference to a file (structure) table entry.
// The user thread then requests the file system to allocate a new entry
// of the system-maintained file (structure) table. This entry includes
// the seek pointer of this file, a reference to the inode corresponding
// to the file, the inode number, the count to maintain #threads sharing
// this file (structure) table, and the access mode. The seek pointer is
// set to the front or the tail of this file depending on the file access mode.
// The file system locates the corresponding inode and records it in this
// file (structure) table entry.
// The user thread finally registers a reference to this file (structure)
// table entry in its file descriptor table entry of the TCB.

public class FileTableEntry {
    public int seekPtr;                 //    a file seek pointer
    public final Inode inode;           //    a reference to its inode
    public final short iNumber;         //    this inode number
    public int count;                   //    # threads sharing this entry
    public final String mode;           //    "r", "w", "w+", or "a"


    public FileTableEntry ( Inode i, short inumber, String m ) {
        seekPtr = 0;             // the seek pointer is set to the file top
        inode = i;
        iNumber = inumber;
        count = 1;               // at least on thread is using this entry
        mode = m;                // once access mode is set, it never changes
        if ( mode.compareTo( "a" ) == 0 ) // if mode is append,
            seekPtr = inode.length;        // seekPtr points to the end of file
    }
}
