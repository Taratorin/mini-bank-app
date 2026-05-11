package ru.cloudwithout.accountsservice.model;

public record NotificationRequest(String service, String operation, String message) {
}