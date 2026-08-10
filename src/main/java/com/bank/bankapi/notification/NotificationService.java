package com.bank.bankapi.notification;


public interface NotificationService {

    void sendTransferCompletedNotification(TransferNotification notification);

}
