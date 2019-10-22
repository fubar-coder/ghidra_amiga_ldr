package hunk;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

class HunkLoadSegFile {

	private final List<HunkSegment> segments;
	
	HunkLoadSegFile() {
		segments = new ArrayList<>();
	}
	
	HunkSegment[] getSegments() {
		return segments.toArray(HunkSegment[]::new);
	}
	
	void parseBlockFile(HunkBlockFile bf) throws HunkParseError {
		
		if (bf == null) {
			return;
		}
		
		SortedMap<Long, HunkBlock> blocks = bf.getHunkBlocks();
		
		if (blocks == null || blocks.isEmpty()) {
			throw new HunkParseError("No hunk blocks found!");
		}
		
		HunkHeaderBlock hdr = (HunkHeaderBlock) blocks.get(0L);
		
		if (hdr.getHunkType() != HunkType.HUNK_HEADER) {
			throw new HunkParseError("No HEADER block found!");
		}

		List<List<HunkBlock>> first = new ArrayList<>();
		List<HunkBlock> current = null;
		
		for (HunkBlock block : blocks.values()) {
			if (block instanceof HunkHeaderBlock) {
				continue;
			}
			
			if (block.getHunkType() == HunkType.HUNK_END) {
				current = null;
			} else {
				if (!(block.isValidLoadsegBeginHunk() || block.isValidLoadsegExtraHunk())) {
					throw new HunkParseError(String.format("Invalid block found: %d", block.getHunkType().getValue()));
				}
				
				if (current == null) {
					current = new ArrayList<>();
					first.add(current);
				}
				
				current.add(block);
			}
		}
		
		List<List<HunkBlock>> second = new ArrayList<>();
		
		for (List<HunkBlock> l : first) {
			List<Long> posSeg = new ArrayList<>();
			
			long off = 0;
			
			for (HunkBlock block : l) {
				if (block.isValidLoadsegBeginHunk()) {
					posSeg.add(off);
				}
				
				off++;
			}
			
			int n = posSeg.size();
			
			if (n == 1) {
				second.add(l);
			} else if (n > 1) {
				List<HunkBlock> newList = null;
				
				for (HunkBlock block : l) {
					if (block.isValidLoadsegBeginHunk()) {
						newList = new ArrayList<>();
						newList.add(block);
					} else if (newList != null) {
						newList.add(block);
					} else {
						throw new HunkParseError("Can't split block list");
					}
				}
			}
		}
		
		if (hdr.getHunkTable().length != second.size()) {
			throw new HunkParseError("Can't match hunks to header");
		}
		
		for (List<HunkBlock> l : second) {
			HunkSegment seg = new HunkSegment();
			seg.parse(l);
			segments.add(seg);
		}
		
		int n = second.size();
		
		for (int i = 0; i < n; ++i) {
			HunkSegment seg = segments.get(i);
			seg.setSizeLongs(hdr.getHunkTable()[i]);
			segments.set(i, seg);
		}
	}
}
