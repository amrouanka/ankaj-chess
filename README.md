# AnkaJ Chess

![Gameplay](docs/screenshot.png)

A chess engine with a graphical interface, written in Java 21. The Java counterpart
to [AnkaX](https://github.com/amrouanka/ankax-chess) (C#). Originally written years ago,
now fully refactored with a modern project structure, proper types, and a responsive
Swing GUI.

## Features

- **Complete legal move generation** — all pieces, castling (kingside & queenside),
  en passant, pawn promotion (auto-queen), check / checkmate / stalemate detection
- **Negamax search** with alpha-beta pruning at depth 4
- **Quiescence search** — extends the search for captures and promotions beyond
  depth 4 to avoid the horizon effect (leaving pieces hanging)
- **MVV-LVA move ordering** — captures searched first by Most Valuable Victim /
  Least Valuable Attacker, improving pruning efficiency
- **Positional evaluation** — material (P=100, N/B=325, R=500, Q=975),
  distance-from-center table (DFCT), bishop pair bonus
- **Responsive Swing GUI** — drag-and-drop moves, dynamic board sizing, play as
  White or Black, engine search runs on a background thread so the interface
  never freezes

## Requirements

- JDK 21 or newer (tested with Eclipse Temurin 21)
- Maven 3.9+

## Running

Clone and run with Maven:

```bash
git clone https://github.com/amrouanka/ankaj-chess.git
cd ankaj-chess
mvn clean compile exec:java
```

Or run the executable JAR (see [Releases](../../releases)):

```bash
java -jar ankaj-chess.jar
```

## How to Play

1. Start the game and choose your color in the dialog
2. Click a piece, then click its destination square
3. The engine thinks in the background and replies — search stats are printed
   to the console
4. **Castling:** drag the king two squares toward the rook
5. **En passant:** capture diagonally behind an enemy pawn that just double-pushed
6. **Game over:** a dialog announces checkmate or stalemate

## Testing

Run the perft suite to verify move generation correctness:

```bash
mvn -q exec:java "-Dexec.mainClass=com.amr.chess.engine.Perft"
```

Expected results (starting position): 20 → 400 → 8,902 → 197,281.

## Architecture

The engine uses a **flip-board convention**: after every half-move the board is
flipped vertically (rows swap, columns stay) and piece colors are swapped, so the
side to move is always "white" (uppercase). This means move generation and
king-safety logic only need to handle one color.

| File | Responsibility |
| --- | --- |
| `model/Board.java` | Board state (`char[8][8]`), make/undo move, vertical flip, copy constructor, castling rights, en passant |
| `model/Move.java` | Immutable move record with flags for castling, en passant, and promotion |
| `engine/MoveGenerator.java` | Generates all legal moves for the side to move |
| `engine/GameLogic.java` | King safety (attack detection) and move legality via make-check-unmake |
| `engine/Evaluation.java` | Single-pass material + positional scoring from the side to move's perspective |
| `engine/AlphaBeta.java` | Negamax search with alpha-beta pruning, quiescence search, and MVV-LVA move ordering |
| `engine/Perft.java` | Perft testing utility with FEN parser and divide function |
| `ui/UserInterface.java` | Swing rendering, input handling, game-over detection, background search thread |
| `Main.java` | Entry point, window setup, color selection |

### Design notes

- **No global state** — everything is instance-based, so the GUI and the search
  each work on their own `Board` clone (avoids threading issues)
- **Legality by simulation** — a move is legal if making it leaves the mover's
  king safe; the move is made, tested, and unmade
- **Fast evaluation** — single pass over 64 squares with no cloning or move
  generation, keeping the engine responsive even with quiescence search
- **Display decoupled from data** — the GUI renders from the player's perspective
  regardless of the board's internal flip state

## Limitations

- Promotion always to queen (no underpromotion)
- Fixed search depth (4 plies + quiescence)
- No draw by repetition, 50-move rule, or insufficient material detection
- No transposition table, opening book, or UCI protocol
