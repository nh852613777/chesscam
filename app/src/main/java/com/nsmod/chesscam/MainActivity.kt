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
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ChessScreen() }
    }

    @Composable
    fun ChessScreen() {
        var myColor by remember { mutableStateOf("w") }
        var grid by remember { mutableStateOf(startGrid()) }
        var selected by remember { mutableStateOf<Pair<Int, Int>?>(null) }
        var status by remember { mutableStateOf("Chọn màu của bạn.") }
        var isAnalyzing by remember { mutableStateOf(false) }
        
        // Lưu gợi ý AI (từ ô, đến ô)
        var suggested by remember { mutableStateOf<Pair<Pair<Int, Int>, Pair<Int, Int>>?>(null) }
        var analysisText by remember { mutableStateOf("") }

        fun onSquareClick(f: Int, r: Int) {
            if (isAnalyzing) return
            val piece = grid[r][f]
            
            // Xác định lượt hiện tại
            val currentTurn = if (countPieces(grid, 'P') > 0 || countPieces(grid, 'K') > 0) "w" else "b" // Fallback đơn giản
            // Lượt thực tế dựa vào nước đi gần nhất (Trắng đi trước)
            val turn = if (isWhiteTurn(grid)) "w" else "b"

            if (turn != myColor) {
                // Nhập nước đối thủ
                if (selected == null) {
                    if (piece != '.') {
                        selected = Pair(f, r)
                        status = "Đang nhập nước đối thủ. Chạm ô đích."
                    }
                } else {
                    val from = selected!!
                    if (from == Pair(f, r)) {
                        selected = null
                        status = "Bỏ chọn."
                    } else {
                        // Thực hiện đi (không check luật chặt chẽ để tránh lỗi lib)
                        val newG = Array(8) { grid[it].copyOf() }
                        newG[r][f] = newG[from.second][from.first]
                        newG[from.second][from.first] = '.'
                        grid = newG
                        selected = null
                        suggested = null
                        status = "Đã nhập nước đối thủ. Bấm 'AI Phân Tích'."
                    }
                }
            } else {
                status = "Lượt của bạn. Bấm 'AI Phân Tích' để xem gợi ý."
            }
        }

        Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Bạn chơi màu gì?", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { myColor = "w"; grid = startGrid(); suggested = null; status = "Bạn chơi Trắng." },
                    colors = ButtonDefaults.buttonColors(containerColor = if (myColor == "w") Color.Green else Color.Gray)
                ) { Text("Trắng") }
                
                Button(
                    onClick = { myColor = "b"; grid = startGrid(); suggested = null; status = "Bạn chơi Đen." },
                    colors = ButtonDefaults.buttonColors(containerColor = if (myColor == "b") Color.Green else Color.Gray)
                ) { Text("Đen") }
            }

            Spacer(Modifier.height(16.dp))
            
            BoardView(grid, selected, suggested, ::onSquareClick)

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
                        suggested = null
                        val fen = gridToFen(grid, myColor)
                        
                        Thread {
                            val bestMoveStr = runStockfish(fen)
                            runOnUiThread {
                                if (bestMoveStr != null && bestMoveStr != "(none)") {
                                    val f1 = bestMoveStr[0] - 'a'; val r1 = 8 - (bestMoveStr[1] - '0')
                                    val f2 = bestMoveStr[2] - 'a'; val r2 = 8 - (bestMoveStr[3] - '0')
                                    suggested = Pair(Pair(f1, r1), Pair(f2, r2))
                                    
                                    analysisText = "Nước đi tốt nhất: ${bestMoveStr.substring(0,2)} -> ${bestMoveStr.substring(2,4)}"
                                } else {
                                    analysisText = "Không tìm thấy nước đi."
                                }
                                isAnalyzing = false
                            }
                        }.start()
                    },
                    enabled = isWhiteTurn(grid) == (myColor == "w") && !isAnalyzing
                ) { Text("AI Phân Tích") }

                Button(onClick = { 
                    grid = startGrid()
                    selected = null
                    suggested = null
                    analysisText = ""
                    status = "Đã reset bàn cờ." 
                }) { Text("Reset") }
            }
            
            Spacer(Modifier.height(8.dp))
            Text(status, fontSize = 14.sp, color = Color.DarkGray)
        }
    }

    fun isWhiteTurn(g: Array<Array<Char>>): Boolean {
        var w = 0; var b = 0
        for (r in 0..7) for (f in 0..7) {
            val p = g[r][f]
            if (p.isUpperCase() && p != '.') w++
            if (p.isLowerCase() && p != '.') b++
        }
        return w <= b
    }

    fun countPieces(g: Array<Array<Char>>, p: Char): Int {
        var c = 0
        for (r in 0..7) for (f in 0..7) if (g[r][f] == p) c++
        return c
    }

    fun startGrid(): Array<Array<Char>> {
        val g = Array(8) { Array(8) { '.' } }
        val back = "RNBQKBNR".toCharArray()
        for (f in 0 until 8) {
            g[0][f] = back[f].lowercaseChar()
            g[1][f] = 'p'
            g[6][f] = 'P'
            g[7][f] = back[f]
        }
        return g
    }

    fun gridToFen(g: Array<Array<Char>>, turn: String): String {
        val rows = mutableListOf<String>()
        for (r in 0 until 8) {
            val sb = StringBuilder()
            var empty = 0
            for (f in 0 until 8) {
                if (g[r][f] == '.') empty++
                else {
                    if (empty > 0) { sb.append(empty); empty = 0 }
                    sb.append(g[r][f])
                }
            }
            if (empty > 0) sb.append(empty)
            rows.add(sb.toString())
        }
        return rows.joinToString("/") + " $turn - - 0 1"
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
            writer.write("go depth 12\n"); writer.flush()

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
        g: Array<Array<Char>>, 
        selected: Pair<Int, Int>?, 
        suggested: Pair<Pair<Int, Int>, Pair<Int, Int>>?, 
        onSquareClick: (Int, Int) -> Unit
    ) {
        Column {
            for (r in 0..7) {
                Row {
                    for (f in 0..7) {
                        val piece = g[r][f]
                        val light = (r + f) % 2 == 0
                        val isSelected = selected == Pair(f, r)
                        
                        val isSugFrom = suggested?.first == Pair(f, r)
                        val isSugTo = suggested?.second == Pair(f, r)
                        
                        val bgColor = when {
                            isSelected -> Color(0xFFFFD700)
                            isSugFrom -> Color(0xFF90EE90)
                            isSugTo -> Color(0xFF32CD32)
                            light -> Color(0xFFEEEED2)
                            else -> Color(0xFF769656)
                        }

                        Surface(
                            color = bgColor,
                            modifier = Modifier.size(40.dp).clickable { onSquareClick(f, r) },
                            border = if (isSugTo) BorderStroke(2.dp, Color.Red) else null
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = when (piece) {
                                        'P' -> "♙"; 'N' -> "♘"; 'B' -> "♗"; 'R' -> "♖"; 'Q' -> "♕"; 'K' -> "♔"
                                        'p' -> "♟"; 'n' -> "♞"; 'b' -> "♝"; 'r' -> "♜"; 'q' -> "♛"; 'k' -> "♚"
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
