#!/usr/bin/env bash

echo "Kompiluję..."
c++ -O3 EnergyCalculator.cpp Helper.cpp main.cpp MonteCarlo.cpp RandomNumberGenerator.cpp Simulation.cpp
echo "Skompilowano"