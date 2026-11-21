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
        
        var status by remember { mutableStateOf("Chọn màu của bạn.") }
        var isAnalyzing by remember { mutableStateOf(false) }
        
        // Kết quả phân tích giống Python
        var bestMoveText by remember { mutableStateOf("") }
        var scoreText by remember { mutableStateOf("") }
        var pvText by remember { mutableStateOf("") }
        
        var suggestedMove by remember { mutableStateOf<Pair<Square, Square>?>(null) }

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
                        bestMoveText = ""
                        scoreText = ""
                        pvText = ""
                        status = "Đã nhập nước đối thủ. Bấm 'AI Phân Tích'."
                    } else {
                        status = "Nước đi không hợp lệ!"
                        selectedSquare = null
                    }
                }
            } else {
                // LƯỢT CỦA BẠN
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
            
            BoardView(board, selectedSquare, suggestedMove, ::onSquareClick)

            Spacer(Modifier.height(16.dp))
            
            // Bảng phân tích
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("KẾT QUẢ PHÂN TÍCH (Stockfish)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    if (isAnalyzing) {
                        Text("Đang tính toán...", color = Color.Blue)
                    } else {
                        Text("Score: $scoreText", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Nước tốt nhất: $bestMoveText", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF008000))
                        Text("Biến thể: $pvText", fontSize = 14.sp, color = Color.DarkGray)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        isAnalyzing = true
                        bestMoveText = "..."
                        scoreText = "..."
                        pvText = "..."
                        suggestedMove = null
                        val fen = board.fen
                        
                        Thread {
                            val result = runStockfish(fen)
                            runOnUiThread {
                                if (result != null) {
                                    scoreText = result.score
                                    bestMoveText = result.bestMove
                                    pvText = result.pv
                                    
                                    if (result.bestMove != "(none)") {
                                        try {
                                            val sq1 = Square.valueOf(result.bestMove.substring(0, 2).uppercase())
                                            val sq2 = Square.valueOf(result.bestMove.substring(2, 4).uppercase())
                                            suggestedMove = Pair(sq1, sq2)
                                        } catch (e: Exception) {}
                                    }
                                    status = "Phân tích hoàn tất."
                                } else {
                                    status = "Lỗi khi chạy Stockfish."
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
                    bestMoveText = ""
                    scoreText = ""
                    pvText = ""
                    status = "Đã reset bàn cờ." 
                }) { Text("Reset") }
            }
            
            Spacer(Modifier.height(8.dp))
            Text(status, fontSize = 14.sp, color = Color.DarkGray)
        }
    }

    data class AnalysisResult(val bestMove: String, val score: String, val pv: String)

    fun runStockfish(fen: String): AnalysisResult? {
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
            while (true) { line = reader.readLine() ?: break; if (line == "uciok") break }

            writer.write("position fen $fen\n"); writer.flush()
            writer.write("go depth 15\n"); writer.flush()

            var bestMove: String? = null
            var finalScore = "0.0"
            var finalPv = ""

            while (true) {
                line = reader.readLine() ?: break
                if (line.startsWith("info depth") && line.contains("score")) {
                    val parts = line.split(" ")
                    for (i in parts.indices) {
                        if (parts[i] == "score") {
                            if (parts[i+1] == "cp") {
                                val cp = parts[i+2].toIntOrNull() ?: 0
                                finalScore = String.format("%+.1f", cp / 100.0)
                            } else if (parts[i+1] == "mate") {
                                finalScore = "Mate trong ${parts[i+2]}"
                            }
                        }
                        if (parts[i] == "pv") {
                            finalPv = parts.subList(i + 1, minOf(parts.size, i + 4)).joinToString(" ")
                            break
                        }
                    }
                }
                if (line.startsWith("bestmove")) {
                    bestMove = line.split(" ")[1]
                    break
                }
            }
            writer.write("quit\n"); writer.flush()
            process.waitFor()
            
            return if (bestMove != null) AnalysisResult(bestMove, finalScore, finalPv) else null
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
                        
                        val isSugFrom = suggested?.first == sq
                        val isSugTo = suggested?.second == sq
                        
                        val bgColor = when {
                            isSelected -> Color(0xFFFFD700)
                            isSugFrom -> Color(0xFF90EE90)
                            isSugTo -> Color(0xFF32CD32)
                            light -> Color(0xFFEEEED2)
                            else -> Color(0xFF769656)
                        }

                        Surface(
                            color = bgColor,
                            modifier = Modifier.size(40.dp).clickable { onSquareClick(sq) },
                            border = if (isSugTo) BorderStroke(2.dp, Color.Red) else null
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
