import java.awt.*;
import java.io.*;
import java.nio.ByteBuffer;

// klasa reprezentuje stronę dyskową - blok danych
public class Tape {
    // stałe oznaczające rozmiar strony i rekordu - 20 rekordów na stronę
    private static final int PAGE_SIZE = 960;
    // Double = 8 B, so one record (6 doubles) is 6x8=48 B long.
    public static final int RECORD_SIZE = 6*Double.BYTES;

    private int numOfImportOperations;
    private int numOfExportOperations;

    private int nextInputPageInd;
    private int nextInputRecordInd;
    private int maxInputRecordInd;
    private boolean endOfInputFile;
    private int nextOutputRecordInd;
    private File tapeFile;
    private Record[] inputBuffer;
    private Record[] outputBuffer;

    // Konstruktor: jeśli plik istnieje, to wczytujemy go jako taśmę,
    //              w przeciwnym wypadku tworzymy nowy plik na zapis taśmy.
    // Opcjonalnie wypisuje nazwę i rozmiar czytanego/tworzonego pliku taśmy.
    public Tape(String filepath) {
        this(filepath, false);
    }

    public Tape(String filepath, boolean writeLog) {
        this.tapeFile = new File(filepath);

        // Jeśli plik istnieje, to wczytujemy go jako taśmę,
        // a w przeciwnym wypadku tworzymy nowy plik na zapis taśmy.
        boolean success = false;
        try {
            success = tapeFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(writeLog) {
            if (success)
                System.out.print("Creating new file ");
            else
                System.out.print("Reading file ");

            System.out.print(tapeFile.getName() + " as tape, size in bytes " + tapeFile.length() + "\n");
        }

        inputBuffer = new Record[PAGE_SIZE/RECORD_SIZE];
        outputBuffer = new Record[PAGE_SIZE/RECORD_SIZE];
        for(int i=0; i < inputBuffer.length; i++) {
            inputBuffer[i] = new Record();
            outputBuffer[i] = new Record();
        }

        numOfImportOperations = 0;
        numOfExportOperations = 0;
    }


    public int getNumOfRuns() {
        startReadingTape();
        double prevArea = 0.0;
        Record record;
        int numOfRuns = 0;

        while((record = getRecord()) != null) {
            if (prevArea > record.getArea())
                numOfRuns++;
            prevArea = record.getArea();
        }
        numOfImportOperations = 0;

        endReadingTape(false);
        return numOfRuns+1;
    }

    public int getNumOfReads() {
        return numOfImportOperations;
    }

    public int getNumOfWrites() {
        return numOfExportOperations;
    }

    public String getName() {
        return tapeFile.getName();
    }

    public String getPath() {
        return tapeFile.getPath();
    }

    // Wypisuje klucze sortowania (pole trójkąta) z całej taśmy.
    public void printTape() {
        // Bufor na wczytanie strony.
        byte[] pageBuffer = new byte[RECORD_SIZE];
        ByteBuffer bb;
        int read = 0;
        Record record = new Record();
        double prevArea = 0.0;

        // Czytanie strony ze strumienia pliku.
        try (FileInputStream fis = new FileInputStream(tapeFile))
        {
            read = fis.read(pageBuffer);
            while(read == RECORD_SIZE) {
                bb = ByteBuffer.wrap(pageBuffer);

                // Odczytywanie kolejnych rekordów z wczytanego strumienia bajtów.
                record.setA(bb.getDouble(), bb.getDouble());
                record.setB(bb.getDouble(), bb.getDouble());
                record.setC(bb.getDouble(), bb.getDouble());

                if(prevArea > record.getArea())
                    System.out.print("| ");
                System.out.print( String.format("%.3f", record.getArea()) + " ");
                prevArea = record.getArea();

                read = fis.read(pageBuffer);
            }
        }
        catch(FileNotFoundException e) {
            System.out.println("Cannot open the input file");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        System.out.print("\n");
    }

    public boolean hasNextRecord() {
        return tapeFile.length() != 0;
    }

    public void startReadingTape(){
        resetInput();
    }

    // Nadpisuje plik taśmy rekordami, które nie zostały użyte:
    // ostatnim odczytanym oraz nieodczytanymi.
    // Sprawdzone - działa okej.
    public void endReadingTape(boolean clearTape) {
        if(clearTape) {
            if (endOfInputFile == false) {
                // Stwórz plik tymczasowy tmp.
                File tmpFile;
                try {
                    // creates temporary file
                    tmpFile = File.createTempFile("tmp", null);

                    // deletes file when the virtual machine terminate
                    tmpFile.deleteOnExit();
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }

                // Zapisz nieodczytane rekordy do pliku tmp.
                try (InputStream is = new FileInputStream(tapeFile);
                     OutputStream os = new FileOutputStream(tmpFile)
                ) {
                    // Pomiń strony i rekordy, które zostały odczytane.
                    is.skip((nextInputPageInd - 1) * PAGE_SIZE + (nextInputRecordInd - 1) * RECORD_SIZE);

                    // Zapisz pozostałe bajty do pliku tmp.
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = is.read(buffer)) > 0) {
                        os.write(buffer, 0, read);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                // Przepisz plik tmp do taśmy.
                try (InputStream is = new FileInputStream(tmpFile);
                     OutputStream os = new FileOutputStream(tapeFile, false)
                ) {
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = is.read(buffer)) > 0) {
                        os.write(buffer, 0, read);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                // Plik tmp zostanie usunięty przy zamykaniu JVM.
            }
            else
                clearTapeFile();
        }
    }

    public void startWritingTape() {
        clearTapeFile();
        endOfInputFile = false;
    }

    public void endWritingTape() {
        flushOutputBuffer();
        //System.out.println("Saved file with bytes " + tapeFile.length() + " at " + filepath);
        endOfInputFile = true;
    }

    private void resetInput(){
        nextInputPageInd = 0;
        nextInputRecordInd = 0;
        maxInputRecordInd = -1;
        endOfInputFile = false;
    }

    private void clearTapeFile() {
        try {
            new FileOutputStream(tapeFile).close();
        } catch (IOException e) {
            System.out.println("Failed to clear tape file: " + tapeFile.getName());
            e.printStackTrace();
        }
        nextOutputRecordInd = 0;
    }

    // Zapisuje pozostałe w buforze dane do pliku.
    // Trzeba pamiętać o wywołaniu po zakończeniu pisania do taśmy.
    private void flushOutputBuffer() {
        // Gdy nextOutputRecordInd = 0, to bufor do zapisania jest pusty,
        // gdy nextOutputRecordInd == outputBuffer.lenght, to bufor jest pełny.
        if(0 < nextOutputRecordInd && nextOutputRecordInd <= outputBuffer.length)
            exportPage();
    }


    // Double = 8 B, so one record (3 doubles) is 3x8=24 B long.
    // RECORD_SIZE = 24;
    // ustawia endOfInputFile = true, kiedy nie udało się wczytać żadnego
    // pełnego rekordu z importowanej strony
    // @return true - odczytano dane,
    // @return false - koniec taśmy (lub błąd?).
    private boolean importPage() {
        // Bufor na wczytanie strony.
        byte[] pageBuffer = new byte[PAGE_SIZE];
        int read = 0;

        // Czytanie strony ze strumienia pliku.
        try (FileInputStream fis = new FileInputStream(tapeFile))
        {
            // Pomiń poprzednie strony + aktualną i odczytaj bajty na kolejnej stronie.
            fis.skip(nextInputPageInd * PAGE_SIZE);
            read = fis.read(pageBuffer);
        }
        catch(FileNotFoundException e)
        {
            System.out.println("Cannot Open the Input File");
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // Sprawdzanie, czy odczytywana część była niepusta.
        // Jeśli tak, to odczytujemy rekordy ze strony.
        if(read != -1) {
            int recordNum = 0;
            ByteBuffer bb = ByteBuffer.wrap(pageBuffer);

            // Odczytywanie kolejnych rekordów z wczytanego strumienia bajtów.
            while(read >= RECORD_SIZE) {
                inputBuffer[recordNum].setA(bb.getDouble(), bb.getDouble());
                inputBuffer[recordNum].setB(bb.getDouble(), bb.getDouble());
                inputBuffer[recordNum].setC(bb.getDouble(), bb.getDouble());

                read -= RECORD_SIZE;
                recordNum++;
            }

            // jeśli odczytano pełną stronę, to maxInputRecordNum = PAGE_SIZE/RECORD_SIZE
            // jeśli nie odczytano pełnej strony, to maxInputRecordNum < PAGE_SIZE/RECORD_SIZE
            maxInputRecordInd = recordNum - 1;

            // ustawiamy licznik rekordu do pobrania na początek bufora
            nextInputRecordInd = 0;

            // jeśli wczytano kilka znaków, ale nie wypełniły one rekordu, to znaczy, że koniec pliku
            if(maxInputRecordInd < 0)
                endOfInputFile = true;

            // zwiększamy numer strony (nawet jeśli nie wczytano żadnego pełnego rekordu)
            nextInputPageInd++;

            // zwiększamy licznik odczytów strony z dysku
            numOfImportOperations++;

            return true;
        }
        endOfInputFile = true;
        return false;
    }

    // Pobiera rekord z bufora.
    // @return pointer to table inputBuffer elem
    // or @return null if endOfInputFile
    public Record getRecord() {
        if (endOfInputFile == true) {
            return null;
        } else if (nextInputRecordInd <= maxInputRecordInd) {
            return inputBuffer[nextInputRecordInd++];
        }
        // Jeśli ostatnio nie odczytano pełnej strony
        // i nextInputRecordInd > maxInputRecordInd
        else if (maxInputRecordInd >= 0
                && maxInputRecordInd < PAGE_SIZE / RECORD_SIZE - 1) {
            endOfInputFile = true;
            return null;
        }
        // maxInputRecordInd < 0 lub
        // maxInputRecorInd == PAGE_SIZE/RECORD_SIZE-1, więc wczytano ostatnio pełną stronę
        // i  nextInputRecordInd > maxInputRecordInd
        // czyli najpierw importujemy następną stronę i dopiero wtedy próbujemy pobrać rekord.
        else {
            importPage();
            return getRecord();
        }
    }

    private boolean exportPage() {
        // nextRecordInd wskazuje na puste miejsce, przygotowane na kolejny rekord,
        // stąd konieczność pomniejszenia o 1, by uzyskać indeks ostatniego rekordu w buforze.
        int maxExportableRecordInd = nextOutputRecordInd - 1;

        // Bufor na zapisanie strony.
        byte[] pageBuffer = new byte[(maxExportableRecordInd+1)*RECORD_SIZE];

        int recordInd = 0;
        ByteBuffer bb = ByteBuffer.wrap(pageBuffer);

        // Zapisywanie kolejnych rekordów do bufora.
        while (recordInd <= maxExportableRecordInd) {
            bb.put(outputBuffer[recordInd].convertToBytes());
            recordInd++;
        }

        // Dopisanie zawartości bufora do pliku.
        try (FileOutputStream fos = new FileOutputStream(tapeFile, true))
        {
            fos.write(pageBuffer);
            fos.flush();
        }
        catch(FileNotFoundException e)
        {
            System.out.println("Cannot open the output file");
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // Ustawiamy licznik rekordu do zapisania na początek bufora.
        nextOutputRecordInd = 0;

        // Skoro dopisujemy do końca pliku, to nie trzeba liczyć stron.

        // Zwiększamy licznik zapisów strony na dysk.
        numOfExportOperations++;

        return true;
    }

    public void saveRecord(Record record) {
        if(nextOutputRecordInd < outputBuffer.length) {
            // Zapisujemy rekord do bufora (poprzez kopiowanie rekordu wejściowego).
            outputBuffer[nextOutputRecordInd++].copyFrom(record);
        }
        else {
            exportPage();
            saveRecord(record);
        }
    }

}
