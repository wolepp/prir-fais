#!/usr/bin/env bash

THREADS=6

bash compile_multithread.sh
if [ "$?" -ne 0 ]; then
  echo "Błędy kompilacji"
  exit 1
fi
echo "Uruchamiam z $THREADS wątkami..."
time env OMP_NUM_THREADS=6 ./a.out