cp serial_260.txt serial.txt
mpirun -np 2 ./a.out 260 64 1901 100.0 0.1 200
