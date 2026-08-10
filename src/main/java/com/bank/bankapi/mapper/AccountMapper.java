package com.bank.bankapi.mapper;

import com.bank.bankapi.dto.AccountResponse;
import com.bank.bankapi.entity.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    @Mapping(target = "userId", source = "owner.id")
    @Mapping(target = "username", source = "owner.username")
    AccountResponse toResponse(Account account);
}
