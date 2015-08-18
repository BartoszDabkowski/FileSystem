// Xiaoyu Liang & Bartosz Dabkowski
// CSS430 Final Project
// Directory

// The "/" root directory maintains each file in a different directory entry that
// contains its file name (maximum 30 characters; 60 bytes in Java) and the
// corresponding inode number. The directory receives the maximum number of inodes
// to be created, (i.e., thus the max. number of files to be created) and keeps track
// of which inode numbers are in use. Since the directory itself is considered as a
// file, its contents are maintained by an inode, specifically inode 0. This can be
// located in the first 32 bytes of the disk block 1.
public class Directory {
    private static int maxChars = 30; // max characters of each file name

    // Directory entries
    private int fsizes[];                   // each element stores a different file size.
    private char fnames[][];                // each element stores a different file name.

    public Directory( int maxInumber ) {    // directory constructor
        fsizes = new int[maxInumber];       // maxInumber = max files
        for ( int i = 0; i < maxInumber; i++ )
            fsizes[i] = 0;                  // all file size initialized to 0
        fnames = new char[maxInumber][maxChars];
        String root = "/";                  // entry(inode) 0 is "/"
        fsizes[0] = root.length( );         // fsize[0] is the size of "/".
        root.getChars( 0, fsizes[0], fnames[0], 0 ); // fnames[0] includes "/"
    }

    //---------------------------------------------------------------------------
    // assumes data[] received directory information from disk
    // initializes the Directory instance with this data[]
    // copied directly from online slides
    public int bytes2directory( byte data[] ) {
        int offset = 0;
        for (int i = 0; i < fsizes.length; i++) {
            fsizes[i] = SysLib.bytes2int(data, offset);
            offset += 4;
        }
        for (int i = 0; i < fnames.length; i++) {
            String string = new String(data, offset, maxChars * 2);
            string.getChars(0, fsizes[i], fnames[i], 0);
            offset = offset + maxChars * 2;
        }
        return offset;
    }

    //---------------------------------------------------------------------------
    // converts and return Directory information into a plain byte array
    // this byte array will be written back to disk
    // note: only meaningfull directory information should be converted
    // into bytes.
    public byte[] directory2bytes( ) {
        byte[] data = new byte[fsizes.length * 4 + fnames.length * maxChars * 2];
        int index = 0;
        for (int i = 0; i < fsizes.length; i++) {
            SysLib.int2bytes(fsizes[i], data, index);
            index += 4;
        }

        for (int i = 0; i < fnames.length; i++) {
            String fName = new String(fnames[i], 0, fsizes[i]);
            byte[] temp = fName.getBytes();
            System.arraycopy(temp, 0, data, index, temp.length);
            index = index + maxChars * 2;
        }

        return data;
    }

    //---------------------------------------------------------------------------
    // filename is the one of a file to be created.
    // allocates a new inode number for this filename
    public short ialloc( String filename ) {
        for (int i = 0; i < fsizes.length; i++) {
            if (fsizes[i] == 0)
            {
                if (filename.length() < maxChars) {
                    fsizes[i] = filename.length();
                } else {
                    fsizes[i] = maxChars;
                }
                filename.getChars(0, fsizes[i], fnames[i], 0);
                return (short)i;
            }
        }
        return -1;
    }

    //---------------------------------------------------------------------------
    // deallocates this inumber (inode number)
    // the corresponding file will be deleted.
    public boolean ifree( short iNumber ) {
        if(fsizes[iNumber] > 0) {
            fsizes[iNumber] = 0;
            for (int i = 0; i < fsizes[iNumber]; i++) {
                fnames[iNumber][i] = (char) 0;
            }
            return true;
        }
        return false;
    }

    //---------------------------------------------------------------------------
    // returns the inumber corresponding to this filename
    public short namei( String filename ) {
        for (int i = 0; i < fsizes.length; i++) {
            if (fsizes[i] > 0) {
                String fName = new String(fnames[i], 0, fsizes[i]);
                if (fName.equals(filename)) {
                    return (short)i;
                }
            }
        }
        return -1;
    }
}
