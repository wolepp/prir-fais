#include "Simulation.h"
#include "Helper.h"
#include <stdlib.h>
#include <iostream>
#include <math.h>
#include <iomanip>

using namespace std;


void Simulation::d(char *f, char *str) const {
    printf("DEBUG-sim:%s@%d %s\n", f, myRank, str);
}

Simulation::Simulation(MyMPI *_mmpi) {
    mmpi = _mmpi;
}

void Simulation::setRandomNumberGenerator(RandomNumberGenerator *randomNumberGenerator) {
    this->rng = randomNumberGenerator;
}

void Simulation::setEnergyCalculator(EnergyCalculator *energyCalculator) {
    this->energyCalculator = energyCalculator;
}

void Simulation::setMonterCarlo(MonteCarlo *mc) {
    this->mc = mc;
}

void Simulation::setMaxChange(double maxChange) {
    this->maxChange = maxChange;
}

void Simulation::setInitialData(double *data, int size) {
    this->data = data;
    this->size = size;
    reducedSize = size - 4;
}

void Simulation::init() {
    // ustaw dane dotyczące procesów
    mmpi->MPI_Comm_rank(MPI_COMM_WORLD, &myRank);
    mmpi->MPI_Comm_size(MPI_COMM_WORLD, &processes);

    // roześlij size i przygotuj pamięć dla innych procesów
    mmpi->MPI_Bcast(&size, 1, MPI_INT, 0, MPI_COMM_WORLD);
    if (myRank) {
        data = new double[size * size];
    }

    // roześlij data
    mmpi->MPI_Bcast(data, size * size, MPI_DOUBLE, 0, MPI_COMM_WORLD);

    // przydziel każdemu procesowi własną część do obliczeń
    int part = size / processes;
    myColStart = myRank * part + 2;
    if (myRank == processes - 1) {
        myColEnd = size - 2;
    } else {
        myColEnd = myColStart + part - 1;
    }
}

void Simulation::calcInitialTotalEnergy() {
    Etot = this->calcTotalEnergy();
}

double Simulation::calcTotalEnergy() {
    double Etot = 0.0;
    double reducedEtot;
    for (int row = 2; row < size - 2; row++)
        for (int col = myColStart; col < myColEnd; col++)
            Etot += energyCalculator->calc(data, size, row, col);
    mmpi->MPI_Allreduce(&Etot, &reducedEtot, 1, MPI_DOUBLE, MPI_SUM, MPI_COMM_WORLD);
    return reducedEtot * 0.5;
}

double Simulation::getTotalEnergy() {
    return Etot;
}

void Simulation::setDataToChangeInSingleStep(int dataToChange) {
    this->dataToChange = dataToChange;
    rows = new int[dataToChange];
    cols = new int[dataToChange];
    delta = new double[dataToChange];
}

void Simulation::generateDataChange() {
    for (int i = 0; i < dataToChange; i++) {
        rows[i] = 2 + rng->getInt(reducedSize);
        cols[i] = 2 + rng->getInt(reducedSize);
        delta[i] = maxChange * (1.0 - 2.0 * rng->getDouble());
    }
}

void Simulation::changeData() {
    for (int i = 0; i < dataToChange; i++) {
        Helper::updateValue(data, size, rows[i], cols[i], delta[i]);
    }
}

void Simulation::changeDataUndo() {
    for (int i = 0; i < dataToChange; i++) {
        Helper::updateValue(data, size, rows[i], cols[i], -delta[i]);
    }
}

void Simulation::singleStep(int i) {
    if (!myRank) {
        generateDataChange(); // wygenerowanie danych potrzebnych do zmiany stanu
    }
    mmpi->MPI_Bcast(rows, dataToChange, MPI_INT, 0, MPI_COMM_WORLD);
    mmpi->MPI_Bcast(cols, dataToChange, MPI_INT, 0, MPI_COMM_WORLD);
    mmpi->MPI_Bcast(delta, dataToChange, MPI_DOUBLE, 0, MPI_COMM_WORLD);
    changeData(); // zmiana danych (stanu)

    // calcTotalEnergy
    double newEtot = calcTotalEnergy(); // wyliczenie nowej energii całkowitej

    bool accepted = true;
    if (!myRank) {
        // decyzja modulu MonteCarlo o akceptacji zmiany
        if (mc->accept(Etot, newEtot)) {
            cout << i << ". Accepted Eold " << Etot << " newE " << newEtot << endl;
            Etot = newEtot;
            // zaakceptowano zmiane -> nowa wartosc energii calkowitej
        } else {
            accepted = false;
            cout << i << ". Not accepted Eold " << Etot << " newE " << newEtot << endl;
            // zmiany nie zaakceptowano -> przywracany stary stan, energia bez zmiany
        }
    }

    mmpi->MPI_Bcast(&accepted, 1, MPI_CXX_BOOL, 0, MPI_COMM_WORLD);
    if (!accepted)
        changeDataUndo();
}