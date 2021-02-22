// Sortowanie: natural merge 2+2 (?)
// Temat:
//          49. Rekordy pliku: 3 współrzędne w układzie kartezjańskim.
//          Uporządkowanie wg pola trójkąta tworzonego przez te współrzędne.


import com.sun.xml.internal.ws.api.ha.StickyFeature;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Scanner;

public class MainClass {

    public static void main(String[] args) throws IOException {
        boolean exit = false;
        Scanner scan = new Scanner(System.in);

        String filepath; // src\main\resources\tape_test
        Tape tape = null;
        String input, errorStr = "";

        while (!exit) {
            System.out.println("Enter number of action:" +
                    "\n     0) exit" +
                    "\n     1) import a tape form file" +
                    "\n     2) create a tape from keyboard input" +
                    "\n     3) generate a tape" +
                    "\n     4) generate a tape with range restriction" +
                    "\n     5) sort");

            input = scan.next();

            switch (input) {
                case "0":
                    // exit
                    exit = true;
                    break;
                case "1":
                    // import tape form file
                    System.out.println("Enter filepath");
                    filepath = scan.next();

                    tape = new Tape(filepath);
                    break;
                case "2":
                    // create tape from keyboard input
                    System.out.println("Enter filepath to save created tape");
                    filepath = scan.next();

                    tape = TapeGenerator.readRecordsFromKeyboard(filepath);
                    break;
                case "3":
                    // generate tape
                    System.out.println("Enter filepath to save generated tape");
                    filepath = scan.next();
                    System.out.println("Enter number of records");
                    int number = Integer.parseInt(scan.next());

                    tape = TapeGenerator.generateRecords(filepath, number);
                    break;
                case "4":
                    // generate tape with range restriction
                    System.out.println("Enter filepath to save generated tape");
                    filepath = scan.next();
                    System.out.println("Enter number of records");
                    int number2 = Integer.parseInt(scan.next());
                    System.out.println("Enter min and max value (in double) of coords in records");
                    double min = Double.parseDouble(scan.next());
                    double max = Double.parseDouble(scan.next());

                    tape = TapeGenerator.generateRecords(filepath, number2, min, max);
                    break;
                case "5":
                    // sort
                    if(tape != null) {
                        System.out.println("Do you want to print stages? [y/n]");
                        input = scan.next();
                        if (input.equals("y"))
                            MergeSort.fibonacciSort(tape, true);
                        else if (input.equals("n"))
                            MergeSort.fibonacciSort(tape, false);
                        else
                            errorStr = "Wrong input";
                    }
                    else {
                        errorStr ="Cannot perform sorting. Please load any tape.";
                    }
                    break;
                default:
                    errorStr = "Please enter proper number";
                    break;
            }

            scan.nextLine();
            System.out.println("----------------------------------------------------");
            System.out.println(errorStr);
            errorStr = "";
        }
    }

}