class CompressedPosition {
	private long hash; 
	private int freq; // How many times it has occurred

	public CompressedPosition(long hash, int freq) {
		this.hash = hash;
		this.freq = freq;
	}

	public CompressedPosition(long hash) {
		this(hash, 0);
	}

	public boolean equals(Object other) {
		if (!(other instanceof CompressedPosition)) {
			return false;
		}
		if (other == this) {
			return true;
		}
		
		CompressedPosition otherCompressedPosition = (CompressedPosition)other;
		return hash==otherCompressedPosition.getHash()&&freq==otherCompressedPosition.getFreq();
	}

	public long getHash() { return hash; }
	public int getFreq() { return freq; }
	public void setHash(long hash) { this.hash = hash; }
	public void setFreq(int freq) { this.freq = freq; }

	public String toString() {
		String output = "";
		output += hash+", "+freq;
		return output;
	}
}
