package ru.cloudwithout.commonmodels.common.dto;

public record NotificationRequest(String service, String operation, String message) {
}