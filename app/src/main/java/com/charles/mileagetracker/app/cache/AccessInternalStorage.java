package com.charles.mileagetracker.app.cache;

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by charles on 6/10/14.
 */
public class AccessInternalStorage {

    public AccessInternalStorage() {}

    private static Lock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();

    public synchronized static void writeObject(Context context, String key, Object object) throws IOException {
        lock.lock();//Some safety to keep from trying to write two objects at once
        FileOutputStream fos = context.openFileOutput(key, Context.MODE_PRIVATE);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(object);
        oos.close();
        fos.close();
        lock.unlock();
    }

    public static Object readObject(Context context, String key) throws IOException,
            ClassNotFoundException {
        FileInputStream fis = context.openFileInput(key);
        ObjectInputStream ois = new ObjectInputStream(fis);
        Object object = ois.readObject();
        return object;
    }
}
