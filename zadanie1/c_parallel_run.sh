cp serial_200.txt serial.txt
mpirun -np 6 ./a.out 200 64 1901 100.0 0.1 200
