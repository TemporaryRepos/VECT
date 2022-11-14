package test;

import com.alibaba.fastjson.JSONArray;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;

public class objectToBytes{
    private int age = 13;
    private temp ob = new temp();
    public static void main(String[] args) {
        objectToBytes o = new objectToBytes();
        Object obj = JSONArray.toJSON(o);
        System.out.println(o.hashCode());
        System.out.println(obj);
        System.out.println(obj.toString().hashCode());
    }

    public int getAge() {
        return age;
    }
    public int getAg() {
        return age+10;
    }
    public void setAge(int age) {
        this.age = age;
    }

    public temp getOb() {
        return ob;
    }

    public void setOb(temp ob) {
        this.ob = ob;
    }

    public static byte[] toByteArray(Object obj) {
        byte[] bytes = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            bytes = bos.toByteArray ();
            oos.close();
            bos.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return bytes;
    }
}
class temp{
private int anInt = 1;

    public int getAnInt() {
        return anInt;
    }

    public void setAnInt(int anInt) {
        this.anInt = anInt;
    }
}