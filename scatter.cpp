#include <iostream>
#include <mpi.h>
#include <stdlib.h>
#include <sys/time.h>

using namespace std;

int main(int argc, char ** argv)
{
	MPI_Init(&argc,&argv);

        int rank ; // id procesu
        MPI_Comm_rank( MPI_COMM_WORLD, &rank );
        int size ; // liczba procesow
        MPI_Comm_size( MPI_COMM_WORLD, &size );

        int num_data = 10;  // ilosc danych na proces
        int *bufor = new int [ num_data ]; // bufor do odbioru danych
        int *dane;
        
        if ( ! rank ) {
                dane = new int [ size * num_data ]; // alokowanie duzego bufora
                for ( int i = 0; i < size * num_data; i++ ) 
                  dane[ i ] = i;        // na pozycje i wpisujemy i
        } 
        
        MPI_Scatter( dane, num_data, MPI_INT, bufor, num_data, MPI_INT, 0, MPI_COMM_WORLD );
               // rozeslanie danych zapisanych w duzym buforze dane
               // trafiaja do mniejszych buforow (bufor) po kawalku 
               // dane -> N x bufor

        cout << "Tu proces " << rank << " bufor[ 0 ] = " << bufor[ 0 ] << endl;
  		// kazdy proces pokazuje zawartosc pierwszej komorki swojego bufora
        
        for ( int i = 0; i < num_data; i++ )
        {
           bufor[ i ] ++; // tu cos liczymy ;-)
        }

        MPI_Gather( bufor, num_data, MPI_INT, dane, num_data, MPI_INT, 0, MPI_COMM_WORLD );
           	// zbieramy dane. Dane z mniejszych buforow 
           	// umieszczane sa w duzym buforze dane
           	// N x bufor -> dane
        
        if ( ! rank )
          for ( int i = 0; i < num_data * size; i++ )
            cout << "( " << i << ", " << dane[ i ] << " ) ";
        
	MPI_Finalize();
}