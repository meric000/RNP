package Praktikum01.Testweb;

import java.util.ArrayList;
import java.util.List;

public class testcvlass {



    ArrayList<Integer> s = new ArrayList<>(3);

    public void mehtod(){
        s.add(1);
        s.add(1);
        s.add(1);

        System.out.println(s.size());
    }

    public static void main(String[] args) {
        testcvlass t = new testcvlass();
        t.mehtod();
    }

}
