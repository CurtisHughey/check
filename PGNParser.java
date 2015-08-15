import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.lang.*;

import java.nio.file.Paths;

public class PGNParser {
	/*
	0x88=10001000
	112 113 114 115 116 117 118 119
							.
							.
							.
	48 49 50 51 52 53 54 55 56 57 58 59 60 61 62 63
	32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47
	16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31
	0  1  2  3  4  5  6  7  8  9  10 11 12 13 14 15
	(For reference.  Chose to go to an 0x88 representation)
	*/

	public static void writeGames(boolean append) {

		try {
			String inputPath = Paths.get(".").toAbsolutePath().normalize().toString()+"/pgn_data/";
			String outputFileName = "data/database.txt";  // TODO eventually do appending ^^^

			File directory = new File(inputPath);
			File files[] = directory.listFiles();

			long gameCounter = 0;
			int fileCounter = 0;

			BufferedWriter out = new BufferedWriter(new FileWriter(outputFileName, append));

			for (File file : files) {
				String data = "";
				String line;
				if (file.isFile() && file.getName().indexOf('~')==-1) {
					BufferedReader input = new BufferedReader(new FileReader(file.getCanonicalPath()));
	
					while ((line = input.readLine()) != null) {
						data += line+"\n";
					}
			
					input.close();

					String elementNames[] = {"Event", "Site", "Date", "White", "Black", "Result", "WhiteElo", "BlackElo", "TimeControl", "Termination", "Moves"};  //Use this to abstract REGEX above

					String pgn_regex = "";
					for (int i = 0; i < elementNames.length-1; i++) { // -1 to not include Match
						pgn_regex += ("\\["+elementNames[i]+" \"(.*)\"\\]\n");
					}

					pgn_regex += "\n"+"(([^\n]+\n)+)"+"\n"+"\n";  // Forming a complete entry of a pgn game
					Pattern pgn_pattern = Pattern.compile(pgn_regex);		
					Matcher pgn_matcher = pgn_pattern.matcher(data);

					while (pgn_matcher.find()) {

						String date = pgn_matcher.group(3);
						String white = pgn_matcher.group(4);
						String black = pgn_matcher.group(5);
						String whiteElo = pgn_matcher.group(7);
						String blackElo = pgn_matcher.group(8);
						String timeControl = pgn_matcher.group(9);
						String moveString = pgn_matcher.group(11);

						Game game = new Game("chughey", date, white, black, whiteElo, blackElo, timeControl, moveString);

						String output = "";
						output += "[P:"+game.getUser()+"]";
						output += "[O:"+game.getOp()+"]";
						output += "[E:"+game.getElo()+"]";
						output += "[F:"+game.getOpElo()+"]";
						output += "[D:"+date+"]";  // Not bothering with a double conversion, format is fine
						output += "[C:"+game.getColor().ordinal()+"]";
						output += "[T:"+game.getTimeControl().ordinal()+"]";
						output += "[R:"+game.getResult().ordinal()+"]";
						output += "[M:";  

						ArrayList<String> moves = game.getMoves();
						for (String move : moves) {
							output += move+",";
						}
						output+="]";
						output += "[H:";  
						ArrayList<CompressedPosition> compressedPositions = game.getCompressedPositions();
						for (CompressedPosition compressedPosition : compressedPositions) {
							output += compressedPosition.getHash()+","+compressedPosition.getFreq()+"|";
						}
						output+="]\n";
	
						out.write(output);

						gameCounter++;
					}
					fileCounter++;
				}
			}

			System.out.println("Files processed: "+fileCounter);
			System.out.println("Games processed: "+gameCounter);

			out.close();
			System.out.println("Output saved!");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}



