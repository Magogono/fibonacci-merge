import java.io.*;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.Scanner;

public class TapeGenerator {

    public static Tape generateRecords(String filepath, int numberOfRecords) {
        return generateRecords(filepath, numberOfRecords, 0, 1);
    }


    public static Tape generateRecords(String filepath, int numberOfRecords, double minCoordValue, double maxCoordValue) {
        // Tworzenie lub nadpisywanie pliku na taśmę.
        File file = makeFile(filepath);
        if(file == null) {
            System.out.println("Failed to generate tape at " + filepath);
            return null;
        }

        // Generowanie pliku z rekordami.
        double genDouble;
        Random rand = new Random();

        // Obsługa strumienia do pliku.
        try (DataOutputStream dos = new DataOutputStream(
                new FileOutputStream(file))
        ) {

            // Losowanie @numberOfRecords rekordów.
            for(int recNum = 0; recNum < numberOfRecords; recNum++) {

                // Losowanie rekordu i zapisywanie do strumienia.
                for(int coordNum = 0; coordNum < 6; coordNum++) {
                    genDouble = minCoordValue + rand.nextDouble() * (maxCoordValue - minCoordValue);
                    dos.writeDouble(genDouble);
                }
            }

            dos.flush();
        }
        catch(FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        // System.out.println("Generated tape with " + numberOfRecords + " records at " + filepath);

        return new Tape(filepath);
    }


    public static Tape readRecordsFromKeyboard(String filepath) {
        // Tworzenie lub nadpisywanie pliku na taśmę.
        File file = makeFile(filepath);
        if(file == null) {
            System.out.println("Failed to read tape from keyboard to " + filepath);
            return null;
        }

        // Wczytywanie rekordów z klawiatury
        Scanner scan = new Scanner(System.in);
        double readDouble;
        byte[] pageBuffer = new byte[Tape.RECORD_SIZE];
        ByteBuffer bb = ByteBuffer.wrap(pageBuffer);
        int numberOfRecords = 0;

        System.out.println("Enter 6 decimals to create a record or press q to quit:");
        String input = scan.next();

        while(!input.equals("q")) {

            // Jeśli nie "q", to użytkownik musi podać 6 liczb do wczytania, żeby zapisać rekord.
            int coordsRead = 0;

            while(coordsRead < 6) {
                try {
                    readDouble = Double.parseDouble(input);     // rzuca wyjątek
                    bb.putDouble(readDouble);
                    coordsRead++;
                } catch (NumberFormatException e) {
                    System.out.println("Enter decimal value");
                }

                if(coordsRead < 6)
                    input = scan.next();
            }

            // Dopisanie zawartości bufora do pliku.
            try (FileOutputStream fos = new FileOutputStream(filepath, true))
            {
                fos.write(pageBuffer);
                fos.flush();
            }
            catch (IOException e) {
                e.printStackTrace();
                return null;
            }

            bb.clear();

            System.out.println("Enter 6 decimals to create a record or press q to quit:");
            input = scan.next();
        }


        System.out.println("Succcessfully created tape with " + file.length()/Tape.RECORD_SIZE + "records at " + filepath);

        return new Tape(filepath);
    }


    private static File makeFile(String filepath) {
        File newFile = new File(filepath);

        // Tworzenie pliku na taśmę, jeśli nie istnieje.
        boolean success = false;
        try {
            success = newFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        /*
        // Czyszczenie pliku na taśmę, jeśli już istnieje.
        if(success == false) {
            try {
                new FileOutputStream(filepath).close();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
         */
        return newFile;
    }
}
