// Represents a game.  Also takes care of Zobrist hashing (same random numbers)

import java.io.*;
import java.util.regex.*;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

class Game {

	enum Color {WHITE, BLACK};  // The color that the user was
	enum Result {WIN, DRAW, LOSS};
	enum TimeControl {BULLET, BLITZ, STANDARD, TURN, OTHER};

	//private static final int maxAllowedRepetitions = 5;  // The number number of times a repeating position is recorded ^^

	private static int knightMoveGenerator[] = {18,33,31,14,-18,-33,-31,-14};
	private static int bishopMoveGenerator[] = {17,15,-17,-15};
	private static int rookMoveGenerator[]   = {1,16,-1,-16};
	private static int queenMoveGenerator[]  = {1,17,16,15,-1,-17,-16,-15};
	private static int kingMoveGenerator[]   ={1,17,16,15,-1,-17,-16,-15};
	
	public static SimpleDateFormat sdf;

	String user;  // The user name 
	String opponent;
	Date date;
	Color color; // The user's color
	Result result;
	int elo;
	int opElo;
	TimeControl timeControl;
	ArrayList<String> moves;
	ArrayList<HashedPosition> positions; // The hashed positions of the game.  Might not go all the way to the end...

	static {
		sdf = new SimpleDateFormat("yyyy.MM.dd");  // Is this the best way ^^?
	}

