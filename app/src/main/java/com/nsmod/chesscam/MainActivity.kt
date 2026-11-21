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
        
        var moveCount by remember { mutableIntStateOf(0) }
        val currentTurn = if (moveCount % 2 == 0) "w" else "b"

        var suggested by remember { mutableStateOf<Pair<Pair<Int, Int>, Pair<Int, Int>>?>(null) }
        var analysisText by remember { mutableStateOf("") }

        fun onSquareClick(f: Int, r: Int) {
            if (isAnalyzing) return
            val piece = grid[r][f]

            if (currentTurn != myColor) {
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
                        if (isValidMove(grid, from.first, from.second, f, r)) {
                            val newG = Array(8) { grid[it].copyOf() }
                            newG[r][f] = newG[from.second][from.first]
                            newG[from.second][from.first] = '.'
                            grid = newG
                            selected = null
                            suggested = null
                            moveCount++
                            status = "Đã nhập nước đối thủ. Bấm 'AI Phân Tích'."
                        } else {
                            status = "Nước đi không hợp lệ!"
                            selected = null
                        }
                    }
                }
            } else {
                status = "Lượt của bạn. Bấm 'AI Phân Tích'."
            }
        }

        Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Bạn chơi màu gì?", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { myColor = "w"; grid = startGrid(); suggested = null; moveCount = 0; status = "Bạn chơi Trắng." },
                    colors = ButtonDefaults.buttonColors(containerColor = if (myColor == "w") Color.Green else Color.Gray)
                ) { Text("Trắng") }
                Button(
                    onClick = { myColor = "b"; grid = startGrid(); suggested = null; moveCount = 0; status = "Bạn chơi Đen. Hãy nhập nước Trắng trước." },
                    colors = ButtonDefaults.buttonColors(containerColor = if (myColor == "b") Color.Green else Color.Gray)
                ) { Text("Đen") }
            }

            Spacer(Modifier.height(16.dp))
            BoardView(grid, myColor, selected, suggested, ::onSquareClick)

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
                        val fen = gridToFen(grid, currentTurn)
                        Thread {
                            try {
                                // Gọi JNI native
                                val bestMove = runStockfishNative(fen)
                                runOnUiThread {
                                    if (bestMove != null && bestMove != "(none)") {
                                        val f1 = bestMove[0] - 'a'; val r1 = 8 - (bestMove[1] - '0')
                                        val f2 = bestMove[2] - 'a'; val r2 = 8 - (bestMove[3] - '0')
                                        suggested = Pair(Pair(f1, r1), Pair(f2, r2))
                                        analysisText = "Nước đi tốt nhất: ${bestMove.substring(0,2)} -> ${bestMove.substring(2,4)}"
                                    } else {
                                        analysisText = "Không tìm thấy nước đi."
                                    }
                                    isAnalyzing = false
                                }
                            } catch (e: Exception) {
                                runOnUiThread {
                                    analysisText = "Lỗi: ${e.message}"
                                    isAnalyzing = false
                                }
                            }
                        }.start()
                    },
                    enabled = currentTurn == myColor && !isAnalyzing
                ) { Text("AI Phân Tích") }

                Button(onClick = { grid = startGrid(); selected = null; suggested = null; analysisText = ""; moveCount = 0; status = "Đã reset." }) { Text("Reset") }
            }
            Spacer(Modifier.height(8.dp))
            Text(status, fontSize = 14.sp, color = Color.DarkGray)
        }
    }

    // Hàm JNI native
    external fun runStockfishNative(fen: String): String?

    // Load thư viện native khi khởi động
    companion object {
        init {
            System.loadLibrary("stockfish")
        }
    }

    fun isValidMove(g: Array<Array<Char>>, f1: Int, r1: Int, f2: Int, r2: Int): Boolean {
        val p = g[r1][f1]
        if (p == '.') return false
        val isWhite = p.isUpperCase()
        val target = g[r2][f2]
        if (target != '.' && target.isUpperCase() == isWhite) return false
        val df = kotlin.math.abs(f2 - f1); val dr = kotlin.math.abs(r2 - r1)
        return when (p.uppercaseChar()) {
            'P' -> {
                val dir = if (isWhite) -1 else 1
                if (f1 == f2 && target == '.') (r2 == r1 + dir) || (r1 == (if (isWhite) 6 else 1) && r2 == r1 + 2*dir && g[r1+dir][f1] == '.')
                else if (df == 1 && r2 == r1 + dir) target != '.'
                else false
            }
            'N' -> (df == 2 && dr == 1) || (df == 1 && dr == 2)
            'B' -> df == dr && df > 0
            'R' -> (f1 == f2 || r1 == r2) && (df + dr > 0)
            'Q' -> (df == dr || f1 == f2 || r1 == r2) && (df + dr > 0)
            'K' -> df <= 1 && dr <= 1 && (df + dr > 0)
            else -> false
        }
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
                else { if (empty > 0) { sb.append(empty); empty = 0 }; sb.append(g[r][f]) }
            }
            if (empty > 0) sb.append(empty)
            rows.add(sb.toString())
        }
        return rows.joinToString("/") + " $turn - - 0 1"
    }

    @Composable
    fun BoardView(g: Array<Array<Char>>, myColor: String, selected: Pair<Int, Int>?, suggested: Pair<Pair<Int, Int>, Pair<Int, Int>>?, onSquareClick: (Int, Int) -> Unit) {
        Column {
            val rRange = if (myColor == "w") 0..7 else 7 downTo 0
            val fRange = if (myColor == "w") 0..7 else 7 downTo 0
            for (r in rRange) {
                Row {
                    for (f in fRange) {
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
