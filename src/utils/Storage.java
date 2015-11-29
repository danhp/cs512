package utils;

import java.io.*;

public class Storage {
    public static synchronized void set(Object object, String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            System.out.println("File not found, creating: " + path);
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
        ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(file, false));
        output.writeObject(object);
        output.close();
    }

    public static synchronized Object get(String path) throws IOException, ClassNotFoundException {
        ObjectInputStream input = new ObjectInputStream(new FileInputStream(path));
        Object file = input.readObject();
        input.close();
        return file;
    }
}
