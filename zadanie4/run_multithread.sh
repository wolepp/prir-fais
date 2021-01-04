#!/usr/bin/env bash

THREADS=6

bash compile_multithread.sh
echo "Uruchamiam z $THREADS wątkami..."
env OMP_NUM_THREADS="$THREADS" ./a.out