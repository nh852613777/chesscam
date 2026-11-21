cmake_minimum_required(VERSION 3.22.1)
project("stockfish")

# Không tải source, chỉ biên dịch wrapper
add_library(stockfish SHARED stockfish_wrapper.cpp)

# Tải file binary đã biên dịch sẵn (từ repo uy tín)
set(STOCKFISH_BIN_URL "https://github.com/niklasb/stockfish-android/raw/master/libs/arm64-v8a/libstockfish.so")
set(STOCKFISH_BIN_PATH "${CMAKE_CURRENT_BINARY_DIR}/libstockfish_real.so")

if(NOT EXISTS "${STOCKFISH_BIN_PATH}")
    file(DOWNLOAD "${STOCKFISH_BIN_URL}" "${STOCKFISH_BIN_PATH}" SHOW_PROGRESS)
endif()

# Liên kết với thư viện thực
add_library(stockfish_real SHARED IMPORTED)
set_target_properties(stockfish_real PROPERTIES IMPORTED_LOCATION "${STOCKFISH_BIN_PATH}")
target_link_libraries(stockfish stockfish_real)
