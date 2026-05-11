package ru.cloudwithout.transferservice.model.dto;

public record NotificationRequest(String service, String operation, String message) {
}