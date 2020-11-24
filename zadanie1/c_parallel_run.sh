cp serial_2000.txt serial.txt
mpirun -np 6 ./a.out 2000 64 1901 100.0 0.1 200
