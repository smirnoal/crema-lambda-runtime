package com.smirnoal.lambda.handlers;

import com.google.gson.Gson;
import com.smirnoal.lambda.LambdaApplication;
import com.smirnoal.lambda.LambdaHandler;
import com.smirnoal.lambda.serde.LambdaSerde;

import java.util.function.Function;

public class PojoHandler {

    static class Person {
        String name;
        int age;
    }

    record City(String name, int yearFounded) {
    }

    static class MyLambdaSerde implements LambdaSerde<Person, City> {
        Gson gson = new Gson();

        @Override
        public Function<byte[], Person> inputDeserializer() {
            return bytes -> gson.fromJson(new String(bytes), Person.class);
        }

        @Override
        public Function<City, byte[]> outputSerializer() {
            return city -> gson.toJson(city).getBytes();
        }
    }

    public City handle(Person person) {
        System.out.println("Person %s, age %d".formatted(person.name, person.age));
        return new City("Dublin", 841);
    }

    public static void main(String[] args) {
        PojoHandler myHandler = new PojoHandler();

        LambdaHandler<Person, City> handler = new LambdaHandler<Person, City>()
                .withLambdaSerde(new MyLambdaSerde())
                .withHandler(myHandler::handle);

        LambdaApplication app = new LambdaApplication();
        app.run(handler);
    }
}
