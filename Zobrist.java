import java.io.*;

public class Zobrist {

	private static final int numRandomNumbers = 770;

	public static long[][] zobristTable;

	static {
		makeZobristTable();
	}

	// Populates the random numbers in the zobrist table
	private static void makeZobristTable() {
		long[] randomNumbers = getRandomNumbers();
		assert randomNumbers.length == numRandomNumbers : "There were: "+randomNumbers.length+" numbers, not: "+numRandomNumbers+" as required!";
		
		int counter = 0;
		zobristTable = new long[13][120];
		for (int i = 1; i <= 12; i++) { 
			for (int j = 0; j < 120; j++) {
				if ((j&0x88) == 0)  {
					zobristTable[i][j] = randomNumbers[counter++];
				}
			}
		}

		for (int j = 8; j <= 9; j++) {
			zobristTable[0][j] = randomNumbers[counter++];  // These are used for turns
		}
	}

	// Reads in the random numbers
	private static long[] getRandomNumbers() {
		long randomNumbers[] = new long[numRandomNumbers];  
		try {
			BufferedReader randomInput = new BufferedReader(new FileReader("randomGenerator/randomNumbers.txt"));
			String line = "";
			int i = 0;
			while ((line = randomInput.readLine()) != null && i < numRandomNumbers) {
				assert line.length() == 16 : "Random number: "+line+" is the wrong size!";
				String nibble = line.substring(0,1);
				long num = Long.parseLong(line.substring(1), 16);
				long nibbleNum = Long.parseLong(nibble, 16) << 60;  // Repairing it.
				num |= nibbleNum; 
				randomNumbers[i++] = num;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return randomNumbers;
	}

	// Takes in a 120 length board and hashes it.  Used for initialization.  Need to include repeated positions (actually, maybe make that another responsibility)
	public static long makeHash(int board[]) {
		long hash = 0;
		for (int i = 0; i < board.length; i++) {
			if ((i & 0x88) == 0) {
				hash ^= zobristTable[board[i]][i];
			}
		}
		hash ^= zobristTable[0][8+board[8]];  // so either zobristTable[0][8] or zobristTable[0][9]
		return hash;
	}
}
