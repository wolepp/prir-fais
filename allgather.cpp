#include <stdlib.h>
#include<iostream>
#include<mpi.h>
#include <unistd.h>

using namespace std;


void fill( double *ptr, int size, double value ) {
   cout << "Wypelniam bufor" << endl;
   for ( int i = 0; i < size; i++ ) 
      ptr[ i ] = value;
}

int main(int argc, char *argv[]) {
  
   int numprocs;
   int myrank;
 
   MPI_Init(&argc,&argv); 
   MPI_Comm_rank(MPI_COMM_WORLD, &myrank); 
   MPI_Comm_size(MPI_COMM_WORLD, &numprocs); 

   srandom( myrank * 100 );
   int randomSize = 3 + random() % 7;

   if ( myrank == 2 ) {
     randomSize = 0; // brak czastek
   }

   cout << " W procesie numer " << myrank << " wylosowano " << randomSize << endl;
 
   double *ptr = new double[ randomSize ];
   fill( ptr, randomSize, myrank ); // tablice wypelniamy danymi

   int *sizes = new int[ numprocs ];
   int *displs = new int[ numprocs ];

//                skad         ile wyslac  gdzie  ile z kazdego procesu odbierac
//                 V           V             V    V
   MPI_Allgather( &randomSize, 1, MPI_INT, sizes, 1, MPI_INT, MPI_COMM_WORLD );

   // wyliczam przesuniecia

   displs[ 0 ] = 0;
   for ( int i = 1; i < numprocs; i++ ) 
     displs[ i ] = displs[ i - 1 ] + sizes[ i - 1 ];
   int allData = displs[ numprocs - 1 ] + sizes[ numprocs - 1 ];

   if ( myrank == 0 ) {
     for ( int i = 0; i < numprocs; i++ ) {
       cout << "Proces " << i << " ma " << sizes[ i ] << " danych do przekazania, przesuniecie " << displs[ i ] << endl;
     }
     cout << "Wszystkie dane to " << allData << endl;
   }

   double * bigBuffer4allData = new double[ allData ];

//                 skad ile         jakich danych wyslac
   MPI_Allgatherv( ptr, randomSize, MPI_DOUBLE, 

//                 gdzie              ile z kazdego procesu danych odebrac
                   bigBuffer4allData, sizes, 

//                 przesuniecie w buforze
                   displs, 

                   MPI_DOUBLE, MPI_COMM_WORLD );

     sleep( myrank );

     for ( int i = 0; i < allData; i++ ) {
       cout << "Proces " << myrank << " bigBuffer4allData[ " 
            << i << " ] = " << bigBuffer4allData[ i ] << endl;
     }

   MPI_Finalize();

   return 0;  
}
