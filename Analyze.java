import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.lang.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Analyze {
	public static void main(String args[]) {

		if (args.length > 0) {
			if (args[0].equals("-L")) {  // Overwrite
				PGNParser.writeGames(false);
				return;
			}
			if (args[0].equals("-l")) {
				PGNParser.writeGames(true);  // append
				return;
			}
			else {  // Overwrite and run, will develop more
				PGNParser.writeGames(false);
			}
			// I should do something that deletes the pgn files after, they aren't useful

		}
				

		// Input to get all the games
		try {
			BufferedReader in = new BufferedReader(new FileReader("data/database.txt"));

			ArrayList<Game> whiteGames = new ArrayList<Game>();
			ArrayList<Game> blackGames = new ArrayList<Game>();			

			String game_regex = "\\[P:(.*)\\]\\[O:(.*)\\]\\[E:(.*)\\]\\[F:(.*)\\]\\[D:(.*)\\]\\[C:(.*)\\]\\[T:(.*)\\]\\[R:(.*)\\]\\[M:(.*)\\]\\[H:(.*)\\]";
			Pattern game_pattern = Pattern.compile(game_regex);

			String line = "";
			while ((line = in.readLine()) != null) {
				Matcher game_matcher = game_pattern.matcher(line);

				if (game_matcher.find()) {

					String user = game_matcher.group(1);
					String op = game_matcher.group(2);
					String elo = game_matcher.group(3);
					String opElo = game_matcher.group(4);
					String date = game_matcher.group(5);
					String color = game_matcher.group(6);
					String timeControl = game_matcher.group(7);
					String result = game_matcher.group(8);
				
					ArrayList<String> moves = new ArrayList<String>(Arrays.asList(game_matcher.group(9).split(",")));

					String hashData[] = game_matcher.group(10).split("\\|");

					ArrayList<CompressedPosition> compressedPositions = new ArrayList<CompressedPosition>();
					for (String hashDatum : hashData) {
						String hashAndFreq[] = hashDatum.split(",");
						compressedPositions.add(new CompressedPosition(Long.parseLong(hashAndFreq[0]), Integer.parseInt(hashAndFreq[1])));
					}
					
					Game game = new Game(user, op, Integer.parseInt(elo), Integer.parseInt(opElo), date,
								Integer.parseInt(color), Integer.parseInt(timeControl), Integer.parseInt(result),
								moves, compressedPositions);
					
					if (game.getColor() == Game.Color.WHITE)					
						whiteGames.add(game);
					else
						blackGames.add(game);
				}
			}
			in.close();

			Scanner reader = new Scanner(System.in);

			int startBoard[] = {4,2,3,5,6,3,2,4,0,0,0,0,0,0,0,0, // board[8] = turn, board[9] = how many times the position has been repeated
								1,1,1,1,1,1,1,1,0,0,0,0,0,0,0,0,
								0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
								0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
								0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
								0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
								7,7,7,7,7,7,7,7,0,0,0,0,0,0,0,0,
								10,8,9,11,12,9,8,10};

			long startHash = Zobrist.makeHash(startBoard);

			System.out.println("Welcome to Check!");
			displayGuide();

			boolean keepGoing = true;

			while (keepGoing) {
				System.out.println("Starting search");

				int board[] = startBoard;
				long hash = startHash;
		
				ArrayList<CompressedPosition> compressedPositions = new ArrayList<CompressedPosition>();
				compressedPositions.add(new CompressedPosition(startHash, 1));
				HashMap<Long,Integer> positionFreqs = new HashMap<Long,Integer>();
				positionFreqs.put(startHash, 1);				

				ArrayList<Game> filteredGames = new ArrayList<Game>();

				if (keepGoing == false)
					break;

				while (true) {
					System.out.print("What color do you want to be? white/black: ");
					String color = reader.nextLine();
					if (color.equals("white")) {
						filteredGames = whiteGames;
						break;
					}
					else if (color.equals("black")) {
						filteredGames = blackGames;
						break;
					}
					else {
						System.out.println("Input not recognized.  Please try again.");
					}
				}

				while (true) {
					System.out.print("---------\nInput command: ");
					String command = reader.nextLine();
				
					String tokens[] = command.split(" ");  // Might be worth just moving this to regex

					try {
						if (tokens[0].equals("end")) {
							break; // Pops out of this loop and goes to new search at top of outer loop
						}
						else if (tokens[0].equals("quit")) {
							keepGoing = false;
							break;
						}
						else if (tokens[0].equals("move")) {
							Game.makeMove(board, compressedPositions, positionFreqs, tokens[1]); 
						}
						else if (tokens[0].equals("range")) {
							if (tokens[1].equals("elo")) {
								filteredGames = rangeElo(filteredGames, Integer.parseInt(tokens[2]), Integer.parseInt(tokens[3]));
							}
							else if (tokens[1].equals("opelo")) {
								filteredGames = rangeOpElo(filteredGames, Integer.parseInt(tokens[2]), Integer.parseInt(tokens[3]));
							}
							else if (tokens[1].equals("date")) {
								SimpleDateFormat sdf = Game.getSdf();
								Date low = sdf.parse(tokens[2]);
								Date high = sdf.parse(tokens[3]);
								filteredGames = rangeDate(filteredGames, low, high);
							}
							else {
								System.out.println("Failed to parse range command");
							}
						}
						else if (tokens[0].equals("filter")) {
							if (tokens[1].equals("opponent")) {
								filteredGames = filterOp(filteredGames, tokens[2]);
							}
							else if (tokens[1].equals("time")) {
								filteredGames = filterTimeControl(filteredGames, tokens[2]);
							}
							else {
								System.out.println("Failed to parse filter command");
							}
						}
						else {
							System.out.println("Command not recognized, please try again");
							continue;
						}
					}
					catch (Exception e) {
						e.printStackTrace();
						System.out.println("Command failed to parse, please try again");
						continue;
					}

					if (filteredGames == null) {
						System.out.println("No games found!");
						continue;
					}

					filteredGames = findGamesMatchingBoard(filteredGames, board, positionFreqs.get(Zobrist.makeHash(board)));  // Gets the current amount of frequencies// Need to be keeping track the frequencies of this frikkin board too

					System.out.println(getStats(filteredGames));
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void displayGuide() {
		// Really need to do something that automatically wraps around ^^
		System.out.println("Check allows you to easily search and filter through your games");
		System.out.println("List of commands (do not include colon):");
		System.out.println("end: finish line of study and start a new line of study (old one is erased!)\n");
		System.out.println("range variable low high: filters a number in a range.  E.g. \"filter elo 1000 1200\"\n\treturns all games where your elo was between 1000 and 1200.  variable can be elo,\n\topelo (opponent's elo), or date.  Date must be specified in yyyy.mm.dd format.\n");
		System.out.println("filter variable value: filters for occurences of variable having a value.  E.g. \"filter time blitz\"\n\tgives all blitz games.Full range of options of variable are: opponent (value is\n\tthen the name), time (value is then bullet, blitz, standard)\n");   
		System.out.println("move value: makes a move in the current position given a valid algebraic move as\n\tthe value.  E.g. \"move Nxf3\".  Checks and such need not be specified.\n");
		System.out.println("undo move: undoes the prior move\n");
		System.out.println("quit: exits the program\n");
		System.out.println("help: displays this guide\n");
	}

	// Honestly, move all of these filtering functions into a container for games, to enable easy chaining

	private static ArrayList<Game> findGamesMatchingBoard(ArrayList<Game> games, int[] board, Integer freq) {
		return findGamesMatchingCompressedPosition(games, Zobrist.makeHash(board), freq);  // This will need to get updated if I want to not have to take care of repeated positions in makeHash
	}

	private static ArrayList<Game> findGamesMatchingCompressedPosition(ArrayList<Game> games, long hash, Integer freq) {
		if (freq == null) {
			return null;
		}
		ArrayList<Game> matchingGames = new ArrayList<Game>();
		CompressedPosition CompressedPosition = new CompressedPosition(hash, freq);
		for (Game game : games) {
			if (game.compressedPositions.contains(CompressedPosition))  { // Possible that since I overrode equals I should also override hash?
				matchingGames.add(game);
			}
		}
		return matchingGames;
	}

	// Could also do some sort of aggregation for each thing as well, rather than using filter over and over again
	private static ArrayList<Game> filterResult(ArrayList<Game> games, Game.Result result) {  // Add Won and Drawn, etc. ^^
		ArrayList<Game> matchingGames = new ArrayList<Game>();
		for (Game game : games) {
			if (game.getResult() == result) {
				matchingGames.add(game);
			}
		}
		return matchingGames;
	}

	private static ArrayList<Game> filterOp(ArrayList<Game> games, String op) {  // Filters by the opponent
		ArrayList<Game> matchingGames = new ArrayList<Game>();
		for (Game game : games) {
			if (game.getOp().equals(op)) {
				matchingGames.add(game);
			}
		}
		return matchingGames;
	}

	private static ArrayList<Game> filterColor(ArrayList<Game> games, Game.Color color) {  // Filters by the color of the user
		ArrayList<Game> matchingGames = new ArrayList<Game>();
		for (Game game : games) {
			if (game.getColor() == color) {
				matchingGames.add(game);
			}
		}
		return matchingGames;
	}

	private static ArrayList<Game> rangeElo(ArrayList<Game> games, int low, int high) {  // Filters by the ELO range of the user
		ArrayList<Game> matchingGames = new ArrayList<Game>();
		for (Game game : games) {
			int elo = game.getElo();
			if (low <= elo && elo <= high) {
				matchingGames.add(game);
			}
		}
		return matchingGames;
	}

	private static ArrayList<Game> rangeOpElo(ArrayList<Game> games, int low, int high) {  // Filters by the ELO range of the user
		ArrayList<Game> matchingGames = new ArrayList<Game>();
		for (Game game : games) {
			int opElo = game.getOpElo();
			if (low <= opElo && opElo <= high) {
				matchingGames.add(game);
			}
		}
		return matchingGames;
	}

	private static ArrayList<Game> filterTimeControl(ArrayList<Game> games, String timeControl) {
		Game.TimeControl numericTimeControl;
		if (timeControl.equals("bullet")) {
			numericTimeControl = Game.TimeControl.BULLET;
		}
		else if (timeControl.equals("blitz")) {
			numericTimeControl = Game.TimeControl.BLITZ;
		}
		else if (timeControl.equals("standard")) {
			numericTimeControl = Game.TimeControl.STANDARD;
		}
		else {
			System.out.println("Invalid time control, please try again");
			return null;
		}
		ArrayList<Game> matchingGames = new ArrayList<Game>();
		for (Game game : games) {
			if (game.getTimeControl() == numericTimeControl) {
				matchingGames.add(game);
			}
		}
		return matchingGames;
	}

	private static ArrayList<Game> rangeDate(ArrayList<Game> games, Date start, Date end) { // Change int to JODA/Datetime
		ArrayList<Game> matchingGames = new ArrayList<Game>();
		for (Game game : games) {
			long gameDate = game.getDate().getTime();
			if (start.getTime() <= gameDate && gameDate <= end.getTime()) {
				matchingGames.add(game);
			}
		}
		return matchingGames;		
	}

	// Presumably, we have already filtered for a hash, this could be optional
	private static Statistics getStats(ArrayList<Game> games) {

		double wins = 0;
		double draws = 0;
		double losses = 0;
		int totalGames = games.size();  // should I just increment in the loop below? ^^.  Check if null ^^^

		for (Game game : games) {
			switch (game.getResult()) {
				case WIN:
					wins++;
					break;
				case DRAW:
					draws++;
					break;
				case LOSS:
					losses++;
					break;
				default:
					break;
			}
		}
		return new Statistics(wins/totalGames, draws/totalGames, losses/totalGames, totalGames);
	}

	/*Make filtering functions for:
		Won/Lost/Drawn games
		Date ranges
		White/Black
		Self and opponent ELO's.
		Time control
		Other?

	example raw data to filter on
	[Event "Live Chess"]
	[Site "Chess.com"]
	[Date "2015.06.28"]
	[White "TheBlazedAssassin"]
	[Black "chughey"]
	[Result "1-0"]
	[WhiteElo "2025"]
	[BlackElo "2053"]
	[TimeControl "1|0"]
	[Termination "TheBlazedAssassin won by checkmate"]*/
}

// Just really basic statistics right now.  Would be nice to display results over time, compare with various ELO's, etc, but for the moment we can just use filter
class Statistics {
	double winPerc;
	double drawPerc;
	double lossPerc;
	int totalGames;

	public Statistics(double winPerc, double drawPerc, double lossPerc, int totalGames) {
		this.winPerc = winPerc;
		this.drawPerc = drawPerc;
		this.lossPerc = lossPerc;
		this.totalGames = totalGames;
	}

	public String toString() {

		String output = "";
		output += "Win Percentage:  "+String.format("%.2f", winPerc)+"\n";  // ^^ truncate to 1 decimal place ish
		output += "Draw Percentage: "+String.format("%.2f", drawPerc)+"\n";
		output += "Loss Percentage: "+String.format("%.2f", lossPerc)+"\n";
		output += "Total Games: "+totalGames+"\n";

		return output;
	}
}
