#include <mpi.h>
#include <unistd.h>

long hard_work() {
    long EPOCHS=500000000L;
    long x=2;
    for (long i=3;i<EPOCHS;i++) {
        x=x+i;
    }
    return x;
}

int main(int argc, char *argv[]) {
    int npes;
    int myrank;

    // Inicjalizacja MPI
    MPI_Init(&argc, &argv);

    // Pobierz rozmiar globalnego komunikatora
    MPI_Comm_size(MPI_COMM_WORLD, &npes);

    // Pobierz numer procesu
    MPI_Comm_rank(MPI_COMM_WORLD, &myrank);
    printf("Jestem %d procesem z %d\n", myrank, npes);
    
    if ( ! myrank) {
        long x;
        printf("Tu zero. %d Czekam...\n", myrank);

        MPI_Status status;
        int odebranych;
        int nadawca;
        MPI_Recv(&x, 1, MPI_LONG, MPI_ANY_SOURCE, 222, MPI_COMM_WORLD, &status);
        nadawca = status.MPI_SOURCE;
        MPI_Get_count(&status, MPI_LONG, &odebranych);
        
        printf("Tu zero. Otrzymałem %d od %d, elementów: %d\n", x, nadawca,odebranych);
    } else if ( myrank ) {
        printf("Tu %d. Liczę...\n", myrank);
        long x = hard_work();
        MPI_Send(&x, 1, MPI_LONG, 0, 222, MPI_COMM_WORLD);
        printf("Tu %d. Wysłałem %d\n", myrank, x);
    }
    MPI_Finalize();
    return 0;
}