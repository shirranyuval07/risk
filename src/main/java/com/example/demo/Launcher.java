package com.example.demo;
import javafx.application.Application;
public class Launcher
{
    public static void main(String[] args)
    {
        // אנחנו "מרמים" את ה-JVM וקוראים למחלקה הראשית מכאן
        Application.launch(RiskApplication.class, args);
    }
}