	Game(String user,
		 String opponent,
		 int elo,
		 int opElo,
		 String date,	
		 int color,
		 int timeControl,
		 int result,
		 ArrayList<String> moves,
		 ArrayList<HashedPosition> positions) {
		
		this.user = user;
		this.opponent = opponent;
		this.elo = elo;
		this.opElo = opElo;
		try {
			this.date = sdf.parse(date);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		switch (color) {  // Breaking the fourth wall a bit here ^^^, check serializable stuff
			case 0: this.color = Color.WHITE; break;
			case 1: this.color = Color.BLACK; break;
		}
		switch (timeControl) {
			case 0: this.timeControl = TimeControl.BULLET; break;
			case 1: this.timeControl = TimeControl.BLITZ; break;
			case 2: this.timeControl = TimeControl.STANDARD; break;
			case 3: this.timeControl = TimeControl.TURN; break;
			case 4: this.timeControl = TimeControl.OTHER; break;
		}
		switch (result) {
			case 0: this.result = Result.WIN; break;
			case 1: this.result = Result.DRAW; break;
			case 2: this.result = Result.LOSS; break;
		}
		this.moves = moves;
		this.positions = positions;
	}
		 

	Game(String user,
		 String date, 
		 String whiteColor, 
		 String blackColor, 
		 String whiteElo, 
		 String blackElo, 
		 String timeControl,
		 String moveString) {
		
		this.user = user;

		try {
			this.date = sdf.parse(date);
		}
		catch (Exception e) {
			e.printStackTrace();
		}		

		if (user.equals(whiteColor)) {
			this.color = Color.WHITE;
			this.opponent = blackColor;
			this.elo = Integer.parseInt(whiteElo);
			this.opElo = Integer.parseInt(blackElo);
		}
		else {
			this.color = Color.BLACK;
			this.opponent = whiteColor;
			this.elo = Integer.parseInt(blackElo);
			this.opElo = Integer.parseInt(whiteElo);
		}

		// Add turn.  Also, this could be way better ^^^
		if (timeControl.equals("1|0") || timeControl.equals("1|3") || timeControl.equals("2|1"))
			this.timeControl = TimeControl.BULLET;
		else if (timeControl.equals("3|0") || timeControl.equals("3|2") || timeControl.equals("5|0") || timeControl.equals("10|10"))
			this.timeControl = TimeControl.BLITZ;
		else
			this.timeControl = TimeControl.STANDARD;

		String game_regex = "[0-9]+\\.(O-O-O|O-O|[a-zA-Z1-8=]+)([#\\+]?) (O-O-O|O-O|[a-zA-Z1-8=]+)([#\\+]?)";  // Sort of kind of lazy^^
		Pattern game_pattern = Pattern.compile(game_regex);
		Matcher game_matcher = game_pattern.matcher(moveString);
		moves = new ArrayList<String>();

		while (game_matcher.find()) {
			moves.add(game_matcher.group(1));

			String second = game_matcher.group(3); 
			if (!(second.equals("1") || second.equals("0") || second.equals("1/2")))  // Then game is not finished, hacky ^^
				moves.add(game_matcher.group(3));
		}

		String result_regex = "(1-0|0-1|1/2-1/2)"; //Technically can have 1/2-0
		Pattern result_pattern = Pattern.compile(result_regex);
		Matcher result_matcher = result_pattern.matcher(moveString);
		String resultString = "";

		if (result_matcher.find()) {
			resultString = result_matcher.group(1);  // Overwriting the "x won by y".  I should be recording failures^^
			if (resultString.equals("1-0"))
				this.result = color==Color.WHITE ? Result.WIN : Result.LOSS;
			else if (resultString.equals("0-1"))
				this.result = color==Color.WHITE ? Result.LOSS : Result.WIN;
			else
				this.result = Result.DRAW;
		}

		// Now the hashing stuff
		positions = new ArrayList<HashedPosition>();
		HashMap<Long, Integer> positionFreqs = new HashMap<Long, Integer>();  // Points positions (longs) to number of occurences (ints)

		int board[] =  {4,2,3,5,6,3,2,4,0,0,0,0,0,0,0,0, // board[8] = turn, board[9] = how many times the position has been repeated
						1,1,1,1,1,1,1,1,0,0,0,0,0,0,0,0,
						0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
						0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
						0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
						0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
						7,7,7,7,7,7,7,7,0,0,0,0,0,0,0,0,
						10,8,9,11,12,9,8,10};  // Drop the last 8 zeros, don't need

		long hash = Zobrist.makeHash(board);
		positions.add(new HashedPosition(hash, 1));
		positionFreqs.put(hash, 1);

		//Potentially, checking for draws by repetition might be unnecessary, since if I only look in the first 15 or so moves it is highly unlikely that that would be present

		for (String move : moves) {
			makeMove(board, positions, positionFreqs, move);
						
			////
			long tempHash = Zobrist.makeHash(board);
			if (positions.get(positions.size()-1).hash != tempHash) {  // Keeping this in for the moment ^^
				System.out.println("NOT EQUAL!!!!!!!!!!!!!!!!!!!!!!!!");
				System.out.println(move);				
			}
			////
		}
		
	}

 	private static int findOrigin(int board[], char piece, int pieceNum, int destination, char disambCol, char disambRow, int turn) {

		int[] moveGenerator = {};
		boolean iterate = false;

		switch (piece) {
			case 'P':
				break;  // Handled separately below
			case 'N':
				moveGenerator = knightMoveGenerator;
				iterate = false;
				break;
			case 'B':
				moveGenerator = bishopMoveGenerator;
				iterate = true;
				break;
			case 'R':
				moveGenerator = rookMoveGenerator;
				iterate = true;
				break;
			case 'Q':
				moveGenerator = queenMoveGenerator;
				iterate = true;
				break;
			case 'K':
				moveGenerator = kingMoveGenerator;
				iterate = false;
				break;
		}

		// Now find all the places that the piece could have originated from

		if (piece == 'P') {
			int multiplier = turn==0 ? 1 : -1;

			if (disambCol == ' ') { // Then it is not a capture
				int position = destination+multiplier*(-16);
				if (board[position] == pieceNum)
					return position;
				position = destination+multiplier*(-32);
				if (board[position] == pieceNum)
					return position;
			}
			else {
				int position = destination+multiplier*(-17);
				if (board[position] == pieceNum && getColumn(position) == disambCol)
					return position;
				position = destination+multiplier*(-15);
				if (board[position] == pieceNum && getColumn(position) == disambCol)
					return position;
			}

			System.out.println("Failed to figure out the pawn origin");
			return -1;
		}

		for (int i = 0; i < moveGenerator.length; i++) {
			int j = destination;
			do {
				j += moveGenerator[i];
				// First check if out of went out of bounds;
				if ((j&0x88) != 0)
					break;
				// Now check if there is a piece in the way
				int currentPieceNum = board[j];  // could be empty
				boolean piecePresent = pieceNum > 0;
				if (currentPieceNum > 0) {
					if (pieceNum == currentPieceNum) {  
						if ((disambCol == disambRow /*i.e. ' '*/) || (disambCol == getColumn(j)) || (disambRow == getRow(j)))  // Then we were looking at the right one
							return j;
					}
					break; // Either it was a friendly piece (maybe in wrong place) or an enemy piece, either way we are stopped.  
				}
			} 
			while (iterate);  // A devilishly sneaky way to do this, if I may say so myself.  Possible infinite loop? ^^
		}
		System.out.println("Failed to find an origin square!!!");
		return -1; // Maybe throw exception instead, write to log file, etc. ^^
	}

	private static int getBoardIndex(char col, char row) {  // Maybe error check?^^  But not really necessary since private, and only getting called by makeMove, which verifies regex
		return (col-'a')+(row-'1')*16;  
	}

	private static char getColumn(int i) {
		return (char)('a'+i%8);
	}

	private static char getRow(int i) {
		return (char)((i/16)+'1');
	}

	// move = 0 if white, 1 if black
	public static void makeMove(int board[], ArrayList<HashedPosition> positions, HashMap<Long, Integer> positionFreqs, String move) {  // Need to pass number of times past positions have occurred! ^^^^

		String pawn_regex = "(([a-h])?x?([a-h])([1-8])(=([RNBQ]))?)"; // For pawn moves
		String other_regex = "(([RNBKQ])(([a-h])|([1-8]))?x?([a-h])([1-8]))"; // For other normal piece moves 
		String castle_regex = "((O-O-O)|(O-O))"; // For castling
		String append_regex = "([+#]?)";  // Appended to every regex

		String move_regex = "("+pawn_regex+"|"+other_regex+"|"+castle_regex+")"+append_regex;		

		Pattern move_pattern = Pattern.compile(move_regex);	// Don't compile each time, make it static! ^^
		Matcher move_matcher = move_pattern.matcher(move);
				
		char piece = ' ';
		char disambCol = ' ';
		char disambRow = ' ';
		char column = ' ';  // Be consistent in column/col
		char row = ' ';
		char promotesTo = ' ';
		boolean pawnMove = false;
		boolean otherMove = false;
		boolean castleMove = false;
		boolean kingCastle = false;
		boolean queenCastle = false;

		int turn = board[8];
		long hash = positions.get(positions.size()-1).hash;  // Bit wonky, should really be called currentHash or something

		/*
		0: move
			1: pawn/other/castle move
				2: pawn move
					3: pawn origin column
					4: pawn destination column
					5: pawn destination row
					6: pawn promotion
						7: pawn promotes to
				8: other move
					9: other piece
					10: other disambiguate
						11: other origin column
						12: other origin row
					13: other destination column
					14: other destination row
				15: castle move
					16: queen castle
					17: king castle
		*/	

		if (move_matcher.find()) {
			if (pawnMove = (move_matcher.group(2) != null)) {
				piece = 'P';
				disambCol = move_matcher.group(3) != null ? move_matcher.group(3).charAt(0) : ' ';
				column = move_matcher.group(4).charAt(0);
				row = move_matcher.group(5).charAt(0);
				promotesTo = move_matcher.group(7) != null ? move_matcher.group(7).charAt(0) : ' ';
			}
			else if (otherMove = (move_matcher.group(8) != null)) {
				piece = move_matcher.group(9).charAt(0);
				if (move_matcher.group(10) != null) {
					disambCol = move_matcher.group(11) != null ? move_matcher.group(11).charAt(0) : ' ';
					disambRow = move_matcher.group(12) != null ? move_matcher.group(12).charAt(0) : ' ';
				}
				column = move_matcher.group(13).charAt(0);
				row = move_matcher.group(14).charAt(0);
			}
			else if (castleMove = (move_matcher.group(15) != null)) {
				//piece = 'K'; // Used with the hashing to simplify
				if (move_matcher.group(16) != null)  // Then it matched
					queenCastle = true;
				else
					queenCastle = false;
				kingCastle = !queenCastle;  // Exactly 1 is true...
			} 	
		}
	
		if (castleMove) {
			if (kingCastle) {
				board[4+turn*112] = 0;
				board[7+turn*112] = 0;
				board[6+turn*112] = 6+turn*6;
				board[5+turn*112] = 4+turn*6;

				hash ^= Zobrist.zobristTable[6+turn*6][4+turn*112];  // king
				hash ^= Zobrist.zobristTable[4+turn*6][7+turn*112];  // king rook 
				hash ^= Zobrist.zobristTable[6+turn*6][6+turn*112];
				hash ^= Zobrist.zobristTable[4+turn*6][5+turn*112];	
			}
			else { // queenCastle == true
				board[4+turn*112] = 0;
				board[0+turn*112] = 0;
				board[2+turn*112] = 6+turn*6;
				board[3+turn*112] = 4+turn*6;

				hash ^= Zobrist.zobristTable[6+turn*6][4+turn*112];  // king
				hash ^= Zobrist.zobristTable[4+turn*6][0+turn*112];  // queen rook 
				hash ^= Zobrist.zobristTable[6+turn*6][2+turn*112];
				hash ^= Zobrist.zobristTable[4+turn*6][3+turn*112];	
			}
		}
		else {  // Pawn or other move, can treat much of it the same
			// First must find the origin square
			int pieceNum = 0;
			switch (piece) {  // Programming pearls would be ashamed, I should make this an array or something.
				case 'P':
					pieceNum = 1+turn*6;  // Shifts if black
					break;
				case 'N':
					pieceNum = 2+turn*6;
					break;
				case 'B':
					pieceNum = 3+turn*6;
					break;
				case 'R':
					pieceNum = 4+turn*6;
					break;
				case 'Q':
					pieceNum = 5+turn*6;
					break;
				case 'K':
					pieceNum = 6+turn*6;
					break;
			}

			int destination = getBoardIndex(column, row);
			int origin = findOrigin(board, piece, pieceNum, destination, disambCol, disambRow, turn);  // Could overload to pass either disambCol or disambRow or nothing..

			board[origin] = 0;
			hash ^= Zobrist.zobristTable[pieceNum][origin];  // Removing the piece (if no piece, then 0,position is guaranteed to be 0)

			if (piece == 'P' && disambCol != ' ' && board[destination] == 0) {  // Then it is a capturing move but there is no piece at destination => ep
				int captureLocation;
				if (disambCol < column)  // Then captures to the right
					captureLocation = origin+1;
				else
					captureLocation = origin-1;
				hash ^= Zobrist.zobristTable[board[captureLocation]][captureLocation];
				board[captureLocation] = 0;
			}
			else {
				hash ^= Zobrist.zobristTable[board[destination]][destination];
			}

			int destinationPiece = pieceNum;
			if (promotesTo != ' ')
				destinationPiece = pieceToNumPiece(promotesTo, turn);

			board[destination] = destinationPiece;
			hash ^= Zobrist.zobristTable[destinationPiece][destination];
		}		

		hash ^= Zobrist.zobristTable[0][8+turn];  // Removing the turn
		board[8] = turn==0 ? 1 : 0;  // Toggles
		hash ^= Zobrist.zobristTable[0][8+board[8]]; // The special turn hashed position

		// Now must xor the hash with the appropriate number of past positions.  Ugh

		Integer freq = positionFreqs.get(hash);
		if (freq != null) {
			positions.add(new HashedPosition(hash, freq+1));
			positionFreqs.put(hash, freq+1);
		}
		else {
			positions.add(new HashedPosition(hash, 1));
			positionFreqs.put(hash, 1);
		}
			

		/*Integer freq = positionFreqs.get(hash);
		int hashAt = 0;
		if (freq != null) {
			positionFreqs.put(hash, freq+1);
			hashAt = 8+freq+1;
		}
		else {
			positionFreqs.put(hash, 1);
			hashAt = 10;  // Just repeated once
		}
		hash ^= Zobrist.zobristTable[0][hashAt]; // Oh God this sucks ^^^, check everything.  Don't bother removing recording 1, 2 repetitions, etc.*/

		//return hash;
	}

	public static String boardToString(int[] board, long hash) {
		String output = "";
		String line = "";
		for (int i = 0; i < board.length; i++) {
			line += (pieceNumToPiece(board[i])+" ");
			if (i%16 == 15 || i == board.length-1) {
				output = line+"\n"+output;
				line = "";
			}
		}
		output = "Board: \n"+output;
		output += "Hash: "+hash;
		return output;
	}

	private static char pieceNumToPiece(int pieceNum) {
		char pieces[] = {'-','P','N','B','R','Q','K','p','n','b','r','q','k'};
		return pieces[pieceNum];
	}

	private static int pieceToNumPiece(char piece, int turn) {
		int pieceNum = 0;
		switch (piece) {  // Programming pearls would be ashamed, I should make this an array or something.
			case 'P':
				pieceNum = 1+turn*6;  // Shifts if black
				break;
			case 'N':
				pieceNum = 2+turn*6;
				break;
			case 'B':
				pieceNum = 3+turn*6;
				break;
			case 'R':
				pieceNum = 4+turn*6;
				break;
			case 'Q':
				pieceNum = 5+turn*6;
				break;
			case 'K':
				pieceNum = 6+turn*6;
				break;
		}		
		return pieceNum;
	}

	public String toString() {
		String output = "";
		output += "Player: "+user+", Elo: "+elo+"\n";
		output += "Opponent: "+opponent+", Elo: "+opElo+"\n";
		output += "Date: "+date+"\n";
		output += "Color: "+color+"\n";
		output += "Time Control: "+timeControl+"\n";
		output += "Moves: "+moves+"\n";
		output += "Result: "+result+"\n";
		//output += "Hashes: "+positions;  // Taking out for the moment, makes it crowded
		
		return output;
	}
}
