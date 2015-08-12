
import java.io.*;

public class Zobrist {

	public static long[][] zobristTable;

	static {
		makeZobristTable();
	}

	private static void makeZobristTable() {
		long[] randomNumbers = getRandomNumbers();  // Gets 774 random numbers.  Abstract out to get arbitrary.  At least should assert that I have enough
		int counter = 0;
		zobristTable = new long[13][120];  // Possibly will change to fill in random numbers on an 0x88 board
		for (int i = 1; i <= 12; i++) { 
			for (int j = 0; j < 120; j++) {
				if ((j&0x88) == 0)  // Hacky
					zobristTable[i][j] = randomNumbers[counter++];
			}
		}

		for (int j = 8; j <= 13; j++)  // Figure this out ^^^^ not handling turn correctly.  zobristTable[0][8] and zobristTable[0][9] both used for turn
			zobristTable[0][j] = randomNumbers[counter++];
	}

	private static long[] getRandomNumbers() {
		long randomNumbers[] = new long[774];  // Might have to change in the future to accommodate other state changes, but low probability.  Ha, just did, for turn and repeated positions
		try {
			BufferedReader randomInput = new BufferedReader(new FileReader("randomGenerator/randomNumbers.txt"));
			String line = "";
			int i = 0;
			while ((line = randomInput.readLine()) != null && i < 774) {
				String nibble = line.substring(0,1);
				long num = Long.parseLong(line.substring(1), 16);
				long nibbleNum = Long.parseLong(nibble, 16) << 60;  // Repairing it.  This is so stupid
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
			if ((i & 0x88) == 0)  // Again, hacky ^^.
				hash ^= zobristTable[board[i]][i];
			//else if (10 <= i && i <= 13) // To accommodate i=10-13 (repeated positions, up to 4)
			//	hash ^= zobristTable[0][i]; // This is seriously hacky, but I guess ok, as long as OOB? ^^
		}
		hash ^= zobristTable[0][8+board[8]];  // so either zobristTable[0][8] or zobristTable[0][9]
		return hash;
	}

}
