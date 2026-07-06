package com.example.ywinked;

public class LawyerModel {
    String name;
    String specialization;
    String experience;
    String price;
    String city;
    String profileImageBase64;

    public LawyerModel(String name, String specialization, String experience, String price, String city, String profileImageBase64) {
        this.name = name;
        this.specialization = specialization;
        this.experience = experience;
        this.price = price;
        this.city = city;
        this.profileImageBase64 = profileImageBase64;
    }
}
