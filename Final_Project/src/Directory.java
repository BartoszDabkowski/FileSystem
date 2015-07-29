/**
 * Created by Michael on 7/23/2015.
 */
public class Directory {
    private static int maxChars = 30; // max characters of each file name

    // Directory entries
    private int fsizes[];        // each element stores a different file size.
    private char fnames[][];    // each element stores a different file name.

    public Directory( int maxInumber ) { // directory constructor
        fsizes = new int[maxInumber];     // maxInumber = max files
        for ( int i = 0; i < maxInumber; i++ )
            fsizes[i] = 0;                 // all file size initialized to 0
        fnames = new char[maxInumber][maxChars];
        String root = "/";                // entry(inode) 0 is "/"
        fsizes[0] = root.length( );        // fsize[0] is the size of "/".
        root.getChars( 0, fsizes[0], fnames[0], 0 ); // fnames[0] includes "/"
    }

    public int bytes2directory( byte data[] ) {
        // assumes data[] received directory information from disk
        // initializes the Directory instance with this data[]


        return -1;
    }

    public byte[] directory2bytes( ) {
        // converts and return Directory information into a plain byte array
        // this byte array will be written back to disk
        // note: only meaningfull directory information should be converted
        // into bytes.
        return null;
    }

    public short ialloc( String filename ) {
        // filename is the one of a file to be created.
        // allocates a new inode number for this filename
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

    public boolean ifree( short iNumber ) {
        // deallocates this inumber (inode number)
        // the corresponding file will be deleted.
        if(fsizes[iNumber] > 0) {
            fsizes[iNumber] = 0;
            for (int i = 0; i < fsizes[iNumber]; i++) {
                fnames[iNumber][i] = (char) 0;
            }
            return true;
        }
        return false;
    }

    public short namei( String filename ) {
        // returns the inumber corresponding to this filename
        for (int i = 0; i < fsizes.length; i++) {
            if (fsizes[i] > 0) {
                String target = new String(fnames[i], 0, fsizes[i]);
                if (target.equals(filename)) {
                    return (short)i;
                }
            }
        }
        return -1;
    }
}
