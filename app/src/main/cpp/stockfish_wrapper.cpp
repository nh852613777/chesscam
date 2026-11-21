#include <jni.h>
#include <string>
#include <iostream>
#include <sstream>
#include <cstdio>
#include <cstdlib>
#include <unistd.h>
#include <sys/wait.h>
#include <fcntl.h>
#include <vector>
#include <cstring>

extern "C" JNIEXPORT jstring JNICALL
Java_com_nsmod_chesscam_MainActivity_runStockfishNative(
        JNIEnv* env, jobject /* this */, jstring fen) {
    
    const char* fen_cstr = env->GetStringUTFChars(fen, nullptr);
    std::string fen_str(fen_cstr);
    env->ReleaseStringUTFChars(fen, fen_cstr);

    // Tạo pipe để giao tiếp với Stockfish
    int in_pipe[2], out_pipe[2];
    if (pipe(in_pipe) < 0 || pipe(out_pipe) < 0) {
        return env->NewStringUTF("(none)");
    }

    pid_t pid = fork();
    if (pid == 0) {
        // Child process: chạy Stockfish
        dup2(in_pipe[0], STDIN_FILENO);
        dup2(out_pipe[1], STDOUT_FILENO);
        close(in_pipe[1]);
        close(out_pipe[0]);

        // Đường dẫn đến file binary Stockfish đã được đặt trong nativeLibraryDir
        // Nhưng vì đây là C++, chúng ta có thể gọi trực tiếp nếu nó được liên kết tĩnh
        // Tuy nhiên, để đơn giản, chúng ta sẽ dùng lệnh execvp với đường dẫn tuyệt đối
        // Đường dẫn này sẽ được truyền từ Java thông qua biến môi trường hoặc tham số
        // Tạm thời, chúng ta sẽ dùng một đường dẫn mặc định (sẽ được sửa sau)
        // Giải pháp: Trả về lỗi để Java tự xử lý
        exit(1);
    } else {
        // Parent process
        close(in_pipe[0]);
        close(out_pipe[1]);

        // Gửi lệnh UCI
        std::string uci_cmd = "uci\n";
        write(in_pipe[1], uci_cmd.c_str(), uci_cmd.size());

        // Đọc phản hồi
        char buffer[4096];
        std::string response;
        ssize_t n;
        while ((n = read(out_pipe[0], buffer, sizeof(buffer) - 1)) > 0) {
            buffer[n] = '\0';
            response += buffer;
            if (response.find("uciok") != std::string::npos) break;
        }

        // Gửi FEN
        std::string pos_cmd = "position fen " + fen_str + "\n";
        write(in_pipe[1], pos_cmd.c_str(), pos_cmd.size());

        // Gửi lệnh go
        std::string go_cmd = "go depth 15\n";
        write(in_pipe[1], go_cmd.c_str(), go_cmd.size());

        // Đọc bestmove
        std::string bestmove = "(none)";
        while ((n = read(out_pipe[0], buffer, sizeof(buffer) - 1)) > 0) {
            buffer[n] = '\0';
            std::string line(buffer);
            if (line.find("bestmove") != std::string::npos) {
                size_t start = line.find("bestmove ") + 9;
                size_t end = line.find(' ', start);
                if (end == std::string::npos) end = line.length();
                bestmove = line.substr(start, end - start);
                break;
            }
        }

        // Gửi quit
        std::string quit_cmd = "quit\n";
        write(in_pipe[1], quit_cmd.c_str(), quit_cmd.size());

        close(in_pipe[1]);
        close(out_pipe[0]);
        waitpid(pid, nullptr, 0);
    }

    return env->NewStringUTF("(none)");
}
