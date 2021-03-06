/**
 * Distribution License:
 * JSword is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License, version 2.1 as published by
 * the Free Software Foundation. This program is distributed in the hope
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The License is available on the internet at:
 *       http://www.gnu.org/copyleft/lgpl.html
 * or by writing to:
 *      Free Software Foundation, Inc.
 *      59 Temple Place - Suite 330
 *      Boston, MA 02111-1307, USA
 *
 * Copyright: 2005
 *     The copyright to this program is held by it's authors.
 *
 * ID: $Id$
 */
package org.crosswire.jsword.book.sword;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.crosswire.common.compress.CompressorType;
import org.crosswire.common.util.IOUtil;
import org.crosswire.common.util.Logger;
import org.crosswire.jsword.book.BookException;
import org.crosswire.jsword.book.sword.state.OpenFileStateManager;
import org.crosswire.jsword.book.sword.state.ZVerseBackendState;
import org.crosswire.jsword.passage.Key;
import org.crosswire.jsword.passage.KeyUtil;
import org.crosswire.jsword.passage.Verse;
import org.crosswire.jsword.versification.Testament;
import org.crosswire.jsword.versification.Versification;
import org.crosswire.jsword.versification.system.Versifications;

/**
 * A backend to read compressed data verse based files. While the text file
 * contains data compressed with ZIP or LZSS, it cannot be uncompressed using a
 * stand alone zip utility, such as WinZip or gzip. The reason for this is that
 * the data file is a concatenation of blocks of compressed data.
 * 
 * <p>
 * The blocks can either be "b", book (aka testament); "c", chapter or "v",
 * verse. The choice is a matter of trade offs. The program needs to uncompress
 * a block into memory. Having it at the book level is very memory expensive.
 * Having it at the verse level is very disk expensive, but takes the least
 * amount of memory. The most common is chapter.
 * </p>
 * 
 * <p>
 * In order to find the data in the text file, we need to find the block. The
 * first index (comp) is used for this. Each verse is indexed to a tuple (block
 * number, verse start, verse size). This data allows us to find the correct
 * block, and to extract the verse from the uncompressed block, but it does not
 * help us uncompress the block.
 * </p>
 * 
 * <p>
 * Once the block is known, then the next index (idx) gives the location of the
 * compressed block, its compressed size and its uncompressed size.
 * </p>
 * 
 * <p>
 * There are 3 files for each testament, 2 (comp and idx) are indexes into the
 * third (text) which contains the data. The key into each index is the verse
 * index within that testament, which is determined by book, chapter and verse
 * of that key.
 * </p>
 * 
 * <p>
 * All numbers are stored 2-complement, little endian.
 * </p>
 * <p>
 * Then proceed as follows, at all times working on the set of files for the
 * testament in question:
 * </p>
 * 
 * <pre>
 * in the comp file, seek to the index * 10
 * read 10 bytes.
 * the block-index is the first 4 bytes (32-bit number)
 * the next bytes are the verse offset and length of the uncompressed block.
 * in the idx file seek to block-index * 12
 * read 12 bytes
 * the text-block-index is the first 4 bytes
 * the data-size is the next 4 bytes
 * the uncompressed-size is the next 4 bytes
 * in the text file seek to the text-block-index
 * read data-size bytes
 * decipher them if they are encrypted
 * unGZIP them into a byte uncompressed-size
 * </pre>
 * 
 * @see gnu.lgpl.License for license details.<br>
 *      The copyright to this program is held by it's authors.
 * @author Joe Walker [joe at eireneh dot com]
 */
public class ZVerseBackend extends AbstractBackend<ZVerseBackendState> {
    /**
     * Simple ctor
     */
    public ZVerseBackend(SwordBookMetaData sbmd, BlockType blockType) {
        super(sbmd);
        this.blockType = blockType;
    }

    /* This method assumes single keeps. It is the responsibility of the caller to provide the iteration. 
     * 
     * FIXME: this could be refactored to push the iterations down, but no performance benefit would be gained since we have a manager that keeps the file accesses open
     * (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.AbstractBackend#contains(org.crosswire.jsword.passage.Key)
     */
    @Override
    public boolean contains(Key key) {
        ZVerseBackendState rafBook = null;
        try {
            rafBook = OpenFileStateManager.getZVerseBackendState(getBookMetaData(), blockType);

            String v11nName = getBookMetaData().getProperty(ConfigEntryType.VERSIFICATION).toString();
            Versification v11n = Versifications.instance().getVersification(v11nName);
            Verse verse = KeyUtil.getVerse(key, v11n);

            int index = v11n.getOrdinal(verse);
            Testament testament = v11n.getTestament(index);
            index = v11n.getTestamentOrdinal(index);

            RandomAccessFile compRaf = testament == Testament.NEW ? rafBook.getNtCompRaf() : rafBook.getOtCompRaf();

            // If Bible does not contain the desired testament, then false
            if (compRaf == null) {
                return false;
            }

            // 10 because the index is 10 bytes long for each verse
            byte[] temp = SwordUtil.readRAF(compRaf, 1L * index * COMP_ENTRY_SIZE, COMP_ENTRY_SIZE);

            // If the Bible does not contain the desired verse, return nothing.
            // Some Bibles have different versification, so the requested verse
            // may not exist.
            if (temp == null || temp.length == 0) {
                return false;
            }

            // The data is little endian - extract the blockNum, verseStart and
            // verseSize
            int verseSize = SwordUtil.decodeLittleEndian16(temp, 8);

            return verseSize > 0;

        } catch (IOException e) {
            return false;
        } catch (BookException e) {
            // FIXME(CJB): fail silently as before, but i don't think this is
            // correct behaviour - would cause API changes
            log.fatal("Unable to ascertain key validity", e);
            return false;
        } finally {
            IOUtil.close(rafBook);
        }
    }

