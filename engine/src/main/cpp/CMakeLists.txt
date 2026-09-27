cmake_minimum_required(VERSION 3.22.1)
project("llama_android")

add_library(
    llama_android
    SHARED
    llm_jni.cpp
)

find_library(log-lib log)

target_link_libraries(
    llama_android
    ${log-lib}
)
