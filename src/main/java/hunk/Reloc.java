package hunk;

public class Reloc {

	private final int offset;
	private final int width;
	private final int addend;
	
	Reloc(int offset) {
		this.offset = offset;
		this.width = 4;
		this.addend = 0;
	}

	public final int getOffset() {
		return offset;
	}

	public final int getWidth() {
		return width;
	}

	public final int getAddend() {
		return addend;
	}
	
}
