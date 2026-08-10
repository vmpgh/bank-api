package com.bank.bankapi.service;

import com.bank.bankapi.dto.TransferRequest;
import com.bank.bankapi.dto.TransferResponse;

public interface TransferService {

    TransferResponse transfer(TransferRequest request);


}
