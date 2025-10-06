package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.inbound.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.use_case.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.outbound.BinRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.reactive.TransactionalOperator;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.outbound.SubtypeReadOnlyRepository;


@Configuration
public class BinUseCaseConfig {

    @Bean
    GetBinUseCase getBinUseCase(BinRepository repo) { return new GetBinService(repo); }
    @Bean
    ListBinsUseCase listBinsUseCase(BinRepository repo) { return new ListBinsService(repo); }
    @Bean public CreateBinUseCase createBinUseCase(BinRepository repo, TransactionalOperator tx) { return new CreateBinService(repo, tx); }
    @Bean public ChangeBinStatusUseCase changeBinStatusUseCase(BinRepository repo, TransactionalOperator tx) { return new ChangeBinStatusService(repo, tx); }
    @Bean public UpdateBinUseCase updateBinUseCase(BinRepository repo, SubtypeReadOnlyRepository subtypeRepo, TransactionalOperator tx) { return new UpdateBinService(repo, subtypeRepo, tx); }

}
