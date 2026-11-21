package com.nsmod.chesscam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.move.Move
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ChessScreen() }
    }

    @Composable
    fun ChessScreen() {
        var myColor by remember { mutableStateOf(Side.WHITE) }
        var board by remember { mutableStateOf(Board()) }
        var selectedSquare by remember { mutableStateOf<Square?>(null) }
        var status by remember { mutableStateOf("Chọn màu của bạn ở trên.") }
        var isAnalyzing by remember { mutableStateOf(false) }
        
        var suggestedMove by remember { mutableStateOf<Pair<Square, Square>?>(null) }
        var analysisText by remember { mutableStateOf("") }

        fun onSquareClick(sq: Square) {
            if (isAnalyzing) return
            
            val currentTurn = board.sideToMove
            val piece = board.getPiece(sq)

            if (currentTurn != myColor) {
                // LƯỢT ĐỐI THỦ: Cho phép chạm để nhập nước đi
                if (selectedSquare == null) {
                    if (piece != Piece.NONE && piece.pieceSide == currentTurn) {
                        selectedSquare = sq
                        status = "Đang nhập nước đối thủ. Chạm ô đích."
                    }
                } else {
                    val move = Move(selectedSquare, sq)
                    if (board.isMoveLegal(move, true)) {
                        board.doMove(move)
                        selectedSquare = null
                        suggestedMove = null
                        status = "Đã nhập nước đối thủ. Bấm 'AI Phân Tích' để xem nước đi."
                    } else {
                        status = "Nước đi không hợp lệ!"
                        selectedSquare = null
                    }
                }
            } else {
                // LƯỢT CỦA BẠN: Yêu cầu bấm nút phân tích
                status = "Lượt của bạn. Bấm 'AI Phân Tích'."
            }
        }

        Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            
            Text("Bạn chơi màu gì?", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { myColor = Side.WHITE; board = Board(); suggestedMove = null; status = "Bạn chơi Trắng." },
                    colors = ButtonDefaults.buttonColors(containerColor = if (myColor == Side.WHITE) Color.Green else Color.Gray)
                ) { Text("Trắng") }
                
                Button(
                    onClick = { myColor = Side.BLACK; board = Board(); suggestedMove = null; status = "Bạn chơi Đen." },
                    colors = ButtonDefaults.buttonColors(containerColor = if (myColor == Side.BLACK) Color.Green else Color.Gray)
                ) { Text("Đen") }
            }

            Spacer(Modifier.height(16.dp))
            
            // Bàn cờ (truyền cờ suggested để tô màu xanh)
            BoardView(board, selectedSquare, suggestedMove, ::onSquareClick)

            Spacer(Modifier.height(16.dp))
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Trợ lý AI (Stockfish)", fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(4.dp))
                    if (isAnalyzing) {
                        Text("Đang tính toán...", color = Color.Blue)
                    } else {
                        Text(analysisText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        isAnalyzing = true
                        analysisText = "Đang tìm nước đi..."
                        suggestedMove = null
                        val fen = board.fen
                        
                        Thread {
                            val bestMoveStr = runStockfish(fen)
                            runOnUiThread {
                                if (bestMoveStr != null && bestMoveStr != "(none)") {
                                    val sq1 = Square.valueOf(bestMoveStr.substring(0, 2).uppercase())
                                    val sq2 = Square.valueOf(bestMoveStr.substring(2, 4).uppercase())
                                    suggestedMove = Pair(sq1, sq2)
                                    
                                    val moveString = "${bestMoveStr.substring(0,2)} -> ${bestMoveStr.substring(2,4)}"
                                    analysisText = "Nước đi tốt nhất: $moveString"
                                } else {
                                    analysisText = "Không tìm thấy nước đi."
                                }
                                isAnalyzing = false
                            }
                        }.start()
                    },
                    enabled = board.sideToMove == myColor && !isAnalyzing && !board.isMated && !board.isDraw
                ) { Text("AI Phân Tích") }

                Button(onClick = { 
                    board = Board()
                    selectedSquare = null
                    suggestedMove = null
                    analysisText = ""
                    status = "Đã reset bàn cờ." 
                }) { Text("Reset") }
            }
            
            Spacer(Modifier.height(8.dp))
            Text(status, fontSize = 14.sp, color = Color.DarkGray)
        }
    }

    fun runStockfish(fen: String): String? {
        try {
            val binFile = File(filesDir, "stockfish")
            if (!binFile.exists()) {
                assets.open("stockfish.bin").use { input ->
                    binFile.outputStream().use { output -> input.copyTo(output) }
                }
                binFile.setExecutable(true)
            }

            val process = ProcessBuilder(binFile.absolutePath).redirectErrorStream(true).start()
            val writer = process.outputStream.bufferedWriter()
            val reader = process.inputStream.bufferedReader()

            writer.write("uci\n"); writer.flush()
            var line: String?
            while (true) {
                line = reader.readLine() ?: break
                if (line == "uciok") break
            }

            writer.write("position fen $fen\n"); writer.flush()
            writer.write("go depth 15\n"); writer.flush()

            var bestMove: String? = null
            while (true) {
                line = reader.readLine() ?: break
                if (line.startsWith("bestmove")) {
                    bestMove = line.split(" ")[1]
                    break
                }
            }
            writer.write("quit\n"); writer.flush()
            process.waitFor()
            return bestMove
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    @Composable
    fun BoardView(
        board: Board, 
        selected: Square?, 
        suggested: Pair<Square, Square>?, 
        onSquareClick: (Square) -> Unit
    ) {
        Column {
            for (r in 7 downTo 0) {
                Row {
                    for (f in 0..7) {
                        val file = com.github.bhlangonijr.chesslib.File.values()[f]
                        val rank = com.github.bhlangonijr.chesslib.Rank.values()[r]
                        val sq = Square.getSquare(file, rank)
                        
                        val piece = board.getPiece(sq)
                        val light = (r + f) % 2 == 0
                        val isSelected = selected == sq
                        
                        val isSuggestedFrom = suggested?.first == sq
                        val isSuggestedTo = suggested?.second == sq
                        
                        val bgColor = when {
                            isSelected -> Color(0xFFFFD700) // Vàng (đang chọn)
                            isSuggestedFrom -> Color(0xFF90EE90) // Xanh lá nhạt (điểm đi)
                            isSuggestedTo -> Color(0xFF32CD32)   // Xanh lá đậm (điểm đến)
                            light -> Color(0xFFEEEED2)
                            else -> Color(0xFF769656)
                        }

                        Surface(
                            color = bgColor,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { onSquareClick(sq) },
                            border = if (isSuggestedTo) BorderStroke(2.dp, Color.Red) else null
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = when (piece) {
                                        Piece.WHITE_PAWN -> "♙"; Piece.WHITE_KNIGHT -> "♘"; Piece.WHITE_BISHOP -> "♗"; Piece.WHITE_ROOK -> "♖"; Piece.WHITE_QUEEN -> "♕"; Piece.WHITE_KING -> "♔"
                                        Piece.BLACK_PAWN -> "♟"; Piece.BLACK_KNIGHT -> "♞"; Piece.BLACK_BISHOP -> "♝"; Piece.BLACK_ROOK -> "♜"; Piece.BLACK_QUEEN -> "♛"; Piece.BLACK_KING -> "♚"
                                        else -> ""
                                    },
                                    fontSize = 28.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
