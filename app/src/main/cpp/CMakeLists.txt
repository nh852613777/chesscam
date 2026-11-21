cmake_minimum_required(VERSION 3.22.1)
project("stockfish")

add_library(stockfish SHARED stockfish_wrapper.cpp)

# Tải toàn bộ mã nguồn Stockfish và biên dịch
include(FetchContent)
FetchContent_Declare(
    stockfish
    GIT_REPOSITORY https://github.com/official-stockfish/Stockfish.git
    GIT_TAG sf_16.1
)
FetchContent_MakeAvailable(stockfish)

target_link_libraries(stockfish PRIVATE stockfish-lib)
