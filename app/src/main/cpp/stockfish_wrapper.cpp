#include <jni.h>
#include <string>
#include <iostream>
#include <sstream>
#include "stockfish/src/uci.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_nsmod_chesscam_MainActivity_runStockfishNative(
        JNIEnv* env, jobject /* this */, jstring fen) {
    
    const char* fen_cstr = env->GetStringUTFChars(fen, nullptr);
    std::string fen_str(fen_cstr);
    env->ReleaseStringUTFChars(fen, fen_cstr);

    // Khởi tạo engine
    Stockfish::UCI::init(Stockfish::Options);
    Stockfish::Position pos;
    Stockfish::StateListPtr states(new std::deque<Stockfish::StateInfo>(1));
    pos.set(fen_str, false, &states->back(), nullptr);

    // Thiết lập giới hạn độ sâu
    Stockfish::Limits limits;
    limits.depth = 15;

    // Tìm nước đi tốt nhất
    Stockfish::Search::LimitsType searchLimits;
    searchLimits.depth = 15;
    Stockfish::Search::start_search(pos, states, searchLimits);

    // Lấy kết quả
    Stockfish::Move best = Stockfish::Search::best_move();
    std::string best_str = Stockfish::UCI::move(best, false);

    return env->NewStringUTF(best_str.c_str());
}
