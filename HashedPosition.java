class HashedPosition {
	long hash; 
	int freq; // How many times it has occurred

	public HashedPosition(long hash, int freq) {
		this.hash = hash;
		this.freq = freq;
	}

	public HashedPosition(long hash) {
		this(hash, 0);
	}

	public boolean equals(Object other) {
		if (!(other instanceof HashedPosition))
			return false;
		if (other == this)
			return true;
		
		HashedPosition otherHashedPosition = (HashedPosition)other;
		return hash==otherHashedPosition.hash&&freq==otherHashedPosition.freq;
	}

	public String toString() {
		String output = "";
		output += hash+", "+freq;
		return output;
	}
}
