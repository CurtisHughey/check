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

			ArrayList<MatchingGame> whiteGames = new ArrayList<MatchingGame>();
			ArrayList<MatchingGame> blackGames = new ArrayList<MatchingGame>();			

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
					
					if (game.getColor() == Game.Color.WHITE) {				
						whiteGames.add(new MatchingGame(game,0));
					}
					else {
						blackGames.add(new MatchingGame(game,0));
					}
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

				ArrayList<MatchingGame> matchingGames = new ArrayList<MatchingGame>();

				if (keepGoing == false) {
					break;
				}

				while (true) {
					System.out.print("What color do you want to be? white/black: ");
					String color = reader.nextLine();
					if (color.equals("white")) {
						matchingGames = whiteGames;
						break;
					}
					else if (color.equals("black")) {
						matchingGames = blackGames;
						break;
					}
					else {
						System.out.println("Input not recognized.  Please try again.");
					}
				}

				ArrayList<ArrayList<MatchingGame>> savedMatchingGamesLists = new ArrayList<ArrayList<MatchingGame>>();
				savedMatchingGamesLists.add(matchingGames);

				boolean beforeSearch = true;

				while (true) {
					System.out.print("---------\nInput command: ");
					String command = reader.nextLine();
					String tokens[] = command.split(" ");  // Might be worth just moving this to regex ^^

					boolean undo = false;

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
						else if (tokens[0].equals("undo")) {
							if (savedMatchingGamesLists.size() == 1) {
								System.out.println("You cannot undo any further!");
								continue;
							}
							undo = true;
							savedMatchingGamesLists.remove(savedMatchingGamesLists.size()-1);  // Deletes the last one
							matchingGames = savedMatchingGamesLists.get(savedMatchingGamesLists.size()-1);  // Loads the previous one
						}
						else if (tokens[0].equals("range")) {
							if (!beforeSearch) {
								System.out.println("You cannot execute a range command now!");
							}
							if (tokens[1].equals("elo")) {
								matchingGames = rangeElo(matchingGames, Integer.parseInt(tokens[2]), Integer.parseInt(tokens[3]));
							}
							else if (tokens[1].equals("opelo")) {
								matchingGames = rangeOpElo(matchingGames, Integer.parseInt(tokens[2]), Integer.parseInt(tokens[3]));
							}
							else if (tokens[1].equals("date")) {
								SimpleDateFormat sdf = Game.getSdf();
								Date low = sdf.parse(tokens[2]);
								Date high = sdf.parse(tokens[3]);
								matchingGames = rangeDate(matchingGames, low, high);
							}
							else {
								System.out.println("Failed to parse range command");
								continue;
							}
						}
						else if (tokens[0].equals("filter")) {
							if (!beforeSearch) {
								System.out.println("You cannot execute a filter command now!");
								continue;
							}
							if (tokens[1].equals("opponent")) {
								matchingGames = filterOp(matchingGames, tokens[2]);
							}
							else if (tokens[1].equals("time")) {
								matchingGames = filterTimeControl(matchingGames, tokens[2]);
							}
							else {
								System.out.println("Failed to parse filter command");
								continue;
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

					if (matchingGames == null) {
						System.out.println("No games found!");
						continue;
					}

					if (!undo) {
						matchingGames = findGamesMatchingBoard(matchingGames, board, positionFreqs.get(Zobrist.makeHash(board)));  // Gets the current amount of frequencies// Need to be keeping track the frequencies of this frikkin board too
						savedMatchingGamesLists.add(matchingGames);
					}

					beforeSearch = false;

					System.out.println(getStats(matchingGames));
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
		System.out.println("undo: undoes the prior move\n");
		System.out.println("quit: exits the program\n");
		System.out.println("help: displays this guide\n");
		System.out.println("Note that range and filter commands can only be issued at the start of a\nsearch (i.e. before you start entering moves\n");
	}

	private static ArrayList<MatchingGame> findGamesMatchingBoard(ArrayList<MatchingGame> currentMatchingGames, int[] board, Integer freq) {
		return findGamesMatchingCompressedPosition(currentMatchingGames, Zobrist.makeHash(board), freq);  // This will need to get updated if I want to not have to take care of repeated positions in makeHash
	}

	private static ArrayList<MatchingGame> findGamesMatchingCompressedPosition(ArrayList<MatchingGame> currentMatchingGames, long hash, Integer freq) {
		if (freq == null) {
			return null;
		}
		ArrayList<MatchingGame> newMatchingGames = new ArrayList<MatchingGame>();
		CompressedPosition CompressedPosition = new CompressedPosition(hash, freq);
		for (MatchingGame currentMatchingGame : currentMatchingGames) {
			Game game = currentMatchingGame.getGame();
			int moveIndex = -1;
			if ((moveIndex = game.compressedPositions.indexOf(CompressedPosition)) >= 0)  {
				newMatchingGames.add(new MatchingGame(game, moveIndex));
			}
		}
		return newMatchingGames;
	}

	// Could also do some sort of aggregation for each thing as well, rather than using filter over and over again
	private static ArrayList<MatchingGame> filterResult(ArrayList<MatchingGame> currentMatchingGames, Game.Result result) {  // Add Won and Drawn, etc. ^^
		ArrayList<MatchingGame> newMatchingGames = new ArrayList<MatchingGame>();
		for (MatchingGame currentMatchingGame : currentMatchingGames) {
			Game game = currentMatchingGame.getGame();
			if (game.getResult() == result) {
				newMatchingGames.add(new MatchingGame(game, 0));
			}
		}
		return newMatchingGames;
	}

	private static ArrayList<MatchingGame> filterOp(ArrayList<MatchingGame> currentMatchingGames, String op) {  // Filters by the opponent
		ArrayList<MatchingGame> newMatchingGames = new ArrayList<MatchingGame>();
		for (MatchingGame currentMatchingGame : currentMatchingGames) {
			Game game = currentMatchingGame.getGame();
			if (game.getOp().equals(op)) {
				newMatchingGames.add(new MatchingGame(game, 0));
			}
		}
		return newMatchingGames;
	}

	private static ArrayList<MatchingGame> filterColor(ArrayList<MatchingGame> currentMatchingGames, Game.Color color) {  // Filters by the color of the user
		ArrayList<MatchingGame> newMatchingGames = new ArrayList<MatchingGame>();
		for (MatchingGame currentMatchingGame : currentMatchingGames) {
			Game game = currentMatchingGame.getGame();
			if (game.getColor() == color) {
				newMatchingGames.add(new MatchingGame(game, 0));
			}
		}
		return newMatchingGames;
	}

	private static ArrayList<MatchingGame> rangeElo(ArrayList<MatchingGame> currentMatchingGames, int low, int high) {  // Filters by the ELO range of the user
		ArrayList<MatchingGame> newMatchingGames = new ArrayList<MatchingGame>();
		for (MatchingGame currentMatchingGame : currentMatchingGames) {
			Game game = currentMatchingGame.getGame();
			int elo = game.getElo();
			if (low <= elo && elo <= high) {
				newMatchingGames.add(new MatchingGame(game, 0));
			}
		}
		return newMatchingGames;
	}

	private static ArrayList<MatchingGame> rangeOpElo(ArrayList<MatchingGame> currentMatchingGames, int low, int high) {  // Filters by the ELO range of the user
		ArrayList<MatchingGame> newMatchingGames = new ArrayList<MatchingGame>();
		for (MatchingGame currentMatchingGame : currentMatchingGames) {
			Game game = currentMatchingGame.getGame();
			int opElo = game.getOpElo();
			if (low <= opElo && opElo <= high) {
				newMatchingGames.add(new MatchingGame(game, 0));
			}
		}
		return newMatchingGames;
	}

	private static ArrayList<MatchingGame> filterTimeControl(ArrayList<MatchingGame> currentMatchingGames, String timeControl) {
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
		ArrayList<MatchingGame> newMatchingGames = new ArrayList<MatchingGame>();
		for (MatchingGame currentMatchingGame : currentMatchingGames) {
			Game game = currentMatchingGame.getGame();
			if (game.getTimeControl() == numericTimeControl) {
				newMatchingGames.add(new MatchingGame(game, 0));
			}
		}
		return newMatchingGames;
	}

	private static ArrayList<MatchingGame> rangeDate(ArrayList<MatchingGame> currentMatchingGames, Date start, Date end) { // Change int to JODA/Datetime
		ArrayList<MatchingGame> newMatchingGames = new ArrayList<MatchingGame>();
		for (MatchingGame currentMatchingGame : currentMatchingGames) {
			Game game = currentMatchingGame.getGame();
			long gameDate = game.getDate().getTime();
			if (start.getTime() <= gameDate && gameDate <= end.getTime()) {
				newMatchingGames.add(new MatchingGame(game, 0));
			}
		}
		return newMatchingGames;		
	}

	// Presumably, we have already filtered for a hash, this could be optional
	private static Statistics getStats(ArrayList<MatchingGame> matchingGames) { 

		if (matchingGames == null) {
			System.out.println("No games to give stats for!");
			return null;
		}

		double wins = 0;
		double draws = 0;
		double losses = 0;
		int totalGames = matchingGames.size();

		for (MatchingGame matchingGame : matchingGames) {
			Game game = matchingGame.getGame();  // I should also get the index eventually to display the next moves
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
	private double winPerc;
	private double drawPerc;
	private double lossPerc;
	private int totalGames;

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

class MatchingGame {  // A game that matches to a specific CompressedPosition
	final private Game game;
	final private int index; // The index of the move that matches;

	public MatchingGame(Game game, int index) {
		this.game = game;
		this.index = index;
	}

	public Game getGame() { return game; }
	public int getIndex() { return index; }
}