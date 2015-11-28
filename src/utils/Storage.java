package utils;

import java.io.*;

public class Storage {
    public synchronized void set(Object object, String path) throws IOException {
        ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(path));
        output.writeObject(object);
        output.close();
    }

    public synchronized Object get(String path) throws IOException, ClassNotFoundException {
        ObjectInputStream input = new ObjectInputStream(new FileInputStream(path));
        Object file = input.readObject();
        input.close();
        return file;
    }
}
