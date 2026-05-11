package ru.cloudwithout.cashservice.model.dto;

public record NotificationRequest(String service, String operation, String message) {
}