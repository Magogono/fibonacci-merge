/*
Warstwa ta powinna udostępniać warstwie sorowania co najmniej 2
operacje: odczytu oraz zapisu pojedynczego rekordu. Rekordy do posortowania powinny być
generowane zarówno losowo, jak i z klawiatury. Program powinien wyświetlić zawartość pliku przed
posortowaniem i po posortowaniu. Program powinien też dawać możliwość wyświetlenia pliku po
każdej fazie sortowania. Na zakończenie należy wyświetlić liczbę faz sortowania oraz liczbę odczytów
i zapisów stron na dysk. Dodatkowo program powinien dawać możliwość wczytywania danych
testowych z pliku testowego.
 */

import sun.security.timestamp.TSRequest;

import java.awt.event.KeyEvent;
import java.io.*;

// Schemat 2+2
public class MergeSort {

    private static final int NUM_OF_TAPES = 3;
    private static boolean isOnlyOneRun;
    private static int dummyTape;
    private static int numOfDummyRuns;

    /*
     Sortowanie rosnące (najpierw zapisywane rekordy o mniejszym kluczu).
     */
    public static Tape fibonacciSort(Tape originalTape, boolean writePhases) {
        // Stwórz 3 pliki tymczasowe na taśmy pomocnicze.
        // Pliki zostaną usunięte przy zamykaniu JVM.
        File tmpFiles[] = new File[NUM_OF_TAPES];
        Tape tapes[] = new Tape[NUM_OF_TAPES];

        try {
            for (int i = 0; i < NUM_OF_TAPES; i++) {
                tmpFiles[i] = File.createTempFile("tmp", null);
                tmpFiles[i].deleteOnExit();

                tapes[i] = new Tape(tmpFiles[i].getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        System.out.println("\n=============== Fibonacci sort - start ===============");
        System.out.print("Original tape with " + originalTape.getNumOfRuns() + " runs " + originalTape.getName() + " : ");
        originalTape.printTape();

        // Ustawienie warunku końcowego na false.
        isOnlyOneRun = false;

        // ================ ALGORYTM ================
        int howManyEmptyTapes;
        int outputIndex = 2;
        int phases = 1;

        initialDistribution(originalTape, tapes);

        if(writePhases) {
            System.out.println("\n=============== Performed initial distrubution: ===============");
            System.out.print("Tape 0: ");
            tapes[0].printTape();
            System.out.print("Tape 1: ");
            tapes[1].printTape();
        }

        System.out.println("\n=============== Sorting: ===============");

        while(!isOnlyOneRun) {
            merge(tapes, outputIndex);

            // Wypisz efekt fazy.
            if(writePhases) {
                System.out.println("->  After phase " + (phases++) + ":");
                for (int i = 0; i < NUM_OF_TAPES; i++) {
                    System.out.print("Tape " + i + ": ");
                    tapes[i].printTape();
                }
            }

            // Sprawdź osiągnięcie warunku końcowego - jednej posortowanej taśmy.
            howManyEmptyTapes = 0;
            for(int i=0; i < NUM_OF_TAPES; i++) {
                if(!tapes[i].hasNextRecord()) {
                    howManyEmptyTapes++;
                    outputIndex = i;
                }
            }

            if(howManyEmptyTapes == 2)
                isOnlyOneRun = true;
        }

        // Wypisz informacje zwrotne.
        Tape sortedTape = returnSortedTape(tapes, originalTape.getPath());
        System.out.print("\nSorted tape " + sortedTape.getName() + " : ");
        sortedTape.printTape();
        System.out.println("=============== Fibonacci sort - end ===============\n");
        System.out.println("Total number of phases: " + (phases-1));
        int reads = 0, writes = 0;
        for(int i = 0; i < NUM_OF_TAPES; i++) {
            reads += tapes[i].getNumOfReads();
            writes += tapes[i].getNumOfWrites();
        }
        reads += originalTape.getNumOfReads();
        System.out.println("Total number operations: " + reads + " reads, " + writes + " writes");

        return sortedTape;
    }

    /*
    Zwraca taśmę jako plik trwały utworzony z otrzymanej posortowanej taśmy pomocniczej (pliku tmp).
     */
    private static Tape returnSortedTape(Tape[] tapes, String filename) {
        int i = 0;
        for(; i < NUM_OF_TAPES; i++) {
            if(tapes[i].hasNextRecord())
                break;
        }

        File sortedFile = new File(tapes[i].getPath());

        // Stwórz plik.
        File newFile = new File(filename + "_sorted");
        boolean success = false;
        try {
            success = newFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }


        // Zapisz do pliku.
        try (InputStream is = new FileInputStream(sortedFile);
                 OutputStream os = new FileOutputStream(newFile, false)
        ) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) > 0) {
                os.write(buffer, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return new Tape(newFile.getAbsolutePath());
    }

    /*
    Scala kolejne serie z taśm wejściowych (o indeksach innych niż @outIndex)
    na taśmę wyjściową (@tapes[outIndex]).
    W momencie, kiedy jedna z pobieranych taśm się skończy, dopisuje resztę bieżącej serii
    z drugiej taśmy na wyjściową, a następne nieodczytane rekordy zostawia na tej taśmie.
    */
    private static void merge(Tape[] tapes, int outIndex) {
        // Ustal indeksy taśm wypełnionych seriami.
        int a, b;
        if(outIndex != 0) {
            a = 0;
            if(outIndex != 1)
                b = 1;
            else
                b = 2;
        }
        else {
            a = 1;
            b = 2;
        }

        // Stwórz wrappery na taśmy czytane i taśmę zapisu, indeksy A i B odnoszą się do wrapperów.
        Tape[] inputTapes = {tapes[a], tapes[b]};
        Tape outputTape = tapes[outIndex];
        final int A = 0, B = 1;

        // Zacznij czytanie i pisanie taśm.
        // Taśmy czytane to @inputTapes[A] i @inputTapes[B], a taśma wyjściowa to @outputTape.
        inputTapes[A].startReadingTape();
        inputTapes[B].startReadingTape();
        outputTape.startWritingTape();


        // ===========================================
        // Scalanie - rozpoczęcie.

        boolean[] endOfRun = {false, false};
        Record[]  currRec = {null, null},
                prevRec = {new Record(), new Record()};

        // Wczytaj pierwszy rekord z każdej taśmy.
        currRec[A] = inputTapes[A].getRecord();
        currRec[B] = inputTapes[B].getRecord();

        // !!! Wykonywane tylko przy pierwszym scalaniu:
        // uwzględnij puste serie na dłuższej taśmie, jeśli istnieją,
        // przepisz @numOfDummyRuns serii z mniejszej taśmy na taśmę wyjściową.
        if(numOfDummyRuns > 0) {
            // W inicialDistribution() wypełniamy taśmy o indeksach 0 i 1.
            int ind = (dummyTape + 1) % 2; // Bierzemy krótszą taśmę.

            while (numOfDummyRuns > 0) {
                currRec[ind] = saveRun(currRec[ind], prevRec[ind], inputTapes[ind], outputTape);

                numOfDummyRuns--;
            }
        }

        // ===========================================
        // Właściwy algorytm.

        // Póki nie skończy się jedna z taśm, scalaj po jednej z serii z taśm wejściowych na taśmę o wyjściową.
        while(currRec[A] != null && currRec[B] != null) {

            // Scalaj, aż nie skończy się jedna z serii (lub plik).
            while(!endOfRun[A] && !endOfRun[B]) {
                // Zapisz rekord, który jest mniejszy/równy.
                int ind =
                        currRec[A].compareTo(currRec[B]) <= 0 ? A : B;

                prevRec[ind].copyFrom(currRec[ind]);
                outputTape.saveRecord(currRec[ind]);

                // Pobierz kolejny rekord z taśmy.
                currRec[ind] = inputTapes[ind].getRecord();

                // Sprawdź czy nie skończył się plik lub seria.
                if (currRec[ind] == null
                        || currRec[ind].compareTo(prevRec[ind]) < 0) {
                    endOfRun[ind] = true;
                }
            }

            // Jeśli skończyła się seria, to przepisz resztę tej nieskończonej na taśmę i wróć na początek.
            int ind = !endOfRun[A] ? A : B;

            while (!endOfRun[ind]) {
                prevRec[ind].copyFrom(currRec[ind]);
                outputTape.saveRecord(currRec[ind]);

                // Pobierz kolejny rekord z taśmy B.
                currRec[ind] = inputTapes[ind].getRecord();

                // Sprawdź czy nie skończył się plik lub seria.
                if (currRec[ind] == null
                        || currRec[ind].compareTo(prevRec[ind]) < 0) {
                    endOfRun[ind] = true;
                }
            }

            // Wyzeruj flagi dla serii.
            endOfRun[A] = false;
            endOfRun[B] = false;
        }

        // ===========================================

        // Zakończ czytanie i pisanie taśm.
        inputTapes[A].endReadingTape(true);
        inputTapes[B].endReadingTape(true);
        outputTape.endWritingTape();
    }

    /*
    Dzieli serie z taśmy wejściowej na dwie taśmy o indeksach 0 i 1 w taki sposób,
    że liczby serii zapisanych taśmach to kolejne liczby Fibonacciego.
    Ustawia pole @dummyTape na indeks taśmy wynikowej z większą liczbą serii
    oraz pole @numOfDummyRuns na liczbę pustych serii, którymi "uzupełniamy"
    tę taśmę do pełnej liczby Fibonacciego.
     */
    private static void initialDistribution(Tape inputTape, Tape[] tapes) {
        // Należy kontrolować czy przy naprzemiennym pisaniu na taśmy, serie na jednej z nich się nie sklejają.
        inputTape.startReadingTape();
        tapes[0].startWritingTape();
        tapes[1].startWritingTape();

        // @currRec to referencja na Rekord zwracany przez funkcję getRecord() taśmy.
        // @lastRec[i] to rekord, który przechowuje skopiowaną wartość ostatniego zapisanego,
        // wskazuje zawsze ten sam obiekt.
        Record currRec;
        Record[] lastRec = new Record[2];
        int[] numOfRuns = {0, 0};
        int whichTape = 0;
        int nextFibNum = 1;


        // Odczytaj pierwszy rekord.
        currRec = inputTape.getRecord();

        while(currRec != null) {
            // Zwiększ licznik, jeśli seria nie sklei się z poprzednią.
            if(lastRec[whichTape] == null) {
                lastRec[whichTape] = new Record();
                numOfRuns[whichTape]++;
            }
            else if(currRec.compareTo(lastRec[whichTape]) < 0) {
                numOfRuns[whichTape]++;
            }

            // Zapisz kolejną serię na wybraną taśmę.
            currRec = saveRun(currRec, lastRec[whichTape], inputTape, tapes[whichTape]);

            if(currRec != null) {
                // Jeśli osiągnięto następną liczbę Fibonacciego z serii, to zmień taśmę.
                if (numOfRuns[whichTape] == nextFibNum) {
                    int otherTape =
                            whichTape == 0 ? 1 : 0;
                    nextFibNum += numOfRuns[otherTape];
                    whichTape = otherTape;
                }
            }
        }

        dummyTape = whichTape;

        if(numOfRuns[dummyTape] != nextFibNum)
            numOfDummyRuns = nextFibNum - numOfRuns[dummyTape];

        inputTape.endReadingTape(false);
        tapes[0].endWritingTape();
        tapes[1].endWritingTape();
    }

    /*
    Zapisuje serię składającą się z @currRec i kolejnych odczytanych rekordów.
    Zwraca referencję na ostatni zapisany rekord z serii.
    Ustawia przekazany parametr @currRec na ostatni odczytany z taśmy rekord.
    */
    private static Record saveRun(Record currRec, Record prevRec, Tape input, Tape output) {

        if(currRec == null) {
            currRec = input.getRecord();
            if(currRec == null)
                return null;
        }

        prevRec.copyFrom(currRec);
        output.saveRecord(currRec);

        while((currRec = input.getRecord()) != null) {
            if (currRec.compareTo(prevRec) >= 0) {
                // Odczytany rekord należy do serii - zapisz go.
                prevRec.copyFrom(currRec);
                output.saveRecord(currRec);
            }
            else
                // Odczytany rekord nie należy do serii - zakończ.
                break;
        }

        // Zwróć ostatni zapisany rekord.
        return currRec;
    }

}
