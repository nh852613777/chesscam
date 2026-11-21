package com.nsmod.chesscam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ChessScreen() }
    }

    @Composable
    fun ChessScreen() {
        var grid by remember { mutableStateOf(startGrid()) }
        var input by remember { mutableStateOf("") }
        var status by remember { mutableStateOf("Nhập UCI, vd: e2e4") }

        Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("ChessCam", fontSize = 22.sp)
            Spacer(Modifier.height(12.dp))
            BoardView(grid)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Nước đi (UCI)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row {
                Button(onClick = {
                    val r = applyMove(grid, input.trim())
                    if (r == null) status = "Nước không hợp lệ: $input"
                    else { grid = r.first; status = r.second }
                    input = ""
                }) { Text("Đi") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { grid = startGrid(); status = "Reset" }) { Text("Reset") }
            }
            Spacer(Modifier.height(8.dp))
            Text(status, fontSize = 14.sp)
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

    fun applyMove(g: Array<Array<Char>>, uci: String): Pair<Array<Array<Char>>, String>? {
        if (uci.length < 4) return null
        val f1 = uci[0] - 'a'; val r1 = 8 - (uci[1] - '0')
        val f2 = uci[2] - 'a'; val r2 = 8 - (uci[3] - '0')
        if (f1 !in 0..7 || r1 !in 0..7 || f2 !in 0..7 || r2 !in 0..7) return null
        val piece = g[r1][f1]
        if (piece == '.') return null
        val newG = Array(8) { g[it].copyOf() }
        newG[r2][f2] = piece
        newG[r1][f1] = '.'
        return Pair(newG, "Đã đi $uci")
    }

    @Composable
    fun BoardView(g: Array<Array<Char>>) {
        Column {
            for (r in 0 until 8) {
                Row {
                    for (f in 0 until 8) {
                        val piece = g[r][f]
                        val light = (r + f) % 2 == 0
                        Surface(
                            color = if (light) Color(0xFFEEEED2) else Color(0xFF769656),
                            modifier = Modifier.size(40.dp)
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
