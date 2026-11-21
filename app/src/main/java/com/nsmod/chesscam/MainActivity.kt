    fun runStockfish(fen: String): AnalysisResult? {
        try {
            val binFile = File(filesDir, "stockfish")
            if (!binFile.exists()) {
                // Copy từ assets ra bộ nhớ nội bộ
                assets.open("stockfish.bin").use { input ->
                    binFile.outputStream().use { output -> input.copyTo(output) }
                }
            }
            // Cấp quyền thực thi (chmod 755) cho file
            binFile.setExecutable(true, false)

            // Khởi chạy tiến trình
            val process = ProcessBuilder(binFile.absolutePath).redirectErrorStream(true).start()
            val writer = process.outputStream.bufferedWriter()
            val reader = process.inputStream.bufferedReader()

            writer.write("uci\n"); writer.flush()
            var line: String?
            var uciOk = false
            while (true) { 
                line = reader.readLine() ?: break
                if (line == "uciok") { uciOk = true; break } 
            }
            
            if (!uciOk) return null // Nếu không nhận được uciok, báo lỗi

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
