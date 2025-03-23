package com.smirnoal.lambda;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

public class SandboxTest {

    static class Person {
        String name;
        int age;
    }

    record City(String name, int yearFounded) {
    }

    @Test
    void testGson() {
        Gson gson = new Gson();
        Person p = new Person();
        p.name = "John";
        p.age = 22;
        System.out.println(gson.toJson(p));

        String s = "{\"name\":\"John\",\"age\":22}";
        Person o = gson.fromJson(s, Person.class);
        System.out.println(o.name);
        System.out.println(o.age);
    }
}
