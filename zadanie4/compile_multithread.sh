#!/usr/bin/env bash

echo "Kompiluję z openMP..."
c++ -O3 -fopenmp EnergyCalculator.cpp Helper.cpp main.cpp MonteCarlo.cpp RandomNumberGenerator.cpp Simulation.cpp
if [ "$?" -ne 0 ]; then
  exit 1
fi
echo "Skompilowano"
