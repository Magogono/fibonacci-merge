//          49. Rekordy pliku: 3 punkty (6 współrzędnych) w układzie kartezjańskim.
//          Uporządkowanie wg pola trójkąta tworzonego przez te współrzędne.

import java.nio.ByteBuffer;

import static java.lang.Math.abs;

public class Record {

    private class Point {
        private double x;
        private double y;

        private Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        private String getString(){
            return x + ", " + y;
        }
    }

    private double area;
    private Point a;
    private Point b;
    private Point c;

    public Record() {
        a = new Point(0, 0);
        b = new Point(0, 0);
        c = new Point(0, 0);
        area = 0;
    }

    public void setA(double x, double y) {
        this.a.x = x;
        this.a.y = y;
    }

    public void setB(double x, double y) {
        this.b.x = x;
        this.b.y = y;
    }

    public void setC(double x, double y) {
        this.c.x = x;
        this.c.y = y;
    }

    @Override
    public String toString() {
        return "a: " + a.getString() + " b: " + b.getString() + " c: " + c.getString();
    }

    public double getArea() {
        area = 0.5 * abs(
                (b.x - a.x)*(c.y - a.y) - (b.y - a.y)*(c.x - a.x)
        );
        return area;
    }

    public void copyFrom(Record record) {
        this.setA(record.a.x, record.a.y);
        this.setB(record.b.x, record.b.y);
        this.setC(record.c.x, record.c.y);
        area = getArea();
    }

    public  byte[] convertToBytes() {
        ByteBuffer bb = ByteBuffer.allocate(6*Double.BYTES);

        bb.putDouble(a.x);
        bb.putDouble(a.y);
        bb.putDouble(b.x);
        bb.putDouble(b.y);
        bb.putDouble(c.x);
        bb.putDouble(c.y);

        return bb.array();
    }

    public int compareTo(Record other) {
        if(this.getArea() > other.getArea())
            return 1;
        else if(this.getArea() == other.getArea())
            return 0;
        else
            return -1;
    }
}
