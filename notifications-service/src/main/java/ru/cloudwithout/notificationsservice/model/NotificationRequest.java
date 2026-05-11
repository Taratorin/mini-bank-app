package ru.cloudwithout.notificationsservice.model;

public record NotificationRequest(String service, String operation, String message) {
}