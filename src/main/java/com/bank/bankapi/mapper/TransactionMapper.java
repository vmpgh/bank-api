package com.bank.bankapi.mapper;

import com.bank.bankapi.dto.TransactionResponse;
import com.bank.bankapi.transaction.AccountTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "accountId", source = "account.id")
    TransactionResponse toResponse(AccountTransaction transaction);
}
