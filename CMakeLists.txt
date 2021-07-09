cmake_minimum_required(VERSION 3.19)
project(JarInjector C)

set(CMAKE_C_STANDARD 99)

add_library(JarInjector SHARED library.c library.h)
target_include_directories(JarInjector PRIVATE /usr/lib/jvm/java-8-openjdk/include/linux /usr/lib/jvm/java-8-openjdk/include)

list(APPEND CMAKE_DL_LIBS "/usr/lib/jvm/java-8-openjdk/jre/lib/amd64/server")

target_link_libraries(JarInjector ${CMAKE_DL_LIBS})