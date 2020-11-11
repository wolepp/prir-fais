cmake_minimum_required(VERSION 3.10)

project(Zadanie1)

find_package(MPI REQUIRED)
include_directories(${MPI_INCLUDE_PATH})

SET(CMAKE_C_COMPILER mpicc)
SET(CMAKE_CXX_COMPILER mpicxx)

add_executable(Zadanie1 EnergyCalculator.cpp Helper.cpp main.cpp MonteCarlo.cpp mpiusage.cpp MyMPI.cpp RandomNumberGenerator.cpp Simulation.cpp)
target_link_libraries(Zadanie1 ${SOURCE_FILES})