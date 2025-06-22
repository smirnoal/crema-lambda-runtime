package com.smirnoal.lambda.handlers;

import com.google.gson.Gson;
import com.smirnoal.lambda.LambdaApplication;
import com.smirnoal.lambda.LambdaHandler;

public class PojoHandler2 {

    static class Person {
        String name;
        int age;
    }

    record City(String name, int yearFounded) {
    }

    public City handle(Person person) {
        System.out.printf("Person %s, age %d%n", person.name, person.age);
        return new City("Dublin", 841);
    }

    public static void main(String[] args) {
        PojoHandler2 myHandler = new PojoHandler2();
        Gson gson = new Gson();

        LambdaHandler<Person, City> handler2 = new LambdaHandler<Person, City>()
                .withInputTypeDeserializer(bytes -> gson.fromJson(new String(bytes), Person.class))
                .withOutputTypeSerializer(city -> gson.toJson(city).getBytes())
                .withHandler(myHandler::handle);

        LambdaApplication app = new LambdaApplication();
        app.run(handler2);
    }
}
