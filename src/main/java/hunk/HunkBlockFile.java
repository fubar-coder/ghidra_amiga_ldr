package hunk;

import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;

import ghidra.app.util.bin.BinaryReader;

public class HunkBlockFile {
	private SortedMap<Long, HunkBlock> blocksList;
	private HunkBlockType blockType;
	private BinaryReader reader;
	
	public static boolean isHunkBlockFile(BinaryReader reader) {
		HunkBlockFile hbf;
		try {
			hbf = new HunkBlockFile(reader);
			return hbf.getHunkBlockType() != HunkBlockType.TYPE_UNKNOWN;
		} catch (HunkParseError e) {
			return false;
		}
	}
	
	public HunkBlockFile(BinaryReader reader) throws HunkParseError {
		blocksList = new TreeMap<>();
		blockType = peekType(reader);
		this.reader = reader;
	}
	
	public SortedMap<Long, HunkBlock> getHunkBlocks() {
		return blocksList;
	}
	
	public void load() throws HunkParseError {
		try {
			long pos = reader.getPointerIndex();
			while (pos + 4 <= reader.length()) {
				int tag = reader.readNextInt();

				HunkBlock block = HunkBlock.fromHunkType(HunkType.fromInteger(tag & HunkType.HUNK_TYPE_MASK), reader);
				
				if (block == null) {
					throw new HunkParseError(String.format("Unsupported hunk type: %04d", tag & HunkType.HUNK_TYPE_MASK));
				}

				blocksList.put(pos, block);
				
				pos = reader.getPointerIndex();
			}
		} catch (IOException e) {
			throw new HunkParseError(e);
		}
	}
	
	public HunkBlockType getHunkBlockType() {
		return blockType;
	}
	
	private static HunkBlockType peekType(BinaryReader reader) {
		long pos = reader.getPointerIndex();
		
		try {
			int tag = reader.readNextInt();
			reader.setPointerIndex(pos);
			
			HunkType blkId = HunkType.fromInteger(tag);
			return mapHunkTypeToHunkBlockType(blkId);
		} catch (IOException e) {
			reader.setPointerIndex(pos);
			return HunkBlockType.TYPE_UNKNOWN;
		}
	}
	
	private static HunkBlockType mapHunkTypeToHunkBlockType(HunkType blkId) {
		if (blkId == HunkType.HUNK_HEADER) {
			return HunkBlockType.TYPE_LOADSEG;
		} else if (blkId == HunkType.HUNK_UNIT) {
			return HunkBlockType.TYPE_UNIT;
		} else if (blkId == HunkType.HUNK_LIB) {
			return HunkBlockType.TYPE_LIB;
		} else {
			return HunkBlockType.TYPE_UNKNOWN;
		}
	}
}
