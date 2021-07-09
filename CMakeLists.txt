cmake_minimum_required(VERSION 3.19)
project(JarInjector_so C)

set(CMAKE_C_STANDARD 99)

add_library(JarInjector_so SHARED library.c library.h)
target_include_directories(JarInjector_so PRIVATE /usr/lib/jvm/java-8-openjdk/include/linux /usr/lib/jvm/java-8-openjdk/include)

list(APPEND CMAKE_DL_LIBS "/usr/lib/jvm/java-8-openjdk/jre/lib/amd64/server")

target_link_libraries(JarInjector_so ${CMAKE_DL_LIBS})