    public ZVerseBackendState initState() throws BookException {
        return OpenFileStateManager.getZVerseBackendState(getBookMetaData(), blockType);
    }

    public String readRawContent(ZVerseBackendState rafBook, Key key, String keyName) throws IOException {

        SwordBookMetaData bookMetaData = getBookMetaData();
        final String charset = bookMetaData.getBookCharset();
        final String compressType = (String) bookMetaData.getProperty(ConfigEntryType.COMPRESS_TYPE);

        final String v11nName = getBookMetaData().getProperty(ConfigEntryType.VERSIFICATION).toString();
        final Versification v11n = Versifications.instance().getVersification(v11nName);
        Verse verse = KeyUtil.getVerse(key, v11n);

        int index = v11n.getOrdinal(verse);
        final Testament testament = v11n.getTestament(index);
        index = v11n.getTestamentOrdinal(index);
        final RandomAccessFile compRaf;
        final RandomAccessFile idxRaf;
        final RandomAccessFile textRaf;

        if (testament == Testament.OLD) {
            compRaf = rafBook.getOtCompRaf();
            idxRaf = rafBook.getOtIdxRaf();
            textRaf = rafBook.getOtTextRaf();
        } else {
            compRaf = rafBook.getNtCompRaf();
            idxRaf = rafBook.getNtIdxRaf();
            textRaf = rafBook.getNtTextRaf();
        }

        // If Bible does not contain the desired testament, return nothing.
        if (compRaf == null) {
            return "";
        }

        // 10 because the index is 10 bytes long for each verse
        byte[] temp = SwordUtil.readRAF(compRaf, 1L * index * COMP_ENTRY_SIZE, COMP_ENTRY_SIZE);

        // If the Bible does not contain the desired verse, return nothing.
        // Some Bibles have different versification, so the requested verse
        // may not exist.
        if (temp == null || temp.length == 0) {
            return "";
        }

        // The data is little endian - extract the blockNum, verseStart
        // and
        // verseSize
        final long blockNum = SwordUtil.decodeLittleEndian32(temp, 0);
        final int verseStart = SwordUtil.decodeLittleEndian32(temp, 4);
        final int verseSize = SwordUtil.decodeLittleEndian16(temp, 8);

        // Can we get the data from the cache
        byte[] uncompressed = null;
        if (blockNum == rafBook.getLastBlockNum() && testament == rafBook.getLastTestament()) {
            uncompressed = rafBook.getLastUncompressed();
        } else {
            // Then seek using this index into the idx file
            temp = SwordUtil.readRAF(idxRaf, blockNum * IDX_ENTRY_SIZE, IDX_ENTRY_SIZE);
            if (temp == null || temp.length == 0) {
                return "";
            }

            final int blockStart = SwordUtil.decodeLittleEndian32(temp, 0);
            final int blockSize = SwordUtil.decodeLittleEndian32(temp, 4);
            final int uncompressedSize = SwordUtil.decodeLittleEndian32(temp, 8);

            // Read from the data file.
            final byte[] data = SwordUtil.readRAF(textRaf, blockStart, blockSize);

            decipher(data);

            uncompressed = CompressorType.fromString(compressType).getCompressor(data).uncompress(uncompressedSize).toByteArray();

            // cache the uncompressed data for next time
            rafBook.setLastBlockNum(blockNum);
            rafBook.setLastTestament(testament);
            rafBook.setLastUncompressed(uncompressed);
        }

        // and cut out the required section.
        final byte[] chopped = new byte[verseSize];
        System.arraycopy(uncompressed, verseStart, chopped, 0, verseSize);

        return SwordUtil.decode(keyName, chopped, charset);

    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.AbstractBackend#setAliasKey(org.crosswire.jsword.passage.Key, org.crosswire.jsword.passage.Key)
     */
    public void setAliasKey(ZVerseBackendState rafBook, Key alias, Key source) throws IOException {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.AbstractBackend#setRawText(org.crosswire.jsword.passage.Key, java.lang.String)
     */
    public void setRawText(ZVerseBackendState rafBook, Key key, String text) throws BookException, IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Whether the book is blocked by Book, Chapter or Verse.
     */
    private final BlockType blockType;

    /**
     * How many bytes in the comp index?
     */
    private static final int COMP_ENTRY_SIZE = 10;

    /**
     * How many bytes in the idx index?
     */
    private static final int IDX_ENTRY_SIZE = 12;

    /**
     * The log stream
     */
    private static final Logger log = Logger.getLogger(ZVerseBackend.class);
}
